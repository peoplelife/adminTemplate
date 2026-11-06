export type SseEvent = {
	event: string;
	data: string;
};

export const createSseParser = (onEvent: (event: SseEvent) => void) => {
	let buffer = '';

	const dispatch = (frame: string) => {
		let event = 'message';
		const data: string[] = [];

		for (const line of frame.split(/\r?\n/)) {
			if (!line || line.startsWith(':')) continue;
			const separator = line.indexOf(':');
			const field = separator < 0 ? line : line.slice(0, separator);
			const value = separator < 0 ? '' : line.slice(separator + 1).replace(/^ /, '');

			if (field === 'event') event = value;
			if (field === 'data') data.push(value);
		}

		if (data.length) onEvent({ event, data: data.join('\n') });
	};

	return {
		push(chunk: string) {
			buffer += chunk;
			const frames = buffer.split(/\r?\n\r?\n/);
			buffer = frames.pop() || '';
			frames.forEach(dispatch);
		},
		flush() {
			if (buffer) dispatch(buffer);
			buffer = '';
		}
	};
};
