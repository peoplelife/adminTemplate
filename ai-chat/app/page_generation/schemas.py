from copy import deepcopy
from typing import Any, Literal, TypedDict

from pydantic import BaseModel, ConfigDict, Field, StrictFloat, StrictInt, StrictStr, create_model, field_validator, model_validator

from app.schemas.base import to_camel


FieldKind = Literal["input", "number", "select", "radio"]
PageAction = Literal["create", "edit", "delete", "detail"]
ComponentSize = Literal["small", "default", "large"]
Scalar = StrictStr | StrictInt | StrictFloat


class StrictSchema(BaseModel):
    model_config = ConfigDict(
        extra="forbid",
        alias_generator=to_camel,
        populate_by_name=True,
        allow_inf_nan=False,
    )


class SelectOption(StrictSchema):
    label: str = Field(min_length=1, max_length=40)
    value: str = Field(min_length=1, max_length=80)
    color: str | None = Field(default=None, max_length=20)


class ColumnConfig(StrictSchema):
    width: int | None = Field(default=None, ge=40, le=1200)
    min_width: int = Field(default=130, ge=40, le=1200)
    align: Literal["left", "center", "right"] = "left"
    fixed: Literal["left", "right"] | None = None
    sortable: bool = False
    show_overflow_tooltip: bool = False


class ControlAttrs(StrictSchema):
    placeholder: str | None = Field(default=None, max_length=200)
    clearable: bool = True
    disabled: bool = False
    maxlength: int | None = Field(default=None, ge=1, le=10000)
    min: float | None = None
    max: float | None = None
    step: float | None = Field(default=None, gt=0)
    precision: int | None = Field(default=None, ge=0, le=10)

    @model_validator(mode="after")
    def validate_range(self):
        if self.min is not None and self.max is not None and self.min > self.max:
            raise ValueError("attrs.min 不能大于 attrs.max")
        return self


class TableConfig(StrictSchema):
    border: bool = True
    stripe: bool = False
    size: ComponentSize = "default"
    show_header: bool = True
    height: int | None = Field(default=None, ge=160, le=1600)


class PaginationConfig(StrictSchema):
    enabled: bool = True
    page_size: int = Field(default=2, ge=1, le=100)
    page_sizes: list[int] = Field(default_factory=lambda: [2, 5, 10, 20], min_length=1, max_length=10)
    background: bool = True

    @field_validator("page_sizes")
    @classmethod
    def validate_page_sizes(cls, value: list[int]) -> list[int]:
        if any(size < 1 or size > 100 for size in value) or len(value) != len(set(value)):
            raise ValueError("page_sizes 必须是 1 到 100 之间的不重复整数")
        return value


class FormConfig(StrictSchema):
    label_position: Literal["left", "right", "top"] = "top"
    label_width: int = Field(default=100, ge=40, le=400)
    size: ComponentSize = "default"
    disabled: bool = False


class PageField(StrictSchema):
    key: str = Field(min_length=1, max_length=50, pattern=r"^[a-zA-Z][a-zA-Z0-9_]*$")
    label: str = Field(min_length=1, max_length=80)
    kind: FieldKind
    required: bool = False
    searchable: bool = False
    options: list[SelectOption] = Field(default_factory=list, max_length=30)
    column: ColumnConfig = Field(default_factory=ColumnConfig)
    attrs: ControlAttrs = Field(default_factory=ControlAttrs)
    default_value: Scalar | None = None

    @field_validator("key")
    @classmethod
    def reject_reserved_key(cls, value: str) -> str:
        if value in {"id", "created_at", "updated_at", "constructor", "prototype", "__proto__", "then", "toString", "hasOwnProperty"}:
            raise ValueError(f"字段名保留：{value}")
        return value

    @model_validator(mode="after")
    def validate_options(self):
        if self.kind in {"select", "radio"} and not self.options:
            raise ValueError(f"字段 {self.key} 需要 options")
        if self.kind not in {"select", "radio"} and self.options:
            raise ValueError(f"字段 {self.key} 不支持 options")
        values = [option.value for option in self.options]
        if len(values) != len(set(values)):
            raise ValueError(f"字段 {self.key} 的选项值不能重复")
        if self.kind in {"select", "radio"} and self.default_value is not None and self.default_value not in values:
            raise ValueError(f"字段 {self.key} 的 default_value 不在 options 中")
        if self.kind == "number" and self.default_value is not None and (isinstance(self.default_value, bool) or not isinstance(self.default_value, (int, float))):
            raise ValueError(f"字段 {self.key} 的 default_value 必须是数字")
        if self.kind == "input" and self.default_value is not None and not isinstance(self.default_value, str):
            raise ValueError(f"字段 {self.key} 的 default_value 必须是文本")
        return self


