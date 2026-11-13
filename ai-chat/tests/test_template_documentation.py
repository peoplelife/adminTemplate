import unittest
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import patch

from app.llm.schemas import LLMProviderName, LLMResponse, LLMRole, LLMToolCall
from app.page_generation.agent import PageAgent
from app.page_generation.compiler import compile_preview
from app.page_generation.documentation import search_template_docs
from app.page_generation.tools import apply_page_patch, execute_tool
from app.page_generation.schemas import PageSpec
from tests.test_page_generation import FakeGenerator


class TemplateDocumentationTests(unittest.TestCase):
    def test_search_reads_real_document_sections_for_chinese_border_request(self):
        result = execute_tool("search_template_docs", {"query": "表格不要边框 border"}, None)
        self.assertTrue(result.ok)
        sections = result.data["items"]
        self.assertTrue(sections)
        self.assertIn("border", "\n".join(item["content"] for item in sections))
        self.assertTrue(all(item["source"].startswith("frontend/docs/") for item in sections))

    def test_search_finds_pagination_and_form_usage_and_does_not_invent_properties(self):
        for query, token in [("每页显示20条分页", "pageSize"), ("表单标签位置", "labelPosition")]:
            result = execute_tool("search_template_docs", {"query": query}, None)
            self.assertTrue(result.ok)
            self.assertIn(token, "\n".join(item["content"] for item in result.data["items"]))
        result = execute_tool("search_template_docs", {"query": ""}, None)
        self.assertFalse(result.ok)

    def test_patch_preserves_nested_settings_and_custom_sample_data(self):
        spec = PageSpec.model_validate({
            "page_key": "people", "title": "员工", "fields": [
                {"key": "name", "label": "姓名", "kind": "input"},
                {"key": "age", "label": "年龄", "kind": "number"},
            ],
            "table": {"border": True, "stripe": True},
            "sample_data": [{"name": "张三", "age": 28}],
        })
        result = apply_page_patch(spec, {"table": {"border": False}, "pagination": {"page_size": 20}})
        self.assertTrue(result.ok)
        self.assertFalse(result.page_spec.table.border)
        self.assertTrue(result.page_spec.table.stripe)
        self.assertEqual(result.preview.rows[0]["name"], "张三")
        self.assertEqual(result.page_spec.pagination.page_size, 20)
        self.assertTrue(spec.table.border)

    def test_configuration_and_samples_are_validated_without_executing_code(self):
        base = {"page_key": "people", "title": "员工", "fields": [
            {"key": "name", "label": "姓名", "kind": "input"},
            {"key": "age", "label": "年龄", "kind": "number"},
        ]}
        for extra in [
            {"table": {"onClick": "evil()"}},
            {"sample_data": [{"name": "张三", "age": "not-a-number"}]},
            {"sample_data": [{"unknown": "value"}]},
            {"pagination": {"page_size": 0}},
        ]:
            with self.subTest(extra=extra), self.assertRaises(ValueError):
                PageSpec.model_validate({**base, **extra})

    def test_sample_replacement_empty_reset_and_structural_changes(self):
        spec = PageSpec.model_validate({"page_key": "people", "title": "员工", "fields": [
            {"key": "name", "label": "姓名", "kind": "input"},
            {"key": "age", "label": "年龄", "kind": "number"},
        ], "sample_data": [{"name": "张三", "age": 28}]})
        replaced = apply_page_patch(spec, {"sample_data": [{"name": "李四", "age": 32}]})
        self.assertEqual(replaced.preview.rows, [{"id": "1", "name": "李四", "age": 32}])
        self.assertEqual(apply_page_patch(spec, {"sample_data": []}).preview.rows, [])
        self.assertEqual(len(apply_page_patch(spec, {"sample_data": None}).preview.rows), 3)
        changed = apply_page_patch(spec, {"remove_field_keys": ["age"], "add_fields": [
            {"key": "phone", "label": "电话", "kind": "input", "default_value": "待补充"},
        ]})
        self.assertTrue(changed.ok)
        self.assertEqual(changed.preview.rows, [{"id": "1", "name": "张三", "phone": "待补充"}])
        invalid = apply_page_patch(spec, {"update_fields": [{"key": "name", "kind": "number"}]})
        self.assertFalse(invalid.ok)
        self.assertEqual(spec.sample_data, [{"name": "张三", "age": 28}])

    def test_preview_propagates_camel_case_field_config_and_partial_update(self):
        spec = PageSpec.model_validate({"page_key": "people", "title": "员工", "fields": [
            {"key": "name", "label": "姓名", "kind": "input", "searchable": True,
             "column": {"align": "center", "minWidth": 180}, "attrs": {"placeholder": "姓名", "maxlength": 40}, "defaultValue": "张三"},
        ]})
        result = apply_page_patch(spec, {"update_fields": [{"key": "name", "column": {"sortable": True}, "attrs": {"disabled": True}}]})
        self.assertTrue(result.ok)
        for fields in (result.preview.columns, result.preview.filters, result.preview.form):
            self.assertEqual(fields[0]["column"]["minWidth"], 180)
            self.assertEqual(fields[0]["column"]["align"], "center")
            self.assertTrue(fields[0]["column"]["sortable"])
            self.assertEqual(fields[0]["attrs"]["maxlength"], 40)
            self.assertTrue(fields[0]["attrs"]["disabled"])
            self.assertEqual(fields[0]["defaultValue"], "张三")

    def test_document_search_respects_code_fences_and_missing_bundle(self):
        with TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "base-table-usage.md").write_text("# 示例\n```\n# border inside code\n```\n说明\n", encoding="utf-8")
            (root / "form-usage.md").write_text("# 表单\n默认值", encoding="utf-8")
            with patch("app.page_generation.documentation._document_directory", return_value=root):
                result = search_template_docs("border")
                self.assertEqual(result[0]["title"], "示例")
                self.assertIn("说明", result[0]["content"])
                self.assertEqual(search_template_docs("nonexistent-property"), [])
                (root / "form-usage.md").unlink()
                self.assertFalse(execute_tool("search_template_docs", {"query": "border"}, None).ok)


class TemplateDocumentationAgentTests(unittest.IsolatedAsyncioTestCase):
    async def test_graph_retrieves_documentation_then_patches_border_and_sample_data(self):
        current = PageSpec.model_validate({"page_key": "people", "title": "员工", "fields": [
            {"key": "name", "label": "姓名", "kind": "input"},
        ]})
        def call(name, arguments):
            return LLMResponse(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="", tool_calls=[
                LLMToolCall(id=name, name=name, arguments=arguments),
            ])
        generator = FakeGenerator([
            call("search_template_docs", {"query": "表格边框 border 示例数据 sampleData"}),
            call("patch_page_spec", {"table": {"border": False}, "sample_data": [{"name": "张三"}]}),
            LLMResponse(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="已取消边框并修改示例数据。"),
        ])
        events = [event async for event in PageAgent(generator=generator).stream("取消边框，示例姓名改成张三", current)]
        result = events[-1]
        self.assertTrue(result.page_changed)
        self.assertFalse(result.page_spec.table.border)
        self.assertEqual(result.preview.rows[0]["name"], "张三")
        self.assertTrue(any(getattr(event, "message", "") == "正在查询组件用法" for event in events))
        observation = next(message for message in generator.calls[1] if message.role == LLMRole.TOOL)
        sources = json.loads(observation.content)["data"]["items"]
        self.assertTrue(any("border" in item["content"] for item in sources))
        self.assertIn("先调用 search_template_docs", generator.calls[0][0].content)


if __name__ == "__main__":
    unittest.main()
