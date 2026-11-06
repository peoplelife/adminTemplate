import { DEFAULT_PRIMARY } from '@/config';
import { useGlobalStore } from '@/stores/modules/global';

export const useTheme = () => {
	const globalStore = useGlobalStore();
	const { isDark, primaryColor } = storeToRefs(globalStore);
	const Switch = () => {
		const html = document.documentElement as HTMLElement;
		if (isDark.value) {
			console.log(isDark.value);
			html.setAttribute('class', 'dark');
		} else {
			html.setAttribute('class', '');
		}
	};
	const ChagePrimary = (color: string | null) => {
		if (!color) {
			color = DEFAULT_PRIMARY;
		}
		const html = document.documentElement as HTMLElement;
		html.style.setProperty('--el-color-primary', color);
		globalStore.setGlobalState('primaryColor', color);
	};
	const initTheme = () => {
		ChagePrimary(primaryColor.value);
		Switch();
	};
	return {
		initTheme,
		Switch,
		ChagePrimary
	};
};
