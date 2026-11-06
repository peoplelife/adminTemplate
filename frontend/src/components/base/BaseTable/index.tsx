import {
	ElPagination,
	ElTable,
	ElTableColumn,
	ElButton,
	ElIcon,
	ElForm,
	ElFormItem,
	ElInput,
	ElSelect,
	ElDatePicker,
	ElInputNumber,
	ElCollapseTransition,
	ElDrawer,
	ElUpload
} from 'element-plus';
import type { ButtonProps } from 'element-plus';
import { ArrowUpBold, Tools } from '@element-plus/icons-vue';

import { Card } from '@/components/base/Card';
import { Form } from '@/components/base/Form';
import { defineComponent, toRefs, type PropType, ref, computed, onMounted, type ExtractPropTypes, nextTick } from 'vue';
import http from '@/utils/axios';
import { isObject, isString } from '@/utils/is';
import { download, getFileNameFromDisposition } from '@/utils/file';

//搜索表单配置
type ElFormProps = ExtractPropTypes<(typeof ElForm)['props']>;
type ElFormItemProps = ExtractPropTypes<(typeof ElFormItem)['props']>;
type ElInputProps = ExtractPropTypes<(typeof ElInput)['props']>;
type ElSelectProps = ExtractPropTypes<(typeof ElSelect)['props']>;
type ElDatePickerProps = ExtractPropTypes<(typeof ElDatePicker)['props']>;
type ElInputNumberProps = ExtractPropTypes<(typeof ElInputNumber)['props']>;
type ElUploadProps = ExtractPropTypes<(typeof ElUpload)['props']>;
type TFieldAttrsMap = {
	input: Partial<ElInputProps>;
	select: Partial<ElSelectProps>;
	date: Partial<ElDatePickerProps>;
	number: Partial<ElInputNumberProps>;
	upload: Partial<ElUploadProps>;
	custom: Record<string, any>;
	iconselect: Record<string, any>;
	treeSelect: Record<string, any>;
	radio: Record<string, any>;
	inputNumber: Record<string, any>;
};
// 从 TFieldAttrsMap 派生，确保类型一致性
type TSearchFieldType = keyof TFieldAttrsMap;
type TSearchFieldAttrs<Type extends TSearchFieldType> = TFieldAttrsMap[Type];
export type TSearchFormAttrs = Partial<ElFormProps>;
export type TSearchFormItemAttrs = Partial<ElFormItemProps>;
export interface TSearchField<T = Record<string, any>, Type extends TSearchFieldType = TSearchFieldType> {
	prop: keyof T | string;
	type: Type;
	attrs?: TSearchFieldAttrs<Type>;
	defaultValue?: any;
}
export interface TSearchFormItem<T = Record<string, any>> {
	col?: {
		span?: number;
		offset?: number;
	};
	attrs?: TSearchFormItemAttrs;
	fields: TSearchField<T>[];
}
export interface TSearchSchema<T = Record<string, any>> {
	attrs?: TSearchFormAttrs;
	formItems?: TSearchFormItem<T>[];
}

//按钮组类型
export type TSearchActions = 'reset' | 'search';
export type TTableActions = 'add' | 'export' | 'tools';
export type TDrawerActions = 'close' | 'submit';
export type TActionOption = {
	text: string;
	handle?: (expose: any) => void;
	buttonProps?: Partial<ButtonProps>;
};

//表格配置
type ElTableProps = ExtractPropTypes<(typeof ElTable)['props']>;
export type vitalTableAttrs = Partial<ElTableProps>;
export interface vitalTableFetch<T = any> {
	url?: string;
	api?: (params: T) => Promise<any>;
	params?: T;
	body?: T;
	method?: 'get' | 'post';
	afterFetch?: (result: any) => any;
	converter?: (result: any) => Array<any>;
}
type ElTableColumnProps = ExtractPropTypes<(typeof ElTableColumn)['props']>;
export type TTableColumn = {
	attrs: Partial<ElTableColumnProps> & {
		render?: (scope: any) => any;
	};
};

//分页配置
type ElPaginationProps = ExtractPropTypes<(typeof ElPagination)['props']>;
export interface PaginationParams {
	requestBeginPage: number;
	pageKey: string;
	sizeKey: string;
}
export type PaginationConfig = Partial<ElPaginationProps> & {
	paginationParams: PaginationParams;
};

//表格最终配置
export interface TBaseTableConfig<T = any> {
	attrs?: vitalTableAttrs;
	fetch?: vitalTableFetch<T>;
	columns?: TTableColumn[];
	pagination?: PaginationConfig;
}
export type TBaseTableConfigFactory = (exposes: any) => TBaseTableConfig;

