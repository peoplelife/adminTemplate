import { getV1MeMenus } from '@/api';
import { getAllBreadcrumbList, getFlatMenuList, getMenuTree, getShowMenuList } from '@/utils/menu';
import { defineStore } from 'pinia';
import type { AuthState } from '../type';
export const useAuthStore = defineStore('auth', {
	state: (): AuthState => ({
		//总菜单按钮权限列表
		authList: [],
		//按钮权限列表
		authButtonList: [],
		//菜单权限列表
		authMenuList: [],
		//所有菜单列表
		allMenuList: []
	}),
	getters: {
		// 菜单权限列表 ==> 这里的菜单没有经过任何处理
		authMenuListGet: (state) => state.authMenuList,
		// 菜单权限列表 ==> 扁平化之后的一维数组菜单，主要用来添加动态路由
		flatMenuListGet: (state) => getFlatMenuList(state.authMenuList),
		// 菜单权限列表 ==> 左侧菜单栏渲染，需要剔除 isHide == true
		showMenuListGet: (state) => getShowMenuList(state.authMenuList),
		// 递归处理后的所有面包屑导航列表
		breadcrumbListGet: (state) => getAllBreadcrumbList(state.authMenuList)
	},
	actions: {
		async getAuthButtonList() {
			this.authButtonList = this.authList
				.filter((item) => item.type === 'B' && item.buttonPermission)
				.map((item) => item.buttonPermission as string);
		},
		async getAuthMenuList() {
			this.authMenuList = getMenuTree(this.authList);
		},
		async getAuthList() {
			const { list } = await getV1MeMenus();
			this.authList = list;
			this.allMenuList = this.authList;
			this.getAuthMenuList();
			this.getAuthButtonList();
		}
	}
});
