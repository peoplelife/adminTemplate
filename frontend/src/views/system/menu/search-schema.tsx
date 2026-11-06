import type { TSearchSchema } from '@/components/base/BaseTable';

export const searchSchema = (): TSearchSchema => {
	return {
		attrs: {
			labelPosition: 'left'
		},
		formItems: [
			{
				col: {
					span: 6
				},
				attrs: {
					label: '菜单名称'
				},
				fields: [
					{
						prop: 'menuName',
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
					span: 6
				},
				attrs: {
					label: '菜单类型'
				},
				fields: [
					{
						prop: 'menuType',
						type: 'select',
						attrs: {
							clearable: true,
							placeholder: '请选择',
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
						defaultValue: ''
					}
				]
			}
		]
	};
};
