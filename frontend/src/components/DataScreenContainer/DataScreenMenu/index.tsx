import './index.scss';
export default defineComponent({
	setup() {
		return () => (
			<div class="mci-data-screen-menu">
				<div class="left-content">
					{[1, 1, 1, 1, 1].map((item) => {
						return <div class="menu-item">智慧武装</div>;
					})}
				</div>
				<div class="right-content">
					{[1, 1, 1, 1, 1].map((item) => {
						return <div class="menu-item">智慧武装</div>;
					})}
				</div>
			</div>
		);
	}
});
