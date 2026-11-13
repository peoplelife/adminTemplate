import type { ChatMessage } from '@/components/ai/messageBubble';
import type { TSearchSchema } from '@/components/base/BaseTable';

type FieldKind = 'input' | 'number' | 'select' | 'radio';
type PageAction = 'create' | 'edit' | 'delete' | 'detail';
type PageOption = { label: string; value: string; color?: string | null };
type ComponentSize = 'large' | 'default' | 'small';
type ColumnConfig = {
	width?: number;
	minWidth?: number;
	align?: 'left' | 'center' | 'right';
	fixed?: 'left' | 'right' | null;
	sortable?: boolean;
	showOverflowTooltip?: boolean;
};
type FieldAttrs = {
	placeholder?: string;
	clearable?: boolean;
	disabled?: boolean;
	maxlength?: number;
	min?: number;
	max?: number;
	step?: number;
	precision?: number;
};
type FieldConfig = { column?: ColumnConfig; attrs?: FieldAttrs; defaultValue?: string | number | null };
export type TableConfig = {
	border?: boolean;
	stripe?: boolean;
	size?: ComponentSize;
	showHeader?: boolean;
	height?: number;
};
export type PaginationConfig = { enabled?: boolean; pageSize?: number; pageSizes?: number[]; background?: boolean };
export type FormConfig = {
	labelPosition?: 'left' | 'right' | 'top';
	labelWidth?: number;
	size?: ComponentSize;
	disabled?: boolean;
};
export type PreviewField = FieldConfig & {
	key: string;
	label: string;
	type: FieldKind;
	required?: boolean;
	options: PageOption[];
};
export type PageRow = { id: string; [key: string]: string | number | null };
export type PageSpec = {
	pageKey: string;
	title: string;
	fields: (FieldConfig & {
		key: string;
		label: string;
		kind: FieldKind;
		required: boolean;
		searchable: boolean;
		options: PageOption[];
	})[];
	actions: PageAction[];
	formColumns: number;
	table?: TableConfig;
	pagination?: PaginationConfig;
	form?: FormConfig;
	sampleData?: Record<string, string | number | null>[] | null;
};
export type PagePreview = {
	columns: PreviewField[];
	filters: PreviewField[];
	form: PreviewField[];
	actions: PageAction[];
	formColumns: number;
	rows: PageRow[];
	total: number;
};
export type PageDocument = { pageSpec: PageSpec; preview: PagePreview };

export const pickConfig = <T extends object>(config: T | undefined, keys: (keyof T)[]): Partial<T> =>
	Object.fromEntries(
		keys.filter((key) => config?.[key] !== undefined && config[key] !== null).map((key) => [key, config[key]])
	) as Partial<T>;

const controlTypes = { input: 'input', number: 'inputNumber', select: 'select', radio: 'radio' } as const;
const safeKey = (key: unknown) =>
	typeof key === 'string' &&
	/^[a-zA-Z][a-zA-Z0-9_]*$/.test(key) &&
	!['id', 'constructor', 'prototype', '__proto__', 'then', 'toString', 'hasOwnProperty'].includes(key);
const safeKind = (kind: unknown): kind is FieldKind =>
	typeof kind === 'string' && Object.prototype.hasOwnProperty.call(controlTypes, kind);
const validOptions = (options: unknown): options is PageOption[] =>
	Array.isArray(options) &&
	options.length <= 30 &&
	options.every((option) => option && typeof option.label === 'string' && typeof option.value === 'string');
const validActions = (actions: unknown): actions is PageAction[] =>
	Array.isArray(actions) && actions.every((action) => ['create', 'edit', 'delete', 'detail'].includes(action));
const validColumns = (columns: unknown) => Number.isInteger(columns) && Number(columns) >= 1 && Number(columns) <= 4;

export const readPageDocument = (pageSpec: unknown, preview: unknown): PageDocument => {
	const spec = pageSpec as PageSpec;
	const page = preview as PagePreview;
	const validFields = (fields: unknown): fields is PreviewField[] =>
		Array.isArray(fields) &&
		fields.length <= 30 &&
		fields.every(
			(field) =>
				field && safeKey(field.key) && typeof field.label === 'string' && safeKind(field.type) && validOptions(field.options)
		);
	if (
		!spec ||
		typeof spec.pageKey !== 'string' ||
		typeof spec.title !== 'string' ||
		!validColumns(spec.formColumns) ||
		!validActions(spec.actions) ||
		!Array.isArray(spec.fields) ||
		!spec.fields.length ||
		spec.fields.length > 30 ||
		!spec.fields.every(
			(field) =>
				field && safeKey(field.key) && typeof field.label === 'string' && safeKind(field.kind) && validOptions(field.options)
		) ||
		!page ||
		!validColumns(page.formColumns) ||
		!validActions(page.actions) ||
		!validFields(page.columns) ||
		!page.columns.length ||
		!validFields(page.filters) ||
		!validFields(page.form) ||
		!Array.isArray(page.rows) ||
		!page.rows.every(
			(row) =>
				row &&
				typeof row.id === 'string' &&
				Object.values(row).every((value) => value === null || typeof value === 'string' || typeof value === 'number')
		)
	) {
		throw new Error('页面数据不完整或包含不支持的控件，请重新描述需求。');
	}
	return { pageSpec: spec, preview: page };
};

