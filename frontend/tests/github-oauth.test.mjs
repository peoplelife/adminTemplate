import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import ts from 'typescript';

const source = await readFile(new URL('../src/utils/githubOAuth.ts', import.meta.url), 'utf8').catch(() => '');
const { outputText } = ts.transpileModule(source, {
	compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 }
});
const { startGitHubOAuth, consumeGitHubOAuth, GITHUB_OAUTH_FLOW_KEY } = await import(
	`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`
);

const createStorage = () => {
	const values = new Map();
	return {
		getItem: (key) => values.get(key) ?? null,
		setItem: (key, value) => values.set(key, value),
		removeItem: (key) => values.delete(key)
	};
};
const authorization = {
	authorizationUrl: 'https://github.com/login/oauth/authorize?client_id=app&state=once&code_challenge=challenge',
	state: 'once'
};

test('saves only flow context before redirecting to GitHub without adding a token to the URL', () => {
	assert.equal(typeof startGitHubOAuth, 'function');
	const storage = createStorage();
	let destination;
	startGitHubOAuth(authorization, { mode: 'bind', userId: 42, token: 'private-jwt' }, storage, (url) => {
		assert.deepEqual(JSON.parse(storage.getItem(GITHUB_OAUTH_FLOW_KEY)), { state: 'once', mode: 'bind', userId: 42 });
		destination = url;
	});
	assert.equal(destination, authorization.authorizationUrl);
	assert.ok(!destination.includes('private-jwt'));
	assert.ok(!storage.getItem(GITHUB_OAUTH_FLOW_KEY).includes('private-jwt'));
});

test('consumes a saved authorization once and clears it before returning the state', () => {
	assert.equal(typeof consumeGitHubOAuth, 'function');
	const storage = createStorage();
	storage.setItem(GITHUB_OAUTH_FLOW_KEY, '{"state":"once","mode":"login"}');
	assert.deepEqual(consumeGitHubOAuth(storage), { state: 'once', mode: 'login' });
	assert.equal(storage.getItem(GITHUB_OAUTH_FLOW_KEY), null);
	assert.equal(consumeGitHubOAuth(storage), null);
});

test('malformed, incomplete and unknown stored flows are cleared without completing authorization', () => {
	assert.equal(typeof consumeGitHubOAuth, 'function');
	for (const value of [
		'{',
		'null',
		'[]',
		'{}',
		'{"state":"","mode":"login"}',
		'{"state":"once","mode":"other"}',
		'{"state":"once","mode":"bind","userId":"42"}'
	]) {
		const storage = createStorage();
		storage.setItem(GITHUB_OAUTH_FLOW_KEY, value);
		assert.equal(consumeGitHubOAuth(storage), null, value);
		assert.equal(storage.getItem(GITHUB_OAUTH_FLOW_KEY), null, value);
	}
});

test('rejects redirects outside the GitHub authorization endpoint before saving a flow', () => {
	assert.equal(typeof startGitHubOAuth, 'function');
	for (const url of [
		'https://github.com.attacker.test/login/oauth/authorize',
		'http://github.com/login/oauth/authorize',
		'https://github.com/settings/profile',
		'javascript:alert(1)'
	]) {
		const storage = createStorage();
		assert.throws(() =>
			startGitHubOAuth({ ...authorization, authorizationUrl: url }, { mode: 'login' }, storage, () =>
				assert.fail('must not navigate')
			)
		);
		assert.equal(storage.getItem(GITHUB_OAUTH_FLOW_KEY), null);
	}
});

test('does not navigate when the browser cannot persist the flow', () => {
	assert.equal(typeof startGitHubOAuth, 'function');
	const storage = createStorage();
	storage.setItem = () => {
		throw new Error('storage disabled');
	};
	assert.throws(
		() => startGitHubOAuth(authorization, { mode: 'login' }, storage, () => assert.fail('must not navigate')),
		/storage disabled/
	);
});
