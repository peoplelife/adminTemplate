import { useWaterMark } from '@/hooks/useWaterMark';
export default defineComponent({
	props: {
		text: {
			type: String,
			default: 'hwqAdminTemplate'
		},
		fontSize: {
			type: Number,
			default: 50
		},
		rotate: {
			type: Number,
			default: 50
		},
		opacity: {
			type: Number,
			default: 0.04
		}
	},
	setup(props, { slots }) {
		const { waterMarkBg } = useWaterMark(props);
		const waterParent = ref(null);
		let div;

		const resetWaterMark = () => {
			if (!waterParent.value) return;
			if (div) {
				waterParent.value.removeChild(div);
			}
			div = document.createElement('div');
			div.style.position = 'absolute';
			div.style.backgroundImage = `url("${waterMarkBg.value}")`;
			div.style.backgroundRepeat = 'repeat';
			div.style.pointerEvents = 'none';
			div.style.zIndex = '1000';
			div.style.inset = '0';
			waterParent.value.appendChild(div);
		};

		const ob = new MutationObserver((entires) => {
			for (const entry of entires) {
				for (const node of entry.removedNodes) {
					if (node === div) {
						resetWaterMark();
					}
				}
				if (entry.target === div) {
					resetWaterMark();
				}
			}
		});
		onMounted(() => {
			resetWaterMark();
			ob.observe(waterParent.value, {
				attributes: true, // 监听属性变化
				childList: true, // 监听子节点变化
				subtree: true // 监听后代节点变化
			});
		});
		onUnmounted(() => {
			ob.disconnect();
		});
		return () => (
			<>
				<div
					ref={waterParent}
					class="watermark-container"
					style={{
						display: 'flex',
						flex: '1 1 0',
						position: 'relative',
						width: '100%',
						height: '100%'
					}}
				>
					{slots.default?.()}
				</div>
			</>
		);
	}
});
