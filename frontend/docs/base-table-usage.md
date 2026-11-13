# BaseTable 使用说明

本文档说明项目内 `src/components/base/BaseTable` 的实际用法。`BaseTable` 是基于 Element Plus `ElTable`、`ElPagination`、`Form` 和 `Card` 封装的后台列表组件，统一处理搜索、远程查询、分页、导出和新增/编辑抽屉。

## 1. 快速开始

### 1.1 导入组件

```tsx
import { BaseTable } from '@/components/base';
import { tableConfig } from './table-schema';
import { searchSchema } from './search-schema';
import { editSchema } from './edit-schema';

export default defineComponent({
	setup() {
		return () => (
			<BaseTable
				rowKey="id"
				tableConfig={tableConfig}
				searchSchema={searchSchema()}
				editViewSchema={editSchema()}
			/>
		);
	}
});
```

`rowKey` 是必填属性。建议把表格配置、搜索配置和编辑配置分别放在 `table-schema.tsx`、`search-schema.tsx` 和 `edit-schema.tsx`，与现有用户、角色、菜单页面保持一致。

### 1.2 最小表格配置

```tsx
import type { TBaseTableConfig } from '@/components/base/BaseTable';
import { getV1Users } from '@/api';

export const tableConfig = (): TBaseTableConfig => ({
	attrs: {
		rowKey: 'id'
	},
	fetch: {
		api: getV1Users,
		converter: (res) => res.list ?? []
	},
	columns: [
		{
			attrs: {
				label: '用户名',
				prop: 'username'
			}
		}
	]
});
```

## 2. BaseTable 属性

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `data` | `any[]` | `[]` | 本地数据。没有配置 `fetch.api` 或 `fetch.url` 时使用。 |
| `tableConfig` | `(exposes) => TBaseTableConfig` | `() => ({})` | 表格配置工厂函数。需要动态使用 `exposes` 时使用函数参数。 |
| `searchSchema` | `TSearchSchema` | `{}` | 顶部搜索表单配置。 |
| `editViewSchema` | `TSearchSchema` | `{}` | 新增、编辑和详情抽屉中的表单配置。 |
| `searchTitle` | `string` | `筛选` | 搜索卡片标题。 |
| `tableTitle` | `string` | `列表` | 表格卡片标题。 |
| `searchActions` | `('reset' \| 'search' \| TActionOption)[]` | `['reset', 'search']` | 搜索卡片右侧按钮。 |
| `collapse` | `boolean` | `true` | 是否显示搜索表单的收起/展开按钮。 |
| `drawerWidth` | `number \| string` | `45%` | 新增、编辑和详情抽屉宽度。 |
| `addActions` | `('close' \| 'submit' \| TActionOption)[]` | `['close', 'submit']` | 新增抽屉底部按钮。 |
| `editActions` | `('close' \| 'submit' \| TActionOption)[]` | `['close', 'submit']` | 编辑抽屉底部按钮。 |
| `rowKey` | `string` | 必填 | 行唯一键，例如 `id`。 |
| `tableActions` | `('add' \| 'export' \| 'tools' \| TActionOption)[]` | `['add']` | 表格卡片右侧按钮。 |
| `exportConfig` | `TExportConfig` | `{}` | 导出接口和文件名配置。 |

`tableConfig` 会在组件内部调用，并把组件暴露的方法作为参数传入：

```tsx
export const tableConfig = (exposes): TBaseTableConfig => ({
	columns: [
		{
			attrs: {
				label: '操作',
				render: ({ row }) => (
					<ElButton link type="primary" onClick={() => exposes.openEdit(row)}>
						编辑
					</ElButton>
				)
			}
		}
	]
});
```

## 3. tableConfig

### 3.1 `attrs`

`attrs` 会直接透传给 Element Plus 的 `ElTable`，可以使用 `ElTable` 支持的属性，例如树形表格：

```tsx
attrs: {
	rowKey: 'id',
	treeProps: { children: 'children' },
	defaultExpandAll: false,
	border: true
}
```

`BaseTable` 自己的 `rowKey` 属性和 `tableConfig.attrs.rowKey` 都可以设置行键。项目页面建议保持两处一致，优先在组件上明确传入：

```tsx
<BaseTable rowKey="id" tableConfig={tableConfig} />
```

### 3.2 `columns`

每一列配置放在 `attrs` 中，属性会透传给 `ElTableColumn`：

