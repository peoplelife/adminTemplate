import { ElAside, ElContainer, ElHeader, ElMenu, ElScrollbar } from 'element-plus';
import ToolBarLeft from './Header/ToolBarLeft/ToolBarLeft';
import ToolBarRight from './Header/ToolBarRight/ToolBarRight';
import SubMenu from './Menu/index';
import Main from './Main';
import './index.scss';
import { useAuthStore } from '@/stores/modules/auth';
import { useGlobalStore } from '@/stores/modules/global';
import { useRoute } from 'vue-router';
import ThemeDrawer from './components/ThemeDrawer';
export default defineComponent({
	setup() {
		const authStore = useAuthStore();
		const globalStore = useGlobalStore();
		const route = useRoute();
		const menuList = computed(() => authStore.showMenuListGet || []);
		const activeMenu = computed(() => route.path);
		return () => (
			<>
				<div class="layout">
					<ElContainer>
						<ElHeader>
							<div class="header-lf">
								<div class="logo">
									<img src="" />
									<span class="logo-text">后台管理系统模板</span>
								</div>
								<ToolBarLeft />
							</div>
							<div class="header-ri">
								<ToolBarRight />
							</div>
						</ElHeader>
						<ElContainer>
							<ElAside>
								<div class="aside-box" style={{ width: globalStore.isCollapse ? '65px' : '210px' }}>
									<ElScrollbar>
										<ElMenu
											collapse={globalStore.isCollapse}
											collapseTransition={false}
											router={false}
											defaultActive={activeMenu.value}
										>
											<SubMenu menuList={menuList.value} />
										</ElMenu>
									</ElScrollbar>
								</div>
							</ElAside>
							<ElContainer class="layout-main">
								<Main />
							</ElContainer>
						</ElContainer>
					</ElContainer>
				</div>
				<ThemeDrawer />
			</>
		);
	}
});
