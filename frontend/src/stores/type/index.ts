import type { IGetV1MeMenusRes } from '@/api';

type MenuItem = IGetV1MeMenusRes['list'][number];

export interface AuthState {
	authList: MenuItem[];
	authButtonList: string[];
	authMenuList: Menu.MenuOptions[];
	allMenuList: any[];
}
