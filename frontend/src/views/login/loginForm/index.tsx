import { ElButton, ElForm, ElFormItem, ElInput, ElNotification, type FormInstance, type FormRules } from 'element-plus';
import { Lock, User } from '@element-plus/icons-vue';

import { useRouter } from 'vue-router';
import { getV1Me, postV1AuthLogin, type IPostV1AuthLoginReq } from '@/api';
import { useUserStore } from '@/stores/modules/user';
import { initDynamicRouters } from '@/routers/modules/dynamicRouters';
import { useAuthStore } from '@/stores/modules/auth';
import { HOME_URL } from '@/config';
import { getTimeState } from '@/utils/time';

export default defineComponent({
	setup() {
		const userStore = useUserStore();
		const authStore = useAuthStore();
		const menuList = computed(() => authStore.showMenuListGet);
		const router = useRouter();
		const loginRef = ref<FormInstance>();
		const passwordInputRef = ref<HTMLInputElement>();
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
		const getFirstMenuPath = (menu: Menu.MenuOptions) => {
			let path = '';
			if (menu.children && menu.children.length) {
				return getFirstMenuPath(menu.children[0]);
			} else {
				path = menu.path;
			}
			return path;
		};
		//提交逻辑
		const handleSumbit = async () => {
			if (!loginRef.value) return;

			// await loginRef.value.validate();
			const { token } = await postV1AuthLogin(formData.value);
			//将token保存 后续注入请求头中
			userStore.setToken(token);

			//获取用户信息
			const userInfo = await getV1Me();
			userStore.setUserInfo(userInfo);

			//添加动态路由
			await initDynamicRouters();
			const homeAuth = menuList.value?.find((item) => item.path === HOME_URL);
			console.log(homeAuth);
			if (homeAuth) {
				// 有首页权限跳转到首页
				router.push(HOME_URL);
			} else {
				// 没有首页权限,跳转至有权限的第一个菜单
				const firstMenuPath = getFirstMenuPath(menuList.value[0]);
				router.push(firstMenuPath);
			}

			ElNotification({
				title: getTimeState(),
				message: '欢迎登录后台模板系统',
				type: 'success',
				duration: 3000
			});
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
			<ElForm ref={loginRef} class="w-300" model={formData.value} rules={rules.value}>
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
					<ElButton class="w-full" type="primary" onClick={handleSumbit}>
						登录
					</ElButton>
				</ElFormItem>
			</ElForm>
		);
	}
});
