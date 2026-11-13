import { getV1Me } from '@/api';
import { HOME_URL } from '@/config';
import router from '@/routers';
import { initDynamicRouters } from '@/routers/modules/dynamicRouters';
import { useAuthStore } from '@/stores/modules/auth';
import { useUserStore } from '@/stores/modules/user';
import { getTimeState } from '@/utils/time';
import { ElNotification } from 'element-plus';
import type { Router } from 'vue-router';

const getFirstMenuPath = (menu?: Menu.MenuOptions): string | undefined => {
	if (menu?.children?.length) return getFirstMenuPath(menu.children[0]);
	return menu?.path;
};

export const getLoginDestination = (menus: Menu.MenuOptions[]): string => {
	const destination = menus.some((menu) => menu.path === HOME_URL) ? HOME_URL : getFirstMenuPath(menus[0]);
	if (!destination) throw new Error('当前账号无任何菜单权限，请联系系统管理员！');
	return destination;
};

export const finishLogin = async (token: string, targetRouter: Router = router) => {
	const userStore = useUserStore();
	const authStore = useAuthStore();
	try {
		if (!token) throw new Error('登录未返回有效令牌，请重试');
		userStore.setToken(token);
		userStore.setUserInfo(await getV1Me());
		await initDynamicRouters();
		await targetRouter.replace(getLoginDestination(authStore.showMenuListGet));
		ElNotification({
			title: getTimeState(),
			message: '欢迎登录后台模板系统',
			type: 'success',
			duration: 3000
		});
	} catch (error) {
		userStore.setToken('');
		userStore.setUserInfo({});
		authStore.$reset();
		throw error;
	}
};
