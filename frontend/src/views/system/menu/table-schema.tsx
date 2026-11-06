import * as icons from '@element-plus/icons-vue';
import type { TBaseTableConfig } from '@/components/base/BaseTable';
import { getV1Menus } from '@/api';
import { buildRouteMenuTree } from '@/utils/menu';
import { ElButton, ElIcon, ElInput, ElInputNumber } from 'element-plus';

export const tableSchema = (exposes): TBaseTableConfig => {
	return {
		attrs: {
			rowKey: 'id',
			treeProps: { children: 'children' },
			defaultExpandAll: false
		},
		fetch: {
			api: getV1Menus,
			converter: (res) => {
				const list = res.list ?? [];
				const tree = buildRouteMenuTree(list as any);
				return tree;
			}
		},
		columns: [
			{
				attrs: {
					label: '菜单名称',
					prop: 'name'
				}
			},
			{
				attrs: {
					label: '路由路径',
					prop: 'path'
				}
			},
			{
				attrs: {
					label: '组件路径',
					prop: 'component'
				}
			},
			{
				attrs: {
					label: '菜单图标',
					render: ({ row }) => {
						const Icon = icons[row.icon];
						return (
							<div>
								<ElIcon>
									<Icon />
								</ElIcon>
							</div>
						);
					}
				}
			},
			{
				attrs: {
					label: '显示顺序',
					prop: 'sort'
				}
			},
			{
				attrs: {
					label: '是否隐藏',
					prop: 'hidden'
				}
			},
			{
				attrs: {
					label: '菜单类型',
					prop: 'type'
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
		]
	};
};
