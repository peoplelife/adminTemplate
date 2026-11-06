import { useAuthStore } from '@/stores/modules/auth';
import { type RouteRecordRaw } from 'vue-router';
import router from '@/routers/index';
import { ElNotification } from 'element-plus';
import { useUserStore } from '@/stores/modules/user';
import { LOGIN_URL } from '@/config';
// 引入 views 文件夹下所有 vue,tsx 文件
const vueFiles = import.meta.glob('@/views/**/*.vue');
const tsxFiles = import.meta.glob('@/views/**/*.tsx');
/**
 * @description 初始化动态路由
 */
export const initDynamicRouters = async () => {
	const authStore = useAuthStore();
	const userStore = useUserStore();
	try {
		await authStore.getAuthList();
		if (!authStore.authMenuListGet) {
			ElNotification({
				title: '无权限访问',
				message: '当前账号无任何菜单权限，请联系系统管理员！',
				type: 'warning',
				duration: 3000
			});
			userStore.setToken('');
			router.replace(LOGIN_URL);
			return Promise.reject('无权限');
		}
		//添加动态路由
		authStore.flatMenuListGet.forEach((item) => {
			//扁平化不需要子数组
			if (item.children) delete item.children;
			if (item.component && typeof item.component === 'string') {
				const component = vueFiles['/src/views' + item.component + '.vue'] ?? tsxFiles['/src/views' + item.component + '.tsx'];
				item.component = component;
			}
			//将不是一级路由的进行嵌套在layout中
			if (item.meta.is_full) {
				router.addRoute(item as unknown as RouteRecordRaw);
			} else {
				router.addRoute('layout', item as unknown as RouteRecordRaw);
			}
		});
	} catch (error) {
		return Promise.reject(error);
	}
};
