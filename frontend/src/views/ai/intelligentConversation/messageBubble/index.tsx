import { ArrowDown, ArrowUp, CopyDocument, MagicStick } from '@element-plus/icons-vue';
import { ElIcon, ElMessage } from 'element-plus';
import type { Component, PropType } from 'vue';
import { renderMarkdown } from '@/utils/markdown';
import './index.scss';

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
};

export const groupSourcesByDocument = (sources: ChatSource[]) => {
	const groups = new Map<string, { documentId?: string; documentName?: string; matches: ChatSource[] }>();
	sources.forEach((source, index) => {
		const key = source.documentId || source.documentName || String(index);
		const group = groups.get(key);
		if (group) group.matches.push(source);
		else groups.set(key, { documentId: source.documentId, documentName: source.documentName, matches: [source] });
	});
	return [...groups.values()];
};

export default defineComponent({
	name: 'MessageBubble',
	props: {
		message: {
			type: Object as PropType<ChatMessage>,
			required: true
		}
	},
	setup(props) {
		const sourcesExpanded = ref(false);
		const sourceGroups = computed(() => groupSourcesByDocument(props.message.sources || []));
		const copyMessage = async (_content: string) => {
			if (!_content) return;
			await navigator.clipboard.writeText(_content);
			ElMessage.success('复制成功');
		};
		const handleAssistantPanelClick = (_event: MouseEvent) => {};
		const renderIcon = (icon: Component, className?: string) => <ElIcon class={className}>{h(icon)}</ElIcon>;

		return () =>
			props.message.role === 'user' ? (
				<div class="message-row message-row--user">
					<div class="message-bubble message-bubble--user">
						<p>{props.message.content}</p>
						<button
							class="message-copy message-copy--user"
							type="button"
							title="复制"
							onClick={() => copyMessage(props.message.content)}
						>
							{renderIcon(CopyDocument)}
						</button>
					</div>
				</div>
			) : (
				<div class="message-row message-row--assistant">
					<div class="assistant-avatar">{renderIcon(MagicStick, 'assistant-avatar__icon')}</div>
					<div class="assistant-panel">
						{props.message.status === 'thinking' && !props.message.content ? (
							<p class="assistant-panel__thinking">
								{props.message.streamStage === 'retrieving' ? '正在检索知识库' : '正在生成回答'}
								<span class="thinking-dots">...</span>
							</p>
						) : (
							<div
								class={['assistant-markdown', props.message.status === 'error' && 'assistant-panel__error']}
								innerHTML={renderMarkdown(props.message.content)}
								onClick={(event) => handleAssistantPanelClick(event)}
							/>
						)}
						{props.message.status === 'thinking' && props.message.content ? (
							<span class="assistant-streaming-cursor" role="status" aria-label="正在生成回答" />
						) : null}
						{sourceGroups.value.length ? (
							<div class="message-sources">
								<button
									class="message-sources__toggle"
									type="button"
									aria-expanded={sourcesExpanded.value}
									title={sourcesExpanded.value ? '收起参考来源' : '展开参考来源'}
									onClick={() => (sourcesExpanded.value = !sourcesExpanded.value)}
								>
									<span>参考来源 {sourceGroups.value.length}</span>
									{renderIcon(sourcesExpanded.value ? ArrowUp : ArrowDown)}
								</button>
								{sourcesExpanded.value ? (
									<div class="message-sources__list">
										{sourceGroups.value.map((group) => (
											<div class="message-source" key={group.documentId || group.documentName}>
												<p class="message-source__name">
													{group.documentName || '知识库文档'} · {group.matches.length} 个引用片段
												</p>
												{group.matches.map((source, index) => (
													<div class="message-source__match" key={source.citationIndex || source.index || index}>
														<p class="message-source__meta">
															[{source.citationIndex || source.index || index + 1}]
															{source.pageFrom
																? ` 第 ${source.pageFrom}${source.pageTo && source.pageTo !== source.pageFrom ? `-${source.pageTo}` : ''} 页`
																: ''}
														</p>
														{source.sectionTitle && <p class="message-source__section">{source.sectionTitle}</p>}
														{source.excerpt && <p class="message-source__excerpt">{source.excerpt}</p>}
													</div>
												))}
											</div>
										))}
									</div>
								) : null}
							</div>
						) : null}
						{props.message.content ? (
							<button
								class="message-copy message-copy--assistant"
								type="button"
								title="复制"
								onClick={() => copyMessage(props.message.content)}
							>
								{renderIcon(CopyDocument)}
							</button>
						) : null}
					</div>
				</div>
			);
	}
});
