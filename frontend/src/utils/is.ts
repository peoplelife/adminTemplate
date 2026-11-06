export const getType = (value: any): string => {
	const result = /\s(.*)\]$/.exec(toString.call(value));
	if (result) return result[1];
	return '';
};
/** 对象是否是字符串 */
export const isString = (value: any): value is string => getType(value) === 'String';
/** 对象是否是对象 */
export const isObject = <T = any>(value: any): value is T => getType(value) === 'Object';
