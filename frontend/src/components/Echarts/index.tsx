import type { EChartsType } from 'echarts/core';
import echarts from './config';

export default defineComponent({
	props: {
		options: {
			type: Object,
			default: () => ({})
		}
	},
	setup(props, { expose }) {
		const { options } = toRefs(props);
		const echartsRef = ref();
		const chartInstance = ref<EChartsType>();
		const handleResize = () => {
			if (chartInstance.value) {
				chartInstance.value.resize({ animation: { duration: 300 } });
			}
		};
		const init = () => {
			if (!echartsRef.value) return;
			chartInstance.value = echarts.getInstanceByDom(echartsRef.value);
			if (!chartInstance.value) {
				chartInstance.value = echarts.init(echartsRef.value);
			}

			chartInstance.value.setOption(options.value);
		};
		const getInstance = () => chartInstance.value;
		onMounted(() => {
			nextTick(() => init());
			window.addEventListener('resize', handleResize);
		});
		onActivated(() => {
			if (chartInstance.value) {
				chartInstance.value.resize();
			}
		});
		onBeforeUnmount(() => {
			window.removeEventListener('resize', handleResize);
			chartInstance.value?.dispose();
		});
		expose({
			getInstance
		});
		return () => <div ref={echartsRef} class="w-full h-full"></div>;
	}
});
