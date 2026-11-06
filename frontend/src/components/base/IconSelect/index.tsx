import { ElIcon, ElInput, ElPopover, ElScrollbar } from 'element-plus';
import * as icon from '@element-plus/icons-vue';
import { defineComponent, ref, computed, toRefs, nextTick, onMounted } from 'vue';
import { useGlobalStore } from '@/stores/modules/global';

export const IconSelect = defineComponent({
	name: 'IconSelect',
	props: {
		modelValue: {
			type: String,
			default: ''
		}
	},
	emits: ['update:modelValue'],
	setup(props, { emit, attrs }) {
		const icons = Object.keys(icon);
		const iconArray = ref(icon);
		const inputRef = ref<any>(null);
		const scrollbarRef = ref<any>(null);
		const iconRefs = ref<Record<string, HTMLElement | null>>({});
		const popoverRef = ref<any>(null);
		const width = ref(0);
		const searchText = ref('');
		const globalStore = useGlobalStore();
		const { modelValue } = toRefs(props);

		const value = computed({
			get: () => modelValue.value,
			set: (val: string) => {
				emit('update:modelValue', val);
			}
		});

		const popoverWidth = computed(() => {
			return width.value > 0 ? `${width.value}px` : 'auto';
		});

		const setPopoverWidth = () => {
			nextTick(() => {
				if (inputRef.value?.$el) {
					width.value = inputRef.value.$el.offsetWidth;
				}
			});
		};

		const scrollToSelectedIcon = () => {
			if (value.value && iconRefs.value[value.value]) {
				const scrollbarEl = scrollbarRef.value?.$el.querySelector('.el-scrollbar__wrap') as HTMLElement | null;
				const iconEl = iconRefs.value[value.value];
				if (scrollbarEl && iconEl) {
					const iconRect = iconEl.getBoundingClientRect();
					const scrollbarRect = scrollbarEl.getBoundingClientRect();
					const offsetTop = iconEl.offsetTop;
					scrollbarEl.scrollTop = offsetTop - scrollbarRect.height / 2 + iconRect.height / 2;
				}
			}
		};

		const onFocus = () => {
			setPopoverWidth();
			nextTick(() => {
				scrollToSelectedIcon();
			});
		};

		const handleClickIcon = (iconName: string) => {
			value.value = iconName;
			nextTick(() => {
				inputRef.value?.blur();
			});
			popoverRef.value?.hide?.();
		};

		const renderDefault = () => {
			return (
				<div class="w-full">
					<ElScrollbar ref={scrollbarRef} height="300px">
						<div class="flex flex-wrap">
							<ElInput v-model={searchText.value} placeholder="请输入图标" class="w-full mb-2 mr-20" clearable />
							{filterIcons.value.map((iconName) => {
								const IconComponent = icon[iconName];
								return (
									<div
										ref={(el) => (iconRefs.value[iconName] = el as HTMLElement | null)}
										key={iconName}
										onClick={() => handleClickIcon(iconName)}
									>
										<ElIcon
											class={[
												'w-30 h-30 cursor-pointer hover:bg-gray-100 rounded-full',
												value.value === iconName && 'bg-gray-100'
											]}
											style={{
												color: value.value === iconName ? globalStore.primaryColor : '#666666'
											}}
										>
											<IconComponent />
										</ElIcon>
									</div>
								);
							})}
						</div>
					</ElScrollbar>
				</div>
			);
		};

		const renderReference = () => {
			const Icon = iconArray.value[value.value];
			return (
				<ElInput
					ref={inputRef}
					v-model={value.value}
					onFocus={onFocus}
					v-slots={{
						append: () => {
							if (!Icon) return null;
							return (
								<ElIcon>
									<Icon />
								</ElIcon>
							);
						}
					}}
					{...attrs}
				/>
			);
		};

		const filterIcons = computed(() => {
			return icons.filter((iconName) => iconName.toLowerCase().includes(searchText.value.toLowerCase()));
		});

		onMounted(() => {
			setPopoverWidth();
		});

		return () => (
			<div class="w-full">
				<ElPopover
					ref={popoverRef}
					trigger="click"
					width={popoverWidth.value}
					transition="el-zoom-in-top"
					v-slots={{
						reference: () => renderReference(),
						default: () => renderDefault()
					}}
				/>
			</div>
		);
	}
});
