import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import ts from 'typescript';

const source = await readFile(new URL('../src/views/ai/pageAgent/model.ts', import.meta.url), 'utf8').catch(() => '');
const { outputText } = ts.transpileModule(source, {
	compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 }
});
const model = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`);
const field = { key: 'name', label: '姓名', type: 'input', required: true, options: [] };
const number = { key: 'amount', label: '数量', type: 'number', required: true, options: [] };
const select = {
	key: 'status', label: '状态', type: 'select', required: true,
	options: [{ label: '启用', value: 'enabled', color: null }, { label: '停用', value: 'disabled', color: null }]
};
const preview = {
	columns: [field, number, select], filters: [field, number, select], form: [field, number, select],
	actions: ['create', 'edit', 'delete', 'detail'], formColumns: 2,
	rows: [{ id: '1', name: 'Alice', amount: 0, status: 'enabled' }, { id: '2', name: 'Alan', amount: 20, status: 'disabled' }], total: 2
};
const spec = {
	pageKey: 'users', title: '用户管理', actions: preview.actions, formColumns: 2,
	fields: [field, number, select].map(({ type, ...item }) => ({ ...item, kind: type, searchable: true }))
};
const event = (turn, name, payload) => turn.onEvent({ event: name, data: JSON.stringify(payload) });

test('streams actual reasoning separately from tool statuses and the answer, including later tool rounds', () => {
	const message = { id: 'a', role: 'assistant', content: '', status: 'thinking' };
	const turn = model.createPageAgentTurn(message, () => assert.fail('reasoning cannot publish a page'));
	event(turn, 'status', { stage: 'analyzing', message: '正在分析页面需求' });
	event(turn, 'reasoning', { content: '检查字段。' });
	assert.equal(message.reasoningContent, '检查字段。');
	assert.equal(message.reasoningActive, true);
	assert.equal(message.content, '');
	event(turn, 'reasoning', { content: '需要组件文档。' });
	event(turn, 'status', { stage: 'analyzing', message: '正在查询组件用法' });
	assert.equal(message.reasoningActive, false);
	assert.equal(message.statusText, '正在查询组件用法');
	event(turn, 'reasoning', { content: '\n\n确认配置。' });
	assert.equal(message.reasoningActive, true);
	event(turn, 'message', { content: '已更新表格。' });
	assert.equal(message.reasoningActive, false);
	assert.equal(message.reasoningContent, '检查字段。需要组件文档。\n\n确认配置。');
	assert.equal(message.content, '已更新表格。');
	event(turn, 'done', { status: 'completed', pageChanged: false });
	event(turn, 'reasoning', { content: '迟到的推理' });
	assert.equal(message.reasoningActive, false);
	assert.equal(message.status, 'done');
	assert.doesNotMatch(message.reasoningContent, /迟到/);
});

test('preserves partial reasoning and stops its indicator on errors or disconnects', () => {
	for (const interrupted of [false, true]) {
		const message = { id: 'a', role: 'assistant', content: '', status: 'thinking' };
		const turn = model.createPageAgentTurn(message, () => assert.fail('failed reasoning cannot publish a page'));
		event(turn, 'reasoning', { content: '先检查字段。' });
		if (interrupted) turn.finish();
		else event(turn, 'error', { message: '生成失败' });
		assert.equal(message.reasoningActive, false);
		assert.equal(message.reasoningContent, '先检查字段。');
		assert.equal(message.content, '');
		assert.equal(message.status, 'error');
	}
});

test('appends each text delta immediately while keeping the page pending until done', () => {
	const message = { id: 'a', role: 'assistant', content: '', status: 'thinking' };
	const published = [];
	const turn = model.createPageAgentTurn(message, (document) => published.push(document));
	event(turn, 'message', { content: '已生成' });
	assert.equal(message.content, '已生成');
	assert.equal(message.status, 'thinking');
	event(turn, 'status', { stage: 'validating', message: '正在检查页面结构' });
	event(turn, 'message', { content: '客户页面。' });
	assert.equal(message.content, '已生成客户页面。');
	event(turn, 'page_spec', spec);
	event(turn, 'preview', preview);
	assert.equal(published.length, 0);
	event(turn, 'done', { status: 'completed', pageChanged: true });
	assert.deepEqual(published, [{ pageSpec: spec, preview }]);
	assert.equal(message.content, '已生成客户页面。');
	assert.equal(message.status, 'done');
});

test('publishes a complete page only after successful done and ignores later events', () => {
	assert.equal(typeof model.createPageAgentTurn, 'function');
	const message = { id: 'a', role: 'assistant', content: '', status: 'thinking' };
	const published = [];
	const turn = model.createPageAgentTurn(message, (document) => published.push(document));
	event(turn, 'page_spec', spec);
	event(turn, 'preview', preview);
	event(turn, 'message', { content: '已生成' });
	assert.equal(published.length, 0);
	event(turn, 'done', { status: 'completed', pageChanged: true });
	assert.deepEqual(published, [{ pageSpec: spec, preview }]);
	assert.equal(message.status, 'done');
	event(turn, 'error', { message: 'late error' });
	assert.equal(message.status, 'done');
	turn.finish();
	assert.equal(message.errorMessage, undefined);
});

test('an error or interrupted stream preserves the previous page and displays the failure', () => {
	assert.equal(typeof model.createPageAgentTurn, 'function');
	for (const interrupted of [false, true]) {
		const message = { id: 'a', role: 'assistant', content: '部分回复', status: 'thinking' };
		const published = [];
		const turn = model.createPageAgentTurn(message, (document) => published.push(document));
		event(turn, 'page_spec', spec);
		event(turn, 'preview', preview);
		if (interrupted) turn.finish();
		else event(turn, 'error', { code: 'GENERATION_FAILED', message: '生成失败', traceId: 'trace-1' });
		event(turn, 'done', { status: 'completed', pageChanged: true });
		assert.deepEqual(published, []);
		assert.equal(message.status, 'error');
		assert.equal(message.content, '部分回复');
		assert.ok(message.errorMessage);
		if (!interrupted) assert.equal(message.traceId, 'trace-1');
	}
});

test('clarification keeps the current page and does not repeat the question', () => {
	assert.equal(typeof model.createPageAgentTurn, 'function');
	const message = { id: 'a', role: 'assistant', content: '', status: 'thinking' };
	const turn = model.createPageAgentTurn(message, () => assert.fail('clarification cannot replace a page'));
	event(turn, 'status', { stage: 'analyzing', message: '正在分析页面需求', traceId: 'trace-2' });
	assert.equal(message.statusText, '正在分析页面需求');
	event(turn, 'message', { content: '需要哪些字段？' });
	event(turn, 'clarification', { question: '需要哪些字段？' });
	event(turn, 'done', { status: 'completed', pageChanged: false });
	assert.equal(message.content, '需要哪些字段？');
	assert.equal(message.status, 'done');
});

test('rejects an incomplete or unsupported page without replacing the current preview', () => {
	assert.equal(typeof model.createPageAgentTurn, 'function');
	for (const pendingPreview of [null, { ...preview, form: [{ ...field, type: 'script' }] }]) {
		const message = { id: 'a', role: 'assistant', content: '', status: 'thinking' };
		const turn = model.createPageAgentTurn(message, () => assert.fail('invalid preview cannot be published'));
		event(turn, 'page_spec', spec);
		if (pendingPreview) event(turn, 'preview', pendingPreview);
		event(turn, 'done', { status: 'completed', pageChanged: true });
		assert.equal(message.status, 'error');
		assert.ok(message.errorMessage);
	}
});

test('search uses text matching for inputs and exact matches for options and zero-valued numbers', () => {
	assert.equal(typeof model.selectPreviewRows, 'function');
	assert.deepEqual(model.selectPreviewRows(preview, preview.rows, { name: 'AL', __previewPage: 1, __previewPageSize: 1 }), {
		list: [preview.rows[0]], total: 2
	});
	assert.deepEqual(model.selectPreviewRows(preview, preview.rows, { amount: 0 }), { list: [preview.rows[0]], total: 1 });
	assert.deepEqual(model.selectPreviewRows(preview, preview.rows, { status: 'enabled' }), { list: [preview.rows[0]], total: 1 });
	assert.deepEqual(model.selectPreviewRows(preview, preview.rows, { status: 'enable' }), { list: [], total: 0 });
	assert.deepEqual(model.selectPreviewRows(preview, preview.rows, { __previewPage: 2, __previewPageSize: 1 }), { list: [preview.rows[1]], total: 2 });
});

test('required validation accepts numeric zero and rejects blank text and unknown option values', () => {
	assert.equal(typeof model.validatePreviewModel, 'function');
	assert.equal(model.validatePreviewModel(preview, { name: 'Alice', amount: 0, status: 'enabled' }), '');
	assert.equal(model.validatePreviewModel(preview, { name: '  ', amount: 0, status: 'enabled' }), '请填写姓名');
	assert.equal(model.validatePreviewModel(preview, { name: 'Alice', amount: '', status: 'enabled' }), '请填写数量');
	assert.equal(model.validatePreviewModel(preview, { name: 'Alice', amount: 1, status: 'other' }), '请选择有效的状态');
});

test('maps only approved controls to the existing form schema and sets required rules', () => {
	assert.equal(typeof model.createPreviewFormSchema, 'function');
	const schema = model.createPreviewFormSchema(preview.form, 2, true);
	assert.equal(schema.formItems[1].fields[0].type, 'inputNumber');
	assert.equal(schema.formItems[1].col.span, 12);
	assert.equal(schema.formItems[1].attrs.rules[0].required, true);
	assert.equal(schema.formItems[1].attrs.rules[0].type, 'number');
	assert.deepEqual(schema.formItems[2].fields[0].attrs.options, select.options.map(({ label, value }) => ({ label, value })));
	assert.throws(() => model.createPreviewFormSchema([{ ...field, type: 'script' }], 2, true));
});

test('maps form settings and safe field attributes while preserving false and zero defaults', () => {
	const schema = model.createPreviewFormSchema([
		{ ...field, defaultValue: 'Alice', attrs: { placeholder: '员工姓名', clearable: false, disabled: true, maxlength: 8, onInput: 'bad' } },
		{ ...number, defaultValue: 0, attrs: { min: 0, max: 100, step: 0.5, precision: 1 } }
	], 1, true, { labelPosition: 'left', labelWidth: 100, size: 'small', disabled: true, onSubmit: 'bad' });
	assert.deepEqual(schema.attrs, { labelPosition: 'left', labelWidth: 100, size: 'small', disabled: true });
	assert.equal(schema.formItems[0].fields[0].defaultValue, 'Alice');
	assert.equal(schema.formItems[1].fields[0].defaultValue, 0);
	assert.equal(schema.formItems[0].fields[0].attrs.placeholder, '员工姓名');
	assert.equal(schema.formItems[0].fields[0].attrs.clearable, false);
	assert.equal(schema.formItems[0].fields[0].attrs.disabled, true);
	assert.equal(schema.formItems[0].fields[0].attrs.maxlength, 8);
	assert.equal(schema.formItems[0].fields[0].attrs.onInput, undefined);
	assert.equal(schema.formItems[1].fields[0].attrs.min, 0);
	assert.equal(schema.formItems[1].fields[0].attrs.max, 100);
	assert.equal(schema.formItems[1].fields[0].attrs.step, 0.5);
	assert.equal(schema.formItems[1].fields[0].attrs.precision, 1);
});

test('disabled pagination returns all filtered sample rows', () => {
	assert.deepEqual(model.selectPreviewRows(preview, preview.rows, { __previewPage: 2, __previewPageSize: 1 }, { enabled: false }), {
		list: preview.rows, total: 2
	});
	assert.deepEqual(model.selectPreviewRows(preview, preview.rows, { name: 'Alice', __previewPage: 2 }, { enabled: false }), {
		list: [preview.rows[0]], total: 1
	});
});

test('new-record defaults do not become implicit search filters', () => {
	const fields = [
		{ ...field, defaultValue: '新客户' },
		{ ...number, defaultValue: 0 },
		{ ...select, defaultValue: 'enabled' }
	];
	const search = model.createPreviewFormSchema(fields, 2, false);
	const edit = model.createPreviewFormSchema(fields, 2, true);
	const searchValues = Object.fromEntries(search.formItems.map((item) => [item.fields[0].prop, item.fields[0].defaultValue]));
	assert.deepEqual(model.selectPreviewRows(preview, preview.rows, searchValues), { list: preview.rows, total: 2 });
	assert.deepEqual(edit.formItems.map((item) => item.fields[0].defaultValue), ['新客户', 0, 'enabled']);
});
