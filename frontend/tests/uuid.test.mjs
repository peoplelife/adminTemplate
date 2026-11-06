import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { webcrypto } from 'node:crypto';
import vm from 'node:vm';
import test from 'node:test';
import ts from 'typescript';

const source = await readFile(new URL('../src/utils/uuid.ts', import.meta.url), 'utf8');
const { outputText } = ts.transpileModule(source, {
	compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
});

test('generates UUID v4 without the secure-context randomUUID API', () => {
	const context = { exports: {}, crypto: { getRandomValues: webcrypto.getRandomValues.bind(webcrypto) } };
	vm.runInNewContext(outputText, context);
	const ids = Array.from({ length: 1000 }, () => context.exports.createUuid());
	for (const id of ids) assert.match(id, /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
	assert.equal(new Set(ids).size, ids.length);
});
