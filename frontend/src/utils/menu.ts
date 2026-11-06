import type { IGetV1MeMenusRes } from '@/api';
type MenuItem = IGetV1MeMenusRes['list'][number];
type TAccountMenuResItem = MenuItem;
type TGetV1AccountMenuResItem = TAccountMenuResItem & { children?: TAccountMenuResItem[] };
/**
 * @description 创建菜单树
 */
export const getMenuTree = (menuList: MenuItem[]) => {
	const menus = menuList.filter((item) => ['M', 'C'].includes(item.type));
	const tree = buildRouteMenuTree(menus);
	const routes = transformRoutes(tree);
	sortMenu(routes);
	return routes;
};

/**
 * @description 创建路由树
 */
export const buildRouteMenuTree = (menuList: TGetV1AccountMenuResItem[], parentId = '-1') => {
	const tree = [] as TGetV1AccountMenuResItem[];
	menuList.forEach((item) => {
		if (String(item.parentId) === String(parentId)) {
			//递归查找子菜单
			const children = buildRouteMenuTree(menuList, item.menuId);
			if (children && children.length) item['children'] = children;
			//去除响应特性保留原始数据
			tree.push(toRaw(item));
		}
	});
	return tree;
};

/**
 * @description 转换成vue-router可识别的路由集合
 * @param {TGetAccountMenusResItem[]} menuList
 * @returns {Menu.MenuOptions[]} 返回未经过排序的路由表
 */
const transformRoutes = (menuList: TGetV1AccountMenuResItem[]) => {
	return menuList.map((menu) => {
		// 创建新的菜单对象，重新命名字段
		const routeOpts = {
			path: menu.path,
			name: menu.name,
			component: menu.component,
			menuId: menu.menuId,
			meta: {
				icon: menu.icon,
				hidden: menu.hidden,
				is_full: menu.isFull === 1,
				is_hidden: menu.hidden === 1 ? true : false,
				sort: menu.sort
			}
		} as Menu.MenuOptions;

		// 递归处理子菜单
		if (menu.children && menu.children.length > 0) {
			routeOpts.children = transformRoutes(menu.children);
		}

		return routeOpts;
	});
};

/**
 * @description 菜单排序，数值越小越靠前
 * @param menuList 未经过排序的菜单树
 * @returns { Menu.MenuOptions[] } 经过排序的路由表
 */
const sortMenu = (menuList: Menu.MenuOptions[]) => {
	// 对当前层级的菜单项进行排序
	menuList.sort((a, b) => a.meta.sort - b.meta.sort);

	// 递归处理子菜单
	menuList.forEach((menu) => {
		if (menu.children && menu.children.length > 0) {
			sortMenu(menu.children);
		}
	});
};
/**
 * @description 使用递归扁平化菜单，方便添加动态路由
 * @param {Array} menuList 菜单列表
 * @returns {Array}
 */
export function getFlatMenuList(menuList: Menu.MenuOptions[]): Menu.MenuOptions[] {
	const newMenuList: Menu.MenuOptions[] = JSON.parse(JSON.stringify(menuList));
	return newMenuList.flatMap((item) => [item, ...(item.children ? getFlatMenuList(item.children) : [])]);
}

/**
 * @description 使用递归过滤出需要渲染在左侧菜单的列表 (需剔除 isHide == true 的菜单)
 * @param {Array} menuList 菜单列表
 * @returns {Array}
 * */
export function getShowMenuList(menuList: Menu.MenuOptions[]) {
	const newMenuList: Menu.MenuOptions[] = JSON.parse(JSON.stringify(menuList));
	return newMenuList.filter((item) => {
		if (item.children?.length) item.children = getShowMenuList(item.children);
		return item.meta?.hidden === 0;
	});
}
/**
 * @description 使用递归找出所有面包屑存储到 pinia/vuex 中
 * @param {Array} menuList 菜单列表
 * @param {Array} parent 父级菜单
 * @param {Object} result 处理后的结果
 * @returns {Object}
 */
export const getAllBreadcrumbList = (menuList: Menu.MenuOptions[], parent = [], result: { [key: string]: any } = {}) => {
	for (const item of menuList) {
		result[item.path] = [...parent, item];
		if (item.children) getAllBreadcrumbList(item.children, result[item.path], result);
	}
	return result;
};
