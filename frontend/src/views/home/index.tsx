import DataScreenContainer from '@/components/DataScreenContainer';
import ECharts from '@/components/Echarts';
import router from '@/routers';
import { ElButton } from 'element-plus';

export default defineComponent({
	setup() {
		const option = ref({
			title: {
				text: 'ECharts 入门示例'
			},
			tooltip: {},
			xAxis: {
				data: ['衬衫', '羊毛衫', '雪纺衫', '裤子', '高跟鞋', '袜子']
			},
			yAxis: {},
			series: [
				{
					name: '销量',
					type: 'bar',
					data: [5, 20, 36, 10, 10, 20]
				}
			]
		});

		return () => (
			<div style={{ display: 'flex', gap: '12px' }}>
				<ElButton
					onClick={() => {
						router.push('/dataScreen');
					}}
				>
					123
				</ElButton>
				<ElButton type="primary" onClick={() => router.push('/smartArmsScreen')}>
					智慧武装管理服务平台
				</ElButton>
			</div>
		);
	}
});
