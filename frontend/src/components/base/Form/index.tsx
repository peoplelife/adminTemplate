import { ElCol, ElForm, ElFormItem, ElInput, ElInputNumber, ElRow } from 'element-plus';
import { computed, defineComponent, toRefs, ref, watch } from 'vue';
import { IconSelect } from '@/components/base/IconSelect';
import { Radio } from '@/components/base/Radio';
import { BaseSelect } from '@/components/base/Select';
import { TreeSelect } from '@/components/base/TreeSelect';
import { UpLoad } from '@/components/base/UpLoad';
import { BASE_COMPONENTS } from '../config';

export const Form = defineComponent({
	props: {
		mode: {
			type: String,
			default: 'edit'
		},
		searchSchema: {
			type: Object,
			default: () => ({})
		},
		modelValue: {
			type: Object,
			default: () => ({})
		}
	},
	emits: ['update:modelValue'],
	setup(props, { expose, emit }) {
		const { searchSchema, modelValue, mode } = toRefs(props);
		const formRef = ref();
		const localModel = ref({ ...modelValue.value });

		const attrs = computed(() => searchSchema.value.attrs);
		const formItems = computed(() => searchSchema.value.formItems ?? []);

		//渲染组件
		const renderFiled = (field) => {
			const Comp = BASE_COMPONENTS[field.type];
			const prop = field.prop;
			if (mode.value === 'detail') {
				return <div>{localModel.value[prop]}</div>;
			}

			if (!Comp) return null;

			return <Comp {...field.attrs} v-model={localModel.value[prop]} />;
		};

		const reset = () => {
			formRef.value.resetFields();
			emit('update:modelValue', { ...localModel.value });
		};
		const getModel = () => {
			return localModel.value;
		};
		const setModel = (model) => {
			localModel.value = { ...model };
		};

		watch(
			() => localModel.value,
			(newValue) => {
				emit('update:modelValue', { ...newValue });
			},
			{ deep: true }
		);
		expose({ reset, getModel, setModel, validate: () => formRef.value.validate() });
		return () => (
			<ElForm {...attrs.value} ref={formRef} model={localModel.value}>
				<ElRow gutter={20}>
					{formItems.value.length > 0 &&
						formItems.value.map((item) => {
							return (
								<ElCol {...item.col}>
									{item.fields.length > 0 &&
										item.fields.map((field) => {
											return (
												<ElFormItem {...item.attrs} prop={field.prop}>
													{renderFiled(field)}
												</ElFormItem>
											);
										})}
								</ElCol>
							);
						})}
				</ElRow>
			</ElForm>
		);
	}
});
