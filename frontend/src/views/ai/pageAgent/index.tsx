import { defineComponent, nextTick, onMounted, onUnmounted, ref } from 'vue';
import { ArrowDown } from '@element-plus/icons-vue';
import { ElAlert, ElButton, ElEmpty, ElIcon, ElScrollbar, type ScrollbarInstance } from 'element-plus';
import { getV1AiPageAgentWorkspace, postV1AiPageAgentMessagesStream, type IPostV1AiPageAgentMessagesStreamReq } from '@/api';
import ChatInput from '@/components/ai/chatInput';
import MessageBubble, { type ChatMessage } from '@/components/ai/messageBubble';
import http from '@/utils/axios';
import { createUuid } from '@/utils/uuid';
import PageAgentPreview from './preview';
import { createPageAgentTurn, readPageDocument, type PageDocument } from './model';
import './index.scss';

export default defineComponent({
	name: 'PageAgent',
	setup() {
		const messages = ref<ChatMessage[]>([]);
		const document = ref<PageDocument | null>(null);
		const previewVersion = ref(0);
		const loading = ref(true);
		const loadError = ref('');
		const streaming = ref(false);
		const scrollRef = ref<ScrollbarInstance>();
		const following = ref(true);
		let activeController: AbortController | null = null;
		let disposed = false;
		const scrollToLatest = async () => {
			await nextTick();
			if (disposed || !following.value) return;
			const wrap = scrollRef.value?.wrapRef;
			if (wrap) scrollRef.value.setScrollTop(wrap.scrollHeight);
		};
		const publish = (page: PageDocument) => {
			document.value = page;
			previewVersion.value++;
		};
		const restoreWorkspace = async () => {
			loading.value = true;
			loadError.value = '';
			try {
				const workspace = await getV1AiPageAgentWorkspace();
				if (disposed) return;
				messages.value = (workspace.messages || []).map((message) => ({
					id: createUuid(),
					role: message.role as ChatMessage['role'],
					content: message.content,
					reasoningContent: message.reasoningContent,
					status: 'done'
				}));
				if (workspace.pageSpec || workspace.preview) publish(readPageDocument(workspace.pageSpec, workspace.preview));
				scrollToLatest();
			} catch (error) {
				if (!disposed) loadError.value = error instanceof Error ? error.message : '工作区加载失败，请重试。';
			} finally {
				if (!disposed) loading.value = false;
			}
		};
		const sendMessage = async (content: string) => {
			if (streaming.value || loading.value || loadError.value || !content.trim()) return;
			following.value = true;
			messages.value.push({ id: createUuid(), role: 'user', content });
			messages.value.push({
				id: createUuid(),
				role: 'assistant',
				content: '',
				status: 'thinking',
				statusText: '正在分析页面需求'
			});
			const assistant = messages.value[messages.value.length - 1];
			const turn = createPageAgentTurn(assistant, publish);
			const controller = new AbortController();
			activeController = controller;
			streaming.value = true;
			scrollToLatest();
			try {
				const body: IPostV1AiPageAgentMessagesStreamReq = { content };
				const base = (import.meta.env.VITE_BASE_API || '').replace(/\/$/, '');
				await http.fetchStream(`${base}${postV1AiPageAgentMessagesStream.requestConfig.path}`, body, {
					signal: controller.signal,
					onEvent: (event) => {
						turn.onEvent(event);
						scrollToLatest();
					},
					onError: (error) => {
						turn.fail(error);
						scrollToLatest();
					}
				});
				if (!controller.signal.aborted) turn.finish();
			} catch (error) {
				if (!controller.signal.aborted) turn.fail(error instanceof Error ? error : new Error('页面生成失败，请重试。'));
			} finally {
				if (activeController === controller) activeController = null;
				streaming.value = false;
				scrollToLatest();
			}
		};
		onMounted(restoreWorkspace);
		onUnmounted(() => {
			disposed = true;
			activeController?.abort();
		});
		return () => (
			<div class="page-agent-workspace">
				<section class="page-agent-chat" aria-label="页面需求对话" aria-busy={loading.value || streaming.value}>
					{loadError.value && (
						<ElAlert type="error" closable={false} title={loadError.value}>
							<ElButton type="primary" link onClick={restoreWorkspace}>
								重新加载
							</ElButton>
						</ElAlert>
					)}
					<ElScrollbar
						ref={scrollRef}
						onScroll={({ scrollTop }) => {
							const wrap = scrollRef.value?.wrapRef;
							if (wrap) following.value = wrap.scrollHeight - wrap.clientHeight - scrollTop <= 48;
						}}
					>
						{loading.value ? (
							<p class="page-agent-empty" role="status">
								正在恢复工作区…
							</p>
						) : null}
						{!loading.value && !messages.value.length && !loadError.value ? (
							<div class="page-agent-empty">
								<p>描述你想生成的页面</p>
								<span>例如：生成一个员工管理页面，包含姓名、部门和状态，支持搜索、新增和编辑。</span>
							</div>
						) : null}
						{messages.value.map((message) => (
							<MessageBubble key={message.id} message={message} />
						))}
					</ElScrollbar>
					{!following.value && (
						<button
							class="page-agent-follow"
							type="button"
							aria-label="回到最新消息"
							onClick={() => {
								following.value = true;
								scrollToLatest();
							}}
						>
							<ElIcon>
								<ArrowDown />
							</ElIcon>
						</button>
					)}
					<ChatInput
						disabled={loading.value || streaming.value || !!loadError.value}
						placeholder="描述你想生成的页面…"
						onSendMessage={sendMessage}
					/>
				</section>
				<section class="page-agent-canvas" aria-label="页面预览">
					{document.value ? (
						<PageAgentPreview key={previewVersion.value} document={document.value} />
					) : (
						<ElEmpty description="页面生成后将在这里展示" />
					)}
				</section>
			</div>
		);
	}
});
