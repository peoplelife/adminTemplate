from typing import Any

from pydantic import ValidationError

from app.page_generation.compiler import compile_preview
from app.page_generation.documentation import search_template_docs
from app.page_generation.schemas import PagePatch, PageSpec, ToolResult


_COMPONENTS = {
    "input": "Element Plus input",
    "number": "Element Plus input number",
    "select": "项目 BaseSelect",
    "radio": "项目 Radio",
}

def _validation_errors(error: ValidationError) -> list[dict[str, Any]]:
    return [{"loc": item.get("loc", ()), "message": item.get("msg", "参数错误"), "type": item.get("type", "value_error")} for item in error.errors()]


def read_template_components() -> ToolResult:
    return ToolResult(ok=True, tool_name="read_template_components", data={"components": _COMPONENTS})


def create_page_spec(arguments: dict[str, Any]) -> ToolResult:
    try:
        spec = PageSpec.model_validate(arguments)
    except ValidationError as error:
        return ToolResult(ok=False, tool_name="create_page_spec", message="页面结构校验失败", data={"errors": _validation_errors(error)})
    preview = compile_preview(spec)
    return ToolResult(ok=True, tool_name="create_page_spec", page_spec=spec, preview=preview, data={"page_spec": spec.model_dump(mode="json"), "preview": preview.model_dump(mode="json")})


def apply_page_patch(current: PageSpec, arguments: dict[str, Any]) -> ToolResult:
    try:
        patch = PagePatch.model_validate(arguments).model_dump(mode="json", exclude_unset=True)
        add_fields = patch.pop("add_fields", [])
        update_fields = patch.pop("update_fields", [])
        remove_keys = patch.pop("remove_field_keys", [])
        fields = [field.model_dump(mode="json") for field in current.fields]
        field_keys = {field["key"] for field in fields}
        unknown_remove = set(remove_keys) - field_keys
        if unknown_remove:
            return ToolResult(ok=False, tool_name="patch_page_spec", message="删除的字段不存在", data={"fields": sorted(unknown_remove)})
        fields = [field for field in fields if field["key"] not in set(remove_keys)]
        index = {field["key"]: field for field in fields}
        for update in update_fields:
            target = index.get(update["key"])
            if target is None:
                return ToolResult(ok=False, tool_name="patch_page_spec", message="修改的字段不存在", data={"field": update.get("key")})
            for key, value in update.items():
                if key in {"column", "attrs"}:
                    target[key].update(value)
                elif key != "key":
                    target[key] = value
        fields.extend(add_fields)
        payload = current.model_dump(mode="json")
        for key, value in patch.items():
            if key in {"table", "pagination", "form"}:
                payload[key].update(value)
            else:
                payload[key] = value
        payload["fields"] = fields
        if "sample_data" not in patch and payload["sample_data"] is not None:
            remaining = {field["key"] for field in fields}
            payload["sample_data"] = [{key: value for key, value in row.items() if key in remaining} for row in payload["sample_data"]]
        spec = PageSpec.model_validate(payload)
    except ValidationError as error:
        return ToolResult(ok=False, tool_name="patch_page_spec", message="页面修改校验失败", data={"errors": _validation_errors(error)})
    preview = compile_preview(spec)
    return ToolResult(ok=True, tool_name="patch_page_spec", page_spec=spec, preview=preview, data={"page_spec": spec.model_dump(mode="json"), "preview": preview.model_dump(mode="json")})


def validate_page_spec(arguments: dict[str, Any]) -> ToolResult:
    try:
        payload = arguments.get("page_spec", arguments) if isinstance(arguments, dict) else arguments
        spec = PageSpec.model_validate(payload)
    except ValidationError as error:
        return ToolResult(ok=False, tool_name="validate_page_spec", message="页面结构校验失败", data={"errors": _validation_errors(error)})
    return ToolResult(ok=True, tool_name="validate_page_spec", data={"page_spec": spec.model_dump(mode="json")})


def ask_clarification(arguments: dict[str, Any]) -> ToolResult:
    question = arguments.get("question") if isinstance(arguments, dict) else None
    if not isinstance(question, str) or not question.strip():
        return ToolResult(ok=False, tool_name="ask_clarification", message="澄清问题不能为空")
    return ToolResult(ok=True, tool_name="ask_clarification", message=question.strip(), data={"question": question.strip()})


def tool_definitions() -> list[dict[str, Any]]:
    def definition(name, description, parameters):
        return {"type": "function", "function": {"name": name, "description": description, "parameters": parameters}}

    page_schema = PageSpec.model_json_schema(by_alias=False)
    validation_schema = {
        "type": "object", "additionalProperties": False, "required": ["page_spec"],
        "properties": {"page_spec": {key: value for key, value in page_schema.items() if key != "$defs"}},
        "$defs": page_schema.get("$defs", {}),
    }
    return [
        definition("ask_clarification", "向用户询问生成页面所需的关键信息", {"type": "object", "required": ["question"], "properties": {"question": {"type": "string"}}, "additionalProperties": False}),
        definition("read_template_components", "读取允许使用的模板组件", {"type": "object", "properties": {}, "additionalProperties": False}),
        definition("search_template_docs", "根据用户要求查询真实 BaseTable/Form 用法，返回来源、标题和原文；生成或修改页面前先查阅", {"type": "object", "required": ["query"], "properties": {"query": {"type": "string", "minLength": 1, "maxLength": 500}}, "additionalProperties": False}),
        definition("create_page_spec", "根据需求和检索到的组件用法创建页面及示例数据", page_schema),
        definition("patch_page_spec", "局部修改当前页面；嵌套配置合并，sample_data 替换整份示例数据，null 恢复自动示例", PagePatch.model_json_schema(by_alias=False)),
        definition("validate_page_spec", "校验页面结构", validation_schema),
    ]


def execute_tool(name: str, arguments: dict[str, Any], current: PageSpec | None) -> ToolResult:
    if name == "search_template_docs":
        query = arguments.get("query") if isinstance(arguments, dict) else None
        if not isinstance(query, str) or not query.strip() or len(query) > 500 or set(arguments) != {"query"}:
            return ToolResult(ok=False, tool_name=name, message="请输入 1 到 500 字的组件用法查询")
        try:
            items = search_template_docs(query.strip())
        except OSError:
            return ToolResult(ok=False, tool_name=name, message="模板文档不可读取，请检查镜像的 template-docs 构建上下文")
        return ToolResult(ok=True, tool_name=name, data={"items": items})
    if name == "read_template_components":
        return read_template_components()
    if name == "ask_clarification":
        return ask_clarification(arguments)
    if name == "create_page_spec":
        return create_page_spec(arguments)
    if name == "validate_page_spec":
        return validate_page_spec(arguments)
    if name == "patch_page_spec":
        if current is None:
            return ToolResult(ok=False, tool_name=name, message="当前没有可修改的页面")
        return apply_page_patch(current, arguments)
    return ToolResult(ok=False, tool_name=name, message="未知工具")
