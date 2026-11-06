import { ElTreeSelect } from 'element-plus';

export const TreeSelect = defineComponent({
	props: {
		modelValue: {
			type: [String, Number, Array],
			default: ''
		}
	},
	emits: ['update:modelValue'],
	setup(props, { emit, attrs }) {
		const { modelValue } = toRefs(props);
		const value = computed({
			get: () => modelValue.value,
			set: (val) => {
				console.log(val);
				emit('update:modelValue', val);
			}
		});

		const nodeMap = new Map();

		const parentMap = new Map();

		const buildMap = (nodes = [], parent = null) => {
			nodes.forEach((node) => {
				nodeMap.set(node.value, node);
				if (parent) parentMap.set(node.value, parent.value);
				if (node.children?.length) buildMap(node.children, node);
			});
		};

		// 处理 attrs.data 为空的情况，避免报错
		buildMap(((attrs as any).data || []) as any);

		const removeDesc = (node, set) => {
			set.delete(node.value);
			node.children?.forEach((child) => removeDesc(child, set));
		};

		const cleanAncestors = (valueKey, set) => {
			let p = parentMap.get(valueKey);
			while (p) {
				const parentNode = nodeMap.get(p);
				const hasChild = parentNode?.children?.some((c) => set.has(c.value));
				if (!hasChild) set.delete(p);
				p = parentMap.get(p);
			}
		};

		const collectDesc = (node, set) => {
			set.add(node.value);
			node.children?.forEach((c) => collectDesc(c, set));
		};

		const collectAncestors = (valueKey, set) => {
			let p = parentMap.get(valueKey);
			while (p) {
				set.add(p);
				p = parentMap.get(p);
			}
		};

		const onNodeClick = (node) => {
			const next = new Set((value.value as any) || []);
			const current = nodeMap.get(node.value) || node; // 兜底避免找不到
			const isChecked = next.has(node.value);
			if (isChecked) {
				// 取消选中
				removeDesc(current, next);
				cleanAncestors(node.value, next);
			} else {
				collectDesc(current, next);
				collectAncestors(node.value, next);
			}
			emit('update:modelValue', Array.from(next));
		};

		return () => (
			<div class="w-full">
				<ElTreeSelect v-model={value.value} {...attrs} onNodeClick={onNodeClick} />
			</div>
		);
	}
});
