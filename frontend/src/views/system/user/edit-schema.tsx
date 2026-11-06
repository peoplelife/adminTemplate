import type { TSearchSchema } from '@/components/base/BaseTable';

export const editSchema = (roleList: any): TSearchSchema => {
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
					label: '用户名'
				},
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
			},
			{
				col: {
					span: 12
				},
				attrs: {
					label: '昵称'
				},
				fields: [
					{
						prop: 'nickname',
						type: 'input',
						attrs: {
							clearable: true,
							placeholder: '请输入昵称'
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
					label: '邮箱'
				},
				fields: [
					{
						prop: 'email',
						type: 'input',
						attrs: {
							clearable: true,
							placeholder: '请输入邮箱'
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
					label: '手机号'
				},
				fields: [
					{
						prop: 'phone',
						type: 'input',
						attrs: {
							clearable: true,
							placeholder: '请输入手机号'
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
					label: '头像'
				},
				fields: [
					{
						prop: 'avatar',
						type: 'upload',
						defaultValue: ''
					}
				]
			},
			{
				col: {
					span: 12
				},
				attrs: {
					label: '角色'
				},
				fields: [
					{
						prop: 'roleCodes',
						type: 'treeSelect',
						attrs: {
							clearable: true,
							placeholder: '请选择角色',
							multiple: true,
							data: roleList
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
					label: '状态'
				},
				fields: [
					{
						prop: 'status',
						type: 'select',
						attrs: {
							clearable: true,
							placeholder: '请选择状态',
							options: [
								{
									label: '启用',
									value: 1
								},
								{
									label: '禁用',
									value: 0
								}
							]
						},
						defaultValue: 1
					}
				]
			}
		]
	};
};
