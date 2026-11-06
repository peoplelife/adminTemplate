import { DEFAULT_PRIMARY } from '@/config';
import { defineStore } from 'pinia';
import piniaPersistConfig from '../helper/persist';
export const useGlobalStore = defineStore('global', {
	state: () => {
		return {
			isCollapse: false,
			isDark: false,
			primaryColor: DEFAULT_PRIMARY
		};
	},
	getters: {},
	actions: {
		setGlobalState(...args) {
			this.$patch({ [args[0]]: args[1] });
		}
	},
	persist: piniaPersistConfig('admin-template-global')
});
