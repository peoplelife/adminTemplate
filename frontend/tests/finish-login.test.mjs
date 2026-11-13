import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import vm from 'node:vm';
import test from 'node:test';
import ts from 'typescript';

const source = await readFile(new URL('../src/utils/finishLogin.ts', import.meta.url), 'utf8').catch(() => '');
const { outputText } = ts.transpileModule(source, {
	compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
});

const loadLogin = ({ menus = [{ path: '/home' }], failAt } = {}) => {
	const account = {
		token: '',
		userInfo: {},
		setToken(value) {
			this.token = value;
		},
		setUserInfo(value) {
			this.userInfo = value;
		}
	};
	const auth = {
		showMenuListGet: menus,
		$reset() {
			this.showMenuListGet = [];
		}
	};
	const visited = [];
	const notices = [];
	const failure = new Error('后端拒绝访问');
	const router = {
		async replace(path) {
			visited.push(path);
		}
	};
	const modules = {
		'@/api': {
			async getV1Me() {
				assert.equal(account.token, 'jwt');
				if (failAt === 'profile') throw failure;
				return { id: 42, username: 'user' };
			}
		},
		'@/stores/modules/user': { useUserStore: () => account },
		'@/stores/modules/auth': { useAuthStore: () => auth },
		'@/routers/modules/dynamicRouters': {
			async initDynamicRouters() {
				assert.equal(account.userInfo.id, 42);
				if (failAt === 'menus') throw failure;
			}
		},
		'@/routers': { default: router },
		'@/config': { HOME_URL: '/home' },
		'@/utils/time': { getTimeState: () => '你好' },
		'element-plus': { ElNotification: (notice) => notices.push(notice) }
	};
	const context = {
		exports: {},
		require: (id) => {
			assert.ok(id in modules, id);
			return modules[id];
		}
	};
	vm.runInNewContext(outputText, context);
	return { finishLogin: context.exports.finishLogin, account, auth, visited, notices, failure, router };
};

test('login initializes the profile and menu routes before opening the authorized home', async () => {
	const state = loadLogin();
	assert.equal(typeof state.finishLogin, 'function');
	await state.finishLogin('jwt', state.router);
	assert.equal(state.account.token, 'jwt');
	assert.equal(state.account.userInfo.id, 42);
	assert.deepEqual(state.visited, ['/home']);
	assert.equal(state.notices.length, 1);
	assert.equal(state.notices[0].type, 'success');
});

test('accounts without the home menu open their first permitted leaf menu', async () => {
	const state = loadLogin({ menus: [{ path: '/system', children: [{ path: '/users' }] }] });
	assert.equal(typeof state.finishLogin, 'function');
	await state.finishLogin('jwt', state.router);
	assert.deepEqual(state.visited, ['/users']);
});

for (const failAt of ['profile', 'menus']) {
	test(`failed ${failAt} initialization clears the temporary login and preserves the backend error`, async () => {
		const state = loadLogin({ failAt });
		assert.equal(typeof state.finishLogin, 'function');
		await assert.rejects(state.finishLogin('jwt', state.router), (error) => error === state.failure);
		assert.equal(state.account.token, '');
		assert.equal(Object.keys(state.account.userInfo).length, 0);
		assert.equal(state.auth.showMenuListGet.length, 0);
		assert.deepEqual(state.visited, []);
		assert.deepEqual(state.notices, []);
	});
}

test('an account without visible menus cannot retain a partially initialized token', async () => {
	const state = loadLogin({ menus: [] });
	assert.equal(typeof state.finishLogin, 'function');
	await assert.rejects(state.finishLogin('jwt', state.router));
	assert.equal(state.account.token, '');
	assert.deepEqual(state.visited, []);
	assert.deepEqual(state.notices, []);
});
