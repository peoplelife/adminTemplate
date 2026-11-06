import { ElInput, ElInputNumber } from 'element-plus';
// 直接引入具体组件，避免通过 base/index.ts 形成循环依赖
import { BaseSelect } from './Select';
import { IconSelect } from './IconSelect';
import { Radio } from './Radio';
import { TreeSelect } from './TreeSelect';
import { UpLoad } from './UpLoad';
import { Slider } from './Slider';

export const BASE_COMPONENTS = {
	input: ElInput,
	select: BaseSelect,
	iconselect: IconSelect,
	upload: UpLoad,
	treeSelect: TreeSelect,
	radio: Radio,
	inputNumber: ElInputNumber,
	slider: Slider
};
