import { defineComponent, ref, computed } from 'vue';
import { VueDraggable } from 'vue-draggable-plus';
import './index.scss';
import LeftDraggable from './LeftDraggable';
import RightPannel from './RightPannel';
import { BASE_COMPONENTS } from '@/components/base/config';
import { ElButton, ElCol, ElEmpty, ElForm, ElFormItem, ElMessage, ElRow, ElSpace } from 'element-plus';

type FieldConfig = {
	id: string;
	type: string;
	label: string;
	prop: string;
	span?: number;
	placeholder?: string;
	[key: string]: any;
};

const createField = (type: string): FieldConfig => {
	const uid = `${type}_${Date.now()}_${Math.random().toString(16).slice(2, 6)}`;
	const base: FieldConfig = {
		id: uid,
		type,
		label: '新字段',
		prop: `field_${uid}`,
		span: 12,
		placeholder: '请输入'
	};
	if (type === 'select') {
		base.options = [
			{ value: '1', label: '选项一' },
			{ value: '2', label: '选项二' }
		];
		base.placeholder = '请选择';
	}
	if (type === 'slider') {
		base.min = 0;
		base.max = 100;
		base.marks = { 50: '50' };
		base.placeholder = '';
	}
	return base;
};

const generateVueFile = (fields: FieldConfig[]) => {
	const formItems = fields
		.map((f) => {
			const comp = (() => {
				switch (f.type) {
					case 'select':
						return `<el-select v-model="form.${f.prop}" placeholder="${f.placeholder || ''}">
        ${(f.options || [])
					.map((op: any) => `<el-option label="${op.label}" value="${op.value}" />`)
					.join('\n        ')}
      </el-select>`;
					case 'slider':
						return `<el-slider v-model="form.${f.prop}" :min="${f.min ?? 0}" :max="${f.max ?? 100}" />`;
					default:
						return `<el-input v-model="form.${f.prop}" placeholder="${f.placeholder || ''}" />`;
				}
			})();
			return `      <el-col :span="${f.span || 24}">
        <el-form-item label="${f.label}" prop="${f.prop}">
${comp}
        </el-form-item>
      </el-col>`;
		})
		.join('\n');

	const modelInit = fields
		.map((f) => `  ${f.prop}: ${f.type === 'select' ? "''" : 0}`)
		.join(',\n');

	return `<template>
  <el-form :model="form">
    <el-row :gutter="16">
${formItems}
    </el-row>
  </el-form>
</template>

<script setup lang="ts">
import { reactive } from 'vue';

const form = reactive({
${modelInit}
});
</script>
`;
};

export default defineComponent({
	setup() {
		const Draggable = VueDraggable as any;
		const group = ref('components');
		const drawerList = ref<FieldConfig[]>([]);
		const activeId = ref<string | null>(null);
		const activeData = computed(() => drawerList.value.find((f) => f.id === activeId.value) || {});

		const onSelectCom = (id: string) => {
			activeId.value = id;
		};

		const handleUpdateActive = (val: any) => {
			const idx = drawerList.value.findIndex((f) => f.id === activeId.value);
			if (idx >= 0) drawerList.value[idx] = { ...drawerList.value[idx], ...val };
		};

		const onAdd = (evt: any) => {
			const idx = evt.newIndex;
			const raw = drawerList.value[idx];
			const type = raw?.type || raw?.value || 'input';
			const field = createField(type);
			drawerList.value[idx] = field;
			activeId.value = field.id;
		};

		const handleClear = () => {
			drawerList.value = [];
			activeId.value = null;
		};

		const handleExport = () => {
			if (!drawerList.value.length) {
				ElMessage.warning('请先拖入组件再导出');
				return;
			}
			const content = generateVueFile(drawerList.value);
			const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
			const url = URL.createObjectURL(blob);
			const a = document.createElement('a');
			a.href = url;
			a.download = `generated-form.vue`;
			a.click();
			URL.revokeObjectURL(url);
			ElMessage.success('已导出 Vue 文件');
		};

		return () => (
			<div class="builder">
				<LeftDraggable group={{ name: 'components', pull: 'clone', put: false }} />

				<div class="canvas">
					<div class="toolbar">
						<ElSpace>
							<ElButton type="primary" onClick={handleExport}>
								导出 Vue 文件
							</ElButton>
							<ElButton onClick={handleClear}>清空画布</ElButton>
						</ElSpace>
						<div class="toolbar__meta">组件数：{drawerList.value.length}</div>
					</div>

					<Draggable
						class="canvas__body"
						group={group.value}
						v-model={drawerList.value}
						onAdd={onAdd}
						ghostClass="ghost"
					>
						{drawerList.value.length === 0 ? (
							<div class="canvas__empty">
								<ElEmpty description="从左侧拖拽组件到此处" />
							</div>
						) : (
							<ElForm labelPosition="left">
								<ElRow gutter={16}>
									{drawerList.value.map((item) => {
										const { span = 24, prop, label, id, ...attrs } = item;
										const Com = BASE_COMPONENTS[item.type];
										return (
											<ElCol span={span} key={id}>
												<ElFormItem prop={prop} label={label}>
													<div
														class={`field ${activeId.value === id ? 'field--active' : ''}`}
														onClick={() => onSelectCom(id)}
													>
														{Com ? <Com {...attrs} /> : <div class="text-slate-400 text-sm">未知组件</div>}
													</div>
												</ElFormItem>
											</ElCol>
										);
									})}
								</ElRow>
							</ElForm>
						)}
					</Draggable>
				</div>

				<RightPannel activeData={activeData.value} onUpdate:activeData={handleUpdateActive} />
			</div>
		);
	}
});
