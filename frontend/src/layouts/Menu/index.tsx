import { ElSubMenu, ElMenuItem, ElIcon } from 'element-plus';
import { renderIcon } from '@/utils/icon';
import { useRouter } from 'vue-router';
import './index.scss';
interface MenuProps {
	menuList: any[];
}

const SubMenu = defineComponent({
	name: 'SubMenu',
	props: {
		menuList: {
			type: Array as PropType<any[]>,
			required: true,
			default: () => []
		}
	},
	setup(props: MenuProps) {
		const router = useRouter();
		const handleClickMenu = (path: string) => {
			router.push(path);
		};
		return () => (
			<>
				{props.menuList?.map((item) =>
					item.children?.length ? (
						<ElSubMenu
							key={item.menuId}
							index={item.path}
							v-slots={{
								title: () => (
									<>
										<ElIcon>{h(renderIcon(item.meta.icon))}</ElIcon>
										<span>{item.name}</span>
									</>
								)
							}}
						>
							<SubMenu menuList={item.children || []} />
						</ElSubMenu>
					) : (
						<ElMenuItem key={item.menuId} index={item.path} onClick={() => handleClickMenu(item.path)}>
							{() => (
								<>
									<ElIcon>{h(renderIcon(item.meta.icon))}</ElIcon>
									<span>{item.name}</span>
								</>
							)}
						</ElMenuItem>
					)
				)}
			</>
		);
	}
});

export default SubMenu;
