// 注册全局组件
import SvgIcon from '@/components/SvgIcon/index';
import 'virtual:svg-icons-register';

export function setup(app) {
	app.component('SvgIcon', SvgIcon);
}
