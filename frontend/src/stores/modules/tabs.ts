import { defineStore } from 'pinia';
import piniaPersistConfig from '../helper/persist';
import router from '@/routers';
export const useTabsStore = defineStore('tabs', {
	state: () => {
		return {
			tabsList: []
		};
	},
	getters: {},
	actions: {
		addTabs(tabsItem) {
			if (this.tabsList.every((item) => item.path !== tabsItem.path)) {
				this.tabsList.push(tabsItem);
			}
		},
		removeTabs(fullPath: string, isCurrent: boolean) {
			if (isCurrent) {
				this.tabsList.forEach((item, index) => {
					if (item.path !== fullPath) return;
					const nextTab = this.tabsList[index + 1] || this.tabsList[index - 1];
					if (!nextTab) return;
					router.push(nextTab.path);
				});
			}
			this.tabsList = this.tabsList.filter((item) => item.path !== fullPath);
		}
	},
	persist: piniaPersistConfig('admin-template-tabs')
});