```tsx
columns: [
	{
		attrs: {
			label: '角色名称',
			prop: 'name',
			minWidth: 160
		}
	},
	{
		attrs: {
			label: '操作',
			width: 160,
			render: ({ row, column, $index }) => (
				<div class="flex items-center gap-2">
					<ElButton size="small" link type="primary" onClick={() => exposes.openEdit(row)}>
						编辑
					</ElButton>
				</div>
			)
		}
	}
]
```

`render` 会被转换为 `ElTableColumn` 的默认插槽，参数是 Element Plus 的单元格作用域对象。常用字段是 `row`、`column` 和 `$index`。

### 3.3 `fetch`

远程表格可使用项目 API 函数：

```tsx
fetch: {
	api: getV1Roles,
	converter: (res) => res.list ?? []
}
```

也可以直接配置 URL：

```tsx
fetch: {
	url: '/v1/roles',
	method: 'get',
	converter: (res) => res.list ?? []
}
```

字段说明：

| 字段 | 说明 |
| --- | --- |
| `api` | 接收查询参数并返回 Promise 的 API 函数。和 `url` 二选一。 |
| `url` | 使用项目 `http` 封装发起请求的 URL。 |
| `method` | URL 模式的请求方法，支持 `get` 和 `post`，默认 `post`。 |
| `converter` | 把接口响应转换成表格行数组。建议始终显式返回 `res.list ?? []`。 |
| `params` | 类型中已声明，但当前 `BaseTable` 实现不会自动使用。 |
| `body` | 类型中已声明，但当前 `BaseTable` 实现不会自动使用。 |
| `afterFetch` | 类型中已声明，但当前 `BaseTable` 实现不会自动调用。 |

项目 Axios 响应拦截器会把 `success: true` 的统一响应解包为 `data`，因此 `getV1Users` 得到的通常是：

```ts
{
	list: [],
	pagination: {
		page: 1,
		pageSize: 20,
		total: 100
	}
}
```

组件读取总数的顺序是 `res.pagination?.total ?? res.total ?? 0`，读取行数据的顺序是 `converter(res)`，否则使用 `res.list ?? []`。

## 4. 分页

配置 `pagination` 后，组件会在首次挂载时自动加载第一页，并在切换页码或每页条数时重新请求：

```tsx
pagination: {
	pageSize: 20,
	pageSizes: [10, 20, 30, 50, 100],
	background: true,
	layout: 'total, sizes, prev, pager, next',
	size: 'small',
	pagerCount: 5,
	paginationParams: {
		requestBeginPage: 1,
		pageKey: 'page',
		sizeKey: 'pageSize'
	}
}
```

`paginationParams` 是 BaseTable 自己的配置：

| 字段 | 说明 |
| --- | --- |
| `requestBeginPage` | 请求第一页时使用的页码，通常为 `1`。 |
| `pageKey` | 请求参数中的页码字段名。 |
| `sizeKey` | 请求参数中的每页数量字段名。 |

搜索时，组件会重新组装查询参数，并将页码重置为 `requestBeginPage`。切换每页条数时也会回到第一页。

## 5. 搜索表单

`searchSchema` 由 `attrs` 和 `formItems` 组成：

```tsx
import type { TSearchSchema } from '@/components/base/BaseTable';

export const searchSchema = (): TSearchSchema => ({
	attrs: {
		labelPosition: 'left'
	},
	formItems: [
		{
			col: { span: 6 },
			attrs: { label: '用户名' },
			fields: [
				{
					prop: 'username',
					type: 'input',
					attrs: {
						clearable: true,
						placeholder: '请输入用户名'
					},
					defaultValue: ''
				}
			]
		}
	]
});
```

### 5.1 表单配置层级

| 层级 | 字段 | 说明 |
| --- | --- | --- |
| 表单 | `attrs` | 透传给 `ElForm`，例如 `labelPosition`。 |
| 表单项 | `col` | 透传给 `ElCol`，例如 `{ span: 6 }`。 |
| 表单项 | `attrs` | 透传给 `ElFormItem`，例如 `{ label: '用户名' }`。 |
| 字段 | `prop` | 对应查询或提交模型的字段名。 |
| 字段 | `type` | 使用的基础组件类型。 |
| 字段 | `attrs` | 透传给具体基础组件。 |
| 字段 | `defaultValue` | 初始化模型值。 |

当前 `Form` 实际可渲染的字段类型：

`input`、`select`、`iconselect`、`upload`、`treeSelect`、`radio`、`inputNumber`、`slider`。

`date`、`number`、`custom` 等类型虽然出现在类型声明中，但当前没有注册到 `BASE_COMPONENTS`，使用时不会渲染组件。

