import assert from 'node:assert/strict';
import { after, test } from 'node:test';
import { createServer } from 'vite';
import { createSSRApp, h } from 'vue';
import { renderToString } from 'vue/server-renderer';

const server = await createServer({ appType: 'custom', logLevel: 'silent', server: { middlewareMode: true } });
const { default: MessageBubble } = await server.ssrLoadModule('/src/components/ai/messageBubble/index.tsx');
const { default: MessageSources } = await server.ssrLoadModule('/src/views/ai/intelligentConversation/messageSources/index.tsx');
const { default: ChatInput } = await server.ssrLoadModule('/src/components/ai/chatInput/index.tsx');
after(() => server.close());
const render = (component, props, children) => renderToString(createSSRApp({ render: () => h(component, props, children) }));

test('renders the answer before the business source slot and keeps references collapsed', async () => {
	const html = await render(
		MessageBubble,
		{ message: { id: '1', role: 'assistant', content: '**回答**', status: 'thinking' } },
		{
			default: () => h(MessageSources, { sources: [{ documentId: 'd1', documentName: 'policy.md', excerpt: '隐藏的原文' }] })
		}
	);
	assert.ok(html.indexOf('<strong>回答</strong>') < html.indexOf('参考来源'));
	assert.match(html, /aria-expanded="false"/);
	assert.match(html, /assistant-streaming-cursor/);
	assert.doesNotMatch(html, /隐藏的原文/);
});

test('shows custom generation status and leaves source UI to the business slot', async () => {
	const html = await render(MessageBubble, {
		message: { id: '2', role: 'assistant', content: '', status: 'thinking' },
		statusText: '正在分析页面需求'
	});
	assert.match(html, /正在分析页面需求/);
	assert.doesNotMatch(html, /参考来源|知识库/);
});

test('shows provider reasoning separately and collapses it once the answer starts or the turn ends', async () => {
	for (const [status, reasoningActive] of [['thinking', true], ['thinking', false], ['done', false], ['error', false]]) {
		const html = await render(MessageBubble, {
			message: { id: 'reasoning', role: 'assistant', content: '**当前回复**', status, reasoningActive,
				reasoningContent: '先检查字段。\n<script>private()</script>' }
		});
		assert.match(html, /<details[^>]*class="assistant-reasoning"/);
		assert.ok(html.indexOf('模型推理') < html.indexOf('<strong>当前回复</strong>'));
		assert.match(html, /先检查字段。/);
		assert.match(html, /&lt;script&gt;private\(\)&lt;\/script&gt;/);
		assert.doesNotMatch(html, /<script>/);
		if (reasoningActive) {
			assert.match(html, /<details[^>]* open/);
			assert.match(html, /推理中/);
		} else {
			assert.doesNotMatch(html, /<details[^>]* open/);
			assert.match(html, status === 'error' ? /已中断/ : /已结束/);
		}
	}
});

test('does not invent a reasoning panel when the provider returned no reasoning', async () => {
	const html = await render(MessageBubble, {
		message: { id: 'normal', role: 'assistant', content: '普通对话', status: 'done' }
	});
	assert.doesNotMatch(html, /assistant-reasoning|模型推理/);
	assert.match(html, /普通对话/);
});

test('renders failure alongside a partial answer', async () => {
	const html = await render(MessageBubble, {
		message: { id: '3', role: 'assistant', content: '部分内容', status: 'error', errorMessage: '连接中断' }
	});
	assert.match(html, /部分内容/);
	assert.match(html, /role="alert"[^>]*>连接中断/);
});

test('composer respects IME composition, disabled state, shift-enter and trims sent content', async () => {
	const sent = [];
	const props = { disabled: false, placeholder: '描述页面' };
	let renderInput;
	await renderToString(
		createSSRApp({
			setup: () => {
				renderInput = ChatInput.setup(props, { emit: (_event, content) => sent.push(content) });
				return renderInput;
			}
		})
	);
	const textarea = () => renderInput().children[0].children[0];
	textarea().props.onInput({ target: { value: '  员工管理  ' } });
	let prevented = false;
	const enter = {
		key: 'Enter',
		shiftKey: false,
		preventDefault: () => {
			prevented = true;
		}
	};
	textarea().props.onKeydown({ ...enter, isComposing: true });
	textarea().props.onKeydown({ ...enter, shiftKey: true });
	props.disabled = true;
	textarea().props.onKeydown(enter);
	assert.deepEqual(sent, []);
	props.disabled = false;
	textarea().props.onKeydown(enter);
	assert.deepEqual(sent, ['员工管理']);
	assert.equal(textarea().props.value, '');
	assert.equal(prevented, true);
	assert.equal(textarea().props.placeholder, '描述页面');
});