class PageSpec(StrictSchema):
    page_key: str = Field(min_length=1, max_length=50, pattern=r"^[a-z][a-z0-9_]*$")
    title: str = Field(min_length=1, max_length=100)
    fields: list[PageField] = Field(min_length=1, max_length=30)
    actions: list[PageAction] = Field(default_factory=lambda: ["create", "edit", "detail"])
    form_columns: int = Field(default=2, ge=1, le=4)
    table: TableConfig = Field(default_factory=TableConfig)
    pagination: PaginationConfig = Field(default_factory=PaginationConfig)
    form: FormConfig = Field(default_factory=FormConfig)
    sample_data: list[dict[str, Scalar | None]] | None = Field(default=None, max_length=100)

    @model_validator(mode="after")
    def validate_fields_and_actions(self):
        keys = [field.key for field in self.fields]
        if len(keys) != len(set(keys)):
            raise ValueError("字段 key 不能重复")
        if len(self.actions) != len(set(self.actions)):
            raise ValueError("操作不能重复")
        field_map = {field.key: field for field in self.fields}
        if self.sample_data is not None:
            for row in self.sample_data:
                unknown = set(row) - field_map.keys()
                if unknown:
                    raise ValueError(f"示例数据包含未知字段：{', '.join(sorted(unknown))}")
                for key, value in row.items():
                    field = field_map[key]
                    if value is None:
                        continue
                    if field.kind == "number" and (isinstance(value, bool) or not isinstance(value, (int, float))):
                        raise ValueError(f"字段 {key} 的示例值必须是数字")
                    if field.kind == "input" and not isinstance(value, str):
                        raise ValueError(f"字段 {key} 的示例值必须是文本")
                    if field.kind in {"select", "radio"} and value not in {option.value for option in field.options}:
                        raise ValueError(f"字段 {key} 的示例值不在 options 中")
        return self


# 修改沿用字段定义，仅将 key 以外的字段变为可省略；最终合并后仍由 PageSpec 完整校验。
_update_fields = {}
for _name, _info in PageField.model_fields.items():
    _partial = deepcopy(_info)
    if _name != "key":
        _partial.default = None
        _partial.default_factory = None
    _update_fields[_name] = (_info.annotation, _partial)
PageFieldUpdate = create_model("PageFieldUpdate", __base__=StrictSchema, **_update_fields)


class PagePatch(StrictSchema):
    title: str = Field(default="", min_length=1, max_length=100)
    add_fields: list[PageField] = Field(default_factory=list, max_length=30)
    update_fields: list[PageFieldUpdate] = Field(default_factory=list, max_length=30)
    remove_field_keys: list[str] = Field(default_factory=list, max_length=30)
    actions: list[PageAction] = Field(default_factory=list, max_length=4)
    form_columns: int = Field(default=2, ge=1, le=4)
    table: TableConfig = Field(default_factory=TableConfig)
    pagination: PaginationConfig = Field(default_factory=PaginationConfig)
    form: FormConfig = Field(default_factory=FormConfig)
    sample_data: list[dict[str, Scalar | None]] | None = Field(default=None, max_length=100)


class PagePreview(StrictSchema):
    columns: list[dict[str, Any]]
    filters: list[dict[str, Any]]
    form: list[dict[str, Any]]
    actions: list[PageAction]
    form_columns: int
    rows: list[dict[str, Any]]
    total: int = Field(ge=0)


class ToolResult(StrictSchema):
    ok: bool
    tool_name: str
    message: str = ""
    data: dict[str, Any] = Field(default_factory=dict)
    page_spec: PageSpec | None = None
    preview: PagePreview | None = None


class PageAgentState(TypedDict, total=False):
    messages: list[Any]
    page_spec: PageSpec | None
    preview: PagePreview | None
    assistant_message: str
    reasoning_content: str
    tool_calls: list[Any]
    steps: int
    clarification: str | None
    pause: bool
    failed_tools: int
    budget_exhausted: bool
    repair_exhausted: bool


class PageAgentProgress(StrictSchema):
    stage: Literal["analyzing", "generating", "validating"]
    message: str


class PageAgentTextDelta(StrictSchema):
    content: str = Field(description="当前回复文字增量")


class PageAgentReasoningDelta(StrictSchema):
    content: str = Field(description="模型接口返回的推理内容增量")


class PageAgentResult(StrictSchema):
    assistant_message: str
    reasoning_content: str = Field(default="", description="本轮模型返回的完整推理内容")
    page_spec: PageSpec | None = None
    preview: PagePreview | None = None
    clarification: str | None = None
    page_changed: bool = False
