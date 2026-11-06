import { BaseTable } from '@/components/base';
import { tableSchema } from './table-schema';
import { searchSchema } from './search-schema';
import { editschema } from './edit-schema';
import { patchV1MenusById, postV1Menus } from '@/api';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/stores/modules/auth';
import { buildRouteMenuTree } from '@/utils/menu';
export default defineComponent({
	setup() {
		const authStore = useAuthStore();

		const onEdit = async (exposes, setSubmitLoading) => {
			try {
				const model = exposes.editViewRef.value.getModel();
				setSubmitLoading(true);
				await patchV1MenusById({ ...model, id: String(model.id) });
				setSubmitLoading(false);
				ElMessage.success('编辑成功');
				exposes.drawerVisible.value = false;
				exposes.onReset();
			} catch (error) {
				setSubmitLoading(false);
			}
		};

		const onAdd = async (exposes, setSubmitLoading) => {
			try {
				const model = exposes.editViewRef.value.getModel();
				setSubmitLoading(true);
				await postV1Menus(model);
				setSubmitLoading(false);
				ElMessage.success('新增成功');
				exposes.drawerVisible.value = false;
				exposes.onReset();
			} catch (error) {
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

		const menuList = computed(() => [
			{
				value: '-1',
				label: '顶级菜单',
				children: normalizer(buildRouteMenuTree(authStore.allMenuList))
			}
		]);
		return () => (
			<>
				<div>
					<BaseTable
						rowKey="id"
						tableConfig={tableSchema}
						searchSchema={searchSchema()}
						editViewSchema={editschema(menuList.value)}
						onEdit={onEdit}
						onAdd={onAdd}
					/>
				</div>
			</>
		);
	}
});
