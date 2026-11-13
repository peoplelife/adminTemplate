import assert from 'node:assert/strict';
import { after, test } from 'node:test';
import { createServer } from 'vite';
import { createSSRApp, h } from 'vue';
import { renderToString } from 'vue/server-renderer';
import { ElMessage, ElMessageBox } from 'element-plus';

const originalSuccess = ElMessage.success;
const originalConfirm = ElMessageBox.confirm;
ElMessage.success = () => {};
after(() => {
	ElMessage.success = originalSuccess;
	ElMessageBox.confirm = originalConfirm;
});

globalThis.localStorage = {
	getItem: () => null,
	setItem: () => {},
	removeItem: () => {}
};

const server = await createServer({
	appType: 'custom',
	logLevel: 'silent',
	server: { middlewareMode: true },
	plugins: [
		{
			name: 'preview-without-network',
			load(id) {
				if (id.replaceAll('\\', '/').endsWith('/src/utils/axios.ts')) return 'export default {}';
			}
		}
	]
});
const { default: Preview } = await server.ssrLoadModule('/src/views/ai/pageAgent/preview.tsx');
after(() => server.close());
const field = { key: 'name', label: '姓名', type: 'input', required: true, options: [] };
const status = { key: 'status', label: '状态', type: 'select', options: [{ label: '启用', value: 'enabled' }] };
const document = {
	pageSpec: { title: '员工管理' },
	preview: {
		columns: [field],
		filters: [field],
		form: [field, status],
		actions: ['create', 'edit', 'delete', 'detail'],
		formColumns: 2,
		rows: [
			{ id: '1', name: '甲' },
			{ id: '2', name: '乙' },
			{ id: '3', name: '丙' }
		],
		total: 3
	}
};
const previewTable = async (pageDocument = document) => {
	let render;
	await renderToString(
		createSSRApp({
			setup: () => {
				render = Preview.setup({ document: pageDocument });
				return () => h('div');
			}
		})
	);
	return render().children[1];
};

test('preview uses the configured BaseTable pagination keys for its synthetic fetch', async () => {
	const table = await previewTable();
	const config = table.props.tableConfig({});
	const { pageKey, sizeKey } = config.pagination.paginationParams;
	const result = await config.fetch.api({ [pageKey]: 2, [sizeKey]: 2 });
	assert.deepEqual(result, { list: [{ id: '3', name: '丙' }], total: 3 });
});

test('preview create and edit update the synthetic table without mutating the server snapshot', async () => {
	const table = await previewTable();
	const config = table.props.tableConfig({});
	let saved = { name: '丁' };
	let closed = 0;
	const loading = [];
	const exposes = { editViewRef: { value: { getModel: () => saved, validate: async () => true } }, onClose: () => closed++ };
	await table.props.onAdd(exposes, (value) => loading.push(value));
	let result = await config.fetch.api({ name: '丁' });
	assert.equal(result.total, 1);
	assert.equal(result.list[0].name, '丁');
	assert.ok(result.list[0].id);
	saved = { ...result.list[0], name: '戊' };
	await table.props.onEdit(exposes, (value) => loading.push(value));
	result = await config.fetch.api({ name: '戊' });
	assert.equal(result.total, 1);
	assert.equal(result.list[0].id, saved.id);
	assert.equal(closed, 2);
	assert.deepEqual(loading, [true, false, true, false]);
	assert.deepEqual(document.preview.rows, [
		{ id: '1', name: '甲' },
		{ id: '2', name: '乙' },
		{ id: '3', name: '丙' }
	]);
});

test('a rejected required-field validation leaves the preview unchanged and the editor open', async () => {
	const table = await previewTable();
	const config = table.props.tableConfig({});
	let closed = false;
	const exposes = {
		editViewRef: {
			value: {
				getModel: () => ({ name: '' }),
				validate: async () => {
					throw new Error('required');
				}
			}
		},
		onClose: () => {
			closed = true;
		}
	};
	await table.props.onAdd(exposes, () => {});
	assert.equal(closed, false);
	assert.equal((await config.fetch.api({})).total, 3);
});

