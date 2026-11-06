import type { TBaseTableConfig } from '@/components/base/BaseTable';
import { getV1Users } from '@/api';
import { ElButton } from 'element-plus';
export const tableConfig = (exposes: any): TBaseTableConfig => {
	return {
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
			},
			{
				attrs: {
					label: '昵称',
					prop: 'nickname'
				}
			},
			{
				attrs: {
					label: '邮箱',
					prop: 'email'
				}
			},
			{
				attrs: {
					label: '手机号',
					prop: 'phone'
				}
			},
			{
				attrs: {
					label: '头像',
					prop: 'avatar'
				}
			},
			{
				attrs: {
					label: '角色',
					prop: 'roleCodes'
				}
			},
			{
				attrs: {
					label: '最后登录时间',
					prop: 'lastLogin'
				}
			},
			{
				attrs: {
					label: '操作',
					render: ({ row }) => (
						<div class="flex  items-center gap-2">
							<ElButton type="primary" size="small" link onClick={() => exposes.openEdit(row)}>
								编辑
							</ElButton>
							<ElButton type="primary" size="small" link onClick={() => exposes.openView(row)}>
								详情
							</ElButton>
						</div>
					)
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
