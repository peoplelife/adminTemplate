import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { createRequire } from 'node:module';
import vm from 'node:vm';
import test from 'node:test';
import ts from 'typescript';

const require = createRequire(import.meta.url);
const source = await readFile(new URL('../src/views/oauth/github/index.tsx', import.meta.url), 'utf8').catch(() => '');
const { outputText } = ts.transpileModule(source, {
	compilerOptions: {
		module: ts.ModuleKind.CommonJS,
		target: ts.ScriptTarget.ES2022,
		jsx: ts.JsxEmit.ReactJSX,
		jsxImportSource: 'vue'
	}
});

const createCallback = ({ flow = { state: 'once', mode: 'login' }, token = '', userId = 42, failure } = {}) => {
	const mounted = [];
	const calls = [];
	const notices = [];
	let saved = flow;
	const modules = {
		vue: { ...require('vue'), onMounted: (callback) => mounted.push(callback) },
		'vue/jsx-runtime': require('vue/jsx-runtime'),
		'vue-router': {
			useRouter: () => ({
				async replace(path) {
					calls.push(['navigate', path]);
				}
			})
		},
		'@/config': { HOME_URL: '/home', LOGIN_URL: '/login' },
		'@/stores/modules/auth': { useAuthStore: () => ({ showMenuListGet: [{ path: '/users' }] }) },
		'@/routers/modules/dynamicRouters': { async initDynamicRouters() {} },
		'@/stores/modules/user': { useUserStore: () => ({ token, userInfo: { id: userId } }) },
		'@/utils/githubOAuth': {
			consumeGitHubOAuth() {
				const value = saved;
				saved = null;
				return value;
			}
		},
		'@/utils/finishLogin': {
			getLoginDestination: () => '/users',
			async finishLogin(value) {
				calls.push(['login', value]);
			}
		},
		'@/api': {
			async postV1AuthGithubComplete(body) {
				assert.equal(saved, null, 'flow must be removed before the exchange starts');
				calls.push(['complete-login', { ...body }]);
				if (failure) throw failure;
				return { token: 'new-jwt', initialPasswordFlag: false };
			},
			async postV1MeGithubComplete(body) {
				assert.equal(saved, null, 'flow must be removed before the exchange starts');
				calls.push(['complete-bind', { ...body }]);
				if (failure) throw failure;
				return { enabled: true, bound: true, login: 'confirmed-user' };
			}
		},
		'element-plus': { ElAlert: 'alert', ElButton: 'button', ElCard: 'card', ElNotification: (notice) => notices.push(notice) }
	};
	const context = {
		exports: {},
		require: (id) => {
			assert.ok(id in modules, id);
			return modules[id];
		}
	};
	vm.runInNewContext(outputText, context);
	assert.equal(typeof context.exports.default?.setup, 'function');
	const render = context.exports.default.setup();
	return { render, mounted, calls, notices };
};

const textContent = (vnode) => {
	if (typeof vnode === 'string') return vnode;
	if (Array.isArray(vnode)) return vnode.map(textContent).join(' ');
	return vnode ? [vnode.props?.title, textContent(vnode.children)].filter(Boolean).join(' ') : '';
};

test('callback exchanges only the saved state and uses the common login initialization', async () => {
	const state = createCallback();
	await state.mounted[0]();
	assert.deepEqual(state.calls, [
		['complete-login', { state: 'once' }],
		['login', 'new-jwt']
	]);
});

test('missing context offers a retry without exchanging any authorization', async () => {
	const state = createCallback({ flow: null });
	await state.mounted[0]();
	assert.deepEqual(state.calls, []);
	assert.match(textContent(state.render()), /重新|重试/);
});

for (const current of [{ token: '' }, { token: 'current-jwt', userId: 7 }]) {
	test(`binding cannot complete with ${current.token ? 'a different user' : 'no authenticated user'}`, async () => {
		const state = createCallback({ flow: { state: 'once', mode: 'bind', userId: 42 }, ...current });
		await state.mounted[0]();
		assert.deepEqual(state.calls, []);
		assert.match(textContent(state.render()), /重新|重试/);
	});
}

test('binding reports the confirmed account and returns to an authorized menu without requiring home permission', async () => {
	const state = createCallback({ flow: { state: 'once', mode: 'bind', userId: 42 }, token: 'current-jwt' });
	await state.mounted[0]();
	assert.deepEqual(state.calls, [
		['complete-bind', { state: 'once' }],
		['navigate', '/users']
	]);
	assert.match(state.notices[0].message, /confirmed-user/);
});

test('failed authorization displays the backend message without an unhandled rejection', async () => {
	const state = createCallback({ failure: { message: '当前账号已被禁用，请联系管理员' } });
	await state.mounted[0]();
	assert.match(textContent(state.render()), /当前账号已被禁用，请联系管理员/);
	assert.deepEqual(state.calls, [['complete-login', { state: 'once' }]]);
});
