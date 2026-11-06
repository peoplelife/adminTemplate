import { HOME_URL, LOGIN_URL } from '@/config';
import type { RouteRecordRaw } from 'vue-router';

/**
 * staticRouter (静态路由)
 */
export const staticRouter: RouteRecordRaw[] = [
	{
		path: '/',
		redirect: HOME_URL
	},
	{
		path: '/dataScreen',
		name: 'dataScreen',
		component: () => import('@/views/dataScreen/index.tsx'),
		meta: {
			title: '数据大屏'
		}
	},
	{
		path: '/smartArmsScreen',
		name: 'smartArmsScreen',
		component: () => import('@/views/smartArmsScreen/index.tsx'),
		meta: {
			title: '智慧武装管理服务平台'
		}
	},
	{
		path: LOGIN_URL,
		name: 'login',
		component: () => import('@/views/login/index.tsx'),
		meta: {
			title: '登录'
		}
	},
	{
		path: '/404',
		name: '404',
		component: () => import('@/views/error/index.tsx'),
		meta: {
			title: '错误'
		}
	},
	{
		path: '/layout',
		name: 'layout',
		component: () => import('@/layouts/index.tsx'),
		redirect: HOME_URL,
		children: []
	}
];
