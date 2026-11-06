import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import vueJsx from '@vitejs/plugin-vue-jsx';
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons';
import { resolve } from 'path';
import AutoImport from 'unplugin-auto-import/vite';
import path from 'path';
import UnoCSS from 'unocss/vite';
// https://vite.dev/config/
export default defineConfig(({ mode }) => {
	const env = loadEnv(mode, process.cwd());
	const baseAPI = env.VITE_BASE_API;
	const apiURL = env.VITE_BASE_URL;
	return {
		base: '/', //打包后前端静态资源访问的基础路径
		//插件集成
		plugins: [
			vue(),
			vueJsx(),
			UnoCSS(),
			createSvgIconsPlugin({
				iconDirs: [path.resolve(__dirname, 'src/assets/icons')],
				symbolId: 'icon-[name]'
			}),
			//自动导入
			AutoImport({
				imports: ['vue', { 'vue-router': ['createRouter', 'createWebHistory', 'createWebHashHistory'] }, 'pinia'],
				dts: true
			})
		],
		//共享选项
		resolve: {
			//用于import或者require语句中值的别名
			alias: {
				'@': resolve(__dirname, 'src')
			}
		},
		css: {
			preprocessorOptions: {
				scss: {
					additionalData: `@use "@/styles/define.scss" as *;`
				}
			}
		},
		//开发配置
		server: {
			sourcemap: true,
			//代理配置，解决开发环境下的跨域问题
			proxy: {
				[baseAPI]: {
					target: `${apiURL}`,
					changeOrigin: true,
					rewrite: (path) => path.replace(new RegExp(`^${baseAPI}`), '')
				}
			}
		},
		//构建配置
		build: {
			sourcemap: false, //关闭源码映射，减小体积并加快构建
			rollupOptions: {
				//入口规则配置
				input: {
					index: resolve(__dirname, 'index.html')
				},
				//产物输出规则配置
				output: {
					assetFileNames: 'assets/[name].[hash][extname]', //静态资源如图片、svg等命名规则
					chunkFileNames: 'assets/js/[name].[hash].js', //非主入口文件命名规则
					entryFileNames: 'assets/js/[name].[hash].js', //主入口文件命名规则
					//手动分割代码，优化缓存和加载性能
					manualChunks: (id) => {
						if (!id.includes('node_modules')) return;

						if (id.includes('/vue/') || id.includes('/vue-router/') || id.includes('/pinia/')) {
							return 'framework';
						}

						if (id.includes('/element-plus/')) {
							return 'element-plus';
						}

						if (id.includes('/@vueuse/')) {
							return 'vueuse';
						}

						if (id.includes('/echarts/') || id.includes('/zrender/')) {
							return 'echarts';
						}

						if (id.includes('/lodash-es/') || id.includes('/dayjs/') || id.includes('/axios/')) {
							return 'common';
						}

						return 'vendor';
					}
				}
			}
		},
		//开发依赖预构建
		optimizeDeps: {},
		esbuild: {
			drop: ['console', 'debugger']
		}
	};
});
