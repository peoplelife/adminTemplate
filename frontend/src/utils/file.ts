// 从 Content‑Disposition 头中取出 filename
export const getFileNameFromDisposition = (disposition: string): string | null => {
	if (!disposition) return null;
	const regex = /filename\*=UTF-8''([^;]+)|filename="?([^";]+)"?/i;
	const match = regex.exec(disposition);
	if (!match) return null;
	// match[1] 是 RFC5987 编码，match[2] 是普通写法
	const rawName = match[1] || match[2];
	try {
		return decodeURIComponent(rawName);
	} catch {
		return rawName;
	}
};
export const download = (options: {
	data: ArrayBuffer | Blob | Uint8Array | string;
	filename: string;
	mimeType?: string;
}): void => {
	const { data, filename, mimeType = 'application/octet-stream' } = options;
	let blob: Blob;
	if (data instanceof Blob) {
		//  如果已经是 Blob，直接使用
		blob = data;
	} else if (data instanceof ArrayBuffer || data instanceof Uint8Array) {
		// 如果是 ArrayBuffer 或 Uint8Array，则包成 Blob
		blob = new Blob([data], { type: mimeType });
	} else {
		// 其它（如字符串），也包成 Blob
		blob = new Blob([data], { type: mimeType });
	}

	// 标准浏览器：创建对象 URL 并模拟点击
	const url = URL.createObjectURL(blob);
	const a = document.createElement('a');
	a.href = url;
	a.download = filename;
	// 隐藏元素，避免影响页面布局
	a.style.display = 'none';
	document.body.appendChild(a);
	a.click();
	document.body.removeChild(a);
	URL.revokeObjectURL(url);
};
