import { ElCard } from 'element-plus';
import './index.scss';
import LoginForm from './loginForm';
export default defineComponent({
	setup() {
		return () => (
			<div class="login-container flex items-center justify-center">
				<ElCard class="flex items-center justify-center w-400">
					<h4>后台管理系统模板</h4>
					<LoginForm />
				</ElCard>
			</div>
		);
	}
});
