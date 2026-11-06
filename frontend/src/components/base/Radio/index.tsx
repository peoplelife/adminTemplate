import { ElRadioGroup, ElRadio } from 'element-plus';

export const Radio = defineComponent({
	props: {
		modelValue: {
			type: [String, Number],
			default: ''
		}
	},
	emits: ['update:modelValue'],
	setup(props, { attrs, emit }) {
		const { modelValue } = toRefs(props);
		const { options, ...radioAttrs } = attrs as any;
		const value = computed({
			get: () => modelValue.value,
			set: (val) => {
				emit('update:modelValue', val);
			}
		});
		const renderRadio = () => {
			return options.map((item) => {
				return (
					<ElRadio {...radioAttrs} value={item.value} key={item.value}>
						{item.label}
					</ElRadio>
				);
			});
		};
		return () => (
			<div class="w-full">
				<ElRadioGroup v-model={value.value}>{renderRadio()}</ElRadioGroup>
			</div>
		);
	}
});
