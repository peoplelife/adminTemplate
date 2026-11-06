import { defineConfig, presetAttributify, presetUno } from 'unocss';
import presetRemToPx from '@unocss/preset-rem-to-px';

export default defineConfig({
	/** 预设 */
	presets: [
		/** 属性化模式 & 无值的属性模式 */
		presetAttributify(),
		/** 默认预设 */
		presetUno(),
		/** 像素不进行rem换算，保持原始px */
		presetRemToPx({
			baseFontSize: 4
		})
	]
});
