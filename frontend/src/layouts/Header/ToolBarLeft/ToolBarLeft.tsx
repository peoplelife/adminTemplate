import BreadCrumb from '@/layouts/components/BreadCrumb';
import CollapseIcon from '@/layouts/components/CollapseIcon';
import './ToolBarLeft.scss';
export default defineComponent({
	setup() {
		return () => (
			<div class="toolbar-left">
				<CollapseIcon />
				<BreadCrumb />
			</div>
		);
	}
});
