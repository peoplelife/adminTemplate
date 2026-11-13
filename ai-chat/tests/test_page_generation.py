import unittest
import json
from unittest.mock import patch

import httpx

from app.core.exceptions import AppException
from app.llm.schemas import LLMMessage, LLMProviderName, LLMResponse, LLMRole, LLMStreamChunk, LLMToolCall, LLMToolCallDelta
from app.page_generation.compiler import compile_preview
from app.page_generation.schemas import PageSpec
from app.page_generation.tools import apply_page_patch, execute_tool, tool_definitions
from app.page_generation.agent import PageAgent
from app.answer_generation.service import AnswerGenerator
from app.llm.providers.openai_compatible import OpenAICompatibleProvider


class PageGenerationSchemasTests(unittest.TestCase):
    def test_page_spec_rejects_duplicate_and_reserved_fields(self):
        with self.assertRaises(ValueError):
            PageSpec.model_validate({
                "page_key": "customer",
                "title": "客户",
                "fields": [
                    {"key": "name", "label": "名称", "kind": "input"},
                    {"key": "name", "label": "重复", "kind": "input"},
                ],
            })
        with self.assertRaises(ValueError):
            PageSpec.model_validate({
                "page_key": "customer",
                "title": "客户",
                "fields": [{"key": "id", "label": "ID", "kind": "input"}],
            })

    def test_compile_preview_builds_filters_forms_and_synthetic_rows(self):
        spec = PageSpec.model_validate({
            "page_key": "customer",
            "title": "客户管理",
            "fields": [
                {"key": "name", "label": "客户名称", "kind": "input", "required": True, "searchable": True},
                {"key": "level", "label": "等级", "kind": "select", "options": [
                    {"label": "普通", "value": "normal"},
                    {"label": "VIP", "value": "vip"},
                ], "searchable": True},
                {"key": "enabled", "label": "启用", "kind": "radio", "options": [{"label": "是", "value": "true"}, {"label": "否", "value": "false"}]},
            ],
            "actions": ["create", "edit", "detail"],
        })
        preview = compile_preview(spec)
        self.assertEqual([item["key"] for item in preview.columns], ["name", "level", "enabled"])
        self.assertEqual([item["key"] for item in preview.filters], ["name", "level"])
        self.assertEqual(preview.form[0]["required"], True)
        self.assertEqual(len(preview.rows), 3)
        self.assertEqual(preview.rows[0]["level"], "normal")

    def test_patch_changes_only_requested_fields(self):
        spec = PageSpec.model_validate({
            "page_key": "customer", "title": "客户", "fields": [
                {"key": "name", "label": "名称", "kind": "input"},
                {"key": "phone", "label": "手机号", "kind": "input"},
            ],
        })
        result = apply_page_patch(spec, {
            "add_fields": [{"key": "level", "label": "等级", "kind": "select", "options": [{"label": "VIP", "value": "vip"}]}],
            "remove_field_keys": ["phone"],
            "actions": ["create", "detail"],
        })
        self.assertTrue(result.ok)
        self.assertEqual([field.key for field in result.page_spec.fields], ["name", "level"])
        self.assertEqual(result.page_spec.actions, ["create", "detail"])

    def test_tool_definitions_are_restricted(self):
        names = {item["function"]["name"] for item in tool_definitions()}
        self.assertEqual(names, {"create_page_spec", "patch_page_spec", "validate_page_spec", "read_template_components", "search_template_docs", "ask_clarification"})

    def test_patch_updates_by_key_and_preserves_untouched_fields(self):
        spec = PageSpec.model_validate({
            "page_key": "customer", "title": "客户", "fields": [
                {"key": "name", "label": "名称", "kind": "input", "searchable": True},
                {"key": "phone", "label": "手机号", "kind": "input"},
            ],
        })
        result = apply_page_patch(spec, {"update_fields": [{"key": "phone", "required": True}]})
        self.assertTrue(result.ok)
        self.assertTrue(result.page_spec.fields[1].required)
        self.assertTrue(result.page_spec.fields[0].searchable)
        self.assertFalse(spec.fields[1].required)

    def test_failed_patch_is_atomic_and_validation_errors_are_serializable(self):
        spec = PageSpec.model_validate({"page_key": "customer", "title": "客户", "fields": [
            {"key": "name", "label": "名称", "kind": "input"},
        ]})
        original = spec.model_dump()
        result = apply_page_patch(spec, {"add_fields": [{"key": "level", "label": "等级", "kind": "select"}]})
        self.assertFalse(result.ok)
        json.loads(result.model_dump_json())
        self.assertEqual(spec.model_dump(), original)
        self.assertIsNone(result.page_spec)

    def test_patch_rejects_unknown_keys_and_invalid_field_targets(self):
        spec = PageSpec.model_validate({"page_key": "customer", "title": "客户", "fields": [
            {"key": "name", "label": "名称", "kind": "input"},
        ]})
        for arguments in ({"shell": "danger"}, {"update_fields": [{"key": "absent", "required": True}]},
                          {"remove_field_keys": ["absent"]}, {"add_fields": None},
                          {"remove_field_keys": [{}]}, {"update_fields": [{"key": []}]}):
            with self.subTest(arguments=arguments):
                self.assertFalse(apply_page_patch(spec, arguments).ok)

    def test_validation_tool_does_not_replace_the_current_page(self):
        candidate = {"page_key": "customer", "title": "客户", "fields": [
            {"key": "name", "label": "名称", "kind": "input"},
        ]}
        result = execute_tool("validate_page_spec", {"page_spec": candidate}, None)
        self.assertTrue(result.ok)
        self.assertIsNone(result.page_spec)

    def test_boolean_and_enum_display_data_is_available_to_preview(self):
        spec = PageSpec.model_validate({"page_key": "customer", "title": "客户", "fields": [
            {"key": "enabled", "label": "启用", "kind": "radio", "options": [{"label": "是", "value": "true"}, {"label": "否", "value": "false"}], "searchable": True},
            {"key": "level", "label": "等级", "kind": "select", "options": [{"label": "VIP", "value": "vip"}]},
        ], "form_columns": 2})
        preview = compile_preview(spec)
        self.assertEqual(preview.form_columns, 2)
        self.assertEqual(preview.form[0]["type"], "radio")
        self.assertEqual(preview.form[0]["options"], [{"label": "是", "value": "true", "color": None}, {"label": "否", "value": "false", "color": None}])
        self.assertEqual(preview.form[1]["type"], "select")
        self.assertEqual(preview.columns[1]["options"], [{"label": "VIP", "value": "vip", "color": None}])
        self.assertEqual(preview.rows[0]["enabled"], "true")

    def test_rejects_executable_properties_and_unsafe_field_names(self):
        base = {"page_key": "customer", "title": "客户", "fields": [{"key": "name", "label": "名称", "kind": "input"}]}
        for field in ({"key": "constructor", "label": "名称", "kind": "input"},
                      {"key": "toString", "label": "名称", "kind": "input"},
                      {"key": "name", "label": "名称", "kind": "input", "onClick": "evil()"}):
            with self.subTest(field=field), self.assertRaises(ValueError):
                PageSpec.model_validate({**base, "fields": [field]})


