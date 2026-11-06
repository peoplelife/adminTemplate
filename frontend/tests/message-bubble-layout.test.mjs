import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const source = await readFile(new URL('../src/views/ai/intelligentConversation/messageBubble/index.tsx', import.meta.url), 'utf8');
const conversationSource = await readFile(new URL('../src/views/ai/intelligentConversation/index.tsx', import.meta.url), 'utf8');

test('keeps the streamed answer first and collapses references below it', () => {
	const sourcesIndex = source.indexOf('<div class="message-sources">');
	const markdownIndex = source.indexOf("class={['assistant-markdown'");
	const toggleIndex = source.indexOf('message-sources__toggle');

	assert.notEqual(sourcesIndex, -1, 'reference section should exist');
	assert.notEqual(markdownIndex, -1, 'answer markdown should exist');
	assert.ok(sourcesIndex > markdownIndex, 'references should render below answer content');
	assert.notEqual(toggleIndex, -1, 'references should have a collapsed toggle');
	assert.match(source, /assistant-streaming-cursor/);
	assert.match(source, /sourcesExpanded/);
});

test('shows retrieval state and stops auto-following when the user scrolls up', () => {
	assert.match(conversationSource, /streamStage/);
	assert.match(source, /正在检索知识库/);
	assert.match(conversationSource, /autoFollowConversation/);
});
