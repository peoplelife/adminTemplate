import type { TBaseTableConfig } from '@/components/base/BaseTable';
import { getV1Roles } from '@/api';
import { ElButton } from 'element-plus';
export const tableConfig = (exposes: any): TBaseTableConfig => {
	return {
		attrs: {
			rowKey: 'id'
		},
		fetch: {
			api: getV1Roles,
			converter: (res) => res.list ?? []
		},
		columns: [
			{
				attrs: {
					label: '角色代码',
					prop: 'code'
				}
			},
			{
				attrs: {
					label: '角色名称',
					prop: 'name'
				}
			},
			{
				attrs: {
					label: '角色描述',
					prop: 'description'
				}
			},
			{
				attrs: {
					label: '状态',
					prop: 'status'
				}
			},
			{
				attrs: {
					label: '创建时间',
					prop: 'createdAt'
				}
			},
			{
				attrs: {
					label: '更新时间',
					prop: 'updatedAt'
				}
			},

			{
				attrs: {
					label: '操作',
					render: ({ row }) => {
						return row.code === 'super_admin' ? null : (
							<div class="flex  items-center gap-2">
								<ElButton type="primary" size="small" link onClick={() => exposes.openEdit(row)}>
									编辑
								</ElButton>
								<ElButton type="primary" size="small" link onClick={() => exposes.openView(row)}>
									详情
								</ElButton>
							</div>
						);
					}
				}
			}
		],
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
	};
};
