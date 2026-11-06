import type { TSearchSchema } from '@/components/base/BaseTable';

export const editschema = (menuList: any): TSearchSchema => {
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
					label: '上级菜单'
				},
				fields: [
					{
						prop: 'parentId',
						type: 'treeSelect',
						attrs: {
							clearable: true,
							placeholder: '请选择上级菜单',
							defaultExpandAll: true,
							checkStrictly: true,
							data: menuList
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
					label: '菜单名称'
				},
				fields: [
					{
						prop: 'name',
						type: 'input',
						attrs: {
							clearable: true,
							placeholder: '请输入菜单名称'
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
					label: '路由路径'
				},
				fields: [
					{
						prop: 'path',
						type: 'input',
						attrs: {
							clearable: true,
							placeholder: '请输入路由路径'
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
					label: '组件路径'
				},
				fields: [
					{
						prop: 'component',
						type: 'input',
						attrs: {
							clearable: true,
							placeholder: '请输入组件路径'
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
					label: '菜单图标'
				},
				fields: [
					{
						prop: 'icon',
						type: 'iconselect',
						attrs: {
							clearable: true,
							placeholder: '请输入菜单图标'
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
					label: '显示顺序'
				},
				fields: [
					{
						prop: 'sort',
						type: 'inputNumber',
						attrs: {
							min: 0
						},
						defaultValue: 0
					}
				]
			},
			{
				col: {
					span: 12
				},
				attrs: {
					label: '权限标识'
				},
				fields: [
					{
						prop: 'buttonPermission',
						type: 'input',
						attrs: {
							clearable: true,
							placeholder: '请输入权限标识'
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
					label: '菜单类型'
				},
				fields: [
					{
						prop: 'type',
						type: 'radio',
						attrs: {
							clearable: true,
							options: [
								{
									value: 'M',
									label: '目录'
								},
								{
									value: 'C',
									label: '菜单'
								},
								{
									value: 'B',
									label: '按钮'
								}
							]
						},
						defaultValue: 'M'
					}
				]
			},
			{
				col: {
					span: 12
				},
				attrs: {
					label: '是否全屏'
				},
				fields: [
					{
						prop: 'isFull',
						type: 'radio',
						attrs: {
							options: [
								{
									value: 0,
									label: '否'
								},
								{
									value: 1,
									label: '是'
								}
							]
						},
						defaultValue: 0
					}
				]
			},
			{
				col: {
					span: 12
				},
				attrs: {
					label: '是否隐藏'
				},
				fields: [
					{
						prop: 'hidden',
						type: 'radio',
						attrs: {
							clearable: true,

							options: [
								{
									value: 0,
									label: '显示'
								},
								{
									value: 1,
									label: '隐藏'
								}
							]
						},
						defaultValue: 0
					}
				]
			}
		]
	};
};
