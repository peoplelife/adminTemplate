import NProgress from '@/config/nprogress';
import { staticRouter } from './modules/staticRouters';
import { useAuthStore } from '@/stores/modules/auth';
import { initDynamicRouters } from './modules/dynamicRouters';
import { LOGIN_URL } from '@/config';
import { useUserStore } from '@/stores/modules/user';

const router = createRouter({
	history: createWebHashHistory(),
	routes: [...staticRouter]
});

//路由拦截
router.beforeEach(async (to, from, next) => {
	const authStore = useAuthStore();
	const userStore = useUserStore();
	NProgress.start();
	//判断访问路由是否为/login,有token就在当前页，没有则重置路由到登录页
	if (to.path.toLocaleLowerCase() === '/login') {
		if (userStore.token) return next(from.fullPath);
		return next();
	}
	// 判断是否有 Token，没有重定向到 login 页面
	if (!userStore.token) return next({ path: LOGIN_URL, replace: true });

	// 如果没有菜单列表，就重新请求菜单列表并添加动态路由
	if (!authStore.authMenuListGet.length) {
		await initDynamicRouters();
		return next({ ...to, replace: true });
	}
	//正常访问页面
	next();
});
//路由跳转结束
router.onError((error) => {
	NProgress.done();
	console.log('路由错误', error.message);
});
router.afterEach(() => {
	NProgress.done();
});
export const setup = (app) => {
	app.use(router);
};
export default router;
