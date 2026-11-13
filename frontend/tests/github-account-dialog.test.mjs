import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { createRequire } from 'node:module';
import vm from 'node:vm';
import test from 'node:test';
import ts from 'typescript';

const require = createRequire(import.meta.url);
const source = await readFile(new URL('../src/layouts/components/DropDown/GitHubAccountDialog.tsx', import.meta.url), 'utf8');
const { outputText } = ts.transpileModule(source, {
	compilerOptions: {
		module: ts.ModuleKind.CommonJS,
		target: ts.ScriptTarget.ES2022,
		jsx: ts.JsxEmit.ReactJSX,
		jsxImportSource: 'vue'
	}
});
const content = (node) => {
	if (typeof node === 'string') return node;
	if (Array.isArray(node)) return node.map(content).join(' ');
	return node ? [node.props?.title, content(node.children)].filter(Boolean).join(' ') : '';
};
const buttons = (node) => {
	if (Array.isArray(node)) return node.flatMap(buttons);
	if (!node || typeof node !== 'object') return [];
	return [...(node.type === 'button' ? [node] : []), ...buttons(node.children)];
};

const openDialog = async ({
	account = { enabled: true, bound: false, login: null },
	confirm = 'confirm',
	deleteError,
	authorize
} = {}) => {
	let onOpen;
	let authorizations = 0;
	let deletions = 0;
	const redirects = [];
	const notices = [];
	const modules = {
		vue: {
			...require('vue'),
			watch: (_, callback) => {
				onOpen = callback;
			}
		},
		'vue/jsx-runtime': require('vue/jsx-runtime'),
		'@/stores/modules/user': { useUserStore: () => ({ token: 'jwt', userInfo: { id: 42 } }) },
		'@/utils/githubOAuth': { startGitHubOAuth: (authorization, flow) => redirects.push([authorization, flow]) },
		'@/api': {
			async getV1MeGithub() {
				return account;
			},
			async postV1MeGithubAuthorize() {
				authorizations++;
				return authorize ? authorize() : { authorizationUrl: 'https://github.com/login/oauth/authorize', state: 'once' };
			},
			async deleteV1MeGithub() {
				deletions++;
				if (deleteError) throw deleteError;
			}
		},
		'element-plus': {
			ElAlert: 'alert',
			ElButton: 'button',
			ElDialog: 'dialog',
			ElMessageBox: {
				confirm: async () => {
					if (confirm === 'cancel') throw 'cancel';
					return confirm;
				}
			},
			ElNotification: (notice) => notices.push(notice)
		}
	};
	const context = {
		exports: {},
		require: (id) => {
			assert.ok(id in modules, id);
			return modules[id];
		}
	};
	vm.runInNewContext(outputText, context);
	const render = context.exports.default.setup({ modelValue: true }, { emit() {} });
	onOpen(true);
	await new Promise(setImmediate);
	return {
		render,
		redirects,
		notices,
		get authorizations() {
			return authorizations;
		},
		get deletions() {
			return deletions;
		}
	};
};

test('disabled GitHub configuration explains that binding is unavailable without claiming the account is unbound', async () => {
	const state = await openDialog({ account: { enabled: false, bound: false, login: null } });
	assert.doesNotMatch(content(state.render()), /尚未绑定/);
	assert.match(content(state.render()), /暂未启用，启用后可查看和管理绑定/);
	assert.equal(buttons(state.render()).length, 0);
});

test('bound account remains visible when GitHub configuration is disabled', async () => {
	const state = await openDialog({ account: { enabled: false, bound: true, login: 'linked-user' } });
	assert.match(content(state.render()), /linked-user/);
});

test('canceling unbind preserves the association without calling delete', async () => {
	const state = await openDialog({ account: { enabled: true, bound: true, login: 'linked-user' }, confirm: 'cancel' });
	await buttons(state.render())[0].props.onClick();
	assert.equal(state.deletions, 0);
	assert.match(content(state.render()), /linked-user/);
	assert.equal(state.notices.length, 0);
});

test('confirmed unbind updates the account state only after a successful delete', async () => {
	const state = await openDialog({ account: { enabled: true, bound: true, login: 'linked-user' } });
	await buttons(state.render())[0].props.onClick();
	assert.equal(state.deletions, 1);
	assert.match(content(state.render()), /尚未绑定/);
	assert.equal(state.notices.length, 1);
});

test('rejected unbind keeps the account and displays the backend message', async () => {
	const state = await openDialog({
		account: { enabled: true, bound: true, login: 'linked-user' },
		deleteError: { message: '无法解除此绑定' }
	});
	await buttons(state.render())[0].props.onClick();
	assert.match(content(state.render()), /linked-user/);
	assert.match(content(state.render()), /无法解除此绑定/);
	assert.equal(state.notices.length, 0);
});

test('binding suppresses duplicate clicks and records the authenticated user for the redirect', async () => {
	let resolve;
	const state = await openDialog({
		authorize: () =>
			new Promise((done) => {
				resolve = done;
			})
	});
	const bind = buttons(state.render())[0].props.onClick;
	const pending = bind();
	await bind();
	assert.equal(state.authorizations, 1);
	resolve({ authorizationUrl: 'https://github.com/login/oauth/authorize', state: 'once' });
	await pending;
	assert.equal(state.redirects.length, 1);
	assert.equal(state.redirects[0][1].mode, 'bind');
	assert.equal(state.redirects[0][1].userId, 42);
});
