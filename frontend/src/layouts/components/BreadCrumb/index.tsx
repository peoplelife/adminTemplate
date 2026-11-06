import { ArrowRight, Search } from '@element-plus/icons-vue';
import { ElBreadcrumb, ElBreadcrumbItem, ElIcon } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import './index.scss';
import { useAuthStore } from '@/stores/modules/auth';
import { renderIcon } from '@/utils/icon';
import { TransitionGroup } from 'vue';
export default defineComponent({
	setup() {
		const route = useRoute();
		const router = useRouter();
		const authStore = useAuthStore();

		//获取当前路由数组
		const breadCrumbData = computed(() => authStore.breadcrumbListGet[route.matched[route.matched.length - 1].path] ?? []);

		const handleClickBreadCrumb = (item, index) => {
			if (index !== breadCrumbData.value.length - 1) router.push(item.path);
		};
		return () => (
			<div class="breadcrumb-box">
				<ElBreadcrumb separatorIcon={ArrowRight}>
					<TransitionGroup name="breadcrumb">
						{breadCrumbData.value.map((item, index) => (
							<ElBreadcrumbItem to={item.path} key={item.menuId}>
								<div class="breadcrumb-inner" onClick={() => handleClickBreadCrumb(item, index)}>
									<ElIcon class="breadcrumb-icon">{h(renderIcon(item.meta.icon))}</ElIcon>
									<span class="breadcrumb-title">{item.name}</span>
								</div>
							</ElBreadcrumbItem>
						))}
					</TransitionGroup>
				</ElBreadcrumb>
			</div>
		);
	}
});
