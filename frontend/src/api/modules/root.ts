/* prettier-ignore-start */
/* tslint:disable */
/* eslint-disable */

/* 该文件由 yapi-to-typescript 自动生成，请勿直接修改！！！ */

// @ts-ignore
// prettier-ignore
import { QueryStringArrayFormat, Method, RequestBodyType, ResponseBodyType, FileData, prepare } from 'yapi-to-typescript'
// @ts-ignore
// prettier-ignore
import type { RequestConfig, RequestFunctionRestArgs } from 'yapi-to-typescript'
// @ts-ignore
import request from '../../utils/request'

type UserRequestRestArgs = RequestFunctionRestArgs<typeof request>

// Request: 目前 React Hooks 功能有用到
export type Request<
  TRequestData,
  TRequestConfig extends RequestConfig,
  TRequestResult,
> = (TRequestConfig['requestDataOptional'] extends true
  ? (requestData?: TRequestData, ...args: RequestFunctionRestArgs<typeof request>) => TRequestResult
  : (requestData: TRequestData, ...args: RequestFunctionRestArgs<typeof request>) => TRequestResult) & {
  requestConfig: TRequestConfig
}

const mockUrl_0_0_0_0 = 'http://127.0.0.1:65259/mock/0' as any
const devUrl_0_0_0_0 = '' as any
const prodUrl_0_0_0_0 = '' as any
const dataKey_0_0_0_0 = 'data' as any

/**
 * 接口 检查服务连通性 的 **请求类型**
 *
 * @分类 服务状态
 * @请求头 `GET /`
 */
export interface IGetReq {}

/**
 * 接口 检查服务连通性 的 **返回类型**
 *
 * @分类 服务状态
 * @请求头 `GET /`
 */
export type IGetRes = any

/**
 * 接口 检查服务连通性 的 **请求配置的类型**
 *
 * @分类 服务状态
 * @请求头 `GET /`
 */
type GetRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:65259/mock/0', '', '', '/', 'data', string, string, true>
>

/**
 * 接口 检查服务连通性 的 **请求配置**
 *
 * @分类 服务状态
 * @请求头 `GET /`
 */
const getRequestConfig: GetRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_0,
  devUrl: devUrl_0_0_0_0,
  prodUrl: prodUrl_0_0_0_0,
  path: '/',
  method: Method.GET,
  requestHeaders: {},
  requestBodyType: RequestBodyType.query,
  responseBodyType: ResponseBodyType.raw,
  dataKey: dataKey_0_0_0_0,
  paramNames: [],
  queryNames: [],
  requestDataOptional: true,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'get',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 检查服务连通性 的 **请求函数**
 *
 * @分类 服务状态
 * @请求头 `GET /`
 */
export const get = /*#__PURE__*/ (requestData?: IGetReq, ...args: UserRequestRestArgs) => {
  return request<IGetRes>(prepare(getRequestConfig, requestData), ...args)
}

get.requestConfig = getRequestConfig

/* prettier-ignore-end */
