import { ElDropdown, ElDropdownItem, ElDropdownMenu, ElMessage } from 'element-plus';
import './index.scss';
import { postV1AuthLogout as logoutRequest } from '@/api';
import resetAll from '@/stores/modules/resetAll';
import router from '@/routers';
import { LOGIN_URL } from '@/config';
import { useUserStore } from '@/stores/modules/user';
import { computed } from 'vue';
import GitHubAccountDialog from './GitHubAccountDialog';
export default defineComponent({
	setup() {
		const userStore = useUserStore();
		const accountDialogVisible = ref(false);
		const avatar = computed(() => import.meta.env.VITE_API_URL + userStore.userInfo.avatar);
		//退出登录
		const logout = async () => {
			await logoutRequest();
			resetAll();
			ElMessage.success('退出登录成功!');
			router.push(LOGIN_URL);
		};

		const handleCommand = (value: string) => {
			if (value === 'account') accountDialogVisible.value = true;
			if (value === 'logout') {
				logout();
			}
		};
		return () => (
			<div class="dropdown">
				<ElDropdown trigger="click" onCommand={handleCommand}>
					{{
						//默认插槽
						default: () => (
							<div class="w-40 h-40 rounded-full overflow-hidden cursor-pointer">
								<img src={avatar.value} alt="头像" class="w-full h-full" />
							</div>
						),
						//下拉菜单插槽
						dropdown: () => (
							<ElDropdownMenu>
								<ElDropdownItem command="account">账号绑定</ElDropdownItem>
								<ElDropdownItem command="logout">退出登录</ElDropdownItem>
							</ElDropdownMenu>
						)
					}}
				</ElDropdown>
				<GitHubAccountDialog
					modelValue={accountDialogVisible.value}
					onUpdate:modelValue={(value) => (accountDialogVisible.value = value)}
				/>
			</div>
		);
	}
});
