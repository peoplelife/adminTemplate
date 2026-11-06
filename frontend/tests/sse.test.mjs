import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import ts from 'typescript';

const source = await readFile(new URL('../src/utils/sse.ts', import.meta.url), 'utf8').catch(() => '');
const { outputText } = ts.transpileModule(source, {
	compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 }
});
const { createSseParser } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`);

test('parses named events across chunks and ignores heartbeat comments', () => {
	assert.equal(typeof createSseParser, 'function');
	if (typeof createSseParser !== 'function') return;

	const events = [];
	const parser = createSseParser((event) => events.push(event));
	parser.push(': ping\r\n\r\nevent: sources\r\ndata: {"items":\r\ndata: [1]}\r\n\r\nevent: message\ndata: {"content":"hello"}\n\n');
	parser.push('event: done\ndata: {"status":"completed"}');
	parser.flush();

	assert.deepEqual(events, [
		{ event: 'sources', data: '{"items":\n[1]}' },
		{ event: 'message', data: '{"content":"hello"}' },
		{ event: 'done', data: '{"status":"completed"}' }
	]);
});

test('uses the default message event and dispatches only frames with data', () => {
	assert.equal(typeof createSseParser, 'function');
	if (typeof createSseParser !== 'function') return;

	const events = [];
	const parser = createSseParser((event) => events.push(event));
	parser.push('id: 1\ndata: first\n\ndata: second\n\n');
	parser.flush();

	assert.deepEqual(events, [
		{ event: 'message', data: 'first' },
		{ event: 'message', data: 'second' }
	]);
});
