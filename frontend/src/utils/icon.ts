import * as Icons from '@element-plus/icons-vue';
export const renderIcon = (iconName: string) => {
	const Icon = Icons[iconName];
	return Icon;
};
