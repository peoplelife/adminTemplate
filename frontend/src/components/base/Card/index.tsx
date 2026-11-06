import { useGlobalStore } from '@/stores/modules/global';
import { ElCard } from 'element-plus';

export const Card = defineComponent({
	props: {
		title: {
			type: String,
			default: ''
		}
	},
	setup(props, { slots }) {
		const { title } = toRefs(props);
		const globalStore = useGlobalStore();
		const primary = computed(() => globalStore.primaryColor);
		const showHeader = computed(() => {
			if (title.value || slots.headerLeft || slots.headerRight) return true;
			return false;
		});
		const renderHeaderLeft = () => {
			if (slots.headerLeft) return slots.headerLeft?.();
			return (
				<div class="text-18 font-bold pl-10 relative">
					<i class="absolute w-5 h-20 top-2 left-0 rounded-10" style={{ background: primary.value }}></i>
					<span>{title.value}</span>
				</div>
			);
		};
		return () => (
			<div>
				<ElCard>
					{showHeader.value && (
						<div class="flex justify-between items-center mb-20">
							{renderHeaderLeft()}
							{slots.headerRight?.()}
						</div>
					)}
					{slots.default?.()}
				</ElCard>
			</div>
		);
	}
});
