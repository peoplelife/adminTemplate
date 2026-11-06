import type { TSearchSchema } from '@/components/base/BaseTable';

export const editSchema = (menuList: any): TSearchSchema => {
	return {
		attrs: {
			labelPosition: 'top'
		},
		formItems: [
			{
				col: {
					span: 12
				},
				attrs: {
					label: '角色代码'
				},
				fields: [
					{
						prop: 'code',
						type: 'input',
						attrs: {
							clearable: true,
							placeholder: '请输入角色代码'
						},
						defaultValue: ''
					}
				]
			},
			{
				col: {
					span: 12
				},
				attrs: {
					label: '角色名称'
				},
				fields: [
					{
						prop: 'name',
						type: 'input',
						attrs: {
							clearable: true,
							placeholder: '请输入角色名称'
						},
						defaultValue: ''
					}
				]
			},
			{
				col: {
					span: 12
				},
				attrs: {
					label: '角色描述'
				},
				fields: [
					{
						prop: 'description',
						type: 'input',
						attrs: {
							clearable: true,
							placeholder: '请输入角色描述'
						},
						defaultValue: ''
					}
				]
			},
			{
				col: {
					span: 12
				},
				attrs: {
					label: '菜单权限'
				},
				fields: [
					{
						prop: 'menuIds',
						type: 'treeSelect',
						attrs: {
							clearable: true,
							placeholder: '请输入菜单权限',
							multiple: true,
							checkStrictly: false,
							data: menuList
						},
						defaultValue: []
					}
				]
			},
			{
				col: {
					span: 12
				},
				attrs: {
					label: '角色状态'
				},
				fields: [
					{
						prop: 'status',
						type: 'select',
						attrs: {
							clearable: true,
							placeholder: '请选择角色状态',
							options: [
								{
									value: 0,
									label: '禁用'
								},
								{
									value: 1,
									label: '启用'
								}
							]
						},
						defaultValue: ''
					}
				]
			}
		]
	};
};