搜索提交时，值为 `''`、`null` 或 `undefined` 的字段会被过滤，不会发送给接口。搜索按钮调用查询，重置按钮先调用表单重置，再重新查询。

## 6. 新增、编辑和详情

编辑抽屉通过 `editViewSchema` 配置，新增和编辑共用同一份 schema：

```tsx
<BaseTable
	rowKey="id"
	tableConfig={tableConfig}
	searchSchema={searchSchema()}
	editViewSchema={editSchema(options)}
	tableActions={['add']}
	onAdd={onAdd}
	onEdit={onEdit}
/>
```

页面事件回调签名：

```tsx
const onAdd = async (exposes, setSubmitLoading) => {
	const model = exposes.editViewRef.value.getModel();
	setSubmitLoading(true);
	try {
		await postV1Users(model);
		exposes.drawerVisible.value = false;
		exposes.onReset();
	} finally {
		setSubmitLoading(false);
	}
};

const onEdit = async (exposes, setSubmitLoading) => {
	const model = exposes.editViewRef.value.getModel();
	setSubmitLoading(true);
	try {
		await patchV1UsersById({ ...model, id: String(model.id) });
		exposes.drawerVisible.value = false;
		exposes.onReset();
	} finally {
		setSubmitLoading(false);
	}
};
```

`mode` 有三种值：

- `add`：点击新增，使用字段的 `defaultValue` 初始化模型。
- `edit`：点击自定义列中的 `exposes.openEdit(row)`，使用当前行数据填充模型。
- `detail`：点击 `exposes.openView(row)`，表单只读，底部只显示关闭按钮。

### 6.1 可用的 exposes

`tableConfig` 的 `exposes` 和新增/编辑事件回调收到的是同一个对象：

| 方法或属性 | 用法 |
| --- | --- |
| `searchRef` | 搜索表单组件引用。 |
| `editViewRef` | 编辑表单组件引用，可调用 `getModel()`、`setModel()`。 |
| `drawerVisible` | 抽屉显示状态的 Ref。 |
| `openEdit(row)` | 打开编辑抽屉并填充行数据。 |
| `openView(row)` | 打开详情抽屉并填充行数据。 |
| `onSerach()` | 重新查询。方法名沿用当前实现中的拼写。 |
| `onReset()` | 重置搜索条件并重新查询。 |
| `onClose()` | 关闭抽屉。 |
| `onSubmit()` | 按当前模式触发新增或编辑事件。 |

## 7. 顶部操作按钮

### 7.1 内置按钮

```tsx
<BaseTable tableActions={['add', 'export']} />
```

当前内置行为如下：

| 值 | 行为 |
| --- | --- |
| `add` | 打开新增抽屉。 |
| `export` | 调用 `exportConfig.api`，并下载 Blob 响应。 |
| `tools` | 当前实现仍调用新增抽屉，不是独立工具菜单。除非确认该行为符合需求，否则不要使用。 |

### 7.2 自定义按钮

```tsx
<BaseTable
	tableActions={[
		{
			text: '批量启用',
			buttonProps: { type: 'success' },
			handle: (exposes) => {
				// 使用 exposes 执行自定义操作
			}
		}
	]}
/>
```

搜索按钮、抽屉底部按钮也支持相同的 `TActionOption` 结构：

```tsx
searchActions={[
	'reset',
	'search',
	{
		text: '刷新',
		buttonProps: { type: 'default' },
		handle: (exposes) => exposes.onSerach()
	}
]}
```

## 8. 导出

```tsx
<BaseTable
	rowKey="id"
	tableConfig={tableConfig}
	exportConfig={{
		api: getV1UsersExport,
		fileName: '用户列表.xlsx'
	}}
	tableActions={['add', 'export']}
/>
```

导出接口会收到当前搜索条件，并额外传入 `{ responseType: 'blob', loading }`：

```ts
api(searchParams, { responseType: 'blob', loading });
```

服务端应返回文件流，并设置 `Content-Disposition`。组件优先使用响应头中的文件名，取不到时使用 `fileName`。

## 9. 本地数据表格

不需要远程请求时，直接传入 `data`，不要配置 `fetch`：

```tsx
<BaseTable
	rowKey="id"
	data={[
		{ id: 1, name: '管理员' },
		{ id: 2, name: '审核员' }
	]}
	tableConfig={() => ({
		columns: [
			{
				attrs: { label: '名称', prop: 'name' }
			}
		]
	})}
/>
```

不要同时依赖 `data` 和 `fetch`：只要存在 `fetch.api` 或 `fetch.url`，组件就会使用远程查询结果。

## 10. 项目现有页面对照

