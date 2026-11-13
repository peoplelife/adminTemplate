# Form 使用说明

本文对应 `src/components/base/Form/index.tsx` 与 `src/components/base/config.ts` 的实际实现。Form 接收 `searchSchema`，通过 `attrs` 配置 ElForm，通过 `formItems[].fields` 渲染已注册控件。

## 表单标签位置与整体配置 labelPosition labelWidth

`searchSchema.attrs` 透传给 ElForm。`labelPosition` 是标签位置，可用 `left`、`right`、`top`；`labelWidth` 是标签宽度；`size` 是 `small`、`default`、`large`；`disabled` 控制整个表单禁用。

页面 Agent 对应 `PageSpec.form`，例如 `{ "labelPosition": "left", "labelWidth": 100, "size": "small", "disabled": false }`。原有 `formColumns` 控制每行字段数量（1～4 列），不会改变字段名称。

## 字段属性 attrs 与默认值 defaultValue

普通表单字段通过 `prop` 对应模型字段，`type` 选择组件，`attrs` 透传组件属性。`defaultValue` 在 BaseTable 打开新增抽屉时初始化模型；编辑时使用当前行值。

```json
{
  "prop": "name",
  "type": "input",
  "attrs": { "placeholder": "请输入姓名", "clearable": true, "maxlength": 40 },
  "defaultValue": ""
}
```

页面 Agent 在 `fields[]` 使用 `key`、`kind`、`attrs`、`defaultValue`，由预览适配为 Form schema。支持的属性：`placeholder`（占位提示）、`clearable`（可清空）、`disabled`（禁用）、`maxlength`（文本长度）、`min` / `max`（数值范围）、`step`（数值步长）、`precision`（数值小数位）。属性应匹配控件，文本值用字符串，数字控件值用数字，select/radio 值必须来自 options。

## 已注册控件与页面 Agent 范围

Form 实际注册 `input`、`select`、`iconselect`、`upload`、`treeSelect`、`radio`、`inputNumber`、`slider`。页面 Agent 当前支持 `input`、`number`、`select`、`radio`，其中 `number` 映射到实际的 `inputNumber`。

`date`、`number`、`custom` 不能直接作为 Form 的 type：虽然类型声明存在，当前未注册对应组件。页面 Agent 也不能生成任意事件、JS/TSX、函数式 render 或绑定任意接口。后续扩展一种控件时须同步结构校验与前端映射，不能只靠提示词声明支持。

## 必填、搜索与详情

页面 Agent 的 `required` 为新增/编辑生成必填规则，`searchable` 决定是否进入搜索表单。搜索时空字符串、null、undefined 不作为过滤条件。详情模式显示当前值，不渲染编辑控件。
