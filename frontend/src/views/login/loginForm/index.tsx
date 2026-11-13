import { ElAlert, ElButton, ElForm, ElFormItem, ElInput, type FormInstance, type FormRules } from 'element-plus';
import { Lock, User } from '@element-plus/icons-vue';

import { useRouter } from 'vue-router';
import { getV1AuthGithubConfig, postV1AuthGithubAuthorize, postV1AuthLogin, type IPostV1AuthLoginReq } from '@/api';
import { finishLogin } from '@/utils/finishLogin';
import { startGitHubOAuth } from '@/utils/githubOAuth';

export default defineComponent({
	setup() {
		const router = useRouter();
		const loginRef = ref<FormInstance>();
		const passwordInputRef = ref<HTMLInputElement>();
		const githubEnabled = ref(false);
		const busy = ref<'password' | 'github' | ''>('');
		const errorMessage = ref('');
		onMounted(async () => {
			try {
				githubEnabled.value = (await getV1AuthGithubConfig()).enabled === true;
			} catch {
				githubEnabled.value = false;
			}
		});
		//表单数据
		const formData = ref<IPostV1AuthLoginReq>({
			username: '',
			password: ''
		});
		//校验规则
		const validateUsername = (_, value: any, callback: any) => {
			// if (!value) {
			// 	callback(new Error('请输入用户名'));
			// } else if (value.length < 8) {
			// 	callback(new Error('用户名长度不能小于8位!'));
			// } else {
			// 	callback();
			// }
		};
		const validatepassword = (_, value: any, callback: any) => {
			// const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,}$/;
			// if (!value) {
			// 	callback(new Error('请输入密码'));
			// } else if (!passwordRegex.test(value)) {
			// 	callback(new Error('密码长度为8位且必须包含大小写字母、数字和特殊符号!'));
			// } else {
			// 	callback();
			// }
		};
		const rules = ref<FormRules<typeof formData>>({
			username: [{ validator: validateUsername, trigger: 'blur' }],
			password: [{ validator: validatepassword, trigger: 'blur' }]
		});
		//提交逻辑
		const handleSumbit = async () => {
			if (!loginRef.value || busy.value) return;
			busy.value = 'password';
			errorMessage.value = '';
			try {
				const { token } = await postV1AuthLogin(formData.value);
				await finishLogin(token, router);
			} catch (error) {
				errorMessage.value = error?.message || '登录失败，请重试';
			} finally {
				busy.value = '';
			}
		};
		const loginWithGitHub = async () => {
			if (busy.value || !githubEnabled.value) return;
			busy.value = 'github';
			errorMessage.value = '';
			try {
				startGitHubOAuth(await postV1AuthGithubAuthorize(), { mode: 'login' });
			} catch (error) {
				errorMessage.value = error?.message || 'GitHub 授权失败，请重试';
				busy.value = '';
			}
		};
		const onUserkeydown = (e) => {
			if (e.key !== 'Enter' || e.isComposing) return;
			e.preventDefault();
			passwordInputRef.value?.focus();
		};
		const onPasswordkeydown = async (e) => {
			if (e.key !== 'Enter' || e.isComposing) return;
			e.preventDefault();
			await handleSumbit();
		};
		return () => (
			<ElForm ref={loginRef} class="w-300" model={formData.value} rules={rules.value} aria-busy={Boolean(busy.value)}>
				{errorMessage.value && <ElAlert type="error" title={errorMessage.value} closable={false} />}
				<ElFormItem prop="username">
					<ElInput
						v-model={formData.value.username}
						placeholder="请输入用户名"
						clearable
						prefixIcon={User}
						onKeydown={onUserkeydown}
					></ElInput>
				</ElFormItem>
				<ElFormItem prop="password">
					<ElInput
						ref={passwordInputRef}
						v-model={formData.value.password}
						placeholder="请输入密码"
						show-password
						clearable
						prefixIcon={Lock}
						onKeydown={onPasswordkeydown}
					></ElInput>
				</ElFormItem>
				<ElFormItem>
					<ElButton
						class="w-full"
						type="primary"
						loading={busy.value === 'password'}
						disabled={Boolean(busy.value)}
						onClick={handleSumbit}
					>
						登录
					</ElButton>
				</ElFormItem>
				{githubEnabled.value && (
					<>
						<ElFormItem>
							<ElButton class="w-full" loading={busy.value === 'github'} disabled={Boolean(busy.value)} onClick={loginWithGitHub}>
								使用 GitHub 登录
							</ElButton>
						</ElFormItem>
						<p class="text-12 text-gray-500">首次授权将自动创建普通用户账号</p>
					</>
				)}
			</ElForm>
		);
	}
});
