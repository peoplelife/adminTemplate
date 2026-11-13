import assert from 'node:assert/strict';
import { after, test } from 'node:test';
import { createServer } from 'vite';

const server = await createServer({
	appType: 'custom',
	logLevel: 'silent',
	server: { middlewareMode: true }
});
const messageBubble = await server.ssrLoadModule('/src/views/ai/intelligentConversation/messageSources/index.tsx');

after(() => server.close());

test('groups reference chunks by document id and keeps citation matches', () => {
	assert.equal(typeof messageBubble.groupSourcesByDocument, 'function');
	if (typeof messageBubble.groupSourcesByDocument !== 'function') return;

	const groups = messageBubble.groupSourcesByDocument([
		{ documentId: 'document-1', documentName: 'policy.md', citationIndex: 1, excerpt: 'first' },
		{ documentId: 'document-1', documentName: 'policy.md', citationIndex: 2, excerpt: 'second' },
		{ documentId: 'document-2', documentName: 'policy.md', citationIndex: 3, excerpt: 'third' }
	]);

	assert.equal(groups.length, 2);
	assert.equal(groups[0].documentId, 'document-1');
	assert.deepEqual(
		groups[0].matches.map((source) => source.citationIndex),
		[1, 2]
	);
	assert.equal(groups[1].documentId, 'document-2');
	assert.deepEqual(
		groups[1].matches.map((source) => source.citationIndex),
		[3]
	);
});
