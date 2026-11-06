const escapeHtml = (value: string): string =>
	value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');

const renderInline = (value: string): string => {
	let result = escapeHtml(value);

	result = result.replace(
		/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/gi,
		(_match, label: string, url: string) => `<a href="${url}" target="_blank" rel="noopener noreferrer">${label}</a>`
	);
	result = result.replace(/`([^`\n]+)`/g, '<code>$1</code>');
	result = result.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
	result = result.replace(/__(.+?)__/g, '<strong>$1</strong>');
	result = result.replace(/\*([^*\n]+)\*/g, '<em>$1</em>');
	result = result.replace(/_([^_\n]+)_/g, '<em>$1</em>');

	return result;
};

const splitTableRow = (line: string): string[] => {
	let row = line.trim();
	if (row.startsWith('|')) row = row.slice(1);
	if (row.endsWith('|')) row = row.slice(0, -1);
	return row.split('|').map((cell) => cell.trim());
};

const isTableSeparator = (line: string): boolean => /^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)+\|?\s*$/.test(line);

const isBlockStart = (line: string): boolean => /^ {0,3}(#{1,6}\s+|```|>\s?|[-*+]\s+|\d+\.\s+|([-*_])\2\2\s*$)/.test(line);

const renderCodeBlock = (language: string, code: string): string => {
	const safeLanguage = language.replace(/[^a-zA-Z0-9+#._-]/g, '').slice(0, 32) || 'text';
	const encodedCode = escapeHtml(encodeURIComponent(code));

	return `<pre class="markdown-code-block"><div class="markdown-code-block__header"><span>${safeLanguage}</span><button type="button" class="markdown-code-block__copy" data-copy-code="${encodedCode}">复制代码</button></div><code class="language-${safeLanguage}">${escapeHtml(code)}</code></pre>`;
};

export const renderMarkdown = (markdown: string): string => {
	const lines = markdown.replace(/\r\n?/g, '\n').split('\n');
	const blocks: string[] = [];

	for (let index = 0; index < lines.length; ) {
		const line = lines[index];
		if (!line.trim()) {
			index += 1;
			continue;
		}

		const fence = line.match(/^ {0,3}```\s*([^\s`]*)\s*$/);
		if (fence) {
			const codeLines: string[] = [];
			index += 1;
			while (index < lines.length && !/^ {0,3}```\s*$/.test(lines[index])) {
				codeLines.push(lines[index]);
				index += 1;
			}
			if (index < lines.length) index += 1;
			blocks.push(renderCodeBlock(fence[1] || 'text', codeLines.join('\n')));
			continue;
		}

		const heading = line.match(/^ {0,3}(#{1,6})\s+(.+?)\s*#*\s*$/);
		if (heading) {
			const level = heading[1].length;
			blocks.push(`<h${level}>${renderInline(heading[2])}</h${level}>`);
			index += 1;
			continue;
		}

		if (index + 1 < lines.length && isTableSeparator(lines[index + 1])) {
			const header = splitTableRow(line);
			const rows: string[][] = [];
			index += 2;
			while (index < lines.length && lines[index].trim() && lines[index].includes('|')) {
				rows.push(splitTableRow(lines[index]));
				index += 1;
			}
			blocks.push(
				`<div class="markdown-table-wrap"><table><thead><tr>${header
					.map((cell) => `<th>${renderInline(cell)}</th>`)
					.join('')}</tr></thead><tbody>${rows
					.map(
						(row) =>
							`<tr>${header.map((_headerCell, cellIndex) => `<td>${renderInline(row[cellIndex] || '')}</td>`).join('')}</tr>`
					)
					.join('')}</tbody></table></div>`
			);
			continue;
		}

		const unorderedItem = line.match(/^ {0,3}[-*+]\s+(.+)$/);
		if (unorderedItem) {
			const items: string[] = [];
			while (index < lines.length) {
				const item = lines[index].match(/^ {0,3}[-*+]\s+(.+)$/);
				if (!item) break;
				items.push(`<li>${renderInline(item[1])}</li>`);
				index += 1;
			}
			blocks.push(`<ul>${items.join('')}</ul>`);
			continue;
		}

		const orderedItem = line.match(/^ {0,3}\d+\.\s+(.+)$/);
		if (orderedItem) {
			const items: string[] = [];
			while (index < lines.length) {
				const item = lines[index].match(/^ {0,3}\d+\.\s+(.+)$/);
				if (!item) break;
				items.push(`<li>${renderInline(item[1])}</li>`);
				index += 1;
			}
			blocks.push(`<ol>${items.join('')}</ol>`);
			continue;
		}

		if (/^ {0,3}>\s?/.test(line)) {
			const quoteLines: string[] = [];
			while (index < lines.length) {
				const quote = lines[index].match(/^ {0,3}>\s?(.*)$/);
				if (!quote) break;
				quoteLines.push(renderInline(quote[1]));
				index += 1;
			}
			blocks.push(`<blockquote>${quoteLines.join('<br>')}</blockquote>`);
			continue;
		}

		if (/^ {0,3}([-*_])(?:\s*\1){2,}\s*$/.test(line)) {
			blocks.push('<hr>');
			index += 1;
			continue;
		}

		const paragraphLines: string[] = [];
		while (index < lines.length && lines[index].trim() && !isBlockStart(lines[index])) {
			paragraphLines.push(renderInline(lines[index]));
			index += 1;
		}
		if (paragraphLines.length) {
			blocks.push(`<p>${paragraphLines.join('<br>')}</p>`);
		} else {
			blocks.push(`<p>${renderInline(line)}</p>`);
			index += 1;
		}
	}

	return blocks.join('');
};
