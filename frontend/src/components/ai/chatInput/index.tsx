import { ArrowUp } from '@element-plus/icons-vue';
import { ElIcon } from 'element-plus';
import { defineComponent, h, ref } from 'vue';
import './index.scss';

export default defineComponent({
	name: 'ChatInput',
	props: {
		disabled: {
			type: Boolean,
			default: false
		},
		placeholder: { type: String, default: '输入您的问题...' }
	},
	emits: ['sendMessage'],
	setup(props, { emit }) {
		const composerValue = ref('');
		const handleInput = (e) => {
			composerValue.value = e.target.value;
		};
		const handleKeydown = (e) => {
			if (e.key === 'Enter' && !e.shiftKey && !e.isComposing && e.keyCode !== 229) {
				sendMessage();
				e.preventDefault();
			}
		};
		const sendMessage = () => {
			const content = composerValue.value.trim();
			if (!content || props.disabled) return;
			emit('sendMessage', content);
			composerValue.value = '';
		};
		const renderIcon = (icon: typeof ArrowUp) => <ElIcon>{h(icon)}</ElIcon>;

		return () => (
			<div class="composer">
				<div class="composer__box">
					<textarea
						class="composer__input"
						placeholder={props.placeholder}
						aria-label={props.placeholder}
						disabled={props.disabled}
						value={composerValue.value}
						onInput={(e) => handleInput(e)}
						onKeydown={(e) => handleKeydown(e)}
					/>
					<div class="composer__footer">
						<button
							class="send-button"
							type="button"
							disabled={props.disabled || !composerValue.value.trim()}
							aria-label="发送消息"
							onClick={() => sendMessage()}
						>
							{renderIcon(ArrowUp)}
						</button>
					</div>
				</div>
				<p class="composer__hint">AI 可能会生成不准确信息，请核实重要内容。</p>
			</div>
		);
	}
});
