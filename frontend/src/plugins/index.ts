import { setup as setupRouter } from '@/routers/index';
import { setup as setupElementPlus } from '@/plugins/element-plus/index';
import { setup as setupSvgIcon } from '@/plugins/icon/index';
export const setup = (app) => {
	setupElementPlus(app);
	setupRouter(app);
	setupSvgIcon(app);
};
