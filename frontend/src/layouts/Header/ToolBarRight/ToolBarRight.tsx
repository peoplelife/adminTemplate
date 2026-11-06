import ThemeSettings from '@/layouts/components/ThemeSettings/index';
import './ToolBarRight.scss';
import DropDown from '@/layouts/components/DropDown/index';
import { useUserStore } from '@/stores/modules/user';
export default defineComponent({
	setup() {
		const userStore = useUserStore();
		return () => (
			<div class="toolbar-left">
				<div class="header-icon">
					<ThemeSettings />
				</div>
				<span class="username">{userStore.userInfo.username}</span>
				<DropDown />
			</div>
		);
	}
});
