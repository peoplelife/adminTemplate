import '@/styles/index.scss';
import App from './App.vue';
import 'virtual:uno.css';
import 'element-plus/theme-chalk/dark/css-vars.css';
import { setup as setupPlugins } from '@/plugins/index';
import pinia from './stores';

const app = createApp(App);
setupPlugins(app);

const appMount = () => {
	app.use(pinia).mount('#app');
};

const enableGlobalMock = import.meta.env.VITE_GLOBALE_REQUEST_MOCK === 'true';

if (enableGlobalMock) {
	import('@/mock/index').then(() => appMount());
} else {
	appMount();
}
