import { ElSlider } from 'element-plus';

export const Slider = defineComponent({
	props: {
		modelValue: {
			type: Number,
			default: 0
		}
	},
	emits: ['update:modelValue'],
	setup(props, { attrs, emit }) {
		const { modelValue } = toRefs(props);
		const value = computed({
			get: () => modelValue.value,
			set: (val) => {
				emit('update:modelValue', val);
			}
		});
		return () => (
			<div class="w-full">
				<ElSlider {...attrs} v-model={value.value}></ElSlider>
			</div>
		);
	}
});
