import { ElColorPicker, ElDrawer, ElSwitch } from 'element-plus';
import { ref } from 'vue';
import './index.scss';
import bus from '@/utils/mitt';
import { useGlobalStore } from '@/stores/modules/global';
import { useTheme } from '@/hooks/useTheme';
export default defineComponent({
	setup() {
		const { Switch, ChagePrimary } = useTheme();
		const drawerVisible = ref(false);
		const globalStore = useGlobalStore();
		bus.on('theme-drawer', () => (drawerVisible.value = true));
		const handleSwitchDark = () => {
			Switch();
		};
		return () => (
			<>
				<ElDrawer v-model={drawerVisible.value} title="主题设置">
					<ElColorPicker v-model={globalStore.primaryColor} onChange={ChagePrimary}></ElColorPicker>
					<ElSwitch v-model={globalStore.isDark} onChange={handleSwitchDark}></ElSwitch>
				</ElDrawer>
			</>
		);
	}
});
