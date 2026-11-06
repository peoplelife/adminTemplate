import { useDataScreenScale } from '@/hooks/useDataScreenScale';
import './index.scss';
import DataScreenMenu from './DataScreenMenu';
export default defineComponent({
	setup(_, { slots }) {
		const { dataScreenRef } = useDataScreenScale();
		return () => (
			<div class="mci-data-screen-container">
				<div class="mci-data-screen-content" ref={dataScreenRef}>
					<DataScreenMenu />
					<div class="mci-data-screen-main">{slots.default?.()}</div>
				</div>
			</div>
		);
	}
});
