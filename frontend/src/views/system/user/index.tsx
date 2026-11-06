import { defineComponent } from 'vue';
import { BaseTable } from '@/components/base';
import { tableConfig } from './table-schema';
import { searchSchema } from './search-schema';
import { editSchema } from './edit-schema';
import {
	getV1Roles,
	getV1UsersExport,
	patchV1UsersById,
	postV1Users,
	type IGetV1RolesRes
} from '@/api';
import { ElMessage } from 'element-plus';

export default defineComponent({
	setup() {
		type RoleItem = IGetV1RolesRes['list'][number];
		const roleList = ref<RoleItem[]>([]);

		const onAdd = async (exposes, setSubmitLoading) => {
			const model = exposes.editViewRef.value.getModel();
			setSubmitLoading(true);
			try {
				await postV1Users(model);
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
				await patchV1UsersById({ ...model, id: String(model.id) });
				ElMessage.success('编辑成功');
				exposes.drawerVisible.value = false;
				exposes.onReset();
			} finally {
				setSubmitLoading(false);
			}
		};

		const getRoleList = async () => {
			const { list } = await getV1Roles({ page: '1', pageSize: '100' });
			roleList.value = list;
		};

		const normalizer = () => {
			return roleList.value.map((item) => {
				return {
					label: item.name,
					value: item.code
				};
			});
		};

		const roleOptions = computed(() => normalizer());

		onMounted(getRoleList);

		return () => (
			<>
				<div class="user-mannagement">
					<BaseTable
						tableConfig={tableConfig}
						searchSchema={searchSchema()}
						editViewSchema={editSchema(roleOptions.value)}
						tableActions={['add', 'export']}
						rowKey="id"
						onAdd={onAdd}
						onEdit={onEdit}
						exportConfig={{
							api: getV1UsersExport
						}}
					/>
				</div>
			</>
		);
	}
});
