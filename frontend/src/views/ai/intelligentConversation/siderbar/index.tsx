import { Plus, UploadFilled } from '@element-plus/icons-vue';
import './index.scss';
import {
	ElButton,
	ElIcon,
	ElMessage,
	ElOption,
	ElProgress,
	ElScrollbar,
	ElSelect,
	ElUpload
} from 'element-plus';
import {
	getV1AiConversations,
	getV1AiConversationsByConversationIdMessages,
	getV1AiKnowledgeBases,
	postV1AiConversations
} from '@/api/modules/v1';
import { useUserStore } from '@/stores/modules/user';
export default defineComponent({
	emits: ['messagePant', 'selectKnowledgeBase', 'selectConversation', 'createKnowledgeBase'],
	setup(_, { emit, expose }) {
		const API_BASE_URL = import.meta.env.VITE_BASE_API;
		const knowledgeBases = ref<any[]>([]);
		const selectedKnowledgeBase = ref<string>('');
		const conversations = ref<any[]>([]);
		const activeConversationId = ref<string>('');
		const userStore = useUserStore();
		const uploadProgress = ref<number>(0);
		const uploading = ref<boolean>(false);
		const hasReadyDocuments = () => {
			if (!selectedKnowledgeBase.value) return true;
			return Number(knowledgeBases.value.find((base) => base.id === selectedKnowledgeBase.value)?.readyDocumentCount) > 0;
		};
		const renderIcon = (icon: any, className?: string) => {
			return <ElIcon class={className}>{h(icon)}</ElIcon>;
		};
		const createConversation = async () => {
			if (!hasReadyDocuments()) {
				ElMessage.warning('请先上传文档并等待索引完成');
				return;
			}
			try {
				const created: any = await postV1AiConversations({
					title: '新会话',
					knowledgeBaseId: selectedKnowledgeBase.value || undefined
				});
				if (created?.success === false) throw new Error(created.message || '创建会话失败');
				if (!created.conversationId) throw new Error('服务端未返回会话 ID');
				await loadConversations(created.conversationId);
			} catch (error: any) {
				ElMessage.error(error?.message || '创建会话失败');
			}
		};
		const loadConversationMessages = async (conversationId: string) => {
			if (conversationId) {
				activeConversationId.value = conversationId;
				emit('messagePant', []);
				emit('selectConversation', conversationId);
				const res: any = await getV1AiConversationsByConversationIdMessages({ conversationId, limit: '100' });
				if (res?.success === false) throw new Error(res.message || '加载会话消息失败');
				emit(
					'messagePant',
					(res.items || []).map((message) => ({
						id: message.id || `${conversationId}-${message.seq}`,
						role: message.role || 'assistant',
						content: message.content || '',
						status: message.status === 'completed' ? 'done' : message.status === 'failed' ? 'error' : message.status,
						sources: message.sources || []
					}))
				);
			}
		};
		const loadConversations = async (preferredConversationId?: string) => {
			const res: any = await getV1AiConversations({
				mode: selectedKnowledgeBase.value ? 'knowledge' : 'chat',
				knowledgeBaseId: selectedKnowledgeBase.value || undefined,
				page: '1', pageSize: '100'
			});
			if (res?.success === false) throw new Error(res.message || '加载会话列表失败');
			conversations.value = res.items || [];
			const selectedConversation =
				conversations.value.find((conversation) => conversation.conversationId === preferredConversationId) ||
				conversations.value[0];
			if (selectedConversation?.conversationId) {
				await loadConversationMessages(selectedConversation.conversationId);
			} else {
				activeConversationId.value = '';
				emit('messagePant', []);
				emit('selectConversation', '');
			}
			emit('selectKnowledgeBase', {
				selectedKnowledgeBase: selectedKnowledgeBase.value,
				conversationId: activeConversationId.value
			});
		};
		const handleCreateKnowledgeBase = () => {
			emit('createKnowledgeBase');
		};
		const loadKnowledgeBases = async (preferredKnowledgeBaseId?: string) => {
			const res: any = await getV1AiKnowledgeBases({ page: '1', pageSize: '100' });
			if (res?.success === false) throw new Error(res.message || '加载知识库列表失败');
			knowledgeBases.value = res.items || [];
			if (preferredKnowledgeBaseId) {
				selectedKnowledgeBase.value = preferredKnowledgeBaseId;
			} else if (!selectedKnowledgeBase.value && knowledgeBases.value[0]?.id) {
				selectedKnowledgeBase.value = knowledgeBases.value[0].id || '';
			}
		};
		const selectKnowledgeBase = async (knowledgeBaseId?: string) => {
			if (knowledgeBaseId !== undefined) selectedKnowledgeBase.value = knowledgeBaseId;
			if (!hasReadyDocuments()) {
				await loadConversations();
				return;
			}
			await createConversation();
		};
		const onSuccess = async (response: any) => {
			uploading.value = false;
			uploadProgress.value = 0;
			if (response?.success === false) {
				ElMessage.error(response.message || '文档上传失败');
				return;
			}
			if (response?.data?.status !== 'ready') {
				ElMessage.error('文档索引未完成');
				return;
			}
			try {
				await loadKnowledgeBases();
				if (!activeConversationId.value) await createConversation();
				ElMessage.success('文档已上传并完成索引');
			} catch (error: any) {
				ElMessage.error(error?.message || '文档索引失败');
			}
		};
		const onProgress = (event: any) => {
			uploading.value = true;
			uploadProgress.value = Math.round(event.percent);
		};
		const onUploadError = (error: any) => {
			uploading.value = false;
			uploadProgress.value = 0;
			ElMessage.error(error?.message || '文档上传失败');
		};
		onMounted(async () => {
			try {
				await loadKnowledgeBases();
				await loadConversations();
			} catch (error: any) {
				ElMessage.error(error?.message || '加载 RAG 会话失败');
			}
		});
		expose({
			loadKnowledgeBases,
			selectKnowledgeBase,
			createConversation
		});
		return () => (
			<div class="conversation-sidebar">
				<button class="new-conversation-btn" onClick={() => void createConversation()}>
					{renderIcon(Plus, 'new-conversation-btn__icon')}
					<span>新建会话</span>
				</button>

				<div class="conversation-list">
					<ElScrollbar>
						{conversations.value.map((conversation) => {
							return (
								<button
									class={['conversation-item', activeConversationId.value === conversation.conversationId ? 'is-active' : '']}
									key={conversation.conversationId}
									onClick={() => void loadConversationMessages(conversation.conversationId)}
								>
									{conversation.title}
								</button>
							);
						})}
					</ElScrollbar>
				</div>

				<div class="sidebar-section sidebar-section--knowledge">
					<p class="sidebar-section__title">知识库</p>
					<div class="knowledge-toolbar">
						{knowledgeBases.value.length ? (
							<ElSelect
								v-model={selectedKnowledgeBase.value}
								class="knowledge-select__control"
								placeholder="选择知识库"
								onChange={() => void selectKnowledgeBase()}
							>
								<ElOption value="" label="不使用知识库" />
								{knowledgeBases.value.map((base) => (
									<ElOption key={base.id} value={base.id} label={`${base.name}（${base.documentCount || 0} 个文件）`} />
								))}
							</ElSelect>
						) : (
							<span class="knowledge-empty">暂无知识库</span>
						)}
						<ElButton class="knowledge-create-button" size="small" type="primary" plain onClick={handleCreateKnowledgeBase}>
							创建
						</ElButton>
					</div>
					<ElUpload
						class="knowledge-upload-control"
						action={`${API_BASE_URL}/v1/ai/knowledge-bases/${selectedKnowledgeBase.value}/documents`}
						name="file"
						headers={{
							Authorization: `Bearer ${userStore.token}`
						}}
						showFileList={false}
						accept=".pdf,.doc,.docx,.txt,.md"
						disabled={!selectedKnowledgeBase.value || uploading.value}
						onSuccess={onSuccess}
						onProgress={onProgress}
						onError={onUploadError}
					>
						<button
							class="knowledge-upload"
							type="button"
							disabled={!selectedKnowledgeBase.value || uploading.value}
						>
							<span class="knowledge-upload__icon">{renderIcon(UploadFilled)}</span>
							<span class="knowledge-upload__title">{selectedKnowledgeBase.value ? '点击选择知识库文档' : '请先选择知识库'}</span>
							<span class="knowledge-upload__hint">支持 PDF、Word、TXT、Markdown</span>
						</button>
					</ElUpload>
					{uploading.value && (
						<>
							<ElProgress percentage={uploadProgress.value} style={{ marginTop: '5px' }} />
							<p class="knowledge-index-status">正在上传并建立索引...</p>
						</>
					)}
				</div>
			</div>
		);
	}
});
