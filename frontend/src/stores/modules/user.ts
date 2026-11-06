import { defineStore } from 'pinia';
import piniaPersistConfig from '../helper/persist';
export const useUserStore = defineStore('user', {
	state: () => {
		return {
			token: '',
			userInfo: {}
		};
	},
	getters: {},
	actions: {
		setToken(token: string) {
			this.token = token;
		},
		setUserInfo(userInfo: object) {
			this.userInfo = userInfo;
		}
	},
	persist: piniaPersistConfig('admin-template-user')
});
