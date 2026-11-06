import { createUuid } from '@/utils/uuid';
import { ArrowDown } from '@element-plus/icons-vue';
import { ElIcon, ElMessage, ElMessageBox, ElScrollbar, type ScrollbarInstance } from 'element-plus';

import './index.scss';
import Siderbar from './siderbar';
import ChatInput from './chatInput';
import MessageBubble from './messageBubble';
import http from '@/utils/axios';
import { postV1AiKnowledgeBases } from '@/api/modules/v1';

type ChatSource = {
	citationIndex?: number;
	index?: number;
	documentId?: string;
	documentName?: string;
	pageFrom?: number;
	pageTo?: number;
	sectionTitle?: string;
	excerpt?: string;
	available?: boolean;
};

type ChatMessage = {
	id: string | number;
	role: 'user' | 'assistant';
	content: string;
	status?: 'thinking' | 'done' | 'error';
	streamStage?: 'retrieving' | 'generating';
	sources?: ChatSource[];
	traceId?: string;
};

export default defineComponent({
	name: 'IntelligentConversation',
	setup() {
		const conversationScrollRef = ref<ScrollbarInstance>(null);
		const siderBarRef = ref();
		const messages = ref<ChatMessage[]>([]);
		const activeConversationId = ref<string>('');
		const streaming = ref(false);
		const autoFollowConversation = ref(true);
		const API_BASE_URL = import.meta.env.VITE_BASE_API;
		let activeStreamController: AbortController | null = null;

		const scrollConversationToBottom = async (force = false) => {
			await nextTick();
			if (!force && !autoFollowConversation.value) return;
			const container = conversationScrollRef.value;
			if (!container?.wrapRef) return;
			const scrollHeight = container.wrapRef.scrollHeight - container.wrapRef.clientHeight;
			container.scrollTo({ top: scrollHeight, behavior: 'auto' });
		};
		const handleConversationScroll = ({ scrollTop }: { scrollTop: number }) => {
			const wrap = conversationScrollRef.value?.wrapRef;
			if (!wrap) return;
			const distanceFromBottom = wrap.scrollHeight - wrap.clientHeight - scrollTop;
			autoFollowConversation.value = distanceFromBottom <= 48;
		};
		const returnToLatest = () => {
			autoFollowConversation.value = true;
			scrollConversationToBottom(true);
		};
		const onMessagePant = (message: ChatMessage[]) => {
			autoFollowConversation.value = true;
			messages.value = message;
			scrollConversationToBottom(true);
		};
		const selectKnowledgeBase = ({
			conversationId
		}: {
			selectedKnowledgeBase: string;
			conversationId: string;
		}) => {
			activeConversationId.value = conversationId;
		};
		const selectConversation = (conversationId: string) => {
			activeConversationId.value = conversationId;
		};
		const createKnowledgeBase = async () => {
			let value: string;
			try {
				const result = await ElMessageBox.prompt('创建知识库', {
					confirmButtonText: '确定',
					cancelButtonText: '取消',
					inputPlaceholder: '请输入知识库名称',
					inputPattern: /\S+/,
					inputErrorMessage: '知识库名称不能为空'
				});
				value = result.value;
			} catch {
				return;
			}

			try {
				const created: any = await postV1AiKnowledgeBases({ name: value });
				if (created?.success === false) throw new Error(created.message || '创建知识库失败');
				const knowledgeBaseId = typeof created === 'string' ? created : created?.id;
				if (!knowledgeBaseId) throw new Error('创建知识库失败：服务端未返回知识库 ID');
				await siderBarRef.value.loadKnowledgeBases();
				await siderBarRef.value.selectKnowledgeBase(knowledgeBaseId);
				ElMessage.success('创建知识库成功');
			} catch (error: any) {
				ElMessage.error(error?.message || '创建知识库失败');
			}
		};
		const onToken = (token: string, assistantMessageId: string | number) => {
			const assistantMessage = messages.value.find((m) => m.id === assistantMessageId);
			if (assistantMessage) {
				assistantMessage.content += token;
			}
			scrollConversationToBottom();
		};
		const onSendMessage = async (message: string) => {
			if (streaming.value) return;
			if (!activeConversationId.value) {
				ElMessage.warning('请先创建会话');
				return;
			}
			const userMessageId = createUuid();
			const assistantMessageId = createUuid();
			messages.value.push({
				id: userMessageId,
				role: 'user',
				content: message,
				status: 'thinking'
			});
			messages.value.push({
				id: assistantMessageId,
				role: 'assistant',
				content: '',
				status: 'thinking',
				streamStage: undefined,
				sources: []
			});
			autoFollowConversation.value = true;
			scrollConversationToBottom(true);

			let terminalEventReceived = false;
			const controller = new AbortController();
			activeStreamController = controller;
			streaming.value = true;
			try {
				await http.fetchStream(
					`${API_BASE_URL}/v1/ai/conversations/${activeConversationId.value}/messages/stream`,
					{ clientRequestId: createUuid(), content: message },
					{
						signal: controller.signal,
						onEvent: (event: { event: string; data: string }) => {
							const payload = JSON.parse(event.data);
							const assistantMessage = messages.value.find((item) => item.id === assistantMessageId);
							if (!assistantMessage) return;
							if (payload.traceId) assistantMessage.traceId = payload.traceId;
							if (event.event === 'start') {
								assistantMessage.streamStage = payload.mode === 'rag' ? 'retrieving' : 'generating';
							} else if (event.event === 'sources') {
								assistantMessage.sources = (payload.items || []).map((source: any) => ({
									...source,
									citationIndex: source.citationIndex ?? source.index
								}));
								assistantMessage.streamStage = 'generating';
							} else if (event.event === 'message') {
								assistantMessage.streamStage = 'generating';
								onToken(payload.content || '', assistantMessageId);
							} else if (event.event === 'done') {
								assistantMessage.status = 'done';
								assistantMessage.streamStage = undefined;
								terminalEventReceived = true;
							} else if (event.event === 'error') {
								assistantMessage.status = 'error';
								assistantMessage.streamStage = undefined;
								assistantMessage.content ||= payload.message || 'AI 回答失败';
								terminalEventReceived = true;
								ElMessage.error(payload.message || 'AI 回答失败');
							}
						},
						onError: (error: Error) => {
							const assistantMessage = messages.value.find((item) => item.id === assistantMessageId);
							if (!assistantMessage) return;
							assistantMessage.status = 'error';
							assistantMessage.streamStage = undefined;
							assistantMessage.content ||= error.message;
							terminalEventReceived = true;
							ElMessage.error(error.message);
						}
					}
				);
				if (!terminalEventReceived && !controller.signal.aborted) {
					const assistantMessage = messages.value.find((item) => item.id === assistantMessageId);
					if (assistantMessage) {
						assistantMessage.status = 'error';
							assistantMessage.streamStage = undefined;
						assistantMessage.content ||= 'AI 会话流意外结束';
					}
				}
			} finally {
				streaming.value = false;
				if (activeStreamController === controller) activeStreamController = null;
			}
		};
		onUnmounted(() => activeStreamController?.abort());
		watch(messages, () => {
			scrollConversationToBottom();
		});

		return () => (
			<div class="intelligent-conversation-page">
				<Siderbar
					ref={siderBarRef}
					onMessagePant={(messages) => onMessagePant(messages)}
					onSelectKnowledgeBase={selectKnowledgeBase}
					onSelectConversation={selectConversation}
					onCreateKnowledgeBase={createKnowledgeBase}
				/>
				<div class="conversation-main">
					<ElScrollbar ref={conversationScrollRef} onScroll={handleConversationScroll}>
						{messages.value.map((message) => {
							return <MessageBubble key={message.id} message={message} />;
						})}
					</ElScrollbar>
					{!autoFollowConversation.value ? (
						<button class="conversation-follow-button" type="button" title="回到底部" aria-label="回到底部" onClick={returnToLatest}>
							<ElIcon>
								<ArrowDown />
							</ElIcon>
						</button>
					) : null}
					<ChatInput disabled={streaming.value || !activeConversationId.value} onSendMessage={onSendMessage} />
				</div>
			</div>
		);
	}
});