class FakeGenerator:
    def __init__(self, responses=None):
        self.calls = []
        self.responses = responses if responses is not None else [
            LLMResponse(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="", tool_calls=[
                LLMToolCall(id="call-1", name="create_page_spec", arguments={
                    "page_key": "customer", "title": "客户管理", "fields": [
                        {"key": "name", "label": "客户名称", "kind": "input", "searchable": True}
                    ], "actions": ["create", "detail"],
                })
            ]),
            LLMResponse(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="已生成客户管理页面。"),
        ]

    async def stream(self, messages, *, model=None, max_tokens=None, **kwargs):
        self.calls.append(messages)
        response = self.responses.pop(0)
        if response.reasoning_content:
            yield LLMStreamChunk(provider=response.provider, model=response.model, reasoning_content=response.reasoning_content)
        if response.content:
            yield LLMStreamChunk(provider=response.provider, model=response.model, content=response.content)
        for index, call in enumerate(response.tool_calls):
            yield LLMStreamChunk(provider=response.provider, model=response.model, tool_calls=[
                LLMToolCallDelta(index=index, id=call.id, name=call.name, arguments=call.raw_arguments if call.raw_arguments != "{}" else json.dumps(call.arguments)),
            ])
        yield LLMStreamChunk(provider=response.provider, model=response.model, finish_reason="tool_calls" if response.tool_calls else "stop")


