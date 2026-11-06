import bus from '@/utils/mitt';
import './index.scss';
import SvgIcon from '@/components/SvgIcon';
export default defineComponent({
	setup() {
		const handleThemeSettings = () => {
			bus.emit('theme-drawer');
		};
		return () => (
			<div class="theme-settings" onClick={handleThemeSettings}>
				<SvgIcon name="icon-theme" size={24}></SvgIcon>
			</div>
		);
	}
});
