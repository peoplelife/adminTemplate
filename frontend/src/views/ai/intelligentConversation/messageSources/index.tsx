import { ArrowDown, ArrowUp } from '@element-plus/icons-vue';
import { ElIcon } from 'element-plus';
import { computed, defineComponent, h, ref, type Component, type PropType } from 'vue';
import './index.scss';

export type ChatSource = {
	citationIndex?: number;
	index?: number;
	documentId?: string;
	documentName?: string;
	pageFrom?: number;
	pageTo?: number;
	sectionTitle?: string;
	excerpt?: string;
	available?: boolean;
};

export const groupSourcesByDocument = (sources: ChatSource[]) => {
	const groups = new Map<string, { documentId?: string; documentName?: string; matches: ChatSource[] }>();
	sources.forEach((source, index) => {
		const key = source.documentId || source.documentName || String(index);
		const group = groups.get(key);
		if (group) group.matches.push(source);
		else groups.set(key, { documentId: source.documentId, documentName: source.documentName, matches: [source] });
	});
	return [...groups.values()];
};

export default defineComponent({
	name: 'MessageSources',
	props: { sources: { type: Array as PropType<ChatSource[]>, default: () => [] } },
	setup(props) {
		const sourcesExpanded = ref(false);
		const sourceGroups = computed(() => groupSourcesByDocument(props.sources));
		const renderIcon = (icon: Component) => <ElIcon>{h(icon)}</ElIcon>;
		return () => (
			<>
				{sourceGroups.value.length ? (
					<div class="message-sources">
						<button
							class="message-sources__toggle"
							type="button"
							aria-expanded={sourcesExpanded.value}
							title={sourcesExpanded.value ? '收起参考来源' : '展开参考来源'}
							onClick={() => (sourcesExpanded.value = !sourcesExpanded.value)}
						>
							<span>参考来源 {sourceGroups.value.length}</span>
							{renderIcon(sourcesExpanded.value ? ArrowUp : ArrowDown)}
						</button>
						{sourcesExpanded.value ? (
							<div class="message-sources__list">
								{sourceGroups.value.map((group) => (
									<div class="message-source" key={group.documentId || group.documentName}>
										<p class="message-source__name">
											{group.documentName || '知识库文档'} · {group.matches.length} 个引用片段
										</p>
										{group.matches.map((source, index) => (
											<div class="message-source__match" key={source.citationIndex || source.index || index}>
												<p class="message-source__meta">
													[{source.citationIndex || source.index || index + 1}]
													{source.pageFrom
														? ` 第 ${source.pageFrom}${source.pageTo && source.pageTo !== source.pageFrom ? `-${source.pageTo}` : ''} 页`
														: ''}
												</p>
												{source.sectionTitle && <p class="message-source__section">{source.sectionTitle}</p>}
												{source.excerpt && <p class="message-source__excerpt">{source.excerpt}</p>}
											</div>
										))}
									</div>
								))}
							</div>
						) : null}
					</div>
				) : null}
			</>
		);
	}
});
