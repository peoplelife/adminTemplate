import type { RequestFunctionParams } from 'yapi-to-typescript';
import http, { type CustomAxiosRequestConfig } from './axios';

/**
 * 统一的请求函数（供 yapi-to-typescript 生成的接口使用）
 * - GET/DELETE: 通过 axios.get/axios.delete，并支持额外的 options（headers、params 等）
 * - 其他方法: 使用 axios.request，可传递 FormData、JSON 等任意数据，并允许自定义 headers
 */
export default function request<TResponseData>(
	payload: RequestFunctionParams,
	options?: Partial<CustomAxiosRequestConfig>
): Promise<TResponseData> {
	const upperMethod = payload.method.toUpperCase();
	const isGetLike = ['GET', 'DELETE'].includes(upperMethod);

	// 对 GET/DELETE，yapi 的 data 实际上是 query params
	if (isGetLike) {
		return http.request<TResponseData>({
			url: payload.path,
			method: upperMethod,
			params: payload.data,
			...options
		});
	}

	// 其他方法（POST/PUT/PATCH 等）统一走 request，方便携带 FormData 或自定义 headers
	return http.request<TResponseData>({
		url: payload.path,
		method: upperMethod,
		data: payload.data,
		...options
	});
}
