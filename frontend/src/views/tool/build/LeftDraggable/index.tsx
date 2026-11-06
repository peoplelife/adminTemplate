import { components } from '@/utils/generator/config';
import { VueDraggable } from 'vue-draggable-plus';
import {
	Document,
	EditPen,
	Finished,
	Notebook,
	Tickets,
	Clock,
	Calendar
} from '@element-plus/icons-vue';

const ICON_MAP: Record<string, any> = {
	input: EditPen,
	textarea: Notebook,
	number: Finished,
	select: Tickets,
	radio: Document,
	checkbox: Notebook,
	date: Calendar,
	time: Clock
};

export default defineComponent({
	props: {
		group: {
			type: Object,
			default: () => ({ name: 'components', pull: 'clone', put: false })
		}
	},
	setup(props) {
		const { group } = toRefs(props);
		const Draggable = VueDraggable as any;
		const list = ref(components);

		const sections = computed(() => {
			return [
				{
					title: '基础字段',
					items: list.value.filter((i) => ['input', 'textarea', 'number'].includes(i.type))
				},
				{
					title: '选择字段',
					items: list.value.filter((i) => ['select', 'radio', 'checkbox'].includes(i.type))
				},
				{
					title: '日期时间',
					items: list.value.filter((i) => ['date', 'time'].includes(i.type))
				}
			].filter((s) => s.items.length > 0);
		});

		const renderIcon = (type: string) => {
			const Icon = ICON_MAP[type] || Document;
			return <Icon />;
		};

		return () => (
			<div class="panel-left compact">
				<div class="panel-left__header">
					<div class="panel-left__title">组件库</div>
					<div class="panel-left__tip">拖拽组件到设计区</div>
				</div>
				<Draggable v-model={list.value} group={group.value} sort={false} class="panel-left__list compact" ghostClass="ghost">
					{sections.value.map((section) => (
						<div class="comp-section" key={section.title}>
							<div class="comp-section__title">{section.title}</div>
							{section.items.map((item: any) => (
								<div class="comp-chip" key={item.type} data-type={item.type}>
									<div class="comp-chip__icon">{renderIcon(item.type)}</div>
									<div class="comp-chip__text">
										<div class="comp-chip__title">{item.label}</div>
									</div>
								</div>
							))}
						</div>
					))}
				</Draggable>
			</div>
		);
	}
});
