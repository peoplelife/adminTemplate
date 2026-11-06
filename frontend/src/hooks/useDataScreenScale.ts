export const useDataScreenScale = () => {
	// 1. 创建一个 DOM 引用，用于获取大屏内容容器
	const dataScreenRef = ref<HTMLElement>();

	// 2. 定义设计稿宽度，项目按照 1920px 开发
	// 3. 定义设计稿高度，项目按照 1080px 开发
	const WIDTH = 1920;
	const HEIGHT = 1080;
	// 4. 定义计算缩放比例的方法
	// 4.1 获取当前浏览器可视区域宽度
	// 4.2 获取当前浏览器可视区域高度
	// 4.3 使用浏览器宽度除以设计稿宽度，计算宽度缩放比例
	// 4.4 使用浏览器高度除以设计稿高度，计算高度缩放比例
	// 4.5 取宽高缩放比例中的较小值，保证大屏完整显示
	// 4.6 返回最终缩放比例
	const getScale = (width = WIDTH, height = HEIGHT) => {
		const clientWidth = window.innerWidth / width;
		const clientHeight = window.innerHeight / height;
		return clientWidth < clientHeight ? clientWidth : clientHeight;
	};

	// 5. 定义更新大屏缩放的方法
	// 5.1 判断大屏 DOM 是否存在，不存在则结束执行
	// 5.2 调用缩放比例计算方法，获取最新比例
	// 5.3 通过 transform 将缩放比例设置到大屏容器
	// 5.4 使用 translate 将大屏容器保持在页面中心
	const scaleDataScreen = () => {
		if (!dataScreenRef.value) return;
		dataScreenRef.value.style.transform = `scale(${getScale()}) translate(-50%,-50%)`;
	};

	// 6. 页面挂载完成后执行初始化
	// 6.1 判断大屏 DOM 是否已经挂载
	// 6.2 将大屏容器宽度设置为设计稿宽度
	// 6.3 将大屏容器高度设置为设计稿高度
	// 6.4 主动调用一次更新方法，完成页面首次缩放
	// 6.5 监听浏览器 resize 事件，窗口变化时重新计算缩放比例
	onMounted(() => {
		if (dataScreenRef.value) {
			dataScreenRef.value.style.transform = `scale(${getScale()}) translate(-50%, -50%)`;
			dataScreenRef.value.style.width = `${WIDTH}px`;
			dataScreenRef.value.style.height = `${HEIGHT}px`;
		}
		window.addEventListener('resize', scaleDataScreen);
	});

	// 7. 组件销毁前执行清理
	// 7.1 移除浏览器 resize 事件监听，避免重复监听和内存泄漏
	onBeforeUnmount(() => {
		window.removeEventListener('resize', scaleDataScreen);
	});

	// 8. 返回大屏 DOM 引用，供页面组件绑定到大屏容器
	return {
		dataScreenRef
	};
};
