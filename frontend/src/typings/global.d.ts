/* Menu */
declare namespace Menu {
	interface MenuOptions {
		path: string;
		name: string;
		component?: string | (() => Promise<unknown>);
		menuId: string;
		meta: MetaProps;
		redirect?: string;
		children?: MenuOptions[];
	}
	interface MetaProps {
		icon: string;
		hidden: number;
		is_full: boolean;
		is_hidden: boolean;
		sort: number;
	}
}
