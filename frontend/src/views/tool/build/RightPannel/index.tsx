import { defineComponent, toRefs, ref } from 'vue';
import { BaseSelect, Slider } from '@/components/base';
import { typeList } from '@/utils/generator/config';
import { ElDivider, ElForm, ElFormItem, ElInput, ElTabPane, ElTabs } from 'element-plus';

export default defineComponent({
	props: {
		activeData: {
			type: Object,
			default: () => ({})
		}
	},
	emits: ['update:activeData'],
	setup(props, { emit }) {
		const { activeData } = toRefs(props);
		const typeOptions = ref(typeList);

		const updateField = (key, val) => {
			const next = { ...activeData.value, [key]: val };
			emit('update:activeData', next);
		};

		return () => (
			<div class="panel-right">
				<ElTabs stretch class="panel-right__tabs" type="border-card">
					<ElTabPane label="组件属性" class="panel-right__pane">
						<div class="panel-right__pane-body">
							{!activeData.value?.type ? (
								<div class="panel-right__placeholder">请选择画布中的组件</div>
							) : (
								<div class="panel-right__card panel-right__card--flat">
									<div class="panel-right__subhead">字段配置</div>
									<ElForm labelPosition="right" labelWidth={90} class="panel-right__form">
										<ElFormItem label="组件类型">
											<BaseSelect modelValue={activeData.value.type} {...({ options: typeOptions.value } as any)} disabled />
										</ElFormItem>
										<ElFormItem label="字段名">
											<ElInput modelValue={activeData.value.prop} onInput={(v) => updateField('prop', v)} />
										</ElFormItem>
										<ElFormItem label="标题">
											<ElInput modelValue={activeData.value.label} onInput={(v) => updateField('label', v)} />
										</ElFormItem>
										<ElFormItem label="占位提示">
											<ElInput
												modelValue={activeData.value.placeholder}
												onInput={(v) => updateField('placeholder', v)}
											/>
										</ElFormItem>
										<ElFormItem label="表单栅格">
											<Slider
												modelValue={activeData.value.span}
												onUpdate:modelValue={(v) => updateField('span', v)}
												{...({ min: 0, max: 24, marks: { 12: '12' } } as any)}
											/>
											<div class="panel-right__note">当前：{activeData.value.span || 0}</div>
										</ElFormItem>
									</ElForm>
									<ElDivider />
									<div class="panel-right__tip">修改会实时同步到画布和导出配置</div>
								</div>
							)}
						</div>
					</ElTabPane>
					<ElTabPane label="表单属性" class="panel-right__pane">
						<div class="panel-right__pane-body">
							<div class="panel-right__placeholder">可扩展：全局校验、布局等</div>
						</div>
					</ElTabPane>
				</ElTabs>
			</div>
		);
	}
});
