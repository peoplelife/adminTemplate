import DataScreenContainer from '@/components/DataScreenContainer';
import './index.scss';
import MapChart from './MapChart';

export default defineComponent({
	setup() {
		return () => (
			<DataScreenContainer>
				<div class="data-content">
					<div class="left"></div>
					<div class="center">
						<MapChart />
					</div>
					<div class="right"></div>
				</div>
			</DataScreenContainer>
		);
	}
});