| 页面 | 配置文件 | 远程接口 | 特殊用法 |
| --- | --- | --- | --- |
| 用户管理 | `src/views/system/user/` | `getV1Users` | 分页、导出、新增、编辑、详情、角色树选择。 |
| 角色管理 | `src/views/system/role/` | `getV1Roles` | 分页、新增、编辑、详情、菜单树选择。 |
| 菜单管理 | `src/views/system/menu/` | `getV1Menus` | 树形表格、菜单图标渲染、新增、编辑、详情。 |

推荐新页面沿用以下结构：

```text
src/views/<module>/
├── index.tsx
├── table-schema.tsx
├── search-schema.tsx
└── edit-schema.tsx
```

## 11. 常见问题

### 表格没有数据

按以下顺序检查：

1. `fetch.api` 是否真正返回 Promise。
2. `converter` 是否返回数组，而不是返回完整响应对象。
3. 接口成功响应解包后是否存在 `list` 字段。
4. 是否把 `fetch.api` 和 `data` 混用。
5. 配置了分页时，`pageKey` 和 `sizeKey` 是否与后端参数一致。

### 分页总数是 0

组件只读取：

```ts
res.pagination?.total ?? res.total ?? 0
```

如果后端使用其他字段名，请在 `converter` 或 API 层完成转换，不能让表格直接猜字段。

### 点击编辑没有回显

确认操作列使用了当前 `exposes`：

```tsx
render: ({ row }) => <ElButton onClick={() => exposes.openEdit(row)}>编辑</ElButton>
```

并确认 `editViewSchema` 中的 `prop` 与行数据字段一致。

### 如何删除数据

当前 `BaseTable` 的类型声明里有 `delete` 事件，但组件没有内置删除按钮，也没有在默认操作按钮中渲染 `delete`。删除功能应暂时通过自定义列按钮实现，并在页面中直接调用删除 API，完成后调用 `exposes.onReset()` 刷新列表。

## 12. 开发约定

- 新页面优先复用 `BaseTable`，不要在页面内重复实现搜索、分页和抽屉状态管理。
- 表格 API 返回字段不一致时，在 `converter` 中做一次转换，不要把转换逻辑散落在列渲染函数里。
- `rowKey` 使用后端稳定唯一 ID，不要使用数组下标。
- 新增和编辑提交时使用 `setSubmitLoading`，并在 `finally` 中恢复按钮状态。
- 操作列只放必要操作，复杂操作使用自定义按钮，不修改 `BaseTable` 内部状态。
- 修改 `BaseTable` 的 props 或类型时，同时检查用户、角色、菜单三个现有页面。

## 8. 页面 Agent 的声明式预览

页面 Agent 查询本文后，使用受约束的 PageSpec 配置，前端再转换为上面的 BaseTable schema；不直接执行文档中的 TSX、render、回调或 URL。

### 8.1 表格边框 border、斑马纹 stripe、固定表头 height

`PageSpec.table` 映射到 `tableConfig.attrs`。取消边框用 `{ "border": false }`；斑马纹用 `{ "stripe": true }`；`size` 可选 small/default/large；`showHeader: false` 隐藏表头；`height: 420` 设置表格高度并让内容区域滚动，表头固定。没有 `header.fixed` 这样的属性。

### 8.2 列宽、对齐、固定列与排序

`fields[].column` 映射到每一列的 attrs：`width` 设置列宽，`minWidth` 设置最小宽度，`align` 为 left/center/right，`fixed` 为 left/right 或 null（取消固定），`sortable` 开启本页数据排序，`showOverflowTooltip` 为溢出文本显示提示。列顺序沿用 fields 顺序。

### 8.3 分页每页条数 pageSize、pageSizes

`PageSpec.pagination` 配置 `enabled`、`pageSize`、`pageSizes`、`background`。例如 `{ "enabled": true, "pageSize": 20, "pageSizes": [10, 20, 50], "background": true }`。设置 `enabled: false` 隐藏分页并显示全部筛选结果，不会只截取默认一页。

### 8.4 示例数据 sampleData

`PageSpec.sampleData` 是对象数组，最多 100 行，每个键必须是已定义的字段 key。数字字段用数字，文本字段用字符串，select/radio 使用 options.value。缺省时生成三行演示数据；空数组显示空表；明确设置 null 可恢复自动示例。行 id 由预览编译器分配，不由模型填写。

例如字段 name/age 定义好后，使用 `{ "sampleData": [{ "name": "张三", "age": 28 }, { "name": "李四", "age": 32 }] }` 替换示例。这个操作只影响预览和工作区，不写入用户业务数据库。
