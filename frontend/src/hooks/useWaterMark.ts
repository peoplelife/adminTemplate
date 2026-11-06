export const useWaterMark = (props: { text: string; opacity: number; rotate: number; fontSize: number }) => {
	const waterMarkBg = ref<string>('');
	const waterMark = ({
		text,
		opacity,
		rotate,
		fontSize
	}: {
		text: string;
		opacity: number;
		rotate: number;
		fontSize: number;
	}) => {
		const canvas = document.createElement('canvas');
		canvas.width = 500;
		canvas.height = 400;
		const ctx = canvas.getContext('2d');
		if (ctx) {
			ctx.font = `${fontSize}px Arial`;
			ctx.fillStyle = `rgba(0, 0, 0, ${opacity})`;
			ctx.textAlign = 'center';
			ctx.textBaseline = 'middle';

			ctx.translate(canvas.width / 2, canvas.height / 2);
			ctx.rotate((-rotate * Math.PI) / 180);
			ctx.fillText(text, 0, 0);

			waterMarkBg.value = canvas.toDataURL('image/png');
		}
	};
	onMounted(() => {
		waterMark({ ...props });
	});
	return {
		waterMarkBg,
		waterMark
	};
};
