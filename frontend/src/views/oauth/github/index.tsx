import { defineComponent, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElAlert, ElButton, ElCard, ElNotification } from 'element-plus';
import { postV1AuthGithubComplete, postV1MeGithubComplete } from '@/api';
import { LOGIN_URL } from '@/config';
import { initDynamicRouters } from '@/routers/modules/dynamicRouters';
import { useAuthStore } from '@/stores/modules/auth';
import { useUserStore } from '@/stores/modules/user';
import { consumeGitHubOAuth } from '@/utils/githubOAuth';
import { finishLogin, getLoginDestination } from '@/utils/finishLogin';

export default defineComponent({
	setup() {
		const router = useRouter();
		const userStore = useUserStore();
		const loading = ref(true);
		const errorMessage = ref('');
		onMounted(async () => {
			try {
				const flow = consumeGitHubOAuth();
				if (!flow) throw new Error('授权上下文已失效，请重新发起 GitHub 登录或绑定');
				if (flow.mode === 'login') {
					const { token } = await postV1AuthGithubComplete({ state: flow.state });
					await finishLogin(token, router);
				} else {
					const currentUserId = (userStore.userInfo as { id?: number }).id;
					if (!userStore.token || (flow.userId !== undefined && flow.userId !== currentUserId)) {
						throw new Error('当前登录账号已失效或发生变化，请重新登录后发起绑定');
					}
					const account = await postV1MeGithubComplete({ state: flow.state });
					if (!account.bound) throw new Error('GitHub 账号未绑定，请重试');
					ElNotification({ message: `已绑定 GitHub 账号 ${account.login || ''}`, type: 'success', duration: 3000 });
					await returnToApp();
				}
			} catch (error) {
				errorMessage.value = error?.message || 'GitHub 授权失败，请重新尝试';
			} finally {
				loading.value = false;
			}
		});
		const returnToApp = async () => {
			try {
				if (userStore.token) {
					await initDynamicRouters();
					await router.replace(getLoginDestination(useAuthStore().showMenuListGet));
				} else {
					await router.replace(LOGIN_URL);
				}
			} catch (error) {
				errorMessage.value = error?.message || '页面跳转失败，请重试';
			}
		};
		return () => (
			<div class="flex items-center justify-center w-full h-full">
				<ElCard style={{ width: 'min(440px, 90vw)' }} aria-busy={loading.value}>
					<h3>GitHub 账号</h3>
					{loading.value ? (
						<p role="status" aria-live="polite">
							正在完成 GitHub 授权，请稍候…
						</p>
					) : (
						<>
							{errorMessage.value && <ElAlert type="error" title={errorMessage.value} closable={false} />}
							<ElButton type="primary" class="mt-20" onClick={returnToApp}>
								{userStore.token ? '返回系统，重新绑定' : '返回登录，重试授权'}
							</ElButton>
						</>
					)}
				</ElCard>
			</div>
		);
	}
});
