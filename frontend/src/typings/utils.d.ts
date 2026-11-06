declare type TAnyFunction<T = unknown> = (...args: any[]) => T;
declare type StreamCallbacks = {
	onToken?: (token: string) => void;
	onEvent?: (event: any) => void;
	onError?: (err: Error) => void;
};
