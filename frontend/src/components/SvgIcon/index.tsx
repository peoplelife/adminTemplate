import './index.scss';

interface Props {
	prefix?: string;
	name?: string;
	color?: string;
	size?: number;
}

export default defineComponent({
	props: {
		prefix: {
			type: String,
			default: 'icon'
		},
		name: {
			type: String,
			default: ''
		},
		color: {
			type: String,
			default: ''
		},
		size: {
			type: Number,
			default: 16
		}
	},
	setup(props: Props) {
		const symbolId = computed(() => {
			return `#${props.prefix}-${props.name}`;
		});
		const svgStyle = computed(() => ({
			width: typeof props.size === 'number' ? `${props.size}px` : props.size,
			height: typeof props.size === 'number' ? `${props.size}px` : props.size
		}));
		return () => (
			<svg aria-hidden="true" class="svg-icon" style={svgStyle.value}>
				<use xlinkHref={symbolId.value} fill={props.color} />
			</svg>
		);
	}
});