//导出配置
export type TExportConfig = {
	api: TAnyFunction<Promise<any>>;
	fileName?: string;
	loading?: boolean;
};

export const BaseTable = defineComponent({
	props: {
		data: {
			type: Array,
			default: () => []
		},
		tableConfig: {
			type: Function as PropType<TBaseTableConfigFactory>,
			default: () => () => ({})
		},
		editViewSchema: {
			type: Object as PropType<TSearchSchema>,
			default: () => ({})
		},
		searchSchema: {
			type: Object as PropType<TSearchSchema>,
			default: () => ({})
		},
		// Search From 标题
		searchTitle: {
			type: String,
			default: '筛选'
		},
		// Table 标题
		tableTitle: {
			type: String,
			default: '列表'
		},
		// Search Form 按钮组
		searchActions: {
			type: Array as PropType<(TActionOption | TSearchActions)[]>,
			default: () => ['reset', 'search']
		},
		// Search Form 是否出现 收起 | 展开 折叠面板效果
		collapse: {
			type: Boolean,
			default: true
		},
		//抽屉宽度
		drawerWidth: {
			type: [Number, String],
			default: '45%'
		},
		//编辑弹窗按钮组
		editActions: {
			type: Array as PropType<(TActionOption | TDrawerActions)[]>,
			default: () => ['close', 'submit']
		},
		//添加弹窗按钮组
		addActions: {
			type: Array as PropType<(TActionOption | TDrawerActions)[]>,
			default: () => ['close', 'submit']
		},
		//列表
		rowKey: {
			type: String,
			required: true
		},
		//表格按钮组配置
		tableActions: {
			type: Array as PropType<(TTableActions | TActionOption)[]>,
			default: () => ['add']
		},
		//导出配置
		exportConfig: {
			type: Object as PropType<TExportConfig>,
			default: () => ({})
		}
	},
	emits: ['add', 'edit', 'delete'],
	setup(props, { emit, expose }) {
		const {
			searchSchema,
			tableConfig,
			searchTitle,
			tableTitle,
			searchActions,
			collapse,
			drawerWidth,
			addActions,
			editActions,
			editViewSchema,
			rowKey,
			tableActions,
			exportConfig
		} = toRefs(props);
		//创建搜索表单数据
		const createFormModel = () => {
			const model = {};
			(searchSchema.value.formItems ?? []).forEach((item) => {
				item.fields.forEach((field) => {
					model[field.prop] = field.defaultValue ?? '';
				});
			});
			return ref(model);
		};
		//创建编辑表单数据
		const createEditFormModel = () => {
			const model = {};
			(editViewSchema.value.formItems ?? []).forEach((item) => {
				item.fields.forEach((field) => {
					model[field.prop] = field.defaultValue ?? '';
				});
			});
			return ref(model);
		};
		const formModel = createFormModel();
		const editFormModel = createEditFormModel();
		const remoteData = ref<any[]>([]);
		const total = ref(0);
		const params = ref<Record<string, any>>({});

		const getSearchParams = () =>
			Object.fromEntries(
				Object.entries(formModel.value).filter(([, value]) => value !== '' && value !== null && value !== undefined)
			);
		const buildRequestParams = () => {
			const pageConfig = pagination.value?.paginationParams;
			const nextParams = getSearchParams();
			if (pageConfig) {
				nextParams[pageConfig.pageKey] = pageConfig.requestBeginPage;
				nextParams[pageConfig.sizeKey] = params.value[pageConfig.sizeKey] ?? (Number(pagination.value?.pageSize) || 20);
			}
			return nextParams;
		};

		const tableRef = ref();

		const searchRef = ref();

		const editViewRef = ref();

		const tableLoading = ref(false);

		const submitLoading = ref(false);

		const exportLoading = ref(false);

		const mode = ref('edit');
		const row = ref({});
		const drawerVisible = ref(false);
		const title = ref('编辑');
		const drawerWidthComputed = computed(() => {
			if (isString(drawerWidth.value) && drawerWidth.value.includes('%')) return drawerWidth.value;
			return Number(drawerWidth.value);
		});
		const isView = computed(() => mode.value === 'detail');
		const isEdit = computed(() => mode.value === 'edit');

		const collapseVisible = ref(true);
		// 计算图标样式：collapse 为 true 则旋转 180 度，否则 0 度
		const arrowStyle = computed(() => ({
			transform: collapseVisible.value ? 'rotate(0deg)' : 'rotate(180deg)',
			transition: 'transform .3s ease'
		}));

		const setSubmitLoading = (loading: boolean) => {
			submitLoading.value = loading;
		};

		const setDrawerConfig = (config) => {
			mode.value = config.mode;
			title.value = config.title;
			drawerVisible.value = config.drawerVisible;
			if (config.row) {
				row.value = config.row;
			}
		};

		const openEdit = (row) => {
			setDrawerConfig({
				title: '编辑',
				drawerVisible: true,
				mode: 'edit',
				row
			});
			nextTick(() => {
				editViewRef.value.setModel(row);
			});
		};
		const exports = () => {
			if (!exportConfig.value.api) return;
			const { api, fileName = '', loading = false } = exportConfig.value;
			const params = getSearchParams();
			exportLoading.value = true;
			api({ ...params }, { responseType: 'blob', loading })
				.then((response) => {
					const filename = getFileNameFromDisposition(response.headers['content-disposition']) || fileName;
					download({ data: response.data, filename });
				})
				.finally(() => {
					exportLoading.value = false;
				});
		};
		const openAdd = () => {
			setDrawerConfig({
				title: '新增',
				drawerVisible: true,
				mode: 'add',
				row: undefined
			});
			nextTick(() => {
				editViewRef.value.setModel(createEditFormModel().value);
			});
		};
		const openView = (row) => {
			setDrawerConfig({
				title: '详情',
				drawerVisible: true,
				mode: 'detail',
				row
			});
			nextTick(() => {
				editViewRef.value.setModel(row);
			});
		};
		const onSerach = () => {
			params.value = buildRequestParams();
			fetchTableList();
		};
		const onReset = () => {
			searchRef.value.reset();
			params.value = buildRequestParams();
			fetchTableList();
		};
		const onClose = () => {
			row.value = undefined;
			drawerVisible.value = false;
		};
		const onSubmit = () => {
			const model = editViewRef.value.getModel() ?? {};
			if (model[rowKey.value]) {
				emit('edit', exposes, setSubmitLoading);
			} else {
				emit('add', exposes, setSubmitLoading);
			}
		};
		//暴露属性方法给外部使用
		const exposes = {
			searchRef,
			editViewRef,
			drawerVisible,
			openEdit,
			onSerach,
			onReset,
			onClose,
			onSubmit,
			openView
		};

		const normalizedTableConfig = computed<TBaseTableConfig>(() => {
			const configFactory = tableConfig.value as TBaseTableConfigFactory;
			const config = configFactory?.(exposes);
			return config ?? {};
		});

		const columns = computed(() => normalizedTableConfig.value.columns ?? []);
		const tableAttrs = computed(() => normalizedTableConfig.value.attrs ?? {});
		const fetch = computed(() => normalizedTableConfig.value.fetch);
		const pagination = computed(() => normalizedTableConfig.value.pagination);
		const tableData = computed(() => (fetch.value?.api || fetch.value?.url ? remoteData.value : props.data));

		const fetchTableList = async () => {
			try {
				if (!fetch.value?.api && !fetch.value?.url) return;
				tableLoading.value = true;
				const res = fetch.value.api
					? await fetch.value.api(params.value)
					: await http[fetch.value.method ?? 'post'](fetch.value.url as string, params.value);
				total.value = res.pagination?.total ?? res.total ?? 0;
				remoteData.value = fetch.value.converter?.(res) ?? res.list ?? [];
				tableLoading.value = false;
			} catch (error) {
				tableLoading.value = false;
			}
		};

		const onCurrentPageChange = (page: number) => {
			const pageConfig = pagination.value?.paginationParams;
			if (!pageConfig) return;
			params.value[pageConfig.pageKey] = page;
			fetchTableList();
		};

		const onPageSizeChange = (pageSize: number) => {
			const pageConfig = pagination.value?.paginationParams;
			if (!pageConfig) return;
			params.value[pageConfig.sizeKey] = pageSize;
			params.value[pageConfig.pageKey] = pageConfig.requestBeginPage;
			fetchTableList();
		};

		const renderPagination = () => {
			if (!pagination.value) return null;
			const { paginationParams, ...paginationAttrs } = pagination.value;
			return (
				<ElPagination
					{...paginationAttrs}
					currentPage={Number(params.value[paginationParams.pageKey])}
					pageSize={Number(params.value[paginationParams.sizeKey])}
					total={total.value}
					onCurrent-change={onCurrentPageChange}
					onSize-change={onPageSizeChange}
				/>
			);
		};

		const renderSearchFormBtns = () => {
			return (
				<div>
					{searchActions.value.map((item: TSearchActions | TActionOption, index) => {
						const action = isString(item) ? item : '';
						const text = isObject(item) ? (item as TActionOption).text : null;
						switch (action) {
							case 'reset':
								return (
									<ElButton key={index} type="info" icon="Refresh" onClick={onReset}>
										重置
									</ElButton>
								);
							case 'search':
								return (
									<ElButton key={index} type="primary" icon="Search" onClick={onSerach}>
										搜索
									</ElButton>
								);
							default:
								const button = item as TActionOption;
								return (
									<ElButton key={index} {...button.buttonProps} onClick={() => button.handle?.(exposes)}>
										{text}
									</ElButton>
								);
						}
					})}
				</div>
			);
		};

		const renderTableAction = () => {
			return (
				<div class="flex items-center">
					{tableActions.value.map((item, index) => {
						const action = isString(item) ? item : '';
						const text = isObject(item) ? (item as TActionOption).text : null;
						switch (action) {
							case 'add':
								return (
									<ElButton type="primary" onClick={openAdd}>
										新增
									</ElButton>
								);
							case 'export':
								return (
									<ElButton type="primary" onClick={exports} loading={exportLoading.value}>
										导出
									</ElButton>
								);
							case 'tools':
								return (
									<ElButton type="primary" onClick={openAdd}>
										工具
									</ElButton>
								);
							default:
								const button = item as TActionOption;
								return (
									<ElButton key={index} {...button.buttonProps} onClick={() => button.handle?.(exposes)}>
										{text}
									</ElButton>
								);
						}
					})}
				</div>
			);
		};

		const renderColumn = (columns: TTableColumn[]) => {
			return columns.map((item) => {
				const { render, ...attrs } = item.attrs;
				const slots = render ? { default: (scope: any) => render(scope) } : null;
				return <ElTableColumn {...attrs} v-slots={slots} />;
			});
		};

		onMounted(() => {
			const pageConfig = pagination.value?.paginationParams;
			if (pageConfig) {
				params.value[pageConfig.pageKey] = pageConfig.requestBeginPage;
				params.value[pageConfig.sizeKey] = Number(pagination.value?.pageSize) || 20;
			}
			fetchTableList();
		});

		expose(exposes);
		return () => (
			<div>
				<Card
					title={searchTitle.value}
					class="mb-10"
					v-slots={{
						headerRight: () => (
							<div class="flex items-center">
								{renderSearchFormBtns()}
								{collapse.value && (
									<div>
										<ElButton type="primary" text onClick={() => (collapseVisible.value = !collapseVisible.value)}>
											<span class="pr-2">{collapseVisible.value ? '收起' : '展开'}</span>
											<ElIcon>
												<ArrowUpBold style={arrowStyle.value} />
											</ElIcon>
										</ElButton>
									</div>
								)}
							</div>
						)
					}}
				>
					<ElCollapseTransition>
						<div v-show={collapseVisible.value}>
							<Form searchSchema={searchSchema.value} v-model={formModel.value} ref={searchRef} />
						</div>
					</ElCollapseTransition>
				</Card>
				<Card title={tableTitle.value} v-slots={{ headerRight: () => renderTableAction() }}>
					<ElTable data={tableData.value} {...tableAttrs.value} ref={tableRef} v-loading={tableLoading.value}>
						{renderColumn(columns.value)}
					</ElTable>
					{pagination.value && <div class="mt-10">{renderPagination()}</div>}
					<ElDrawer
						v-model={drawerVisible.value}
						title={title.value}
						destroy-on-close={true}
						size={drawerWidthComputed.value}
						close-on-press-escape={false}
						append-to-body
						v-slots={{
							footer: () => {
								if (isView.value)
									return (
										<div>
											<ElButton type="default" onClick={onClose}>
												关闭
											</ElButton>
										</div>
									);
								const actions = isEdit.value ? editActions.value : addActions.value;
								return (
									<div class="flex justify-end">
										{actions.map((item, index) => {
											const action = isString(item) ? item : '';
											const text = isObject(item) ? (item as TActionOption).text : null;
											switch (action) {
												case 'close':
													return (
														<ElButton type="default" onClick={onClose}>
															关闭
														</ElButton>
													);
												case 'submit':
													return (
														<ElButton type="primary" loading={submitLoading.value} onClick={onSubmit}>
															提交
														</ElButton>
													);
												default:
													const button = item as TActionOption;
													return (
														<ElButton key={index} {...button.buttonProps} onClick={() => button.handle?.(exposes)}>
															{text}
														</ElButton>
													);
											}
										})}
									</div>
								);
							}
						}}
					>
						<Form mode={mode.value} searchSchema={editViewSchema.value} ref={editViewRef} v-model={editFormModel.value} />
					</ElDrawer>
				</Card>
			</div>
		);
	}
});
