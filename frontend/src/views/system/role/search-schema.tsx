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
					span: 6
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
							placeholder: '请选择',
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