export const createPageAgentTurn = (message: ChatMessage, publish: (document: PageDocument) => void) => {
	let pageSpec: unknown;
	let preview: unknown;
	let terminal = false;
	const fail = (error: Error) => {
		if (terminal) return;
		terminal = true;
		message.status = 'error';
		message.reasoningActive = false;
		message.errorMessage = error.message;
	};
	return {
		fail,
		onEvent: (event: { event: string; data: string }) => {
			if (terminal) return;
			try {
				const payload = JSON.parse(event.data);
				if (!payload || typeof payload !== 'object') throw new Error('页面生成返回了无效数据。');
				if (typeof payload.traceId === 'string') message.traceId = payload.traceId;
				switch (event.event) {
					case 'status':
						message.reasoningActive = false;
						if (typeof payload.message === 'string' && payload.message.trim()) {
							message.statusText = payload.message;
						}
						break;
					case 'reasoning':
						if (typeof payload.content === 'string' && payload.content) {
							message.reasoningContent = (message.reasoningContent || '') + payload.content;
							message.reasoningActive = true;
						}
						break;
					case 'message':
						message.reasoningActive = false;
						if (typeof payload.content === 'string') message.content += payload.content;
						break;
					case 'clarification':
						message.reasoningActive = false;
						if (typeof payload.question === 'string' && !message.content.includes(payload.question)) {
							message.content += `${message.content ? '\n\n' : ''}${payload.question}`;
						}
						break;
					case 'page_spec':
						pageSpec = payload;
						break;
					case 'preview':
						preview = payload;
						break;
					case 'error':
						fail(new Error(payload.message || '页面生成失败，请重试。'));
						break;
					case 'done':
						if (payload.status !== 'completed') throw new Error('页面生成未完成，请重试。');
						if (payload.pageChanged === true) publish(readPageDocument(pageSpec, preview));
						message.status = 'done';
						message.reasoningActive = false;
						terminal = true;
				}
			} catch (error) {
				fail(error instanceof Error ? error : new Error('页面生成返回了无效数据。'));
			}
		},
		finish: () => {
			if (!terminal) fail(new Error('页面生成连接已中断，请重试。'));
		}
	};
};

export const selectPreviewRows = (
	preview: PagePreview,
	rows: PageRow[],
	params: Record<string, unknown>,
	pagination?: PaginationConfig
) => {
	const matched = rows.filter((row) =>
		preview.filters.every((field) => {
			const value = params[field.key];
			if (value === undefined || value === null || value === '') return true;
			if (field.type === 'input')
				return String(row[field.key] ?? '')
					.toLocaleLowerCase()
					.includes(String(value).trim().toLocaleLowerCase());
			return String(row[field.key]) === String(value);
		})
	);
	if (pagination?.enabled === false) return { list: matched, total: matched.length };
	const size = Math.max(1, Number(params.__previewPageSize) || pagination?.pageSize || 5);
	const start = (Math.max(1, Number(params.__previewPage) || 1) - 1) * size;
	return { list: matched.slice(start, start + size), total: matched.length };
};

export const validatePreviewModel = (preview: PagePreview, model: Record<string, unknown>) => {
	for (const field of preview.form) {
		const value = model[field.key];
		const empty = value === null || value === undefined || (typeof value === 'string' && !value.trim());
		if (field.required && empty) return `请填写${field.label}`;
		if (empty) continue;
		if (field.type === 'number' && (typeof value !== 'number' || !Number.isFinite(value))) return `请填写有效的${field.label}`;
		if ((field.type === 'select' || field.type === 'radio') && !field.options.some((option) => option.value === value)) {
			return `请选择有效的${field.label}`;
		}
	}
	return '';
};

export const createPreviewFormSchema = (
	fields: PreviewField[],
	columns: number,
	required = false,
	form?: FormConfig
): TSearchSchema => ({
	attrs: { labelPosition: 'top', ...pickConfig(form, ['labelPosition', 'labelWidth', 'size', 'disabled']) },
	formItems: fields.map((field) => {
		if (!safeKind(field.type)) throw new Error('不支持的页面控件');
		return {
			col: { span: 24 / columns },
			attrs: {
				label: field.label,
				rules:
					required && field.required
						? [
								{
									required: true,
									type: field.type === 'number' ? 'number' : 'string',
									message: `请填写${field.label}`,
									trigger: ['blur', 'change'],
									whitespace: field.type === 'input'
								}
							]
						: []
			},
			fields: [
				{
					prop: field.key,
					type: controlTypes[field.type],
					defaultValue: required
						? (field.defaultValue ?? (field.type === 'number' ? undefined : ''))
						: field.type === 'number'
							? undefined
							: '',
					attrs: {
						clearable: true,
						placeholder: `${field.type === 'select' || field.type === 'radio' ? '请选择' : '请输入'}${field.label}`,
						...(field.type === 'number' ? { controlsPosition: 'right' } : {}),
						...pickConfig(field.attrs, ['placeholder', 'clearable', 'disabled', 'maxlength', 'min', 'max', 'step', 'precision']),
						...(field.type === 'select' || field.type === 'radio'
							? { options: field.options.map(({ label, value }) => ({ label, value })) }
							: {})
					}
				}
			]
		};
	})
});
