import { useUserStore } from '@/stores/modules/user';
import { useTabsStore } from '@/stores/modules/tabs';
import { useGlobalStore } from '@/stores/modules/global';
import { useAuthStore } from '@/stores/modules/auth';
export default function resetAll() {
	const userStore = useUserStore();
	const tabsStore = useTabsStore();
	const globalStore = useGlobalStore();
	const authStore = useAuthStore();
	userStore.$reset();
	tabsStore.$reset();
	globalStore.$reset();
	authStore.$reset();

	//清除所有的持久化
	localStorage.clear();
}
