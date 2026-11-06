import { defineComponent } from 'vue';
import { BaseTable } from '@/components/base';
import { tableConfig } from './table-schema';
import { searchSchema } from './search-schema';
import { editSchema } from './edit-schema';
import { patchV1RolesById, postV1Roles } from '@/api';
import { ElMessage } from 'element-plus';
import { buildRouteMenuTree } from '@/utils/menu';
import { useAuthStore } from '@/stores/modules/auth';

export default defineComponent({
	setup() {
		const authStore = useAuthStore();

		const onAdd = async (exposes, setSubmitLoading) => {
			const model = exposes.editViewRef.value.getModel();
			setSubmitLoading(true);
			try {
				await postV1Roles(model);
				ElMessage.success('新增成功');
				exposes.drawerVisible.value = false;
				exposes.onReset();
			} finally {
				setSubmitLoading(false);
			}
		};
		const onEdit = async (exposes, setSubmitLoading) => {
			const model = exposes.editViewRef.value.getModel();
			setSubmitLoading(true);
			try {
				await patchV1RolesById({ ...model, id: String(model.id) });
				ElMessage.success('编辑成功');
				exposes.drawerVisible.value = false;
				exposes.onReset();
			} finally {
				setSubmitLoading(false);
			}
		};

		const normalizer = (nodeList) => {
			const normalizerList = nodeList.map((item) => {
				return {
					value: item.menuId,
					label: item.name,
					children: item.children && item.children.length > 0 ? normalizer(item.children) : []
				};
			});
			return normalizerList;
		};

		const menuList = computed(() => normalizer(buildRouteMenuTree(authStore.allMenuList)));
		return () => (
			<>
				<div class="user-mannagement">
					<BaseTable
						tableConfig={tableConfig}
						searchSchema={searchSchema()}
						editViewSchema={editSchema(menuList.value)}
						tableActions={['add']}
						rowKey="id"
						onAdd={onAdd}
						onEdit={onEdit}
					/>
				</div>
			</>
		);
	}
});
