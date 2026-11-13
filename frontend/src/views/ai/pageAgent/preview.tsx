import { defineComponent, ref, type PropType } from 'vue';
import { ElButton, ElMessage, ElMessageBox, ElTag } from 'element-plus';
import { BaseTable } from '@/components/base';
import type { TBaseTableConfig } from '@/components/base/BaseTable';
import { createUuid } from '@/utils/uuid';
import {
	createPreviewFormSchema,
	pickConfig,
	selectPreviewRows,
	validatePreviewModel,
	type PageDocument,
	type PageRow
} from './model';

export default defineComponent({
	name: 'PageAgentPreview',
	props: { document: { type: Object as PropType<PageDocument>, required: true } },
	setup(props) {
		const { preview, pageSpec } = props.document;
		const pagination = {
			enabled: true,
			pageSize: 2,
			pageSizes: [2, 5, 10, 20],
			background: true,
			...pickConfig(pageSpec.pagination, ['enabled', 'pageSize', 'pageSizes', 'background'])
		};
		const rows = ref<PageRow[]>(preview.rows.map((row) => ({ ...row })));
		const tableRef = ref();
		const refresh = () => tableRef.value?.onSerach();
		const save = async (exposes, setSubmitLoading) => {
			setSubmitLoading(true);
			try {
				const model = exposes.editViewRef.value.getModel();
				try {
					await exposes.editViewRef.value.validate();
				} catch {
					return;
				}
				const error = validatePreviewModel(preview, model);
				if (error) {
					ElMessage.warning(error);
					return;
				}
				const row: PageRow = { id: model.id || createUuid() };
				preview.form.forEach((field) => {
					row[field.key] = model[field.key] ?? null;
				});
				const index = rows.value.findIndex((item) => item.id === row.id);
				if (index < 0) rows.value.unshift(row);
				else rows.value.splice(index, 1, row);
				exposes.onClose();
				refresh();
				ElMessage.success('保存成功');
			} finally {
				setSubmitLoading(false);
			}
		};
		const remove = async (row: PageRow) => {
			try {
				await ElMessageBox.confirm('确定删除这条记录吗？', '删除确认', {
					confirmButtonText: '删除',
					cancelButtonText: '取消',
					type: 'warning'
				});
			} catch {
				return;
			}
			rows.value = rows.value.filter((item) => item.id !== row.id);
			refresh();
			ElMessage.success('删除成功');
		};
		const detail = (exposes, row: PageRow) => {
			const values = { ...row };
			preview.form.forEach((field) => {
				values[field.key] = field.options.find((option) => option.value === row[field.key])?.label ?? row[field.key];
			});
			exposes.openView(values);
		};
		const tableConfig = (exposes): TBaseTableConfig => ({
			attrs: { rowKey: 'id', border: true, ...pickConfig(pageSpec.table, ['border', 'stripe', 'size', 'showHeader', 'height']) },
			fetch: { api: async (params) => selectPreviewRows(preview, rows.value, params, pagination) },
			columns: [
				...preview.columns.map((field) => ({
					attrs: {
						prop: field.key,
						label: field.label,
						minWidth: 130,
						...pickConfig(field.column, ['width', 'minWidth', 'align', 'fixed', 'sortable', 'showOverflowTooltip']),
						render: ({ row }) => {
							const option = field.options.find((item) => item.value === row[field.key]);
							return option ? <ElTag>{option.label}</ElTag> : <span>{row[field.key] ?? '—'}</span>;
						}
					}
				})),
				...(preview.actions.some((action) => action !== 'create')
					? [
							{
								attrs: {
									label: '操作',
									minWidth: 175,
									fixed: 'right' as const,
									render: ({ row }) => (
										<div class="page-agent-preview__actions">
											{preview.actions.includes('edit') && (
												<ElButton type="primary" link onClick={() => exposes.openEdit(row)}>
													编辑
												</ElButton>
											)}
											{preview.actions.includes('detail') && (
												<ElButton type="primary" link onClick={() => detail(exposes, row)}>
													详情
												</ElButton>
											)}
											{preview.actions.includes('delete') && (
												<ElButton type="danger" link onClick={() => remove(row)}>
													删除
												</ElButton>
											)}
										</div>
									)
								}
							}
						]
					: [])
			],
			pagination: pagination.enabled
				? {
						pageSize: pagination.pageSize,
						pageSizes: pagination.pageSizes,
						background: pagination.background,
						layout: 'total, sizes, prev, pager, next',
						size: 'small',
						pagerCount: 5,
						paginationParams: { requestBeginPage: 1, pageKey: '__previewPage', sizeKey: '__previewPageSize' }
					}
				: undefined
		});
		return () => (
			<div class="page-agent-preview">
				<h2>{pageSpec.title}</h2>
				<BaseTable
					ref={tableRef}
					rowKey="id"
					tableTitle={pageSpec.title}
					tableConfig={tableConfig}
					searchSchema={createPreviewFormSchema(preview.filters, Math.min(preview.formColumns, 2), false, pageSpec.form)}
					editViewSchema={createPreviewFormSchema(preview.form, preview.formColumns, true, pageSpec.form)}
					tableActions={preview.actions.includes('create') ? ['add'] : []}
					searchActions={preview.filters.length ? ['reset', 'search'] : []}
					collapse={false}
					drawerWidth="75%"
					onAdd={save}
					onEdit={save}
				/>
			</div>
		);
	}
});
