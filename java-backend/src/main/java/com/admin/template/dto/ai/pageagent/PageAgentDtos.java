package com.admin.template.dto.ai.pageagent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public final class PageAgentDtos {
    private PageAgentDtos() {
    }

    public enum MessageRole { user, assistant }
    public enum FieldKind { input, number, select, radio }
    public enum PageAction { create, edit, delete, detail }
    public enum ComponentSize { small, @JsonProperty("default") normal, large }
    public enum ColumnAlign { left, center, right }
    public enum ColumnFixed { left, right }
    public enum LabelPosition { left, right, top }

    @Schema(name = "PageAgentTableConfig", description = "受约束的表格配置")
    public record TableConfig(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean border,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean stripe,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ComponentSize size,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean showHeader,
            @Schema(types = {"integer", "null"}, minimum = "160", maximum = "1600", requiredMode = Schema.RequiredMode.REQUIRED)
            Integer height) {
    }

    @Schema(name = "PageAgentPaginationConfig", description = "预览分页配置")
    public record PaginationConfig(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @Min(1) @Max(100) int pageSize,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @Size(min = 1, max = 10) List<@Min(1) @Max(100) Integer> pageSizes,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean background) {
    }

    @Schema(name = "PageAgentFormConfig", description = "预览表单配置")
    public record FormConfig(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LabelPosition labelPosition,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, defaultValue = "100") @Min(40) @Max(400) int labelWidth,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ComponentSize size,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean disabled) {
    }

    @Schema(name = "PageAgentColumnConfig", description = "预览列配置")
    public record ColumnConfig(
            @Schema(types = {"integer", "null"}, requiredMode = Schema.RequiredMode.REQUIRED) @Min(40) @Max(1200) Integer width,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, defaultValue = "130") @Min(40) @Max(1200) int minWidth,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ColumnAlign align,
            @Schema(types = {"string", "null"}, requiredMode = Schema.RequiredMode.REQUIRED) ColumnFixed fixed,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean sortable,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean showOverflowTooltip) {
    }

    @Schema(name = "PageAgentControlAttrs", description = "已有控件支持的声明式属性")
    public record ControlAttrs(
            @Schema(types = {"string", "null"}, requiredMode = Schema.RequiredMode.REQUIRED) @Size(max = 200) String placeholder,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean clearable,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean disabled,
            @Schema(types = {"integer", "null"}, requiredMode = Schema.RequiredMode.REQUIRED) @Min(1) @Max(10000) Integer maxlength,
            @Schema(types = {"number", "null"}, requiredMode = Schema.RequiredMode.REQUIRED) Double min,
            @Schema(types = {"number", "null"}, requiredMode = Schema.RequiredMode.REQUIRED) Double max,
            @Schema(types = {"number", "null"}, requiredMode = Schema.RequiredMode.REQUIRED) Double step,
            @Schema(types = {"integer", "null"}, requiredMode = Schema.RequiredMode.REQUIRED) @Min(0) @Max(10) Integer precision) {
    }

    @Schema(name = "PageAgentMessageRequest", description = "页面生成 Agent 用户消息")
    public record MessageRequest(
            @Schema(description = "用户对页面的自然语言描述", requiredMode = Schema.RequiredMode.REQUIRED,
                    maxLength = 4000, example = "生成一个员工管理页面")
            @NotBlank @Size(max = 4000) String content) {
    }

    @Schema(name = "PageAgentWorkspaceMessage", description = "工作区对话消息")
    public record WorkspaceMessage(
            @Schema(description = "消息角色", allowableValues = {"user", "assistant"}, requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull MessageRole role,
            @Schema(description = "消息内容（用户最多 4000 字符，助手最多 8000 字符）", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 8000)
            @NotBlank @Size(max = 8000) String content,
            @Schema(description = "模型返回的独立推理内容，与回复正文分开保存", requiredMode = Schema.RequiredMode.REQUIRED)
            String reasoningContent) {
    }

    @Schema(name = "PageAgentOption", description = "下拉或单选项")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Option(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String label,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String value,
            @Schema(types = {"string", "null"}, requiredMode = Schema.RequiredMode.REQUIRED, description = "可选颜色") String color) {
    }

    @Schema(name = "PageAgentField", description = "页面字段")
    public record Field(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String key,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String label,
            @Schema(description = "字段组件类型", allowableValues = {"input", "number", "select", "radio"}, requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull FieldKind kind,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean required,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean searchable,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @Valid List<Option> options,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ColumnConfig column,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ControlAttrs attrs,
            @Schema(implementation = String.class, types = {"string", "number", "null"}, requiredMode = Schema.RequiredMode.REQUIRED, description = "控件默认标量值")
            JsonNode defaultValue) {
    }

    @Schema(name = "PageAgentPageSpec", description = "受约束的页面结构描述")
    public record PageSpec(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String pageKey,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String title,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @Valid List<Field> fields,
            @Schema(description = "页面操作", requiredMode = Schema.RequiredMode.REQUIRED)
            List<PageAction> actions,
            @Schema(description = "表单列数", minimum = "1", maximum = "4", requiredMode = Schema.RequiredMode.REQUIRED)
            @Min(1) @Max(4) int formColumns,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TableConfig table,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PaginationConfig pagination,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) FormConfig form,
            @Schema(types = {"array", "null"}, description = "可选合成数据，最多 100 行；字段值为字符串、数字或 null",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @Size(max = 100) List<Map<String, Object>> sampleData) {
    }

    @Schema(name = "PageAgentPreviewColumn", description = "预览表格列或筛选器")
    public record PreviewColumn(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String key,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String label,
            @Schema(description = "组件类型", allowableValues = {"input", "number", "select", "radio"}, requiredMode = Schema.RequiredMode.REQUIRED)
            FieldKind type,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Option> options,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ColumnConfig column,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ControlAttrs attrs,
            @Schema(implementation = String.class, types = {"string", "number", "null"}, requiredMode = Schema.RequiredMode.REQUIRED)
            JsonNode defaultValue) {
    }

    @Schema(name = "PageAgentPreviewFormField", description = "预览表单字段")
    public record PreviewFormField(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String key,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String label,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) FieldKind type,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean required,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Option> options,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ColumnConfig column,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ControlAttrs attrs,
            @Schema(implementation = String.class, types = {"string", "number", "null"}, requiredMode = Schema.RequiredMode.REQUIRED)
            JsonNode defaultValue) {
    }

    @Schema(name = "PageAgentPreview", description = "根据 PageSpec 编译出的安全预览数据")
    public record PagePreview(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<PreviewColumn> columns,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<PreviewColumn> filters,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<PreviewFormField> form,
            @Schema(description = "页面操作", requiredMode = Schema.RequiredMode.REQUIRED) List<PageAction> actions,
            @Schema(minimum = "1", maximum = "4", requiredMode = Schema.RequiredMode.REQUIRED) int formColumns,
            @Schema(description = "合成行；除 id 外键为字段 key，值为标量", requiredMode = Schema.RequiredMode.REQUIRED)
            List<Map<String, Object>> rows,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int total) {
    }

    @Schema(name = "PageAgentWorkspace", description = "当前登录用户的页面生成工作区")
    public record Workspace(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<WorkspaceMessage> messages,
            @Schema(types = {"object", "null"}, requiredMode = Schema.RequiredMode.REQUIRED) PageSpec pageSpec,
            @Schema(types = {"object", "null"}, requiredMode = Schema.RequiredMode.REQUIRED) PagePreview preview) {
    }
}
