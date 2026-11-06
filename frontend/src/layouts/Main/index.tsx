import { ElMain } from 'element-plus';
import { RouterView, useRoute } from 'vue-router';
import Tabs from '../components/Tabs';
import './index.scss';
import { Transition } from 'vue';
export default defineComponent({
	setup() {
		const route = useRoute();
		return () => (
			<>
				<Tabs />
				<ElMain>
					<RouterView
						v-slots={{
							default: ({ Component }) => (
								<Transition name="fade" mode="out-in">
									{Component ? (
										<div key={route.path} class="h-full w-full flex-1 min-h-0 min-w-0">
											<Component />
										</div>
									) : null}
								</Transition>
							)
						}}
					/>
				</ElMain>
			</>
		);
	}
});
