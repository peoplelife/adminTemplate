import { LOGIN_URL } from '@/config';
import router from '@/routers';
import { useUserStore } from '@/stores/modules/user';
import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios';
import { ElMessage, ElNotification } from 'element-plus';
import { createSseParser } from './sse';

// 创建 axios 实例
const service = axios.create({
	baseURL: import.meta.env.VITE_BASE_API,
	timeout: 15000
});

// 请求拦截器
service.interceptors.request.use(
	(config) => {
		const userStore = useUserStore();
		config.headers['X-Trace-Id'] ||= crypto.randomUUID();

		if (userStore.token) {
			config.headers['Authorization'] = `Bearer ${userStore.token}`;
		}

		return config;
	},
	(error) => {
		console.error('请求错误:', error);
		return Promise.reject(error);
	}
);

// 响应拦截器
service.interceptors.response.use(
	(response: AxiosResponse) => {
		const { data } = response;
		if (response.config.responseType === 'blob') return Promise.resolve(response);
		if (data?.success === true) return Promise.resolve(data.data);
		if (data?.success === false) {
			ElMessage.error(data.message || '请求失败');
			if (data.code === 110002) {
				useUserStore().setToken('');
				if (router.currentRoute.value.path !== LOGIN_URL) router.replace(LOGIN_URL);
			}
			return Promise.reject(data);
		}
		return Promise.resolve(data);
	},
	(error) => {
		const status = error.response?.status;
		const payload = error.response?.data;
		const message = payload?.message || error.message || '请求失败';
		const userStore = useUserStore();

		if (status === 401) {
			userStore.setToken('');
			if (router.currentRoute.value.path !== LOGIN_URL) router.replace(LOGIN_URL);
		}

		if (status === 422) {
			ElMessage.error(message);
		} else {
			ElNotification({ message, duration: 5000, type: 'error' });
		}

		return Promise.reject(payload || error);
	}
);
type StreamRequestCallbacks = StreamCallbacks & { signal?: AbortSignal };

const fetchStream = async (url: string, body: object, { onToken, onEvent, onError, signal }: StreamRequestCallbacks = {}) => {
	const userStore = useUserStore();
	try {
		const res = await fetch(url, {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
				Accept: 'text/event-stream',
				'X-Trace-Id': crypto.randomUUID(),
				Authorization: `Bearer ${userStore.token}`
			},
			body: JSON.stringify(body),
			signal
		});
		const contentType = res.headers.get('content-type') || '';
		if (!res.ok || !contentType.includes('text/event-stream')) {
			const payload = await res.json().catch(() => null);
			throw new Error(payload?.message || payload?.msg || `流式请求失败（HTTP ${res.status}）`);
		}

		const reader = res.body?.getReader();
		if (!reader) throw new Error('服务器没有返回流式响应');

		const decoder = new TextDecoder();
		const parser = createSseParser((event) => {
			onEvent?.(event);
			if (onToken) {
				try {
					onToken(JSON.parse(event.data));
				} catch {
					onToken(event.data);
				}
			}
		});

		while (true) {
			const { done, value } = await reader.read();
			if (done) break;
			parser.push(decoder.decode(value, { stream: true }));
		}
		parser.push(decoder.decode());
		parser.flush();
	} catch (error) {
		if (signal?.aborted) return;
		const streamError = error instanceof Error ? error : new Error(String(error));
		if (onError) onError(streamError);
		else throw streamError;
	}
};

// 封装请求方法
export const http = {
	get<T = any>(url: string, params?: object): Promise<T> {
		return service.get(url, { params });
	},

	post<T = any>(url: string, data?: object): Promise<T> {
		return service.post(url, data);
	},

	put<T = any>(url: string, data?: object): Promise<T> {
		return service.put(url, data);
	},

	patch<T = any>(url: string, data?: object): Promise<T> {
		return service.patch(url, data);
	},

	delete<T = any>(url: string, params?: object): Promise<T> {
		return service.delete(url, { params });
	},

	request<T = any>(config: any): Promise<T> {
		return service.request(config);
	},
	fetchStream(url: string, body: object, callbacks: StreamRequestCallbacks): Promise<void> {
		return fetchStream(url, body, callbacks);
	}
};

export type CustomAxiosRequestConfig = AxiosRequestConfig;
export default http;
