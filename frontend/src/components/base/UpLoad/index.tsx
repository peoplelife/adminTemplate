import { postV1Files as uploadFileRequest } from '@/api';
import { Plus } from '@element-plus/icons-vue';
import { ElUpload, ElIcon, ElMessage, type UploadFile } from 'element-plus';
import { defineComponent, toRefs } from 'vue';

export const UpLoad = defineComponent({
	props: {
		modelValue: {
			type: String,
			default: ''
		},
		limit: {
			type: Number,
			default: 1
		}
	},
	emits: ['update:modelValue'],
	setup(props, { emit, attrs }) {
		const API_URL = import.meta.env.VITE_API_URL;
		const { modelValue, limit } = toRefs(props);
		const fileList = ref<UploadFile[]>([]);
		const uploadFile = async (options) => {
			const { file, onError, onSuccess } = options;
			try {
				const data = new FormData();
				data.append('file', file);
				data.append('path', 'uploads');
				const res = await uploadFileRequest(undefined, { data });
				const uploadFile: UploadFile = {
					name: res.originalName,
					url: res.url,
					status: 'success',
					uid: Date.now()
				};
				onSuccess(uploadFile);
				ElMessage.success('上传成功');
			} catch (error) {
				onError(error);
			}
		};

		const handleSuccess = (response) => {
			const url = response.url;
			emit('update:modelValue', url);
		};
		watch(
			() => modelValue.value,
			(newValue) => {
				if (newValue) {
					fileList.value = [
						{
							name: newValue.split('/').pop() ?? 'image',
							url: API_URL + newValue,
							status: 'success',
							uid: Date.now()
						}
					];
				} else {
					fileList.value = [];
				}
			},
			{ immediate: true }
		);
		return () => (
			<ElUpload
				{...attrs}
				listType="picture-card"
				v-model:file-list={fileList.value}
				httpRequest={uploadFile}
				onSuccess={handleSuccess}
				limit={limit.value}
			>
				<ElIcon>
					<Plus />
				</ElIcon>
			</ElUpload>
		);
	}
});
