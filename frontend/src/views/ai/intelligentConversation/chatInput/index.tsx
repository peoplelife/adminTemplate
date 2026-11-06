import { ArrowUp, Microphone, Paperclip, Picture } from '@element-plus/icons-vue';
import { ElIcon } from 'element-plus';
import './index.scss';

export default defineComponent({
	name: 'ChatInput',
	props: {
		disabled: {
			type: Boolean,
			default: false
		}
	},
	emits: ['sendMessage'],
	setup(props, { emit }) {
		const composerValue = ref('');
		const handleInput = (e) => {
			composerValue.value = e.target.value;
		};
		const handleKeydown = (e) => {
			if (e.key === 'Enter' && !e.shiftKey) {
				sendMessage();
				e.preventDefault();
			}
		};
		const handleToolClick = () => {};
		const sendMessage = () => {
			const content = composerValue.value.trim();
			if (!content) return;
			emit('sendMessage', content);
			composerValue.value = '';
		};
		const renderIcon = (icon: typeof ArrowUp) => <ElIcon>{h(icon)}</ElIcon>;

		return () => (
			<div class="composer">
				<div class="composer__box">
					<textarea
						class="composer__input"
						placeholder="输入您的问题..."
						disabled={props.disabled}
						value={composerValue.value}
						onInput={(e) => handleInput(e)}
						onKeydown={(e) => handleKeydown(e)}
					/>
					<div class="composer__footer">
						<div class="composer__tools">
							<button class="icon-button icon-button--muted" type="button" onClick={() => handleToolClick()}>
								{renderIcon(Paperclip)}
							</button>
							<button class="icon-button icon-button--muted" type="button" onClick={() => handleToolClick()}>
								{renderIcon(Picture)}
							</button>
							<button class="icon-button icon-button--muted" type="button" onClick={() => handleToolClick()}>
								{renderIcon(Microphone)}
							</button>
						</div>
						<button class="send-button" type="button" disabled={props.disabled} onClick={() => sendMessage()}>
							{renderIcon(ArrowUp)}
						</button>
					</div>
				</div>
				<p class="composer__hint">AI 可能会生成不准确信息，请核实重要内容。</p>
			</div>
		);
	}
});