class PageAgentTests(unittest.IsolatedAsyncioTestCase):
    async def test_clarification_can_pause_an_existing_page(self):
        current = PageSpec.model_validate({"page_key": "customer", "title": "客户", "fields": [
            {"key": "name", "label": "名称", "kind": "input"},
        ]})
        question = LLMResponse(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="", tool_calls=[
            LLMToolCall(id="question", name="ask_clarification", arguments={"question": "客户等级有哪些选项？"}),
        ])
        result = await PageAgent(generator=FakeGenerator([question])).run("增加客户等级", current)
        self.assertEqual(result.clarification, "客户等级有哪些选项？")
        self.assertEqual(result.page_spec, current)
        self.assertFalse(result.page_changed)

    async def test_new_run_does_not_share_the_previous_page(self):
        generator = FakeGenerator()
        agent = PageAgent(generator=generator)
        first = await agent.run("生成客户页面")
        generator.responses = [LLMResponse(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="请描述页面需求。")]
        second = await agent.run("你好")
        self.assertIsNotNone(first.page_spec)
        self.assertIsNone(second.page_spec)
        self.assertIsNone(second.clarification)

    async def test_agent_executes_tool_and_returns_preview(self):
        generator = FakeGenerator()
        result = await PageAgent(generator=generator).run("生成客户管理页面")
        self.assertEqual(result.assistant_message, "已生成客户管理页面。")
        self.assertEqual(result.page_spec.page_key, "customer")
        self.assertEqual(result.preview.rows[0]["name"], "客户名称 1")
        self.assertGreaterEqual(len(generator.calls), 2)

    async def test_replays_actual_tool_arguments_and_tool_observation(self):
        generator = FakeGenerator()
        await PageAgent(generator=generator).run("生成客户管理页面")
        second_turn = generator.calls[1]
        call_message = next(message for message in second_turn if message.tool_calls)
        self.assertEqual(json.loads(call_message.tool_calls[0]["function"]["arguments"])["page_key"], "customer")
        observation = next(message for message in second_turn if message.role == LLMRole.TOOL)
        self.assertEqual(observation.tool_call_id, "call-1")
        self.assertTrue(json.loads(observation.content)["ok"])

    async def test_continuation_includes_previous_clarification(self):
        generator = FakeGenerator()
        history = [LLMMessage(role=LLMRole.USER, content="生成客户页面"),
                   LLMMessage(role=LLMRole.ASSISTANT, content="客户有哪些字段？")]
        await PageAgent(generator=generator).run("客户名称", history=history)
        contents = [message.content for message in generator.calls[0]]
        self.assertIn("客户有哪些字段？", contents)
        self.assertTrue(contents[-1].endswith("客户名称"))

    async def test_validation_failure_is_returned_to_model_for_one_repair(self):
        invalid_call = LLMResponse(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="", tool_calls=[
            LLMToolCall(id="invalid", name="create_page_spec", arguments={
                "page_key": "customer", "title": "客户", "fields": [{"key": "level", "label": "等级", "kind": "select"}],
            }),
        ])
        valid = FakeGenerator().responses
        generator = FakeGenerator([invalid_call, *valid])
        result = await PageAgent(generator=generator).run("生成客户页面")
        self.assertEqual(result.page_spec.page_key, "customer")
        failed_observation = generator.calls[1][-1]
        self.assertFalse(json.loads(failed_observation.content)["ok"])

    async def test_malformed_tool_json_can_be_repaired(self):
        invalid = LLMResponse(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="", tool_calls=[
            LLMToolCall(id="invalid", name="create_page_spec", raw_arguments='{"page_key":'),
        ])
        generator = FakeGenerator([invalid, *FakeGenerator().responses])
        result = await PageAgent(generator=generator).run("生成客户页面")
        self.assertEqual(result.page_spec.page_key, "customer")
        observation = generator.calls[1][-1]
        self.assertFalse(json.loads(observation.content)["ok"])

    async def test_two_invalid_mutations_fail_without_publishing_a_page(self):
        invalid = LLMResponse(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="", tool_calls=[
            LLMToolCall(id="invalid", name="create_page_spec", arguments={"title": "客户"}),
        ])
        with self.assertRaises(AppException) as raised:
            await PageAgent(generator=FakeGenerator([invalid, invalid])).run("生成客户页面")
        self.assertIn("修复", raised.exception.message)

    async def test_successful_tool_resets_consecutive_repair_failures(self):
        invalid_create = LLMResponse(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="", tool_calls=[
            LLMToolCall(id="invalid-create", name="create_page_spec", arguments={"title": "客户"}),
        ])
        valid_create = LLMResponse(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="", tool_calls=[
            LLMToolCall(id="valid-create", name="create_page_spec", arguments={
                "page_key": "customer", "title": "客户", "fields": [{"key": "name", "label": "名称", "kind": "input"}],
            }),
        ])
        invalid_patch = LLMResponse(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="", tool_calls=[
            LLMToolCall(id="invalid-patch", name="patch_page_spec", arguments={"remove_field_keys": ["absent"]}),
        ])
        final = LLMResponse(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="已生成客户页面。")
        result = await PageAgent(generator=FakeGenerator([invalid_create, valid_create, invalid_patch, final])).run("生成客户页面")
        self.assertEqual(result.page_spec.page_key, "customer")

    async def test_step_budget_and_empty_model_reply_fail_explicitly(self):
        repeat = LLMResponse(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="", tool_calls=[
            LLMToolCall(id="read", name="read_template_components", arguments={}),
        ])
        with self.assertRaises(AppException) as raised:
            await PageAgent(generator=FakeGenerator([repeat, repeat]), max_steps=2).run("生成客户页面")
        self.assertIn("步骤", raised.exception.message)
        with self.assertRaises(AppException):
            await PageAgent(generator=FakeGenerator([
                LLMResponse(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="")
            ])).run("生成客户页面")

    async def test_real_provider_streams_fragmented_tool_calls_and_replays_tool_protocol(self):
        payloads = []
        arguments = {"page_key": "customer", "title": "客户", "fields": [
            {"key": "name", "label": "名称", "kind": "input"},
        ]}

        def respond(request):
            payload = json.loads(request.content)
            payloads.append(payload)
            self.assertTrue(payload["stream"])
            self.assertEqual(payload["thinking"], {"type": "enabled"})
            for tool in payload["tools"]:
                schema = tool["function"]["parameters"]
                self.assertFalse(schema["additionalProperties"])
                for value in schema.get("properties", {}).values():
                    if value.get("type") == "array":
                        self.assertIn("items", value)
            if len(payloads) == 1:
                raw_arguments = json.dumps(arguments)
                deltas = [
                    {"reasoning_content": "查阅"},
                    {"reasoning_content": "组件。"},
                    {"content": None, "tool_calls": [{"index": 1, "id": "read-call", "type": "function", "function": {"name": "read_template_components", "arguments": "{}"}}]},
                    {"tool_calls": [{"index": 0, "id": "http-call", "type": "function", "function": {"name": "create_", "arguments": raw_arguments[:30]}}]},
                    {"tool_calls": [{"index": 0, "function": {"name": "page_spec", "arguments": raw_arguments[30:]}}]},
                ]
            else:
                self.assertEqual(payload["messages"][-1]["role"], "tool")
                observations = [message for message in payload["messages"] if message["role"] == "tool"]
                self.assertEqual([message["tool_call_id"] for message in observations], ["http-call", "read-call"])
                call_message = next(message for message in payload["messages"] if message.get("tool_calls"))
                self.assertEqual(json.loads(call_message["tool_calls"][0]["function"]["arguments"]), arguments)
                self.assertEqual(call_message["reasoning_content"], "查阅组件。")
                deltas = [{"reasoning_content": "确认页面。"}, {"content": "页面已"}, {"content": "生成。"}]
            chunks = [{"model": "deepseek-chat", "choices": [{"delta": delta, "finish_reason": None}]} for delta in deltas]
            chunks.append({"model": "deepseek-chat", "choices": [{"delta": {}, "finish_reason": "tool_calls" if len(payloads) == 1 else "stop"}]})
            chunks.append({"model": "deepseek-chat", "choices": [], "usage": {"total_tokens": 10}})
            body = "".join(f"data: {json.dumps(chunk)}\n\n" for chunk in chunks) + "data: [DONE]\n\n"
            return httpx.Response(200, text=body, headers={"content-type": "text/event-stream"})

        provider = OpenAICompatibleProvider(api_key="test", base_url="https://model.example", default_model="deepseek-chat")
        provider.provider_name = LLMProviderName.DEEPSEEK
        with patch("app.llm.providers.openai_compatible.create_llm_http_client", side_effect=lambda _: httpx.AsyncClient(transport=httpx.MockTransport(respond))):
            result = await PageAgent(generator=AnswerGenerator(provider)).run("生成客户页面")
        self.assertEqual(result.page_spec.page_key, "customer")
        self.assertEqual(result.assistant_message, "页面已生成。")
        self.assertEqual(result.reasoning_content, "查阅组件。\n\n确认页面。")


if __name__ == "__main__":
    unittest.main()
