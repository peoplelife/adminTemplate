import echarts from '@/components/Echarts/config/index';
import chinaJson from './china.json';
import './index.scss';
import Echarts from '@/components/Echarts';
export default defineComponent({
	setup() {
		echarts.registerMap('china', chinaJson as any);
		const option: echarts.EChartsCoreOption = {
			title: {
				text: '中国各省数据分布',
				left: 'center',
				textStyle: { color: '#fff', fontSize: 18 }
			},
			tooltip: {
				trigger: 'item',
				formatter: (params: any) => {
					const value = params.value;
					return `${params.name}<br/>数值：${isNaN(value) ? '无数据' : value}`;
				}
			},
			visualMap: {
				min: 0,
				max: 1000,
				left: 'left',
				bottom: 'bottom',
				text: ['高', '低'],
				textStyle: { color: '#fff' },
				inRange: {
					color: ['#e0f3f8', '#4575b4']
				},
				calculable: true
			},
			series: [
				{
					type: 'map',
					map: 'china',
					roam: true, // 开启缩放和平移
					zoom: 1.2, // 初始缩放比例
					label: {
						show: true,
						fontSize: 10,
						color: '#333'
					},
					itemStyle: {
						areaColor: '#1a3a5c', // 默认区域颜色
						borderColor: '#4a90d9',
						borderWidth: 1
					},
					emphasis: {
						label: { show: true, color: '#fff' },
						itemStyle: { areaColor: '#ffd700' }
					},
					data: [{ name: '中华人民共和国', value: 1000 }]
				}
			]
		};
		// onMounted(() => {
		// 	initMapChart();
		// });
		return () => (
			<div class="map">
				<Echarts options={option} />
			</div>
		);
	}
});
