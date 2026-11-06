import { ElIcon, ElTabPane, ElTabs, type TabPaneName, type TabsPaneContext } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import './index.scss';
import { useTabsStore } from '@/stores/modules/tabs';
import { useAuthStore } from '@/stores/modules/auth';
import { renderIcon } from '@/utils/icon';

export default defineComponent({
	setup() {
		const route = useRoute();
		const router = useRouter();
		const tabsStore = useTabsStore();
		const authStore = useAuthStore();
		const currentTabs = ref(route.fullPath);
		const tabsList = computed(() => tabsStore.tabsList);
		//初始化tabs
		const initTabs = () => {
			authStore.flatMenuListGet.forEach((item) => {
				if (!item.meta.is_full && !item.meta.is_hidden) {
					const params = {
						icon: item.meta.icon,
						name: item.name,
						path: item.path
					};
					tabsStore.addTabs(params);
				}
			});
		};
		//移除tabs,移除当前/其他标签
		const tabRemove = (fullPath: TabPaneName) => {
			tabsStore.removeTabs(fullPath as string, fullPath === route.fullPath);
		};
		//跳转tabs
		const tabClick = (tabs: TabsPaneContext) => {
			router.push(tabs.props.name as string);
		};
		//监听路由变化
		watch(
			() => route.fullPath,
			() => {
				if (route.meta.is_full) return;
				currentTabs.value = route.fullPath;
				const params = {
					icon: route.meta.icon,
					name: route.name,
					path: route.path
				};
				tabsStore.addTabs(params);
			}
		);
		onMounted(() => {
			if (!tabsStore.tabsList) return;
			initTabs();
		});
		return () => (
			<>
				<div class="tabs-box">
					<ElTabs v-model={currentTabs.value} type="card" onTabRemove={tabRemove} onTabClick={tabClick}>
						{tabsList.value.map((item) => (
							<ElTabPane
								key={item.path}
								name={item.path}
								closable
								v-slots={{
									label: () => (
										<div class="tabs-label">
											<ElIcon>{item.icon && h(renderIcon(item.icon))}</ElIcon>
											<span class="tabs-label-text">{item.name}</span>
										</div>
									)
								}}
							></ElTabPane>
						))}
					</ElTabs>
				</div>
			</>
		);
	}
});
