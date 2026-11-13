import { defineComponent, ref, watch } from 'vue';
import { ElAlert, ElButton, ElDialog, ElMessageBox, ElNotification } from 'element-plus';
import { deleteV1MeGithub, getV1MeGithub, postV1MeGithubAuthorize } from '@/api';
import { useUserStore } from '@/stores/modules/user';
import { startGitHubOAuth } from '@/utils/githubOAuth';

export default defineComponent({
	props: { modelValue: Boolean },
	emits: ['update:modelValue'],
	setup(props, { emit }) {
		const userStore = useUserStore();
		const account = ref<Awaited<ReturnType<typeof getV1MeGithub>>>();
		const busy = ref<'load' | 'bind' | 'unbind' | ''>('');
		const errorMessage = ref('');
		const load = async () => {
			if (busy.value) return;
			busy.value = 'load';
			account.value = undefined;
			errorMessage.value = '';
			try {
				account.value = await getV1MeGithub();
			} catch (error) {
				errorMessage.value = error?.message || '获取 GitHub 账号失败，请重试';
			} finally {
				busy.value = '';
			}
		};
		watch(
			() => props.modelValue,
			(visible) => {
				if (visible) void load();
			}
		);
		const bind = async () => {
			if (busy.value || !account.value?.enabled) return;
			busy.value = 'bind';
			errorMessage.value = '';
			try {
				const authorization = await postV1MeGithubAuthorize();
				startGitHubOAuth(authorization, { mode: 'bind', userId: (userStore.userInfo as { id?: number }).id });
			} catch (error) {
				errorMessage.value = error?.message || 'GitHub 绑定授权失败，请重试';
				busy.value = '';
			}
		};
		const unbind = async () => {
			if (busy.value || !account.value?.bound) return;
			busy.value = 'unbind';
			errorMessage.value = '';
			try {
				const confirmed = await ElMessageBox.confirm('解除后将无法使用此 GitHub 账号登录，是否继续？', '解除 GitHub 绑定', {
					type: 'warning',
					confirmButtonText: '解除绑定',
					cancelButtonText: '取消'
				}).catch(() => false);
				if (confirmed !== 'confirm') return;
				await deleteV1MeGithub();
				account.value = { ...account.value, bound: false, login: null };
				ElNotification({ message: '已解除 GitHub 绑定', type: 'success', duration: 3000 });
			} catch (error) {
				errorMessage.value = error?.message || '解除 GitHub 绑定失败，请重试';
			} finally {
				busy.value = '';
			}
		};
		return () => (
			<ElDialog
				title="GitHub 账号"
				width="min(440px, 90vw)"
				modelValue={props.modelValue}
				onUpdate:modelValue={(value) => emit('update:modelValue', value)}
				closeOnClickModal={!busy.value}
				closeOnPressEscape={!busy.value}
				showClose={!busy.value}
				appendToBody
			>
				<div aria-busy={Boolean(busy.value)}>
					{errorMessage.value && <ElAlert type="error" title={errorMessage.value} closable={false} />}
					{busy.value === 'load' && <p role="status">正在获取 GitHub 账号…</p>}
					{account.value && (
						<>
							{!account.value.enabled && <p>GitHub 登录暂未启用，启用后可查看和管理绑定</p>}
							{(account.value.enabled || account.value.bound) && (
								<p>{account.value.bound ? `已绑定：${account.value.login || 'GitHub 账号'}` : '尚未绑定 GitHub 账号'}</p>
							)}
							{account.value.bound ? (
								<ElButton type="danger" plain loading={busy.value === 'unbind'} disabled={Boolean(busy.value)} onClick={unbind}>
									解除绑定
								</ElButton>
							) : account.value.enabled ? (
								<ElButton type="primary" loading={busy.value === 'bind'} disabled={Boolean(busy.value)} onClick={bind}>
									绑定 GitHub
								</ElButton>
							) : null}
						</>
					)}
					{!account.value && !busy.value && <ElButton onClick={load}>重试</ElButton>}
				</div>
			</ElDialog>
		);
	}
});
