import { useGlobalStore } from '@/stores/modules/global';
import { Expand, Fold } from '@element-plus/icons-vue';
import { ElIcon } from 'element-plus';
import './index.scss';
export default defineComponent({
	setup() {
		const globalStore = useGlobalStore();

		const toggleCollapse = () => {
			globalStore.isCollapse = !globalStore.isCollapse;
		};
		return () => (
			<div class="collapse-icon" onClick={toggleCollapse}>
				<ElIcon>{globalStore.isCollapse ? <Expand /> : <Fold />}</ElIcon>
			</div>
		);
	}
});