test('delete removes a synthetic record only after confirmation', async () => {
	const table = await previewTable();
	const config = table.props.tableConfig({});
	const actions = config.columns.at(-1).attrs.render({ row: { id: '1', name: '甲' } });
	const remove = actions.children[2].props.onClick;
	ElMessageBox.confirm = async () => {
		throw new Error('cancel');
	};
	await remove();
	assert.equal((await config.fetch.api({})).total, 3);
	ElMessageBox.confirm = async () => 'confirm';
	await remove();
	assert.deepEqual(
		(await config.fetch.api({})).list.map((row) => row.id),
		['2', '3']
	);
});

test('detail shows option labels without changing the underlying row value', async () => {
	const table = await previewTable();
	let detail;
	const config = table.props.tableConfig({
		openView: (row) => {
			detail = row;
		}
	});
	const row = { id: '1', name: '甲', status: 'enabled' };
	const actions = config.columns.at(-1).attrs.render({ row });
	actions.children[1].props.onClick();
	assert.equal(detail.status, '启用');
	assert.equal(row.status, 'enabled');
});

test('table and column settings override preview defaults, including removing borders', async () => {
	const table = await previewTable({
		...document,
		pageSpec: { ...document.pageSpec, table: { border: false, stripe: true, size: 'small', showHeader: false, height: 360, onRowClick: 'bad' } },
		preview: { ...document.preview, columns: [{ ...field, column: {
			width: 200, minWidth: 160, align: 'center', fixed: 'left', sortable: true, showOverflowTooltip: false, render: 'bad'
		} }] }
	});
	const config = table.props.tableConfig({});
	assert.deepEqual(config.attrs, { rowKey: 'id', border: false, stripe: true, size: 'small', showHeader: false, height: 360 });
	const { render, ...column } = config.columns[0].attrs;
	assert.equal(typeof render, 'function');
	assert.deepEqual(column, {
		prop: 'name', label: '姓名', width: 200, minWidth: 160, align: 'center', fixed: 'left', sortable: true, showOverflowTooltip: false
	});
});

test('PageSpec controls pagination and form settings while compiled preview supplies the sample rows', async () => {
	const sample = [{ id: 'sample-1', name: '示例员工' }];
	const table = await previewTable({
		pageSpec: {
			...document.pageSpec, sampleData: [{ name: '不得由前端重新生成' }],
			pagination: { enabled: false, pageSize: 20, pageSizes: [20, 50], background: false },
			form: { labelPosition: 'right', labelWidth: '100px', size: 'small', disabled: true }
		},
		preview: { ...document.preview, rows: sample }
	});
	const config = table.props.tableConfig({});
	assert.equal(config.pagination, undefined);
	assert.deepEqual(await config.fetch.api({ __previewPage: 2, __previewPageSize: 1 }), { list: sample, total: 1 });
	assert.deepEqual(table.props.editViewSchema.attrs, { labelPosition: 'right', labelWidth: '100px', size: 'small', disabled: true });
	assert.deepEqual(table.props.searchSchema.attrs, { labelPosition: 'right', labelWidth: '100px', size: 'small', disabled: true });
	const enabled = await previewTable({
		...document, pageSpec: { ...document.pageSpec, pagination: { enabled: true, pageSize: 1, pageSizes: [1, 2], background: false } }
	});
	const enabledConfig = enabled.props.tableConfig({});
	assert.equal(enabledConfig.pagination.pageSize, 1);
	assert.deepEqual(enabledConfig.pagination.pageSizes, [1, 2]);
	assert.equal(enabledConfig.pagination.background, false);
	assert.deepEqual((await enabledConfig.fetch.api({})).list, [{ id: '1', name: '甲' }]);
});
