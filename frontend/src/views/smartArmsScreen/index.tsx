import { defineComponent, onBeforeUnmount, onMounted, ref } from 'vue';
import { Aim, ArrowLeft, BellFilled, ChatDotRound, Headset, Minus, Plus, Search, UserFilled } from '@element-plus/icons-vue';
import { ElButton } from 'element-plus';
import { BorderBox1, BorderBox8 } from '@kjgl77/datav-vue3';
import ECharts from '@/components/Echarts';
import router from '@/routers';
import '@kjgl77/datav-vue3/dist/style.css';
import './index.scss';

const communityOption = {
	animation: false,
	grid: { left: 32, right: 12, top: 20, bottom: 22 },
	tooltip: { trigger: 'axis' },
	xAxis: {
		type: 'category',
		data: ['社区1', '社区2', '社区3', '社区4', '社区5', '社区6'],
		axisLine: { lineStyle: { color: '#235278' } },
		axisTick: { show: false },
		axisLabel: { color: '#8baecb', fontSize: 10 }
	},
	yAxis: {
		type: 'value',
		max: 100,
		axisLabel: { color: '#83a2c0', fontSize: 9 },
		axisLine: { show: false },
		axisTick: { show: false },
		splitLine: { lineStyle: { color: 'rgba(60, 124, 168, .2)', type: 'dashed' } }
	},
	series: [
		{
			type: 'bar',
			barWidth: 15,
			data: [38, 58, 58, 54, 58, 58],
			itemStyle: { color: '#18d5df', borderRadius: [4, 4, 0, 0] },
			label: { show: true, position: 'top', color: '#a1f6fa', fontSize: 9 }
		}
	]
};

const monthlyOption = {
	animation: false,
	grid: { left: 32, right: 12, top: 16, bottom: 24 },
	tooltip: { trigger: 'axis' },
	xAxis: {
		type: 'category',
		boundaryGap: false,
		data: ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12'],
		axisLine: { lineStyle: { color: '#235278' } },
		axisTick: { show: false },
		axisLabel: { color: '#8baecb', fontSize: 9 }
	},
	yAxis: {
		type: 'value',
		max: 100,
		axisLabel: { color: '#83a2c0', fontSize: 9 },
		axisLine: { show: false },
		axisTick: { show: false },
		splitLine: { lineStyle: { color: 'rgba(60, 124, 168, .2)', type: 'dashed' } }
	},
	series: [
		{
			type: 'line',
			smooth: true,
			symbol: 'circle',
			symbolSize: 4,
			data: [30, 48, 60, 55, 41, 45, 39, 54, 78, 74, 61, 34],
			lineStyle: { color: '#27d8e5', width: 2 },
			itemStyle: { color: '#7af4ed' },
			areaStyle: { color: 'rgba(24, 177, 207, .15)' }
		}
	]
};

const enlistmentOption = {
	animation: false,
	grid: { left: 30, right: 10, top: 20, bottom: 22 },
	tooltip: { trigger: 'axis' },
	xAxis: {
		type: 'category',
		data: ['社区1', '社区2', '社区3', '社区4', '社区5', '社区6'],
		axisLine: { lineStyle: { color: '#235278' } },
		axisTick: { show: false },
		axisLabel: { color: '#8baecb', fontSize: 9 }
	},
	yAxis: {
		type: 'value',
		max: 100,
		axisLabel: { color: '#83a2c0', fontSize: 9 },
		axisLine: { show: false },
		axisTick: { show: false },
		splitLine: { lineStyle: { color: 'rgba(60, 124, 168, .2)', type: 'dashed' } }
	},
	series: [
		{
			name: '报名人数',
			type: 'bar',
			barWidth: 8,
			data: [36, 56, 61, 52, 50, 45],
			itemStyle: { color: '#16cadb', borderRadius: [3, 3, 0, 0] }
		},
		{
			name: '初检人数',
			type: 'bar',
			barWidth: 8,
			data: [24, 32, 38, 35, 29, 31],
			itemStyle: { color: '#f3b52c', borderRadius: [3, 3, 0, 0] }
		}
	]
};

const processSteps = ['网上报名', '初检初核', '体格检查', '政治考核', '役前训练', '预定新兵', '张榜公示', '批准入伍'];
const focusMetrics = [
	{ label: '登记人数', value: '38' },
	{ label: '应征人数', value: '38' },
	{ label: '实查人数', value: '38' },
	{ label: '登记率', value: '50%' }
];

