import js from '@eslint/js';
import globals from 'globals';
import tseslint from 'typescript-eslint';
import pluginVue from 'eslint-plugin-vue';
import PrettierRecommended from 'eslint-plugin-prettier/recommended';
import { defineConfig } from 'eslint/config';

export default defineConfig([
	// 忽略文件配置
	{
		ignores: [
			// 构建产物
			'dist/**',
			'build/**',
			'public/**',
			'**/*.min.js',

			// 依赖相关
			'node_modules/**',
			'pnpm-lock.yaml',

			// 缓存文件
			'.cache/**',
			'.temp/**',
			'temp/**',

			// 测试覆盖率报告
			'coverage/**',

			// 类型声明文件
			'**/*.d.ts',

			// 其他配置文件
			'.husky/**',
			'.vscode/**',
			'*.config.js',
			'*.config.ts',
			'*.config.cjs',
			'*.config.mjs',

			// 自动生成的文件
			'auto-imports.d.ts',
			'components.d.ts',

			// 环境文件
			'.env',
			'.env.*',

			// 资源文件
			'src/assets/**',

			// 日志文件
			'*.log'
		]
	},
	{
		files: ['**/*.{js,mjs,cjs,ts,mts,cts,vue}'],
		plugins: { js },
		extends: ['js/recommended']
	},
	{
		files: ['**/*.{js,mjs,cjs,ts,mts,cts,vue}'],
		languageOptions: { globals: globals.browser }
	},
	tseslint.configs.recommended,
	pluginVue.configs['flat/essential'],
	{
		files: ['**/*.vue'],
		languageOptions: { parserOptions: { parser: tseslint.parser } }
	},
	{
		rules: {
			// eslint (http://eslint.cn/docs/rules)
			'no-var': 'error', // 要求使用 let 或 const 而不是 var
			// vue 规则
			'vue/multi-word-component-names': 'off', // 关闭组件名必须多词的限制
			// typescript 规则
			'@typescript-eslint/no-explicit-any': 'off', // 关闭禁止使用 any 类型的规则
			'@typescript-eslint/no-inferrable-types': 'off', // 关闭类型推断的规则
			'@typescript-eslint/no-unused-vars': 'off', // 关闭未使用变量的警告
			'@typescript-eslint/prefer-ts-expect-error': 'error' // 禁止使用 @ts-ignore
		}
	},
	PrettierRecommended
]);
