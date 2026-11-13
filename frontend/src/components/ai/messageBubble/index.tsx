import { CopyDocument, MagicStick } from '@element-plus/icons-vue';
import { ElIcon, ElMessage } from 'element-plus';
import { defineComponent, h, type Component, type PropType } from 'vue';
import { renderMarkdown } from '@/utils/markdown';
import './index.scss';

export type ChatMessage = {
	id: string | number;
	role: 'user' | 'assistant';
	content: string;
	status?: 'thinking' | 'done' | 'error';
	statusText?: string;
	reasoningContent?: string;
	reasoningActive?: boolean;
	errorMessage?: string;
	traceId?: string;
};

export default defineComponent({
	name: 'MessageBubble',
	props: {
		message: {
			type: Object as PropType<ChatMessage>,
			required: true
		},
		statusText: { type: String, default: '正在生成回答' }
	},
	setup(props, { slots }) {
		const copyMessage = async (_content: string) => {
			if (!_content) return;
			await navigator.clipboard.writeText(_content);
			ElMessage.success('复制成功');
		};
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
						{props.message.reasoningContent ? (
							<details class="assistant-reasoning" open={props.message.reasoningActive}>
								<summary>
									模型推理
									<span>{props.message.reasoningActive ? '推理中' : props.message.status === 'error' ? '已中断' : '已结束'}</span>
								</summary>
								<p>{props.message.reasoningContent}</p>
							</details>
						) : null}
						{props.message.status === 'thinking' && !props.message.content ? (
							!props.message.reasoningActive && (
								<p class="assistant-panel__thinking">
									{props.message.statusText || props.statusText}
									<span class="thinking-dots">...</span>
								</p>
							)
						) : (
							<div
								class={['assistant-markdown', props.message.status === 'error' && 'assistant-panel__error']}
								innerHTML={renderMarkdown(props.message.content)}
							/>
						)}
						{props.message.status === 'thinking' && props.message.content ? (
							<span class="assistant-streaming-cursor" role="status" aria-label={props.message.statusText || props.statusText} />
						) : null}
						{props.message.errorMessage ? (
							<p class="assistant-panel__error" role="alert">
								{props.message.errorMessage}
							</p>
						) : null}
						{slots.default?.()}
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