export default defineComponent({
	setup() {
		const mapScale = ref(1);
		const screenRef = ref<HTMLElement>();
		const DESIGN_WIDTH = 1980;
		const DESIGN_HEIGHT = 1080;
		let previousHtmlOverflow = '';
		let previousBodyOverflow = '';
		const scaleScreen = () => {
			if (!screenRef.value) return;
			const scale = Math.min(window.innerWidth / DESIGN_WIDTH, window.innerHeight / DESIGN_HEIGHT);
			screenRef.value.style.transform = `scale(${scale}) translate(-50%, -50%)`;
		};

		onMounted(() => {
			previousHtmlOverflow = document.documentElement.style.overflow;
			previousBodyOverflow = document.body.style.overflow;
			document.documentElement.style.overflow = 'hidden';
			document.body.style.overflow = 'hidden';
			if (screenRef.value) {
				screenRef.value.style.width = `${DESIGN_WIDTH}px`;
					screenRef.value.style.height = `${DESIGN_HEIGHT}px`;
			}
			scaleScreen();
			window.addEventListener('resize', scaleScreen);
		});

		onBeforeUnmount(() => {
			window.removeEventListener('resize', scaleScreen);
			document.documentElement.style.overflow = previousHtmlOverflow;
			document.body.style.overflow = previousBodyOverflow;
		});

		return () => (
			<div class="smart-arms-screen" ref={screenRef}>
				<header class="screen-header">
					<div class="header-left">
						<nav class="screen-nav" aria-label="大屏栏目">
							<span>综合态势</span>
							<span>党建管理</span>
							<span>潜力态势</span>
							<span class="active">兵役状态</span>
							<span>民兵态势</span>
						</nav>
					</div>
					<div class="header-title">
						<span class="title-rule" />
						<h1>智慧武装管理服务平台</h1>
						<span class="title-rule" />
					</div>
					<div class="header-right">
						<div class="screen-date">
							<span>星期一</span>
							<strong>2024-08-05</strong>
						</div>
						<span class="header-divider" />
						<div class="user-status"><BellFilled />值班</div>
						<div class="user-status"><UserFilled />你好，管理员</div>
						<ElButton class="exit-screen" text onClick={() => router.push('/home')} aria-label="返回管理端">
							<ArrowLeft />
						</ElButton>
					</div>
				</header>

				<main class="dashboard-grid">
					<aside class="side-column left-column">
						<section class="dashboard-panel registration-panel">
							<BorderBox1 class="panel-frame" color={['#13578f', '#12d4f0']} backgroundColor="rgba(4, 20, 49, .68)">
								<div class="panel-content">
									<div class="panel-heading"><span class="heading-mark" /><h2>兵役登记</h2><span class="year-chip">2025 <span>⌄</span></span></div>
									<div class="sub-heading">重点人群</div>
									<div class="focus-metrics">
										{focusMetrics.map((metric) => (
											<div class="metric-item" key={metric.label}>
												<div class="metric-value">{metric.value}</div>
												<span>{metric.label}</span>
											</div>
										))}
									</div>
									<div class="sub-heading chart-heading">社区登记</div>
									<div class="chart-area"><ECharts options={communityOption} /></div>
								</div>
							</BorderBox1>
						</section>

						<section class="dashboard-panel monthly-panel">
							<BorderBox1 class="panel-frame" color={['#13578f', '#12d4f0']} backgroundColor="rgba(4, 20, 49, .68)">
								<div class="panel-content">
									<div class="panel-heading"><span class="heading-mark" /><h2>分月登记</h2><span class="panel-unit">人</span></div>
									<div class="chart-area line-chart"><ECharts options={monthlyOption} /></div>
								</div>
							</BorderBox1>
						</section>

						<section class="dashboard-panel service-panel">
							<BorderBox8 class="panel-frame" color={['#155a8e', '#10d7f1']} backgroundColor="rgba(4, 20, 49, .68)">
								<div class="panel-content">
									<div class="panel-heading"><span class="heading-mark" /><h2>上门服务</h2></div>
									<div class="service-metrics">
										<div class="service-metric"><span class="service-icon"><Headset /></span><div><small>上门服务</small><strong>380</strong></div></div>
										<div class="service-metric"><span class="service-icon"><ChatDotRound /></span><div><small>短信服务</small><strong>1000</strong></div></div>
									</div>
								</div>
							</BorderBox8>
						</section>
					</aside>

					<section class="center-column">
						<div class="process-flow" aria-label="征兵流程">
							{processSteps.map((step, index) => (
								<div class={{ 'process-step': true, current: index === 0, complete: index < 2 }} key={step}>
									<span class="step-number">{index + 1}</span>
									<strong>{step}</strong>
									<small>{index === 0 ? '5000人' : index === 1 ? '100人' : '0'}</small>
								</div>
							))}
						</div>

						<div class="map-stage">
							<div class="map-graticule" />
							<svg class="district-map" style={{ transform: `scale(${mapScale.value})` }} viewBox="0 0 360 500" role="img" aria-label="辖区地图和服务站点">
								<defs>
									<linearGradient id="district-fill" x1="0" y1="0" x2="1" y2="1">
										<stop offset="0" stop-color="#7b8f67" />
										<stop offset=".48" stop-color="#476650" />
										<stop offset="1" stop-color="#6b8060" />
									</linearGradient>
									<pattern id="field-grid" width="42" height="34" patternUnits="userSpaceOnUse" patternTransform="rotate(-18)">
										<path d="M0 0H42M0 0V34" fill="none" stroke="#b5b48a" stroke-opacity=".18" stroke-width="2" />
										<path d="M10 5L38 2M2 24L28 31" fill="none" stroke="#293f37" stroke-opacity=".34" stroke-width="5" />
									</pattern>
									<clipPath id="district-clip">
										<path d="M171 17L196 27L220 25L221 53L244 63L239 83L258 99L250 124L270 143L259 167L276 190L268 216L248 231L258 255L244 276L253 303L231 322L227 350L205 365L198 393L177 400L165 426L144 430L130 456L108 451L95 475L75 462L56 469L49 445L29 439L38 416L25 396L42 376L36 350L54 330L48 309L64 288L57 265L75 247L72 224L91 208L87 184L108 168L104 144L122 128L117 104L138 91L133 68L152 58L147 39L167 35Z" />
									</clipPath>
									<filter id="map-glow" x="-40%" y="-20%" width="180%" height="140%">
										<feGaussianBlur stdDeviation="4" result="blur" />
										<feMerge><feMergeNode in="blur" /><feMergeNode in="SourceGraphic" /></feMerge>
									</filter>
								</defs>
								<g clip-path="url(#district-clip)">
									<rect x="10" y="10" width="280" height="480" fill="url(#district-fill)" />
									<rect x="10" y="10" width="280" height="480" fill="url(#field-grid)" />
									<path d="M24 384C76 355 75 329 132 313S208 288 264 235M32 423C89 398 112 365 152 347S204 324 260 278M70 105C110 145 150 166 186 205S218 244 257 263M87 185C133 199 158 228 193 249S228 268 263 298M46 317C99 297 116 270 147 246S196 213 249 178" fill="none" stroke="#d8c99b" stroke-opacity=".76" stroke-width="5" />
									<path d="M24 384C76 355 75 329 132 313S208 288 264 235M32 423C89 398 112 365 152 347S204 324 260 278M70 105C110 145 150 166 186 205S218 244 257 263M87 185C133 199 158 228 193 249S228 268 263 298M46 317C99 297 116 270 147 246S196 213 249 178" fill="none" stroke="#f7e0a3" stroke-opacity=".42" stroke-width="1.3" />
									<path d="M93 25L113 83L104 132L132 170L120 216L153 252L146 304L180 347L163 404L193 472M219 35L196 83L215 122L184 157L209 198L173 238L203 273L177 313L200 362L172 411" fill="none" stroke="#30483e" stroke-opacity=".55" stroke-width="11" />
									<path d="M93 25L113 83L104 132L132 170L120 216L153 252L146 304L180 347L163 404L193 472M219 35L196 83L215 122L184 157L209 198L173 238L203 273L177 313L200 362L172 411" fill="none" stroke="#6b7965" stroke-opacity=".85" stroke-width="6" />
									<path d="M13 278L270 238M35 146L261 183M40 360L237 350" fill="none" stroke="#e2c984" stroke-opacity=".58" stroke-width="3" />
								</g>
								<path d="M171 17L196 27L220 25L221 53L244 63L239 83L258 99L250 124L270 143L259 167L276 190L268 216L248 231L258 255L244 276L253 303L231 322L227 350L205 365L198 393L177 400L165 426L144 430L130 456L108 451L95 475L75 462L56 469L49 445L29 439L38 416L25 396L42 376L36 350L54 330L48 309L64 288L57 265L75 247L72 224L91 208L87 184L108 168L104 144L122 128L117 104L138 91L133 68L152 58L147 39L167 35Z" fill="none" stroke="#04f0e8" stroke-width="5" stroke-linejoin="round" filter="url(#map-glow)" />
								<text x="99" y="185" class="map-place-label">洪秀全故居</text>
								<text x="148" y="260" class="map-place-label">团山街道</text>
								<text x="77" y="326" class="map-place-label small">民兵服务站</text>
								<text x="175" y="136" class="map-place-label small">社区服务点</text>
								<text x="88" y="387" class="road-label">S118</text>
								<g class="map-pin" transform="translate(160 116)"><path d="M0 13L-8 1L8 1Z" /><circle cy="0" r="4" /></g>
								<g class="map-pin" transform="translate(220 194)"><path d="M0 13L-8 1L8 1Z" /><circle cy="0" r="4" /></g>
								<g class="map-pin" transform="translate(122 292)"><path d="M0 13L-8 1L8 1Z" /><circle cy="0" r="4" /></g>
								<g class="map-pin" transform="translate(92 404)"><path d="M0 13L-8 1L8 1Z" /><circle cy="0" r="4" /></g>
								<g class="map-pin" transform="translate(205 347)"><path d="M0 13L-8 1L8 1Z" /><circle cy="0" r="4" /></g>
							</svg>
							<div class="map-coordinate">辖区范围 · 武装工作服务点</div>
							<div class="map-zoom-controls">
								<ElButton circle aria-label="放大地图" onClick={() => (mapScale.value = Math.min(1.5, mapScale.value + 0.1))}><Plus /></ElButton>
								<ElButton circle aria-label="缩小地图" onClick={() => (mapScale.value = Math.max(0.8, mapScale.value - 0.1))}><Minus /></ElButton>
								<ElButton circle aria-label="重置地图缩放" onClick={() => (mapScale.value = 1)}><Aim /></ElButton>
							</div>
							<div class="map-search"><span>辖区 / 服务网格</span><Search /></div>
						</div>
					</section>

					<aside class="side-column right-column">
						<section class="dashboard-panel enlistment-panel">
							<BorderBox1 class="panel-frame" color={['#13578f', '#12d4f0']} backgroundColor="rgba(4, 20, 49, .68)">
								<div class="panel-content">
									<div class="panel-heading"><span class="heading-mark" /><h2>征兵报名</h2><span class="year-chip">2025 <span>⌄</span></span></div>
									<div class="sub-heading">征集概览</div>
									<table class="summary-table">
										<thead><tr><th>项目</th><th>春季</th><th>秋季</th></tr></thead>
										<tbody>
											<tr><th>年度征兵目标</th><td>100</td><td>100</td></tr>
											<tr><th>征集应征人数</th><td>90</td><td>80</td></tr>
											<tr><th>完成率</th><td>90%</td><td>80%</td></tr>
										</tbody>
									</table>
									<div class="sub-heading chart-heading">报名入伍</div>
									<div class="chart-key"><span><i class="legend-cyan" />报名人数</span><span><i class="legend-yellow" />初检人数</span></div>
									<div class="chart-area enlistment-chart"><ECharts options={enlistmentOption} /></div>
								</div>
							</BorderBox1>
						</section>

						<section class="dashboard-panel education-panel">
							<BorderBox1 class="panel-frame" color={['#13578f', '#12d4f0']} backgroundColor="rgba(4, 20, 49, .68)">
								<div class="panel-content">
									<div class="panel-heading"><span class="heading-mark" /><h2>学历分布</h2><span class="panel-unit">人</span></div>
									<div class="education-content">
										<div class="education-ring"><div class="ring-center"><strong>100</strong><span>总人数</span></div></div>
										<div class="education-legend">
											<div><i class="legend-cyan" /><span>博士及以上</span><strong>15</strong></div>
											<div><i class="legend-green" /><span>大学本科</span><strong>34</strong></div>
											<div><i class="legend-yellow" /><span>中等职业</span><strong>29</strong></div>
											<div><i class="legend-orange" /><span>普通高中</span><strong>22</strong></div>
										</div>
									</div>
								</div>
							</BorderBox1>
						</section>

						<section class="dashboard-panel service-panel right-service-panel">
							<BorderBox8 class="panel-frame" color={['#155a8e', '#10d7f1']} backgroundColor="rgba(4, 20, 49, .68)">
								<div class="panel-content">
									<div class="panel-heading"><span class="heading-mark" /><h2>上门服务</h2></div>
									<div class="service-metrics">
										<div class="service-metric"><span class="service-icon"><Headset /></span><div><small>上门服务</small><strong>380</strong></div></div>
										<div class="service-metric"><span class="service-icon"><ChatDotRound /></span><div><small>短信服务</small><strong>1000</strong></div></div>
									</div>
								</div>
							</BorderBox8>
						</section>
					</aside>
				</main>
			</div>
		);
	}
});
