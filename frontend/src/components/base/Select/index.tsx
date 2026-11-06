import { ElSelect, ElOption } from 'element-plus';

export interface OptionProps {
	label: string;
	value: string;
	[key: string]: any;
}
export const BaseSelect = defineComponent({
	name: 'BaseSelect',
	props: {
		modelValue: {
			type: [String, Number, Boolean],
			default: ''
		}
	},
	emits: ['update:modelValue'],
	setup(props, { emit, attrs }) {
		const { modelValue } = toRefs(props);

		const { options, ...selectAttrs } = attrs as any;

		const value = computed({
			get: () => modelValue.value,
			set: (val) => {
				emit('update:modelValue', val);
			}
		});
		return () => (
			<ElSelect {...selectAttrs} v-model={value.value}>
				{options?.map((option: OptionProps) => {
					return <ElOption {...option} key={option.value} />;
				})}
			</ElSelect>
		);
	}
});
