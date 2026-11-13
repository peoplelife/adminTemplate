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

const mockUrl_0_0_0_1 = 'http://127.0.0.1:64948/mock/0' as any
const devUrl_0_0_0_1 = '' as any
const prodUrl_0_0_0_1 = '' as any
const dataKey_0_0_0_1 = 'data' as any

/**
 * 接口 修改评测集 的 **请求类型**
 *
 * @分类 RAG 评测
 * @请求头 `PUT /v1/ai/evaluation-datasets/{datasetId}`
 */
export interface IPutV1AiEvaluationDatasetsByDatasetIdReq {
  name: string
  knowledgeBaseId: string
  description?: string
  datasetId: string
}

/**
 * 接口 修改评测集 的 **返回类型**
 *
 * @分类 RAG 评测
 * @请求头 `PUT /v1/ai/evaluation-datasets/{datasetId}`
 */
export interface IPutV1AiEvaluationDatasetsByDatasetIdRes {
  id?: string
  name?: string
  knowledgeBaseId?: string
  description?: string
  createdAt?: string
}

/**
 * 接口 修改评测集 的 **请求配置的类型**
 *
 * @分类 RAG 评测
 * @请求头 `PUT /v1/ai/evaluation-datasets/{datasetId}`
 */
type PutV1AiEvaluationDatasetsByDatasetIdRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/evaluation-datasets/{datasetId}',
    'data',
    'datasetId',
    string,
    false
  >
>

/**
 * 接口 修改评测集 的 **请求配置**
 *
 * @分类 RAG 评测
 * @请求头 `PUT /v1/ai/evaluation-datasets/{datasetId}`
 */
const putV1AiEvaluationDatasetsByDatasetIdRequestConfig: PutV1AiEvaluationDatasetsByDatasetIdRequestConfig =
  /*#__PURE__*/ {
    mockUrl: mockUrl_0_0_0_1,
    devUrl: devUrl_0_0_0_1,
    prodUrl: prodUrl_0_0_0_1,
    path: '/v1/ai/evaluation-datasets/{datasetId}',
    method: Method.PUT,
    requestHeaders: {},
    requestBodyType: RequestBodyType.json,
    responseBodyType: ResponseBodyType.json,
    dataKey: dataKey_0_0_0_1,
    paramNames: ['datasetId'],
    queryNames: [],
    requestDataOptional: false,
    requestDataJsonSchema: {},
    responseDataJsonSchema: {},
    requestFunctionName: 'putV1AiEvaluationDatasetsByDatasetId',
    queryStringArrayFormat: QueryStringArrayFormat.brackets,
    extraInfo: {},
  }

/**
 * 接口 修改评测集 的 **请求函数**
 *
 * @分类 RAG 评测
 * @请求头 `PUT /v1/ai/evaluation-datasets/{datasetId}`
 */
export const putV1AiEvaluationDatasetsByDatasetId = /*#__PURE__*/ (
  requestData: IPutV1AiEvaluationDatasetsByDatasetIdReq,
  ...args: UserRequestRestArgs
) => {
  return request<IPutV1AiEvaluationDatasetsByDatasetIdRes>(
    prepare(putV1AiEvaluationDatasetsByDatasetIdRequestConfig, requestData),
    ...args,
  )
}

putV1AiEvaluationDatasetsByDatasetId.requestConfig = putV1AiEvaluationDatasetsByDatasetIdRequestConfig

/**
 * 接口 删除评测数据 的 **请求类型**
 *
 * @分类 RAG 评测
 * @请求头 `DELETE /v1/ai/evaluation-datasets/{datasetId}`
 */
export interface IDeleteV1AiEvaluationDatasetsByDatasetIdReq {
  datasetId: string
}

/**
 * 接口 删除评测数据 的 **返回类型**
 *
 * @分类 RAG 评测
 * @请求头 `DELETE /v1/ai/evaluation-datasets/{datasetId}`
 */
export type IDeleteV1AiEvaluationDatasetsByDatasetIdRes = any

/**
 * 接口 删除评测数据 的 **请求配置的类型**
 *
 * @分类 RAG 评测
 * @请求头 `DELETE /v1/ai/evaluation-datasets/{datasetId}`
 */
type DeleteV1AiEvaluationDatasetsByDatasetIdRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/evaluation-datasets/{datasetId}',
    'data',
    'datasetId',
    string,
    false
  >
>

/**
 * 接口 删除评测数据 的 **请求配置**
 *
 * @分类 RAG 评测
 * @请求头 `DELETE /v1/ai/evaluation-datasets/{datasetId}`
 */
const deleteV1AiEvaluationDatasetsByDatasetIdRequestConfig: DeleteV1AiEvaluationDatasetsByDatasetIdRequestConfig =
  /*#__PURE__*/ {
    mockUrl: mockUrl_0_0_0_1,
    devUrl: devUrl_0_0_0_1,
    prodUrl: prodUrl_0_0_0_1,
    path: '/v1/ai/evaluation-datasets/{datasetId}',
    method: Method.DELETE,
    requestHeaders: {},
    requestBodyType: RequestBodyType.raw,
    responseBodyType: ResponseBodyType.raw,
    dataKey: dataKey_0_0_0_1,
    paramNames: ['datasetId'],
    queryNames: [],
    requestDataOptional: false,
    requestDataJsonSchema: {},
    responseDataJsonSchema: {},
    requestFunctionName: 'deleteV1AiEvaluationDatasetsByDatasetId',
    queryStringArrayFormat: QueryStringArrayFormat.brackets,
    extraInfo: {},
  }

/**
 * 接口 删除评测数据 的 **请求函数**
 *
 * @分类 RAG 评测
 * @请求头 `DELETE /v1/ai/evaluation-datasets/{datasetId}`
 */
export const deleteV1AiEvaluationDatasetsByDatasetId = /*#__PURE__*/ (
  requestData: IDeleteV1AiEvaluationDatasetsByDatasetIdReq,
  ...args: UserRequestRestArgs
) => {
  return request<IDeleteV1AiEvaluationDatasetsByDatasetIdRes>(
    prepare(deleteV1AiEvaluationDatasetsByDatasetIdRequestConfig, requestData),
    ...args,
  )
}

deleteV1AiEvaluationDatasetsByDatasetId.requestConfig = deleteV1AiEvaluationDatasetsByDatasetIdRequestConfig

/**
 * 接口 修改评测样本 的 **请求类型**
 *
 * @分类 RAG 评测
 * @请求头 `PUT /v1/ai/evaluation-datasets/{datasetId}/cases/{caseId}`
 */
export interface IPutV1AiEvaluationDatasetsByDatasetIdCasesByCaseIdReq {
  question: string
  category?: string
  difficulty?: 'easy' | 'medium' | 'hard'
  answerable: boolean
  expectedEvidence: {
    documentId: string
    text: string
  }[]
  expectedAnswer?: string
  notes?: string
  datasetId: string
  caseId: string
}

/**
 * 接口 修改评测样本 的 **返回类型**
 *
 * @分类 RAG 评测
 * @请求头 `PUT /v1/ai/evaluation-datasets/{datasetId}/cases/{caseId}`
 */
export interface IPutV1AiEvaluationDatasetsByDatasetIdCasesByCaseIdRes {
  id?: string
  datasetId?: string
  createdAt?: string
  question?: string
  category?: string
  difficulty?: string
  answerable?: boolean
  expectedEvidence?: {
    documentId: string
    text: string
  }[]
  expectedAnswer?: string
  notes?: string
}

/**
 * 接口 修改评测样本 的 **请求配置的类型**
 *
 * @分类 RAG 评测
 * @请求头 `PUT /v1/ai/evaluation-datasets/{datasetId}/cases/{caseId}`
 */
type PutV1AiEvaluationDatasetsByDatasetIdCasesByCaseIdRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/evaluation-datasets/{datasetId}/cases/{caseId}',
    'data',
    'datasetId' | 'caseId',
    string,
    false
  >
>

/**
 * 接口 修改评测样本 的 **请求配置**
 *
 * @分类 RAG 评测
 * @请求头 `PUT /v1/ai/evaluation-datasets/{datasetId}/cases/{caseId}`
 */
const putV1AiEvaluationDatasetsByDatasetIdCasesByCaseIdRequestConfig: PutV1AiEvaluationDatasetsByDatasetIdCasesByCaseIdRequestConfig =
  /*#__PURE__*/ {
    mockUrl: mockUrl_0_0_0_1,
    devUrl: devUrl_0_0_0_1,
    prodUrl: prodUrl_0_0_0_1,
    path: '/v1/ai/evaluation-datasets/{datasetId}/cases/{caseId}',
    method: Method.PUT,
    requestHeaders: {},
    requestBodyType: RequestBodyType.json,
    responseBodyType: ResponseBodyType.json,
    dataKey: dataKey_0_0_0_1,
    paramNames: ['datasetId', 'caseId'],
    queryNames: [],
    requestDataOptional: false,
    requestDataJsonSchema: {},
    responseDataJsonSchema: {},
    requestFunctionName: 'putV1AiEvaluationDatasetsByDatasetIdCasesByCaseId',
    queryStringArrayFormat: QueryStringArrayFormat.brackets,
    extraInfo: {},
  }

/**
 * 接口 修改评测样本 的 **请求函数**
 *
 * @分类 RAG 评测
 * @请求头 `PUT /v1/ai/evaluation-datasets/{datasetId}/cases/{caseId}`
 */
export const putV1AiEvaluationDatasetsByDatasetIdCasesByCaseId = /*#__PURE__*/ (
  requestData: IPutV1AiEvaluationDatasetsByDatasetIdCasesByCaseIdReq,
  ...args: UserRequestRestArgs
) => {
  return request<IPutV1AiEvaluationDatasetsByDatasetIdCasesByCaseIdRes>(
    prepare(putV1AiEvaluationDatasetsByDatasetIdCasesByCaseIdRequestConfig, requestData),
    ...args,
  )
}

putV1AiEvaluationDatasetsByDatasetIdCasesByCaseId.requestConfig =
  putV1AiEvaluationDatasetsByDatasetIdCasesByCaseIdRequestConfig

/**
 * 接口 删除评测数据 的 **请求类型**
 *
 * @分类 RAG 评测
 * @请求头 `DELETE /v1/ai/evaluation-datasets/{datasetId}/cases/{caseId}`
 */
export interface IDeleteV1AiEvaluationDatasetsByDatasetIdCasesByCaseIdReq {
  datasetId: string
  caseId: string
}

/**
 * 接口 删除评测数据 的 **返回类型**
 *
 * @分类 RAG 评测
 * @请求头 `DELETE /v1/ai/evaluation-datasets/{datasetId}/cases/{caseId}`
 */
export type IDeleteV1AiEvaluationDatasetsByDatasetIdCasesByCaseIdRes = any

/**
 * 接口 删除评测数据 的 **请求配置的类型**
 *
 * @分类 RAG 评测
 * @请求头 `DELETE /v1/ai/evaluation-datasets/{datasetId}/cases/{caseId}`
 */
type DeleteV1AiEvaluationDatasetsByDatasetIdCasesByCaseIdRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/evaluation-datasets/{datasetId}/cases/{caseId}',
    'data',
    'datasetId' | 'caseId',
    string,
    false
  >
>

/**
 * 接口 删除评测数据 的 **请求配置**
 *
 * @分类 RAG 评测
 * @请求头 `DELETE /v1/ai/evaluation-datasets/{datasetId}/cases/{caseId}`
 */
const deleteV1AiEvaluationDatasetsByDatasetIdCasesByCaseIdRequestConfig: DeleteV1AiEvaluationDatasetsByDatasetIdCasesByCaseIdRequestConfig =
  /*#__PURE__*/ {
    mockUrl: mockUrl_0_0_0_1,
    devUrl: devUrl_0_0_0_1,
    prodUrl: prodUrl_0_0_0_1,
    path: '/v1/ai/evaluation-datasets/{datasetId}/cases/{caseId}',
    method: Method.DELETE,
    requestHeaders: {},
    requestBodyType: RequestBodyType.raw,
    responseBodyType: ResponseBodyType.raw,
    dataKey: dataKey_0_0_0_1,
    paramNames: ['datasetId', 'caseId'],
    queryNames: [],
    requestDataOptional: false,
    requestDataJsonSchema: {},
    responseDataJsonSchema: {},
    requestFunctionName: 'deleteV1AiEvaluationDatasetsByDatasetIdCasesByCaseId',
    queryStringArrayFormat: QueryStringArrayFormat.brackets,
    extraInfo: {},
  }

/**
 * 接口 删除评测数据 的 **请求函数**
 *
 * @分类 RAG 评测
 * @请求头 `DELETE /v1/ai/evaluation-datasets/{datasetId}/cases/{caseId}`
 */
export const deleteV1AiEvaluationDatasetsByDatasetIdCasesByCaseId = /*#__PURE__*/ (
  requestData: IDeleteV1AiEvaluationDatasetsByDatasetIdCasesByCaseIdReq,
  ...args: UserRequestRestArgs
) => {
  return request<IDeleteV1AiEvaluationDatasetsByDatasetIdCasesByCaseIdRes>(
    prepare(deleteV1AiEvaluationDatasetsByDatasetIdCasesByCaseIdRequestConfig, requestData),
    ...args,
  )
}

deleteV1AiEvaluationDatasetsByDatasetIdCasesByCaseId.requestConfig =
  deleteV1AiEvaluationDatasetsByDatasetIdCasesByCaseIdRequestConfig

/**
 * 接口 查询评测集 的 **请求类型**
 *
 * @分类 RAG 评测
 * @请求头 `GET /v1/ai/evaluation-datasets`
 */
export interface IGetV1AiEvaluationDatasetsReq {
  page?: string
  pageSize?: string
}

/**
 * 接口 查询评测集 的 **返回类型**
 *
 * @分类 RAG 评测
 * @请求头 `GET /v1/ai/evaluation-datasets`
 */
export interface IGetV1AiEvaluationDatasetsRes {
  items?: {
    id?: string
    name?: string
    knowledgeBaseId?: string
    description?: string
    createdAt?: string
  }[]
  total?: number
  page?: number
  pageSize?: number
}

/**
 * 接口 查询评测集 的 **请求配置的类型**
 *
 * @分类 RAG 评测
 * @请求头 `GET /v1/ai/evaluation-datasets`
 */
type GetV1AiEvaluationDatasetsRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/evaluation-datasets',
    'data',
    string,
    'page' | 'pageSize',
    false
  >
>

/**
 * 接口 查询评测集 的 **请求配置**
 *
 * @分类 RAG 评测
 * @请求头 `GET /v1/ai/evaluation-datasets`
 */
const getV1AiEvaluationDatasetsRequestConfig: GetV1AiEvaluationDatasetsRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_1,
  devUrl: devUrl_0_0_0_1,
  prodUrl: prodUrl_0_0_0_1,
  path: '/v1/ai/evaluation-datasets',
  method: Method.GET,
  requestHeaders: {},
  requestBodyType: RequestBodyType.query,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_1,
  paramNames: [],
  queryNames: ['page', 'pageSize'],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'getV1AiEvaluationDatasets',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 查询评测集 的 **请求函数**
 *
 * @分类 RAG 评测
 * @请求头 `GET /v1/ai/evaluation-datasets`
 */
export const getV1AiEvaluationDatasets = /*#__PURE__*/ (
  requestData: IGetV1AiEvaluationDatasetsReq,
  ...args: UserRequestRestArgs
) => {
  return request<IGetV1AiEvaluationDatasetsRes>(prepare(getV1AiEvaluationDatasetsRequestConfig, requestData), ...args)
}

getV1AiEvaluationDatasets.requestConfig = getV1AiEvaluationDatasetsRequestConfig

/**
 * 接口 创建评测集 的 **请求类型**
 *
 * @分类 RAG 评测
 * @请求头 `POST /v1/ai/evaluation-datasets`
 */
export interface IPostV1AiEvaluationDatasetsReq {
  name: string
  knowledgeBaseId: string
  description?: string
}

/**
 * 接口 创建评测集 的 **返回类型**
 *
 * @分类 RAG 评测
 * @请求头 `POST /v1/ai/evaluation-datasets`
 */
export interface IPostV1AiEvaluationDatasetsRes {
  id?: string
  name?: string
  knowledgeBaseId?: string
  description?: string
  createdAt?: string
}

/**
 * 接口 创建评测集 的 **请求配置的类型**
 *
 * @分类 RAG 评测
 * @请求头 `POST /v1/ai/evaluation-datasets`
 */
type PostV1AiEvaluationDatasetsRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/ai/evaluation-datasets', 'data', string, string, false>
>

/**
 * 接口 创建评测集 的 **请求配置**
 *
 * @分类 RAG 评测
 * @请求头 `POST /v1/ai/evaluation-datasets`
 */
const postV1AiEvaluationDatasetsRequestConfig: PostV1AiEvaluationDatasetsRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_1,
  devUrl: devUrl_0_0_0_1,
  prodUrl: prodUrl_0_0_0_1,
  path: '/v1/ai/evaluation-datasets',
  method: Method.POST,
  requestHeaders: {},
  requestBodyType: RequestBodyType.json,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_1,
  paramNames: [],
  queryNames: [],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'postV1AiEvaluationDatasets',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 创建评测集 的 **请求函数**
 *
 * @分类 RAG 评测
 * @请求头 `POST /v1/ai/evaluation-datasets`
 */
export const postV1AiEvaluationDatasets = /*#__PURE__*/ (
  requestData: IPostV1AiEvaluationDatasetsReq,
  ...args: UserRequestRestArgs
) => {
  return request<IPostV1AiEvaluationDatasetsRes>(prepare(postV1AiEvaluationDatasetsRequestConfig, requestData), ...args)
}

postV1AiEvaluationDatasets.requestConfig = postV1AiEvaluationDatasetsRequestConfig

/**
 * 接口 查询运行历史 的 **请求类型**
 *
 * @分类 RAG 评测
 * @请求头 `GET /v1/ai/evaluation-datasets/{datasetId}/runs`
 */
export interface IGetV1AiEvaluationDatasetsByDatasetIdRunsReq {
  page?: string
  pageSize?: string
  datasetId: string
}

/**
 * 接口 查询运行历史 的 **返回类型**
 *
 * @分类 RAG 评测
 * @请求头 `GET /v1/ai/evaluation-datasets/{datasetId}/runs`
 */
export interface IGetV1AiEvaluationDatasetsByDatasetIdRunsRes {
  items?: {
    id?: string
    datasetId?: string
    status?: string
    topK?: number
    totalCases?: number
    metrics?: {
      hitAtK?: number
      recallAtK?: number
      mrr?: number
      evaluatedCases?: number
      unanswerableCases?: number
      failedCases?: number
      generatedCases?: number
      generationFailedCases?: number
      answerRelevance?: number
      answerCorrectness?: number
      faithfulness?: number
    }
    config?: {
      dataset_name?: string
      knowledge_base_id?: string
      embedding_model?: string
      chunk_size?: number
      chunk_overlap?: number
      scorer_version?: string
      documents?: {
        document_id?: string
        generation?: number
        file_hash?: string
        parser_version?: string
        splitter_version?: string
      }[]
    }
    createdAt?: string
    completedAt?: string
    results?: {
      id?: string
      caseId?: string
      caseSnapshot?: {
        question: string
        category?: string
        difficulty?: 'easy' | 'medium' | 'hard'
        answerable: boolean
        expectedEvidence: {
          documentId: string
          text: string
        }[]
        expectedAnswer?: string
        notes?: string
      }
      status?: string
      retrievedItems?: {
        chunkId?: string
        documentId?: string
        generation?: number
        content?: string
        fileName?: string
        similarity?: number
        pageFrom?: number
        pageTo?: number
        sectionTitle?: string
      }[]
      hitAtK?: number
      recallAtK?: number
      mrr?: number
      elapsedMs?: number
      errorCode?: number
      generatedAnswer?: string
      generationModel?: string
      promptVersion?: string
      generationStatus?: string
      answerRelevance?: number
      answerCorrectness?: number
      faithfulness?: number
      unsupportedClaims?: string[]
      generationDetails?: {
        context?: string
        judge?: {
          status?: string
          model?: string
          prompt_version?: string
          reason?: string
        }
      }
    }[]
  }[]
  total?: number
  page?: number
  pageSize?: number
}

/**
 * 接口 查询运行历史 的 **请求配置的类型**
 *
 * @分类 RAG 评测
 * @请求头 `GET /v1/ai/evaluation-datasets/{datasetId}/runs`
 */
type GetV1AiEvaluationDatasetsByDatasetIdRunsRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/evaluation-datasets/{datasetId}/runs',
    'data',
    'datasetId',
    'page' | 'pageSize',
    false
  >
>

/**
 * 接口 查询运行历史 的 **请求配置**
 *
 * @分类 RAG 评测
 * @请求头 `GET /v1/ai/evaluation-datasets/{datasetId}/runs`
 */
const getV1AiEvaluationDatasetsByDatasetIdRunsRequestConfig: GetV1AiEvaluationDatasetsByDatasetIdRunsRequestConfig =
  /*#__PURE__*/ {
    mockUrl: mockUrl_0_0_0_1,
    devUrl: devUrl_0_0_0_1,
    prodUrl: prodUrl_0_0_0_1,
    path: '/v1/ai/evaluation-datasets/{datasetId}/runs',
    method: Method.GET,
    requestHeaders: {},
    requestBodyType: RequestBodyType.query,
    responseBodyType: ResponseBodyType.json,
    dataKey: dataKey_0_0_0_1,
    paramNames: ['datasetId'],
    queryNames: ['page', 'pageSize'],
    requestDataOptional: false,
    requestDataJsonSchema: {},
    responseDataJsonSchema: {},
    requestFunctionName: 'getV1AiEvaluationDatasetsByDatasetIdRuns',
    queryStringArrayFormat: QueryStringArrayFormat.brackets,
    extraInfo: {},
  }

/**
 * 接口 查询运行历史 的 **请求函数**
 *
 * @分类 RAG 评测
 * @请求头 `GET /v1/ai/evaluation-datasets/{datasetId}/runs`
 */
export const getV1AiEvaluationDatasetsByDatasetIdRuns = /*#__PURE__*/ (
  requestData: IGetV1AiEvaluationDatasetsByDatasetIdRunsReq,
  ...args: UserRequestRestArgs
) => {
  return request<IGetV1AiEvaluationDatasetsByDatasetIdRunsRes>(
    prepare(getV1AiEvaluationDatasetsByDatasetIdRunsRequestConfig, requestData),
    ...args,
  )
}

getV1AiEvaluationDatasetsByDatasetIdRuns.requestConfig = getV1AiEvaluationDatasetsByDatasetIdRunsRequestConfig

/**
 * 接口 执行检索评测（同步 1 至 50 题） 的 **请求类型**
 *
 * @分类 RAG 评测
 * @请求头 `POST /v1/ai/evaluation-datasets/{datasetId}/runs`
 */
export interface IPostV1AiEvaluationDatasetsByDatasetIdRunsReq {
  topK?: number
  generateAnswers?: boolean
  datasetId: string
}

/**
 * 接口 执行检索评测（同步 1 至 50 题） 的 **返回类型**
 *
 * @分类 RAG 评测
 * @请求头 `POST /v1/ai/evaluation-datasets/{datasetId}/runs`
 */
export interface IPostV1AiEvaluationDatasetsByDatasetIdRunsRes {
  id?: string
  datasetId?: string
  status?: string
  topK?: number
  totalCases?: number
  metrics?: {
    hitAtK?: number
    recallAtK?: number
    mrr?: number
    evaluatedCases?: number
    unanswerableCases?: number
    failedCases?: number
    generatedCases?: number
    generationFailedCases?: number
    answerRelevance?: number
    answerCorrectness?: number
    faithfulness?: number
  }
  config?: {
    dataset_name?: string
    knowledge_base_id?: string
    embedding_model?: string
    chunk_size?: number
    chunk_overlap?: number
    scorer_version?: string
    documents?: {
      document_id?: string
      generation?: number
      file_hash?: string
      parser_version?: string
      splitter_version?: string
    }[]
  }
  createdAt?: string
  completedAt?: string
  results?: {
    id?: string
    caseId?: string
    caseSnapshot?: {
      question: string
      category?: string
      difficulty?: 'easy' | 'medium' | 'hard'
      answerable: boolean
      expectedEvidence: {
        documentId: string
        text: string
      }[]
      expectedAnswer?: string
      notes?: string
    }
    status?: string
    retrievedItems?: {
      chunkId?: string
      documentId?: string
      generation?: number
      content?: string
      fileName?: string
      similarity?: number
      pageFrom?: number
      pageTo?: number
      sectionTitle?: string
    }[]
    hitAtK?: number
    recallAtK?: number
    mrr?: number
    elapsedMs?: number
    errorCode?: number
    generatedAnswer?: string
    generationModel?: string
    promptVersion?: string
    generationStatus?: string
    answerRelevance?: number
    answerCorrectness?: number
    faithfulness?: number
    unsupportedClaims?: string[]
    generationDetails?: {
      context?: string
      judge?: {
        status?: string
        model?: string
        prompt_version?: string
        reason?: string
      }
    }
  }[]
}

/**
 * 接口 执行检索评测（同步 1 至 50 题） 的 **请求配置的类型**
 *
 * @分类 RAG 评测
 * @请求头 `POST /v1/ai/evaluation-datasets/{datasetId}/runs`
 */
type PostV1AiEvaluationDatasetsByDatasetIdRunsRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/evaluation-datasets/{datasetId}/runs',
    'data',
    'datasetId',
    string,
    false
  >
>

/**
 * 接口 执行检索评测（同步 1 至 50 题） 的 **请求配置**
 *
 * @分类 RAG 评测
 * @请求头 `POST /v1/ai/evaluation-datasets/{datasetId}/runs`
 */
const postV1AiEvaluationDatasetsByDatasetIdRunsRequestConfig: PostV1AiEvaluationDatasetsByDatasetIdRunsRequestConfig =
  /*#__PURE__*/ {
    mockUrl: mockUrl_0_0_0_1,
    devUrl: devUrl_0_0_0_1,
    prodUrl: prodUrl_0_0_0_1,
    path: '/v1/ai/evaluation-datasets/{datasetId}/runs',
    method: Method.POST,
    requestHeaders: {},
    requestBodyType: RequestBodyType.json,
    responseBodyType: ResponseBodyType.json,
    dataKey: dataKey_0_0_0_1,
    paramNames: ['datasetId'],
    queryNames: [],
    requestDataOptional: false,
    requestDataJsonSchema: {},
    responseDataJsonSchema: {},
    requestFunctionName: 'postV1AiEvaluationDatasetsByDatasetIdRuns',
    queryStringArrayFormat: QueryStringArrayFormat.brackets,
    extraInfo: {},
  }

/**
 * 接口 执行检索评测（同步 1 至 50 题） 的 **请求函数**
 *
 * @分类 RAG 评测
 * @请求头 `POST /v1/ai/evaluation-datasets/{datasetId}/runs`
 */
export const postV1AiEvaluationDatasetsByDatasetIdRuns = /*#__PURE__*/ (
  requestData: IPostV1AiEvaluationDatasetsByDatasetIdRunsReq,
  ...args: UserRequestRestArgs
) => {
  return request<IPostV1AiEvaluationDatasetsByDatasetIdRunsRes>(
    prepare(postV1AiEvaluationDatasetsByDatasetIdRunsRequestConfig, requestData),
    ...args,
  )
}

postV1AiEvaluationDatasetsByDatasetIdRuns.requestConfig = postV1AiEvaluationDatasetsByDatasetIdRunsRequestConfig

/**
 * 接口 查询评测样本 的 **请求类型**
 *
 * @分类 RAG 评测
 * @请求头 `GET /v1/ai/evaluation-datasets/{datasetId}/cases`
 */
export interface IGetV1AiEvaluationDatasetsByDatasetIdCasesReq {
  page?: string
  pageSize?: string
  datasetId: string
}

/**
 * 接口 查询评测样本 的 **返回类型**
 *
 * @分类 RAG 评测
 * @请求头 `GET /v1/ai/evaluation-datasets/{datasetId}/cases`
 */
export interface IGetV1AiEvaluationDatasetsByDatasetIdCasesRes {
  items?: {
    id?: string
    datasetId?: string
    createdAt?: string
    question?: string
    category?: string
    difficulty?: string
    answerable?: boolean
    expectedEvidence?: {
      documentId: string
      text: string
    }[]
    expectedAnswer?: string
    notes?: string
  }[]
  total?: number
  page?: number
  pageSize?: number
}

/**
 * 接口 查询评测样本 的 **请求配置的类型**
 *
 * @分类 RAG 评测
 * @请求头 `GET /v1/ai/evaluation-datasets/{datasetId}/cases`
 */
type GetV1AiEvaluationDatasetsByDatasetIdCasesRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/evaluation-datasets/{datasetId}/cases',
    'data',
    'datasetId',
    'page' | 'pageSize',
    false
  >
>

/**
 * 接口 查询评测样本 的 **请求配置**
 *
 * @分类 RAG 评测
 * @请求头 `GET /v1/ai/evaluation-datasets/{datasetId}/cases`
 */
const getV1AiEvaluationDatasetsByDatasetIdCasesRequestConfig: GetV1AiEvaluationDatasetsByDatasetIdCasesRequestConfig =
  /*#__PURE__*/ {
    mockUrl: mockUrl_0_0_0_1,
    devUrl: devUrl_0_0_0_1,
    prodUrl: prodUrl_0_0_0_1,
    path: '/v1/ai/evaluation-datasets/{datasetId}/cases',
    method: Method.GET,
    requestHeaders: {},
    requestBodyType: RequestBodyType.query,
    responseBodyType: ResponseBodyType.json,
    dataKey: dataKey_0_0_0_1,
    paramNames: ['datasetId'],
    queryNames: ['page', 'pageSize'],
    requestDataOptional: false,
    requestDataJsonSchema: {},
    responseDataJsonSchema: {},
    requestFunctionName: 'getV1AiEvaluationDatasetsByDatasetIdCases',
    queryStringArrayFormat: QueryStringArrayFormat.brackets,
    extraInfo: {},
  }

/**
 * 接口 查询评测样本 的 **请求函数**
 *
 * @分类 RAG 评测
 * @请求头 `GET /v1/ai/evaluation-datasets/{datasetId}/cases`
 */
export const getV1AiEvaluationDatasetsByDatasetIdCases = /*#__PURE__*/ (
  requestData: IGetV1AiEvaluationDatasetsByDatasetIdCasesReq,
  ...args: UserRequestRestArgs
) => {
  return request<IGetV1AiEvaluationDatasetsByDatasetIdCasesRes>(
    prepare(getV1AiEvaluationDatasetsByDatasetIdCasesRequestConfig, requestData),
    ...args,
  )
}

getV1AiEvaluationDatasetsByDatasetIdCases.requestConfig = getV1AiEvaluationDatasetsByDatasetIdCasesRequestConfig

/**
 * 接口 新增评测样本 的 **请求类型**
 *
 * @分类 RAG 评测
 * @请求头 `POST /v1/ai/evaluation-datasets/{datasetId}/cases`
 */
export interface IPostV1AiEvaluationDatasetsByDatasetIdCasesReq {
  question: string
  category?: string
  difficulty?: 'easy' | 'medium' | 'hard'
  answerable: boolean
  expectedEvidence: {
    documentId: string
    text: string
  }[]
  expectedAnswer?: string
  notes?: string
  datasetId: string
}

/**
 * 接口 新增评测样本 的 **返回类型**
 *
 * @分类 RAG 评测
 * @请求头 `POST /v1/ai/evaluation-datasets/{datasetId}/cases`
 */
export interface IPostV1AiEvaluationDatasetsByDatasetIdCasesRes {
  id?: string
  datasetId?: string
  createdAt?: string
  question?: string
  category?: string
  difficulty?: string
  answerable?: boolean
  expectedEvidence?: {
    documentId: string
    text: string
  }[]
  expectedAnswer?: string
  notes?: string
}

/**
 * 接口 新增评测样本 的 **请求配置的类型**
 *
 * @分类 RAG 评测
 * @请求头 `POST /v1/ai/evaluation-datasets/{datasetId}/cases`
 */
type PostV1AiEvaluationDatasetsByDatasetIdCasesRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/evaluation-datasets/{datasetId}/cases',
    'data',
    'datasetId',
    string,
    false
  >
>

/**
 * 接口 新增评测样本 的 **请求配置**
 *
 * @分类 RAG 评测
 * @请求头 `POST /v1/ai/evaluation-datasets/{datasetId}/cases`
 */
const postV1AiEvaluationDatasetsByDatasetIdCasesRequestConfig: PostV1AiEvaluationDatasetsByDatasetIdCasesRequestConfig =
  /*#__PURE__*/ {
    mockUrl: mockUrl_0_0_0_1,
    devUrl: devUrl_0_0_0_1,
    prodUrl: prodUrl_0_0_0_1,
    path: '/v1/ai/evaluation-datasets/{datasetId}/cases',
    method: Method.POST,
    requestHeaders: {},
    requestBodyType: RequestBodyType.json,
    responseBodyType: ResponseBodyType.json,
    dataKey: dataKey_0_0_0_1,
    paramNames: ['datasetId'],
    queryNames: [],
    requestDataOptional: false,
    requestDataJsonSchema: {},
    responseDataJsonSchema: {},
    requestFunctionName: 'postV1AiEvaluationDatasetsByDatasetIdCases',
    queryStringArrayFormat: QueryStringArrayFormat.brackets,
    extraInfo: {},
  }

/**
 * 接口 新增评测样本 的 **请求函数**
 *
 * @分类 RAG 评测
 * @请求头 `POST /v1/ai/evaluation-datasets/{datasetId}/cases`
 */
export const postV1AiEvaluationDatasetsByDatasetIdCases = /*#__PURE__*/ (
  requestData: IPostV1AiEvaluationDatasetsByDatasetIdCasesReq,
  ...args: UserRequestRestArgs
) => {
  return request<IPostV1AiEvaluationDatasetsByDatasetIdCasesRes>(
    prepare(postV1AiEvaluationDatasetsByDatasetIdCasesRequestConfig, requestData),
    ...args,
  )
}

postV1AiEvaluationDatasetsByDatasetIdCases.requestConfig = postV1AiEvaluationDatasetsByDatasetIdCasesRequestConfig

/**
 * 接口 查询完整评测报告 的 **请求类型**
 *
 * @分类 RAG 评测
 * @请求头 `GET /v1/ai/evaluation-datasets/{datasetId}/runs/{runId}`
 */
export interface IGetV1AiEvaluationDatasetsByDatasetIdRunsByRunIdReq {
  datasetId: string
  runId: string
}

/**
 * 接口 查询完整评测报告 的 **返回类型**
 *
 * @分类 RAG 评测
 * @请求头 `GET /v1/ai/evaluation-datasets/{datasetId}/runs/{runId}`
 */
export interface IGetV1AiEvaluationDatasetsByDatasetIdRunsByRunIdRes {
  id?: string
  datasetId?: string
  status?: string
  topK?: number
  totalCases?: number
  metrics?: {
    hitAtK?: number
    recallAtK?: number
    mrr?: number
    evaluatedCases?: number
    unanswerableCases?: number
    failedCases?: number
    generatedCases?: number
    generationFailedCases?: number
    answerRelevance?: number
    answerCorrectness?: number
    faithfulness?: number
  }
  config?: {
    dataset_name?: string
    knowledge_base_id?: string
    embedding_model?: string
    chunk_size?: number
    chunk_overlap?: number
    scorer_version?: string
    documents?: {
      document_id?: string
      generation?: number
      file_hash?: string
      parser_version?: string
      splitter_version?: string
    }[]
  }
  createdAt?: string
  completedAt?: string
  results?: {
    id?: string
    caseId?: string
    caseSnapshot?: {
      question: string
      category?: string
      difficulty?: 'easy' | 'medium' | 'hard'
      answerable: boolean
      expectedEvidence: {
        documentId: string
        text: string
      }[]
      expectedAnswer?: string
      notes?: string
    }
    status?: string
    retrievedItems?: {
      chunkId?: string
      documentId?: string
      generation?: number
      content?: string
      fileName?: string
      similarity?: number
      pageFrom?: number
      pageTo?: number
      sectionTitle?: string
    }[]
    hitAtK?: number
    recallAtK?: number
    mrr?: number
    elapsedMs?: number
    errorCode?: number
    generatedAnswer?: string
    generationModel?: string
    promptVersion?: string
    generationStatus?: string
    answerRelevance?: number
    answerCorrectness?: number
    faithfulness?: number
    unsupportedClaims?: string[]
    generationDetails?: {
      context?: string
      judge?: {
        status?: string
        model?: string
        prompt_version?: string
        reason?: string
      }
    }
  }[]
}

/**
 * 接口 查询完整评测报告 的 **请求配置的类型**
 *
 * @分类 RAG 评测
 * @请求头 `GET /v1/ai/evaluation-datasets/{datasetId}/runs/{runId}`
 */
type GetV1AiEvaluationDatasetsByDatasetIdRunsByRunIdRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/evaluation-datasets/{datasetId}/runs/{runId}',
    'data',
    'datasetId' | 'runId',
    string,
    false
  >
>

/**
 * 接口 查询完整评测报告 的 **请求配置**
 *
 * @分类 RAG 评测
 * @请求头 `GET /v1/ai/evaluation-datasets/{datasetId}/runs/{runId}`
 */
const getV1AiEvaluationDatasetsByDatasetIdRunsByRunIdRequestConfig: GetV1AiEvaluationDatasetsByDatasetIdRunsByRunIdRequestConfig =
  /*#__PURE__*/ {
    mockUrl: mockUrl_0_0_0_1,
    devUrl: devUrl_0_0_0_1,
    prodUrl: prodUrl_0_0_0_1,
    path: '/v1/ai/evaluation-datasets/{datasetId}/runs/{runId}',
    method: Method.GET,
    requestHeaders: {},
    requestBodyType: RequestBodyType.query,
    responseBodyType: ResponseBodyType.json,
    dataKey: dataKey_0_0_0_1,
    paramNames: ['datasetId', 'runId'],
    queryNames: [],
    requestDataOptional: false,
    requestDataJsonSchema: {},
    responseDataJsonSchema: {},
    requestFunctionName: 'getV1AiEvaluationDatasetsByDatasetIdRunsByRunId',
    queryStringArrayFormat: QueryStringArrayFormat.brackets,
    extraInfo: {},
  }

/**
 * 接口 查询完整评测报告 的 **请求函数**
 *
 * @分类 RAG 评测
 * @请求头 `GET /v1/ai/evaluation-datasets/{datasetId}/runs/{runId}`
 */
export const getV1AiEvaluationDatasetsByDatasetIdRunsByRunId = /*#__PURE__*/ (
  requestData: IGetV1AiEvaluationDatasetsByDatasetIdRunsByRunIdReq,
  ...args: UserRequestRestArgs
) => {
  return request<IGetV1AiEvaluationDatasetsByDatasetIdRunsByRunIdRes>(
    prepare(getV1AiEvaluationDatasetsByDatasetIdRunsByRunIdRequestConfig, requestData),
    ...args,
  )
}

getV1AiEvaluationDatasetsByDatasetIdRunsByRunId.requestConfig =
  getV1AiEvaluationDatasetsByDatasetIdRunsByRunIdRequestConfig

const mockUrl_0_0_0_2 = 'http://127.0.0.1:64948/mock/0' as any
const devUrl_0_0_0_2 = '' as any
const prodUrl_0_0_0_2 = '' as any
const dataKey_0_0_0_2 = 'data' as any

/**
 * 接口 完成当前账号的 GitHub 绑定 的 **请求类型**
 *
 * @分类 GitHub 认证
 * @请求头 `POST /v1/me/github/complete`
 */
export interface IPostV1MeGithubCompleteReq {
  /**
   * 发起授权时收到的 state
   */
  state: string
}

/**
 * 接口 完成当前账号的 GitHub 绑定 的 **返回类型**
 *
 * @分类 GitHub 认证
 * @请求头 `POST /v1/me/github/complete`
 */
export interface IPostV1MeGithubCompleteRes {
  /**
   * 是否启用 GitHub 登录
   */
  enabled?: boolean
  /**
   * 是否已绑定 GitHub
   */
  bound?: boolean
  /**
   * 已绑定的 GitHub 登录名；未绑定时为 null
   */
  login?: string
}

/**
 * 接口 完成当前账号的 GitHub 绑定 的 **请求配置的类型**
 *
 * @分类 GitHub 认证
 * @请求头 `POST /v1/me/github/complete`
 */
type PostV1MeGithubCompleteRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/me/github/complete', 'data', string, string, false>
>

/**
 * 接口 完成当前账号的 GitHub 绑定 的 **请求配置**
 *
 * @分类 GitHub 认证
 * @请求头 `POST /v1/me/github/complete`
 */
const postV1MeGithubCompleteRequestConfig: PostV1MeGithubCompleteRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_2,
  devUrl: devUrl_0_0_0_2,
  prodUrl: prodUrl_0_0_0_2,
  path: '/v1/me/github/complete',
  method: Method.POST,
  requestHeaders: {},
  requestBodyType: RequestBodyType.json,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_2,
  paramNames: [],
  queryNames: [],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'postV1MeGithubComplete',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 完成当前账号的 GitHub 绑定 的 **请求函数**
 *
 * @分类 GitHub 认证
 * @请求头 `POST /v1/me/github/complete`
 */
export const postV1MeGithubComplete = /*#__PURE__*/ (
  requestData: IPostV1MeGithubCompleteReq,
  ...args: UserRequestRestArgs
) => {
  return request<IPostV1MeGithubCompleteRes>(prepare(postV1MeGithubCompleteRequestConfig, requestData), ...args)
}

postV1MeGithubComplete.requestConfig = postV1MeGithubCompleteRequestConfig

/**
 * 接口 发起当前账号的 GitHub 绑定 的 **请求类型**
 *
 * @分类 GitHub 认证
 * @请求头 `POST /v1/me/github/authorize`
 */
export interface IPostV1MeGithubAuthorizeReq {}

/**
 * 接口 发起当前账号的 GitHub 绑定 的 **返回类型**
 *
 * @分类 GitHub 认证
 * @请求头 `POST /v1/me/github/authorize`
 */
export interface IPostV1MeGithubAuthorizeRes {
  /**
   * 固定 GitHub 授权地址，使用当前窗口跳转
   */
  authorizationUrl?: string
  /**
   * 五分钟有效的一次性状态值
   */
  state?: string
}

/**
 * 接口 发起当前账号的 GitHub 绑定 的 **请求配置的类型**
 *
 * @分类 GitHub 认证
 * @请求头 `POST /v1/me/github/authorize`
 */
type PostV1MeGithubAuthorizeRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/me/github/authorize', 'data', string, string, true>
>

/**
 * 接口 发起当前账号的 GitHub 绑定 的 **请求配置**
 *
 * @分类 GitHub 认证
 * @请求头 `POST /v1/me/github/authorize`
 */
const postV1MeGithubAuthorizeRequestConfig: PostV1MeGithubAuthorizeRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_2,
  devUrl: devUrl_0_0_0_2,
  prodUrl: prodUrl_0_0_0_2,
  path: '/v1/me/github/authorize',
  method: Method.POST,
  requestHeaders: {},
  requestBodyType: RequestBodyType.raw,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_2,
  paramNames: [],
  queryNames: [],
  requestDataOptional: true,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'postV1MeGithubAuthorize',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 发起当前账号的 GitHub 绑定 的 **请求函数**
 *
 * @分类 GitHub 认证
 * @请求头 `POST /v1/me/github/authorize`
 */
export const postV1MeGithubAuthorize = /*#__PURE__*/ (
  requestData?: IPostV1MeGithubAuthorizeReq,
  ...args: UserRequestRestArgs
) => {
  return request<IPostV1MeGithubAuthorizeRes>(prepare(postV1MeGithubAuthorizeRequestConfig, requestData), ...args)
}

postV1MeGithubAuthorize.requestConfig = postV1MeGithubAuthorizeRequestConfig

/**
 * 接口 完成 GitHub 登录或注册 的 **请求类型**
 *
 * @分类 GitHub 认证
 * @请求头 `POST /v1/auth/github/complete`
 */
export interface IPostV1AuthGithubCompleteReq {
  /**
   * 发起授权时收到的 state
   */
  state: string
}

/**
 * 接口 完成 GitHub 登录或注册 的 **返回类型**
 *
 * @分类 GitHub 认证
 * @请求头 `POST /v1/auth/github/complete`
 */
export interface IPostV1AuthGithubCompleteRes {
  /**
   * JWT 访问令牌，后续请求使用 Authorization: Bearer <token>
   */
  token?: string
  /**
   * 是否首次登录
   */
  initialPasswordFlag?: boolean
}

/**
 * 接口 完成 GitHub 登录或注册 的 **请求配置的类型**
 *
 * @分类 GitHub 认证
 * @请求头 `POST /v1/auth/github/complete`
 */
type PostV1AuthGithubCompleteRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/auth/github/complete', 'data', string, string, false>
>

/**
 * 接口 完成 GitHub 登录或注册 的 **请求配置**
 *
 * @分类 GitHub 认证
 * @请求头 `POST /v1/auth/github/complete`
 */
const postV1AuthGithubCompleteRequestConfig: PostV1AuthGithubCompleteRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_2,
  devUrl: devUrl_0_0_0_2,
  prodUrl: prodUrl_0_0_0_2,
  path: '/v1/auth/github/complete',
  method: Method.POST,
  requestHeaders: {},
  requestBodyType: RequestBodyType.json,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_2,
  paramNames: [],
  queryNames: [],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'postV1AuthGithubComplete',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 完成 GitHub 登录或注册 的 **请求函数**
 *
 * @分类 GitHub 认证
 * @请求头 `POST /v1/auth/github/complete`
 */
export const postV1AuthGithubComplete = /*#__PURE__*/ (
  requestData: IPostV1AuthGithubCompleteReq,
  ...args: UserRequestRestArgs
) => {
  return request<IPostV1AuthGithubCompleteRes>(prepare(postV1AuthGithubCompleteRequestConfig, requestData), ...args)
}

postV1AuthGithubComplete.requestConfig = postV1AuthGithubCompleteRequestConfig

/**
 * 接口 发起 GitHub 登录或注册 的 **请求类型**
 *
 * @分类 GitHub 认证
 * @请求头 `POST /v1/auth/github/authorize`
 */
export interface IPostV1AuthGithubAuthorizeReq {}

/**
 * 接口 发起 GitHub 登录或注册 的 **返回类型**
 *
 * @分类 GitHub 认证
 * @请求头 `POST /v1/auth/github/authorize`
 */
export interface IPostV1AuthGithubAuthorizeRes {
  /**
   * 固定 GitHub 授权地址，使用当前窗口跳转
   */
  authorizationUrl?: string
  /**
   * 五分钟有效的一次性状态值
   */
  state?: string
}

/**
 * 接口 发起 GitHub 登录或注册 的 **请求配置的类型**
 *
 * @分类 GitHub 认证
 * @请求头 `POST /v1/auth/github/authorize`
 */
type PostV1AuthGithubAuthorizeRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/auth/github/authorize', 'data', string, string, true>
>

/**
 * 接口 发起 GitHub 登录或注册 的 **请求配置**
 *
 * @分类 GitHub 认证
 * @请求头 `POST /v1/auth/github/authorize`
 */
const postV1AuthGithubAuthorizeRequestConfig: PostV1AuthGithubAuthorizeRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_2,
  devUrl: devUrl_0_0_0_2,
  prodUrl: prodUrl_0_0_0_2,
  path: '/v1/auth/github/authorize',
  method: Method.POST,
  requestHeaders: {},
  requestBodyType: RequestBodyType.raw,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_2,
  paramNames: [],
  queryNames: [],
  requestDataOptional: true,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'postV1AuthGithubAuthorize',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 发起 GitHub 登录或注册 的 **请求函数**
 *
 * @分类 GitHub 认证
 * @请求头 `POST /v1/auth/github/authorize`
 */
export const postV1AuthGithubAuthorize = /*#__PURE__*/ (
  requestData?: IPostV1AuthGithubAuthorizeReq,
  ...args: UserRequestRestArgs
) => {
  return request<IPostV1AuthGithubAuthorizeRes>(prepare(postV1AuthGithubAuthorizeRequestConfig, requestData), ...args)
}

postV1AuthGithubAuthorize.requestConfig = postV1AuthGithubAuthorizeRequestConfig

/**
 * 接口 查看当前账号的 GitHub 绑定 的 **请求类型**
 *
 * @分类 GitHub 认证
 * @请求头 `GET /v1/me/github`
 */
export interface IGetV1MeGithubReq {}

/**
 * 接口 查看当前账号的 GitHub 绑定 的 **返回类型**
 *
 * @分类 GitHub 认证
 * @请求头 `GET /v1/me/github`
 */
export interface IGetV1MeGithubRes {
  /**
   * 是否启用 GitHub 登录
   */
  enabled?: boolean
  /**
   * 是否已绑定 GitHub
   */
  bound?: boolean
  /**
   * 已绑定的 GitHub 登录名；未绑定时为 null
   */
  login?: string
}

/**
 * 接口 查看当前账号的 GitHub 绑定 的 **请求配置的类型**
 *
 * @分类 GitHub 认证
 * @请求头 `GET /v1/me/github`
 */
type GetV1MeGithubRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/me/github', 'data', string, string, true>
>

/**
 * 接口 查看当前账号的 GitHub 绑定 的 **请求配置**
 *
 * @分类 GitHub 认证
 * @请求头 `GET /v1/me/github`
 */
const getV1MeGithubRequestConfig: GetV1MeGithubRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_2,
  devUrl: devUrl_0_0_0_2,
  prodUrl: prodUrl_0_0_0_2,
  path: '/v1/me/github',
  method: Method.GET,
  requestHeaders: {},
  requestBodyType: RequestBodyType.query,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_2,
  paramNames: [],
  queryNames: [],
  requestDataOptional: true,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'getV1MeGithub',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 查看当前账号的 GitHub 绑定 的 **请求函数**
 *
 * @分类 GitHub 认证
 * @请求头 `GET /v1/me/github`
 */
export const getV1MeGithub = /*#__PURE__*/ (requestData?: IGetV1MeGithubReq, ...args: UserRequestRestArgs) => {
  return request<IGetV1MeGithubRes>(prepare(getV1MeGithubRequestConfig, requestData), ...args)
}

getV1MeGithub.requestConfig = getV1MeGithubRequestConfig

/**
 * 接口 解除当前账号的 GitHub 绑定 的 **请求类型**
 *
 * @分类 GitHub 认证
 * @请求头 `DELETE /v1/me/github`
 */
export interface IDeleteV1MeGithubReq {}

/**
 * 接口 解除当前账号的 GitHub 绑定 的 **返回类型**
 *
 * @分类 GitHub 认证
 * @请求头 `DELETE /v1/me/github`
 */
export type IDeleteV1MeGithubRes = any

/**
 * 接口 解除当前账号的 GitHub 绑定 的 **请求配置的类型**
 *
 * @分类 GitHub 认证
 * @请求头 `DELETE /v1/me/github`
 */
type DeleteV1MeGithubRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/me/github', 'data', string, string, true>
>

/**
 * 接口 解除当前账号的 GitHub 绑定 的 **请求配置**
 *
 * @分类 GitHub 认证
 * @请求头 `DELETE /v1/me/github`
 */
const deleteV1MeGithubRequestConfig: DeleteV1MeGithubRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_2,
  devUrl: devUrl_0_0_0_2,
  prodUrl: prodUrl_0_0_0_2,
  path: '/v1/me/github',
  method: Method.DELETE,
  requestHeaders: {},
  requestBodyType: RequestBodyType.raw,
  responseBodyType: ResponseBodyType.raw,
  dataKey: dataKey_0_0_0_2,
  paramNames: [],
  queryNames: [],
  requestDataOptional: true,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'deleteV1MeGithub',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 解除当前账号的 GitHub 绑定 的 **请求函数**
 *
 * @分类 GitHub 认证
 * @请求头 `DELETE /v1/me/github`
 */
export const deleteV1MeGithub = /*#__PURE__*/ (requestData?: IDeleteV1MeGithubReq, ...args: UserRequestRestArgs) => {
  return request<IDeleteV1MeGithubRes>(prepare(deleteV1MeGithubRequestConfig, requestData), ...args)
}

deleteV1MeGithub.requestConfig = deleteV1MeGithubRequestConfig

/**
 * 接口 查询 GitHub 登录是否启用 的 **请求类型**
 *
 * @分类 GitHub 认证
 * @请求头 `GET /v1/auth/github/config`
 */
export interface IGetV1AuthGithubConfigReq {}

/**
 * 接口 查询 GitHub 登录是否启用 的 **返回类型**
 *
 * @分类 GitHub 认证
 * @请求头 `GET /v1/auth/github/config`
 */
export interface IGetV1AuthGithubConfigRes {
  /**
   * 是否启用 GitHub 登录
   */
  enabled?: boolean
}

/**
 * 接口 查询 GitHub 登录是否启用 的 **请求配置的类型**
 *
 * @分类 GitHub 认证
 * @请求头 `GET /v1/auth/github/config`
 */
type GetV1AuthGithubConfigRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/auth/github/config', 'data', string, string, true>
>

/**
 * 接口 查询 GitHub 登录是否启用 的 **请求配置**
 *
 * @分类 GitHub 认证
 * @请求头 `GET /v1/auth/github/config`
 */
const getV1AuthGithubConfigRequestConfig: GetV1AuthGithubConfigRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_2,
  devUrl: devUrl_0_0_0_2,
  prodUrl: prodUrl_0_0_0_2,
  path: '/v1/auth/github/config',
  method: Method.GET,
  requestHeaders: {},
  requestBodyType: RequestBodyType.query,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_2,
  paramNames: [],
  queryNames: [],
  requestDataOptional: true,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'getV1AuthGithubConfig',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 查询 GitHub 登录是否启用 的 **请求函数**
 *
 * @分类 GitHub 认证
 * @请求头 `GET /v1/auth/github/config`
 */
export const getV1AuthGithubConfig = /*#__PURE__*/ (
  requestData?: IGetV1AuthGithubConfigReq,
  ...args: UserRequestRestArgs
) => {
  return request<IGetV1AuthGithubConfigRes>(prepare(getV1AuthGithubConfigRequestConfig, requestData), ...args)
}

getV1AuthGithubConfig.requestConfig = getV1AuthGithubConfigRequestConfig

/**
 * 接口 GitHub 授权回调 的 **请求类型**
 *
 * @分类 GitHub 认证
 * @请求头 `GET /v1/auth/github/callback`
 */
export interface IGetV1AuthGithubCallbackReq {
  /**
   * GitHub 一次性授权码
   */
  code?: string
  /**
   * 原授权 state
   */
  state?: string
  /**
   * GitHub 授权错误
   */
  error?: string
}

/**
 * 接口 GitHub 授权回调 的 **返回类型**
 *
 * @分类 GitHub 认证
 * @请求头 `GET /v1/auth/github/callback`
 */
export type IGetV1AuthGithubCallbackRes = any

/**
 * 接口 GitHub 授权回调 的 **请求配置的类型**
 *
 * @分类 GitHub 认证
 * @请求头 `GET /v1/auth/github/callback`
 */
type GetV1AuthGithubCallbackRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/auth/github/callback',
    'data',
    string,
    'code' | 'state' | 'error',
    false
  >
>

/**
 * 接口 GitHub 授权回调 的 **请求配置**
 *
 * @分类 GitHub 认证
 * @请求头 `GET /v1/auth/github/callback`
 */
const getV1AuthGithubCallbackRequestConfig: GetV1AuthGithubCallbackRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_2,
  devUrl: devUrl_0_0_0_2,
  prodUrl: prodUrl_0_0_0_2,
  path: '/v1/auth/github/callback',
  method: Method.GET,
  requestHeaders: {},
  requestBodyType: RequestBodyType.query,
  responseBodyType: ResponseBodyType.raw,
  dataKey: dataKey_0_0_0_2,
  paramNames: [],
  queryNames: ['code', 'state', 'error'],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'getV1AuthGithubCallback',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 GitHub 授权回调 的 **请求函数**
 *
 * @分类 GitHub 认证
 * @请求头 `GET /v1/auth/github/callback`
 */
export const getV1AuthGithubCallback = /*#__PURE__*/ (
  requestData: IGetV1AuthGithubCallbackReq,
  ...args: UserRequestRestArgs
) => {
  return request<IGetV1AuthGithubCallbackRes>(prepare(getV1AuthGithubCallbackRequestConfig, requestData), ...args)
}

getV1AuthGithubCallback.requestConfig = getV1AuthGithubCallbackRequestConfig

const mockUrl_0_0_0_3 = 'http://127.0.0.1:64948/mock/0' as any
const devUrl_0_0_0_3 = '' as any
const prodUrl_0_0_0_3 = '' as any
const dataKey_0_0_0_3 = 'data' as any

/**
 * 接口 查询用户列表 的 **请求类型**
 *
 * @分类 用户管理
 * @请求头 `GET /v1/users`
 */
export interface IGetV1UsersReq {
  /**
   * 页码，从 1 开始
   */
  page?: string
  /**
   * 每页数量，范围 1 到 100
   */
  pageSize?: string
  /**
   * 按用户名或昵称模糊查询
   */
  username?: string
  /**
   * 用户状态：0 禁用，1 启用
   */
  status?: string
}

/**
 * 接口 查询用户列表 的 **返回类型**
 *
 * @分类 用户管理
 * @请求头 `GET /v1/users`
 */
export interface IGetV1UsersRes {
  /**
   * 用户列表
   */
  list?: {
    /**
     * 用户 ID
     */
    id?: number
    /**
     * 用户名
     */
    username?: string
    /**
     * 昵称
     */
    nickname?: string
    /**
     * 邮箱
     */
    email?: string
    /**
     * 手机号
     */
    phone?: string
    /**
     * 头像地址
     */
    avatar?: string
    /**
     * 角色编码列表
     */
    roleCodes?: string[]
    /**
     * 状态：0 禁用，1 启用
     */
    status?: number
    /**
     * 最后登录时间，UTC
     */
    lastLogin?: string
  }[]
  pagination?: {
    /**
     * 当前页码
     */
    page?: number
    /**
     * 每页数量
     */
    pageSize?: number
    /**
     * 记录总数
     */
    total?: number
    /**
     * 总页数
     */
    totalPages?: number
  }
}

/**
 * 接口 查询用户列表 的 **请求配置的类型**
 *
 * @分类 用户管理
 * @请求头 `GET /v1/users`
 */
type GetV1UsersRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/users',
    'data',
    string,
    'page' | 'pageSize' | 'username' | 'status',
    false
  >
>

/**
 * 接口 查询用户列表 的 **请求配置**
 *
 * @分类 用户管理
 * @请求头 `GET /v1/users`
 */
const getV1UsersRequestConfig: GetV1UsersRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_3,
  devUrl: devUrl_0_0_0_3,
  prodUrl: prodUrl_0_0_0_3,
  path: '/v1/users',
  method: Method.GET,
  requestHeaders: {},
  requestBodyType: RequestBodyType.query,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_3,
  paramNames: [],
  queryNames: ['page', 'pageSize', 'username', 'status'],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'getV1Users',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 查询用户列表 的 **请求函数**
 *
 * @分类 用户管理
 * @请求头 `GET /v1/users`
 */
export const getV1Users = /*#__PURE__*/ (requestData: IGetV1UsersReq, ...args: UserRequestRestArgs) => {
  return request<IGetV1UsersRes>(prepare(getV1UsersRequestConfig, requestData), ...args)
}

getV1Users.requestConfig = getV1UsersRequestConfig

/**
 * 接口 创建用户 的 **请求类型**
 *
 * @分类 用户管理
 * @请求头 `POST /v1/users`
 */
export interface IPostV1UsersReq {
  /**
   * 用户名
   */
  username: string
  /**
   * 密码，至少 6 位；省略时使用系统默认密码
   */
  password?: string
  /**
   * 昵称
   */
  nickname?: string
  /**
   * 邮箱
   */
  email?: string
  /**
   * 手机号，6 至 20 位数字
   */
  phone?: string
  /**
   * 头像地址
   */
  avatar?: string
  /**
   * 状态：0 禁用，1 启用
   */
  status?: number
  /**
   * 分配的角色编码列表，省略时为空列表
   */
  roleCodes?: string[]
}

/**
 * 接口 创建用户 的 **返回类型**
 *
 * @分类 用户管理
 * @请求头 `POST /v1/users`
 */
export interface IPostV1UsersRes {
  /**
   * 用户 ID
   */
  id?: number
  /**
   * 用户名
   */
  username?: string
  /**
   * 昵称
   */
  nickname?: string
  /**
   * 邮箱
   */
  email?: string
  /**
   * 手机号
   */
  phone?: string
  /**
   * 头像地址
   */
  avatar?: string
  /**
   * 角色编码列表
   */
  roleCodes?: string[]
  /**
   * 状态：0 禁用，1 启用
   */
  status?: number
}

/**
 * 接口 创建用户 的 **请求配置的类型**
 *
 * @分类 用户管理
 * @请求头 `POST /v1/users`
 */
type PostV1UsersRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/users', 'data', string, string, false>
>

/**
 * 接口 创建用户 的 **请求配置**
 *
 * @分类 用户管理
 * @请求头 `POST /v1/users`
 */
const postV1UsersRequestConfig: PostV1UsersRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_3,
  devUrl: devUrl_0_0_0_3,
  prodUrl: prodUrl_0_0_0_3,
  path: '/v1/users',
  method: Method.POST,
  requestHeaders: {},
  requestBodyType: RequestBodyType.json,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_3,
  paramNames: [],
  queryNames: [],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'postV1Users',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 创建用户 的 **请求函数**
 *
 * @分类 用户管理
 * @请求头 `POST /v1/users`
 */
export const postV1Users = /*#__PURE__*/ (requestData: IPostV1UsersReq, ...args: UserRequestRestArgs) => {
  return request<IPostV1UsersRes>(prepare(postV1UsersRequestConfig, requestData), ...args)
}

postV1Users.requestConfig = postV1UsersRequestConfig

/**
 * 接口 获取用户详情 的 **请求类型**
 *
 * @分类 用户管理
 * @请求头 `GET /v1/users/{id}`
 */
export interface IGetV1UsersByIdReq {
  /**
   * 用户 ID，必须为正整数
   */
  id: string
}

/**
 * 接口 获取用户详情 的 **返回类型**
 *
 * @分类 用户管理
 * @请求头 `GET /v1/users/{id}`
 */
export interface IGetV1UsersByIdRes {
  /**
   * 用户 ID
   */
  id?: number
  /**
   * 用户名
   */
  username?: string
  /**
   * 昵称
   */
  nickname?: string
  /**
   * 邮箱
   */
  email?: string
  /**
   * 手机号
   */
  phone?: string
  /**
   * 头像地址
   */
  avatar?: string
  /**
   * 角色编码列表
   */
  roleCodes?: string[]
  /**
   * 状态：0 禁用，1 启用
   */
  status?: number
}

/**
 * 接口 获取用户详情 的 **请求配置的类型**
 *
 * @分类 用户管理
 * @请求头 `GET /v1/users/{id}`
 */
type GetV1UsersByIdRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/users/{id}', 'data', 'id', string, false>
>

/**
 * 接口 获取用户详情 的 **请求配置**
 *
 * @分类 用户管理
 * @请求头 `GET /v1/users/{id}`
 */
const getV1UsersByIdRequestConfig: GetV1UsersByIdRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_3,
  devUrl: devUrl_0_0_0_3,
  prodUrl: prodUrl_0_0_0_3,
  path: '/v1/users/{id}',
  method: Method.GET,
  requestHeaders: {},
  requestBodyType: RequestBodyType.query,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_3,
  paramNames: ['id'],
  queryNames: [],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'getV1UsersById',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 获取用户详情 的 **请求函数**
 *
 * @分类 用户管理
 * @请求头 `GET /v1/users/{id}`
 */
export const getV1UsersById = /*#__PURE__*/ (requestData: IGetV1UsersByIdReq, ...args: UserRequestRestArgs) => {
  return request<IGetV1UsersByIdRes>(prepare(getV1UsersByIdRequestConfig, requestData), ...args)
}

getV1UsersById.requestConfig = getV1UsersByIdRequestConfig

/**
 * 接口 删除用户 的 **请求类型**
 *
 * @分类 用户管理
 * @请求头 `DELETE /v1/users/{id}`
 */
export interface IDeleteV1UsersByIdReq {
  /**
   * 用户 ID，必须为正整数
   */
  id: string
}

/**
 * 接口 删除用户 的 **返回类型**
 *
 * @分类 用户管理
 * @请求头 `DELETE /v1/users/{id}`
 */
export type IDeleteV1UsersByIdRes = any

/**
 * 接口 删除用户 的 **请求配置的类型**
 *
 * @分类 用户管理
 * @请求头 `DELETE /v1/users/{id}`
 */
type DeleteV1UsersByIdRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/users/{id}', 'data', 'id', string, false>
>

/**
 * 接口 删除用户 的 **请求配置**
 *
 * @分类 用户管理
 * @请求头 `DELETE /v1/users/{id}`
 */
const deleteV1UsersByIdRequestConfig: DeleteV1UsersByIdRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_3,
  devUrl: devUrl_0_0_0_3,
  prodUrl: prodUrl_0_0_0_3,
  path: '/v1/users/{id}',
  method: Method.DELETE,
  requestHeaders: {},
  requestBodyType: RequestBodyType.raw,
  responseBodyType: ResponseBodyType.raw,
  dataKey: dataKey_0_0_0_3,
  paramNames: ['id'],
  queryNames: [],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'deleteV1UsersById',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 删除用户 的 **请求函数**
 *
 * @分类 用户管理
 * @请求头 `DELETE /v1/users/{id}`
 */
export const deleteV1UsersById = /*#__PURE__*/ (requestData: IDeleteV1UsersByIdReq, ...args: UserRequestRestArgs) => {
  return request<IDeleteV1UsersByIdRes>(prepare(deleteV1UsersByIdRequestConfig, requestData), ...args)
}

deleteV1UsersById.requestConfig = deleteV1UsersByIdRequestConfig

/**
 * 接口 更新用户 的 **请求类型**
 *
 * @分类 用户管理
 * @请求头 `PATCH /v1/users/{id}`
 */
export interface IPatchV1UsersByIdReq {
  /**
   * 用户名
   */
  username?: string
  /**
   * 昵称
   */
  nickname?: string
  /**
   * 邮箱
   */
  email?: string
  /**
   * 手机号，6 至 20 位数字
   */
  phone?: string
  /**
   * 头像地址
   */
  avatar?: string
  /**
   * 状态：0 禁用，1 启用
   */
  status?: number
  /**
   * 角色编码列表；提供时替换现有角色
   */
  roleCodes?: string[]
  /**
   * 用户 ID，必须为正整数
   */
  id: string
}

/**
 * 接口 更新用户 的 **返回类型**
 *
 * @分类 用户管理
 * @请求头 `PATCH /v1/users/{id}`
 */
export interface IPatchV1UsersByIdRes {
  /**
   * 用户 ID
   */
  id?: number
  /**
   * 用户名
   */
  username?: string
  /**
   * 昵称
   */
  nickname?: string
  /**
   * 邮箱
   */
  email?: string
  /**
   * 手机号
   */
  phone?: string
  /**
   * 头像地址
   */
  avatar?: string
  /**
   * 角色编码列表
   */
  roleCodes?: string[]
  /**
   * 状态：0 禁用，1 启用
   */
  status?: number
}

/**
 * 接口 更新用户 的 **请求配置的类型**
 *
 * @分类 用户管理
 * @请求头 `PATCH /v1/users/{id}`
 */
type PatchV1UsersByIdRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/users/{id}', 'data', 'id', string, false>
>

/**
 * 接口 更新用户 的 **请求配置**
 *
 * @分类 用户管理
 * @请求头 `PATCH /v1/users/{id}`
 */
const patchV1UsersByIdRequestConfig: PatchV1UsersByIdRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_3,
  devUrl: devUrl_0_0_0_3,
  prodUrl: prodUrl_0_0_0_3,
  path: '/v1/users/{id}',
  method: Method.PATCH,
  requestHeaders: {},
  requestBodyType: RequestBodyType.json,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_3,
  paramNames: ['id'],
  queryNames: [],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'patchV1UsersById',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 更新用户 的 **请求函数**
 *
 * @分类 用户管理
 * @请求头 `PATCH /v1/users/{id}`
 */
export const patchV1UsersById = /*#__PURE__*/ (requestData: IPatchV1UsersByIdReq, ...args: UserRequestRestArgs) => {
  return request<IPatchV1UsersByIdRes>(prepare(patchV1UsersByIdRequestConfig, requestData), ...args)
}

patchV1UsersById.requestConfig = patchV1UsersByIdRequestConfig

/**
 * 接口 导出用户列表 的 **请求类型**
 *
 * @分类 用户管理
 * @请求头 `GET /v1/users/export`
 */
export interface IGetV1UsersExportReq {
  /**
   * 按用户名或昵称模糊查询
   */
  username?: string
  /**
   * 用户状态：0 禁用，1 启用
   */
  status?: string
}

/**
 * 接口 导出用户列表 的 **返回类型**
 *
 * @分类 用户管理
 * @请求头 `GET /v1/users/export`
 */
export type IGetV1UsersExportRes = any

/**
 * 接口 导出用户列表 的 **请求配置的类型**
 *
 * @分类 用户管理
 * @请求头 `GET /v1/users/export`
 */
type GetV1UsersExportRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/users/export',
    'data',
    string,
    'username' | 'status',
    false
  >
>

/**
 * 接口 导出用户列表 的 **请求配置**
 *
 * @分类 用户管理
 * @请求头 `GET /v1/users/export`
 */
const getV1UsersExportRequestConfig: GetV1UsersExportRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_3,
  devUrl: devUrl_0_0_0_3,
  prodUrl: prodUrl_0_0_0_3,
  path: '/v1/users/export',
  method: Method.GET,
  requestHeaders: {},
  requestBodyType: RequestBodyType.query,
  responseBodyType: ResponseBodyType.raw,
  dataKey: dataKey_0_0_0_3,
  paramNames: [],
  queryNames: ['username', 'status'],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'getV1UsersExport',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 导出用户列表 的 **请求函数**
 *
 * @分类 用户管理
 * @请求头 `GET /v1/users/export`
 */
export const getV1UsersExport = /*#__PURE__*/ (requestData: IGetV1UsersExportReq, ...args: UserRequestRestArgs) => {
  return request<IGetV1UsersExportRes>(prepare(getV1UsersExportRequestConfig, requestData), ...args)
}

getV1UsersExport.requestConfig = getV1UsersExportRequestConfig

/**
 * 接口 获取当前用户 的 **请求类型**
 *
 * @分类 用户管理
 * @请求头 `GET /v1/me`
 */
export interface IGetV1MeReq {}

/**
 * 接口 获取当前用户 的 **返回类型**
 *
 * @分类 用户管理
 * @请求头 `GET /v1/me`
 */
export interface IGetV1MeRes {
  /**
   * 用户 ID
   */
  id?: number
  /**
   * 用户名
   */
  username?: string
  /**
   * 昵称
   */
  nickname?: string
  /**
   * 邮箱
   */
  email?: string
  /**
   * 手机号
   */
  phone?: string
  /**
   * 头像地址
   */
  avatar?: string
  /**
   * 角色编码列表
   */
  roleCodes?: string[]
  /**
   * 状态：0 禁用，1 启用
   */
  status?: number
}

/**
 * 接口 获取当前用户 的 **请求配置的类型**
 *
 * @分类 用户管理
 * @请求头 `GET /v1/me`
 */
type GetV1MeRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/me', 'data', string, string, true>
>

/**
 * 接口 获取当前用户 的 **请求配置**
 *
 * @分类 用户管理
 * @请求头 `GET /v1/me`
 */
const getV1MeRequestConfig: GetV1MeRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_3,
  devUrl: devUrl_0_0_0_3,
  prodUrl: prodUrl_0_0_0_3,
  path: '/v1/me',
  method: Method.GET,
  requestHeaders: {},
  requestBodyType: RequestBodyType.query,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_3,
  paramNames: [],
  queryNames: [],
  requestDataOptional: true,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'getV1Me',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 获取当前用户 的 **请求函数**
 *
 * @分类 用户管理
 * @请求头 `GET /v1/me`
 */
export const getV1Me = /*#__PURE__*/ (requestData?: IGetV1MeReq, ...args: UserRequestRestArgs) => {
  return request<IGetV1MeRes>(prepare(getV1MeRequestConfig, requestData), ...args)
}

getV1Me.requestConfig = getV1MeRequestConfig

const mockUrl_0_0_0_4 = 'http://127.0.0.1:64948/mock/0' as any
const devUrl_0_0_0_4 = '' as any
const prodUrl_0_0_0_4 = '' as any
const dataKey_0_0_0_4 = 'data' as any

/**
 * 接口 重置页面生成工作区 的 **请求类型**
 *
 * @分类 AI 页面生成
 * @请求头 `POST /v1/ai/page-agent/reset`
 */
export interface IPostV1AiPageAgentResetReq {}

/**
 * 接口 重置页面生成工作区 的 **返回类型**
 *
 * @分类 AI 页面生成
 * @请求头 `POST /v1/ai/page-agent/reset`
 */
export interface IPostV1AiPageAgentResetRes {
  messages: {
    /**
     * 消息角色
     */
    role: 'user' | 'assistant'
    /**
     * 消息内容（用户最多 4000 字符，助手最多 8000 字符）
     */
    content: string
    /**
     * 模型返回的独立推理内容，与回复正文分开保存
     */
    reasoningContent: string
  }[]
  /**
   * 受约束的页面结构描述
   */
  pageSpec: {
    pageKey: string
    title: string
    fields: {
      key: string
      label: string
      /**
       * 字段组件类型
       */
      kind: 'input' | 'number' | 'select' | 'radio'
      required: boolean
      searchable: boolean
      options: {
        label: string
        value: string
        /**
         * 可选颜色
         */
        color: string | null
      }[]
      /**
       * 预览列配置
       */
      column: {
        width: number | null
        minWidth: number
        align: 'left' | 'center' | 'right'
        fixed: ('left' | 'right') | null
        sortable: boolean
        showOverflowTooltip: boolean
      }
      /**
       * 已有控件支持的声明式属性
       */
      attrs: {
        placeholder: string | null
        clearable: boolean
        disabled: boolean
        maxlength: number | null
        min: number | null
        max: number | null
        step: number | null
        precision: number | null
      }
      /**
       * 控件默认标量值
       */
      defaultValue: string | number | null
    }[]
    /**
     * 页面操作
     */
    actions: ('create' | 'edit' | 'delete' | 'detail')[]
    /**
     * 表单列数
     */
    formColumns: number
    /**
     * 受约束的表格配置
     */
    table: {
      border: boolean
      stripe: boolean
      size: 'small' | 'default' | 'large'
      showHeader: boolean
      height: number | null
    }
    /**
     * 预览分页配置
     */
    pagination: {
      enabled: boolean
      pageSize: number
      pageSizes: number[]
      background: boolean
    }
    /**
     * 预览表单配置
     */
    form: {
      labelPosition: 'left' | 'right' | 'top'
      labelWidth: number
      size: 'small' | 'default' | 'large'
      disabled: boolean
    }
    /**
     * 可选合成数据，最多 100 行；字段值为字符串、数字或 null
     */
    sampleData: {}[] | null
  }
  /**
   * 根据 PageSpec 编译出的安全预览数据
   */
  preview: {
    columns: {
      key: string
      label: string
      /**
       * 组件类型
       */
      type: 'input' | 'number' | 'select' | 'radio'
      options: {
        label: string
        value: string
        /**
         * 可选颜色
         */
        color: string | null
      }[]
      /**
       * 预览列配置
       */
      column: {
        width: number | null
        minWidth: number
        align: 'left' | 'center' | 'right'
        fixed: ('left' | 'right') | null
        sortable: boolean
        showOverflowTooltip: boolean
      }
      /**
       * 已有控件支持的声明式属性
       */
      attrs: {
        placeholder: string | null
        clearable: boolean
        disabled: boolean
        maxlength: number | null
        min: number | null
        max: number | null
        step: number | null
        precision: number | null
      }
      defaultValue: string | number | null
    }[]
    filters: {
      key: string
      label: string
      /**
       * 组件类型
       */
      type: 'input' | 'number' | 'select' | 'radio'
      options: {
        label: string
        value: string
        /**
         * 可选颜色
         */
        color: string | null
      }[]
      /**
       * 预览列配置
       */
      column: {
        width: number | null
        minWidth: number
        align: 'left' | 'center' | 'right'
        fixed: ('left' | 'right') | null
        sortable: boolean
        showOverflowTooltip: boolean
      }
      /**
       * 已有控件支持的声明式属性
       */
      attrs: {
        placeholder: string | null
        clearable: boolean
        disabled: boolean
        maxlength: number | null
        min: number | null
        max: number | null
        step: number | null
        precision: number | null
      }
      defaultValue: string | number | null
    }[]
    form: {
      key: string
      label: string
      type: 'input' | 'number' | 'select' | 'radio'
      required: boolean
      options: {
        label: string
        value: string
        /**
         * 可选颜色
         */
        color: string | null
      }[]
      /**
       * 预览列配置
       */
      column: {
        width: number | null
        minWidth: number
        align: 'left' | 'center' | 'right'
        fixed: ('left' | 'right') | null
        sortable: boolean
        showOverflowTooltip: boolean
      }
      /**
       * 已有控件支持的声明式属性
       */
      attrs: {
        placeholder: string | null
        clearable: boolean
        disabled: boolean
        maxlength: number | null
        min: number | null
        max: number | null
        step: number | null
        precision: number | null
      }
      defaultValue: string | number | null
    }[]
    /**
     * 页面操作
     */
    actions: ('create' | 'edit' | 'delete' | 'detail')[]
    formColumns: number
    /**
     * 合成行；除 id 外键为字段 key，值为标量
     */
    rows: {}[]
    total: number
  }
}

/**
 * 接口 重置页面生成工作区 的 **请求配置的类型**
 *
 * @分类 AI 页面生成
 * @请求头 `POST /v1/ai/page-agent/reset`
 */
type PostV1AiPageAgentResetRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/ai/page-agent/reset', 'data', string, string, true>
>

/**
 * 接口 重置页面生成工作区 的 **请求配置**
 *
 * @分类 AI 页面生成
 * @请求头 `POST /v1/ai/page-agent/reset`
 */
const postV1AiPageAgentResetRequestConfig: PostV1AiPageAgentResetRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_4,
  devUrl: devUrl_0_0_0_4,
  prodUrl: prodUrl_0_0_0_4,
  path: '/v1/ai/page-agent/reset',
  method: Method.POST,
  requestHeaders: {},
  requestBodyType: RequestBodyType.raw,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_4,
  paramNames: [],
  queryNames: [],
  requestDataOptional: true,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'postV1AiPageAgentReset',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 重置页面生成工作区 的 **请求函数**
 *
 * @分类 AI 页面生成
 * @请求头 `POST /v1/ai/page-agent/reset`
 */
export const postV1AiPageAgentReset = /*#__PURE__*/ (
  requestData?: IPostV1AiPageAgentResetReq,
  ...args: UserRequestRestArgs
) => {
  return request<IPostV1AiPageAgentResetRes>(prepare(postV1AiPageAgentResetRequestConfig, requestData), ...args)
}

postV1AiPageAgentReset.requestConfig = postV1AiPageAgentResetRequestConfig

/**
 * 接口 发送页面生成消息 的 **请求类型**
 *
 * @分类 AI 页面生成
 * @请求头 `POST /v1/ai/page-agent/messages/stream`
 */
export interface IPostV1AiPageAgentMessagesStreamReq {
  /**
   * 用户对页面的自然语言描述
   */
  content: string
}

/**
 * 接口 发送页面生成消息 的 **返回类型**
 *
 * @分类 AI 页面生成
 * @请求头 `POST /v1/ai/page-agent/messages/stream`
 */
export interface IPostV1AiPageAgentMessagesStreamRes {
  [k: string]: unknown
}

/**
 * 接口 发送页面生成消息 的 **请求配置的类型**
 *
 * @分类 AI 页面生成
 * @请求头 `POST /v1/ai/page-agent/messages/stream`
 */
type PostV1AiPageAgentMessagesStreamRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/page-agent/messages/stream',
    'data',
    string,
    string,
    false
  >
>

/**
 * 接口 发送页面生成消息 的 **请求配置**
 *
 * @分类 AI 页面生成
 * @请求头 `POST /v1/ai/page-agent/messages/stream`
 */
const postV1AiPageAgentMessagesStreamRequestConfig: PostV1AiPageAgentMessagesStreamRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_4,
  devUrl: devUrl_0_0_0_4,
  prodUrl: prodUrl_0_0_0_4,
  path: '/v1/ai/page-agent/messages/stream',
  method: Method.POST,
  requestHeaders: {},
  requestBodyType: RequestBodyType.json,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_4,
  paramNames: [],
  queryNames: [],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'postV1AiPageAgentMessagesStream',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 发送页面生成消息 的 **请求函数**
 *
 * @分类 AI 页面生成
 * @请求头 `POST /v1/ai/page-agent/messages/stream`
 */
export const postV1AiPageAgentMessagesStream = /*#__PURE__*/ (
  requestData: IPostV1AiPageAgentMessagesStreamReq,
  ...args: UserRequestRestArgs
) => {
  return request<IPostV1AiPageAgentMessagesStreamRes>(
    prepare(postV1AiPageAgentMessagesStreamRequestConfig, requestData),
    ...args,
  )
}

postV1AiPageAgentMessagesStream.requestConfig = postV1AiPageAgentMessagesStreamRequestConfig

/**
 * 接口 恢复页面生成工作区 的 **请求类型**
 *
 * @分类 AI 页面生成
 * @请求头 `GET /v1/ai/page-agent/workspace`
 */
export interface IGetV1AiPageAgentWorkspaceReq {}

/**
 * 接口 恢复页面生成工作区 的 **返回类型**
 *
 * @分类 AI 页面生成
 * @请求头 `GET /v1/ai/page-agent/workspace`
 */
export interface IGetV1AiPageAgentWorkspaceRes {
  messages: {
    /**
     * 消息角色
     */
    role: 'user' | 'assistant'
    /**
     * 消息内容（用户最多 4000 字符，助手最多 8000 字符）
     */
    content: string
    /**
     * 模型返回的独立推理内容，与回复正文分开保存
     */
    reasoningContent: string
  }[]
  /**
   * 受约束的页面结构描述
   */
  pageSpec: {
    pageKey: string
    title: string
    fields: {
      key: string
      label: string
      /**
       * 字段组件类型
       */
      kind: 'input' | 'number' | 'select' | 'radio'
      required: boolean
      searchable: boolean
      options: {
        label: string
        value: string
        /**
         * 可选颜色
         */
        color: string | null
      }[]
      /**
       * 预览列配置
       */
      column: {
        width: number | null
        minWidth: number
        align: 'left' | 'center' | 'right'
        fixed: ('left' | 'right') | null
        sortable: boolean
        showOverflowTooltip: boolean
      }
      /**
       * 已有控件支持的声明式属性
       */
      attrs: {
        placeholder: string | null
        clearable: boolean
        disabled: boolean
        maxlength: number | null
        min: number | null
        max: number | null
        step: number | null
        precision: number | null
      }
      /**
       * 控件默认标量值
       */
      defaultValue: string | number | null
    }[]
    /**
     * 页面操作
     */
    actions: ('create' | 'edit' | 'delete' | 'detail')[]
    /**
     * 表单列数
     */
    formColumns: number
    /**
     * 受约束的表格配置
     */
    table: {
      border: boolean
      stripe: boolean
      size: 'small' | 'default' | 'large'
      showHeader: boolean
      height: number | null
    }
    /**
     * 预览分页配置
     */
    pagination: {
      enabled: boolean
      pageSize: number
      pageSizes: number[]
      background: boolean
    }
    /**
     * 预览表单配置
     */
    form: {
      labelPosition: 'left' | 'right' | 'top'
      labelWidth: number
      size: 'small' | 'default' | 'large'
      disabled: boolean
    }
    /**
     * 可选合成数据，最多 100 行；字段值为字符串、数字或 null
     */
    sampleData: {}[] | null
  }
  /**
   * 根据 PageSpec 编译出的安全预览数据
   */
  preview: {
    columns: {
      key: string
      label: string
      /**
       * 组件类型
       */
      type: 'input' | 'number' | 'select' | 'radio'
      options: {
        label: string
        value: string
        /**
         * 可选颜色
         */
        color: string | null
      }[]
      /**
       * 预览列配置
       */
      column: {
        width: number | null
        minWidth: number
        align: 'left' | 'center' | 'right'
        fixed: ('left' | 'right') | null
        sortable: boolean
        showOverflowTooltip: boolean
      }
      /**
       * 已有控件支持的声明式属性
       */
      attrs: {
        placeholder: string | null
        clearable: boolean
        disabled: boolean
        maxlength: number | null
        min: number | null
        max: number | null
        step: number | null
        precision: number | null
      }
      defaultValue: string | number | null
    }[]
    filters: {
      key: string
      label: string
      /**
       * 组件类型
       */
      type: 'input' | 'number' | 'select' | 'radio'
      options: {
        label: string
        value: string
        /**
         * 可选颜色
         */
        color: string | null
      }[]
      /**
       * 预览列配置
       */
      column: {
        width: number | null
        minWidth: number
        align: 'left' | 'center' | 'right'
        fixed: ('left' | 'right') | null
        sortable: boolean
        showOverflowTooltip: boolean
      }
      /**
       * 已有控件支持的声明式属性
       */
      attrs: {
        placeholder: string | null
        clearable: boolean
        disabled: boolean
        maxlength: number | null
        min: number | null
        max: number | null
        step: number | null
        precision: number | null
      }
      defaultValue: string | number | null
    }[]
    form: {
      key: string
      label: string
      type: 'input' | 'number' | 'select' | 'radio'
      required: boolean
      options: {
        label: string
        value: string
        /**
         * 可选颜色
         */
        color: string | null
      }[]
      /**
       * 预览列配置
       */
      column: {
        width: number | null
        minWidth: number
        align: 'left' | 'center' | 'right'
        fixed: ('left' | 'right') | null
        sortable: boolean
        showOverflowTooltip: boolean
      }
      /**
       * 已有控件支持的声明式属性
       */
      attrs: {
        placeholder: string | null
        clearable: boolean
        disabled: boolean
        maxlength: number | null
        min: number | null
        max: number | null
        step: number | null
        precision: number | null
      }
      defaultValue: string | number | null
    }[]
    /**
     * 页面操作
     */
    actions: ('create' | 'edit' | 'delete' | 'detail')[]
    formColumns: number
    /**
     * 合成行；除 id 外键为字段 key，值为标量
     */
    rows: {}[]
    total: number
  }
}

/**
 * 接口 恢复页面生成工作区 的 **请求配置的类型**
 *
 * @分类 AI 页面生成
 * @请求头 `GET /v1/ai/page-agent/workspace`
 */
type GetV1AiPageAgentWorkspaceRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/ai/page-agent/workspace', 'data', string, string, true>
>

/**
 * 接口 恢复页面生成工作区 的 **请求配置**
 *
 * @分类 AI 页面生成
 * @请求头 `GET /v1/ai/page-agent/workspace`
 */
const getV1AiPageAgentWorkspaceRequestConfig: GetV1AiPageAgentWorkspaceRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_4,
  devUrl: devUrl_0_0_0_4,
  prodUrl: prodUrl_0_0_0_4,
  path: '/v1/ai/page-agent/workspace',
  method: Method.GET,
  requestHeaders: {},
  requestBodyType: RequestBodyType.query,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_4,
  paramNames: [],
  queryNames: [],
  requestDataOptional: true,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'getV1AiPageAgentWorkspace',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 恢复页面生成工作区 的 **请求函数**
 *
 * @分类 AI 页面生成
 * @请求头 `GET /v1/ai/page-agent/workspace`
 */
export const getV1AiPageAgentWorkspace = /*#__PURE__*/ (
  requestData?: IGetV1AiPageAgentWorkspaceReq,
  ...args: UserRequestRestArgs
) => {
  return request<IGetV1AiPageAgentWorkspaceRes>(prepare(getV1AiPageAgentWorkspaceRequestConfig, requestData), ...args)
}

getV1AiPageAgentWorkspace.requestConfig = getV1AiPageAgentWorkspaceRequestConfig

const mockUrl_0_0_0_5 = 'http://127.0.0.1:64948/mock/0' as any
const devUrl_0_0_0_5 = '' as any
const prodUrl_0_0_0_5 = '' as any
const dataKey_0_0_0_5 = 'data' as any

/**
 * 接口 上传单个文件 的 **请求类型**
 *
 * @分类 文件管理
 * @请求头 `POST /v1/files`
 */
export interface IPostV1FilesReq {
  /**
   * 公开文件根目录下的相对目录；省略时为 uploads。
   */
  path?: string
  [k: string]: unknown
}

/**
 * 接口 上传单个文件 的 **返回类型**
 *
 * @分类 文件管理
 * @请求头 `POST /v1/files`
 */
export interface IPostV1FilesRes {
  /**
   * 可公开访问的文件相对 URL。
   */
  url?: string
  /**
   * 服务端生成的文件名。
   */
  filename?: string
  /**
   * 上传时的文件名。
   */
  originalName?: string
  /**
   * 文件字节数。
   */
  size?: number
  /**
   * 上传文件的媒体类型。
   */
  mimetype?: string
}

/**
 * 接口 上传单个文件 的 **请求配置的类型**
 *
 * @分类 文件管理
 * @请求头 `POST /v1/files`
 */
type PostV1FilesRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/files', 'data', string, 'path', false>
>

/**
 * 接口 上传单个文件 的 **请求配置**
 *
 * @分类 文件管理
 * @请求头 `POST /v1/files`
 */
const postV1FilesRequestConfig: PostV1FilesRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_5,
  devUrl: devUrl_0_0_0_5,
  prodUrl: prodUrl_0_0_0_5,
  path: '/v1/files',
  method: Method.POST,
  requestHeaders: {},
  requestBodyType: RequestBodyType.json,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_5,
  paramNames: [],
  queryNames: ['path'],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'postV1Files',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 上传单个文件 的 **请求函数**
 *
 * @分类 文件管理
 * @请求头 `POST /v1/files`
 */
export const postV1Files = /*#__PURE__*/ (requestData: IPostV1FilesReq, ...args: UserRequestRestArgs) => {
  return request<IPostV1FilesRes>(prepare(postV1FilesRequestConfig, requestData), ...args)
}

postV1Files.requestConfig = postV1FilesRequestConfig

/**
 * 接口 删除文件 的 **请求类型**
 *
 * @分类 文件管理
 * @请求头 `DELETE /v1/files`
 */
export interface IDeleteV1FilesReq {
  /**
   * 上传结果中的文件 URL 路径，仅允许删除公开文件目录内的文件。
   */
  filePath: string
}

/**
 * 接口 删除文件 的 **返回类型**
 *
 * @分类 文件管理
 * @请求头 `DELETE /v1/files`
 */
export type IDeleteV1FilesRes = any

/**
 * 接口 删除文件 的 **请求配置的类型**
 *
 * @分类 文件管理
 * @请求头 `DELETE /v1/files`
 */
type DeleteV1FilesRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/files', 'data', string, string, false>
>

/**
 * 接口 删除文件 的 **请求配置**
 *
 * @分类 文件管理
 * @请求头 `DELETE /v1/files`
 */
const deleteV1FilesRequestConfig: DeleteV1FilesRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_5,
  devUrl: devUrl_0_0_0_5,
  prodUrl: prodUrl_0_0_0_5,
  path: '/v1/files',
  method: Method.DELETE,
  requestHeaders: {},
  requestBodyType: RequestBodyType.json,
  responseBodyType: ResponseBodyType.raw,
  dataKey: dataKey_0_0_0_5,
  paramNames: [],
  queryNames: [],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'deleteV1Files',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 删除文件 的 **请求函数**
 *
 * @分类 文件管理
 * @请求头 `DELETE /v1/files`
 */
export const deleteV1Files = /*#__PURE__*/ (requestData: IDeleteV1FilesReq, ...args: UserRequestRestArgs) => {
  return request<IDeleteV1FilesRes>(prepare(deleteV1FilesRequestConfig, requestData), ...args)
}

deleteV1Files.requestConfig = deleteV1FilesRequestConfig

/**
 * 接口 批量上传文件 的 **请求类型**
 *
 * @分类 文件管理
 * @请求头 `POST /v1/files/batch`
 */
export interface IPostV1FilesBatchReq {
  /**
   * 公开文件根目录下的相对目录；省略时为 uploads。
   */
  path?: string
  [k: string]: unknown
}

/**
 * 接口 批量上传文件 的 **返回类型**
 *
 * @分类 文件管理
 * @请求头 `POST /v1/files/batch`
 */
export type IPostV1FilesBatchRes = {
  /**
   * 可公开访问的文件相对 URL。
   */
  url?: string
  /**
   * 服务端生成的文件名。
   */
  filename?: string
  /**
   * 上传时的文件名。
   */
  originalName?: string
  /**
   * 文件字节数。
   */
  size?: number
  /**
   * 上传文件的媒体类型。
   */
  mimetype?: string
}[]

/**
 * 接口 批量上传文件 的 **请求配置的类型**
 *
 * @分类 文件管理
 * @请求头 `POST /v1/files/batch`
 */
type PostV1FilesBatchRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/files/batch', 'data', string, 'path', false>
>

/**
 * 接口 批量上传文件 的 **请求配置**
 *
 * @分类 文件管理
 * @请求头 `POST /v1/files/batch`
 */
const postV1FilesBatchRequestConfig: PostV1FilesBatchRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_5,
  devUrl: devUrl_0_0_0_5,
  prodUrl: prodUrl_0_0_0_5,
  path: '/v1/files/batch',
  method: Method.POST,
  requestHeaders: {},
  requestBodyType: RequestBodyType.json,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_5,
  paramNames: [],
  queryNames: ['path'],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'postV1FilesBatch',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 批量上传文件 的 **请求函数**
 *
 * @分类 文件管理
 * @请求头 `POST /v1/files/batch`
 */
export const postV1FilesBatch = /*#__PURE__*/ (requestData: IPostV1FilesBatchReq, ...args: UserRequestRestArgs) => {
  return request<IPostV1FilesBatchRes>(prepare(postV1FilesBatchRequestConfig, requestData), ...args)
}

postV1FilesBatch.requestConfig = postV1FilesBatchRequestConfig

const mockUrl_0_0_0_6 = 'http://127.0.0.1:64948/mock/0' as any
const devUrl_0_0_0_6 = '' as any
const prodUrl_0_0_0_6 = '' as any
const dataKey_0_0_0_6 = 'data' as any

/**
 * 接口 查询知识库列表 的 **请求类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases`
 */
export interface IGetV1AiKnowledgeBasesReq {
  page?: string
  pageSize?: string
}

/**
 * 接口 查询知识库列表 的 **返回类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases`
 */
export interface IGetV1AiKnowledgeBasesRes {
  items?: {
    id?: string
    name?: string
    description?: string
    status?: 'active' | 'disabled' | 'deleted'
    embeddingModel?: string
    embeddingDimension?: number
    chunkSize?: number
    chunkOverlap?: number
    documentCount?: number
    readyDocumentCount?: number
    createdAt?: string
    updatedAt?: string
  }[]
  total?: number
  page?: number
  pageSize?: number
}

/**
 * 接口 查询知识库列表 的 **请求配置的类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases`
 */
type GetV1AiKnowledgeBasesRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/knowledge-bases',
    'data',
    string,
    'page' | 'pageSize',
    false
  >
>

/**
 * 接口 查询知识库列表 的 **请求配置**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases`
 */
const getV1AiKnowledgeBasesRequestConfig: GetV1AiKnowledgeBasesRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_6,
  devUrl: devUrl_0_0_0_6,
  prodUrl: prodUrl_0_0_0_6,
  path: '/v1/ai/knowledge-bases',
  method: Method.GET,
  requestHeaders: {},
  requestBodyType: RequestBodyType.query,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_6,
  paramNames: [],
  queryNames: ['page', 'pageSize'],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'getV1AiKnowledgeBases',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 查询知识库列表 的 **请求函数**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases`
 */
export const getV1AiKnowledgeBases = /*#__PURE__*/ (
  requestData: IGetV1AiKnowledgeBasesReq,
  ...args: UserRequestRestArgs
) => {
  return request<IGetV1AiKnowledgeBasesRes>(prepare(getV1AiKnowledgeBasesRequestConfig, requestData), ...args)
}

getV1AiKnowledgeBases.requestConfig = getV1AiKnowledgeBasesRequestConfig

/**
 * 接口 创建知识库 的 **请求类型**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/knowledge-bases`
 */
export interface IPostV1AiKnowledgeBasesReq {
  name: string
  description?: string
}

/**
 * 接口 创建知识库 的 **返回类型**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/knowledge-bases`
 */
export interface IPostV1AiKnowledgeBasesRes {
  [k: string]: unknown
}

/**
 * 接口 创建知识库 的 **请求配置的类型**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/knowledge-bases`
 */
type PostV1AiKnowledgeBasesRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/ai/knowledge-bases', 'data', string, string, false>
>

/**
 * 接口 创建知识库 的 **请求配置**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/knowledge-bases`
 */
const postV1AiKnowledgeBasesRequestConfig: PostV1AiKnowledgeBasesRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_6,
  devUrl: devUrl_0_0_0_6,
  prodUrl: prodUrl_0_0_0_6,
  path: '/v1/ai/knowledge-bases',
  method: Method.POST,
  requestHeaders: {},
  requestBodyType: RequestBodyType.json,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_6,
  paramNames: [],
  queryNames: [],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'postV1AiKnowledgeBases',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 创建知识库 的 **请求函数**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/knowledge-bases`
 */
export const postV1AiKnowledgeBases = /*#__PURE__*/ (
  requestData: IPostV1AiKnowledgeBasesReq,
  ...args: UserRequestRestArgs
) => {
  return request<IPostV1AiKnowledgeBasesRes>(prepare(postV1AiKnowledgeBasesRequestConfig, requestData), ...args)
}

postV1AiKnowledgeBases.requestConfig = postV1AiKnowledgeBasesRequestConfig

/**
 * 接口 预览知识库检索 的 **请求类型**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/knowledge-bases/{knowledgeBaseId}/retrieval-preview`
 */
export interface IPostV1AiKnowledgeBasesByKnowledgeBaseIdRetrievalPreviewReq {
  query: string
  topK?: number
  knowledgeBaseId: string
}

/**
 * 接口 预览知识库检索 的 **返回类型**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/knowledge-bases/{knowledgeBaseId}/retrieval-preview`
 */
export interface IPostV1AiKnowledgeBasesByKnowledgeBaseIdRetrievalPreviewRes {
  [k: string]: unknown
}

/**
 * 接口 预览知识库检索 的 **请求配置的类型**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/knowledge-bases/{knowledgeBaseId}/retrieval-preview`
 */
type PostV1AiKnowledgeBasesByKnowledgeBaseIdRetrievalPreviewRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/knowledge-bases/{knowledgeBaseId}/retrieval-preview',
    'data',
    'knowledgeBaseId',
    string,
    false
  >
>

/**
 * 接口 预览知识库检索 的 **请求配置**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/knowledge-bases/{knowledgeBaseId}/retrieval-preview`
 */
const postV1AiKnowledgeBasesByKnowledgeBaseIdRetrievalPreviewRequestConfig: PostV1AiKnowledgeBasesByKnowledgeBaseIdRetrievalPreviewRequestConfig =
  /*#__PURE__*/ {
    mockUrl: mockUrl_0_0_0_6,
    devUrl: devUrl_0_0_0_6,
    prodUrl: prodUrl_0_0_0_6,
    path: '/v1/ai/knowledge-bases/{knowledgeBaseId}/retrieval-preview',
    method: Method.POST,
    requestHeaders: {},
    requestBodyType: RequestBodyType.json,
    responseBodyType: ResponseBodyType.json,
    dataKey: dataKey_0_0_0_6,
    paramNames: ['knowledgeBaseId'],
    queryNames: [],
    requestDataOptional: false,
    requestDataJsonSchema: {},
    responseDataJsonSchema: {},
    requestFunctionName: 'postV1AiKnowledgeBasesByKnowledgeBaseIdRetrievalPreview',
    queryStringArrayFormat: QueryStringArrayFormat.brackets,
    extraInfo: {},
  }

/**
 * 接口 预览知识库检索 的 **请求函数**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/knowledge-bases/{knowledgeBaseId}/retrieval-preview`
 */
export const postV1AiKnowledgeBasesByKnowledgeBaseIdRetrievalPreview = /*#__PURE__*/ (
  requestData: IPostV1AiKnowledgeBasesByKnowledgeBaseIdRetrievalPreviewReq,
  ...args: UserRequestRestArgs
) => {
  return request<IPostV1AiKnowledgeBasesByKnowledgeBaseIdRetrievalPreviewRes>(
    prepare(postV1AiKnowledgeBasesByKnowledgeBaseIdRetrievalPreviewRequestConfig, requestData),
    ...args,
  )
}

postV1AiKnowledgeBasesByKnowledgeBaseIdRetrievalPreview.requestConfig =
  postV1AiKnowledgeBasesByKnowledgeBaseIdRetrievalPreviewRequestConfig

/**
 * 接口 查询知识库文档列表 的 **请求类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases/{knowledgeBaseId}/documents`
 */
export interface IGetV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsReq {
  page?: string
  pageSize?: string
  knowledgeBaseId: string
}

/**
 * 接口 查询知识库文档列表 的 **返回类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases/{knowledgeBaseId}/documents`
 */
export interface IGetV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsRes {
  items?: {
    id?: string
    knowledgeBaseId?: string
    fileName?: string
    mimeType?: string
    fileSize?: number
    status?: string
    activeGeneration?: number
    latestGeneration?: number
    chunkCount?: number
    pageCount?: number
    createdAt?: string
    updatedAt?: string
  }[]
  total?: number
  page?: number
  pageSize?: number
}

/**
 * 接口 查询知识库文档列表 的 **请求配置的类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases/{knowledgeBaseId}/documents`
 */
type GetV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/knowledge-bases/{knowledgeBaseId}/documents',
    'data',
    'knowledgeBaseId',
    'page' | 'pageSize',
    false
  >
>

/**
 * 接口 查询知识库文档列表 的 **请求配置**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases/{knowledgeBaseId}/documents`
 */
const getV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsRequestConfig: GetV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsRequestConfig =
  /*#__PURE__*/ {
    mockUrl: mockUrl_0_0_0_6,
    devUrl: devUrl_0_0_0_6,
    prodUrl: prodUrl_0_0_0_6,
    path: '/v1/ai/knowledge-bases/{knowledgeBaseId}/documents',
    method: Method.GET,
    requestHeaders: {},
    requestBodyType: RequestBodyType.query,
    responseBodyType: ResponseBodyType.json,
    dataKey: dataKey_0_0_0_6,
    paramNames: ['knowledgeBaseId'],
    queryNames: ['page', 'pageSize'],
    requestDataOptional: false,
    requestDataJsonSchema: {},
    responseDataJsonSchema: {},
    requestFunctionName: 'getV1AiKnowledgeBasesByKnowledgeBaseIdDocuments',
    queryStringArrayFormat: QueryStringArrayFormat.brackets,
    extraInfo: {},
  }

/**
 * 接口 查询知识库文档列表 的 **请求函数**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases/{knowledgeBaseId}/documents`
 */
export const getV1AiKnowledgeBasesByKnowledgeBaseIdDocuments = /*#__PURE__*/ (
  requestData: IGetV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsReq,
  ...args: UserRequestRestArgs
) => {
  return request<IGetV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsRes>(
    prepare(getV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsRequestConfig, requestData),
    ...args,
  )
}

getV1AiKnowledgeBasesByKnowledgeBaseIdDocuments.requestConfig =
  getV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsRequestConfig

/**
 * 接口 上传知识库文档 的 **请求类型**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/knowledge-bases/{knowledgeBaseId}/documents`
 */
export interface IPostV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsReq {
  knowledgeBaseId: string
  [k: string]: unknown
}

/**
 * 接口 上传知识库文档 的 **返回类型**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/knowledge-bases/{knowledgeBaseId}/documents`
 */
export interface IPostV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsRes {
  id?: string
  knowledgeBaseId?: string
  fileName?: string
  mimeType?: string
  fileSize?: number
  status?: string
  activeGeneration?: number
  latestGeneration?: number
  chunkCount?: number
  pageCount?: number
  createdAt?: string
  updatedAt?: string
}

/**
 * 接口 上传知识库文档 的 **请求配置的类型**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/knowledge-bases/{knowledgeBaseId}/documents`
 */
type PostV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/knowledge-bases/{knowledgeBaseId}/documents',
    'data',
    'knowledgeBaseId',
    string,
    false
  >
>

/**
 * 接口 上传知识库文档 的 **请求配置**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/knowledge-bases/{knowledgeBaseId}/documents`
 */
const postV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsRequestConfig: PostV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsRequestConfig =
  /*#__PURE__*/ {
    mockUrl: mockUrl_0_0_0_6,
    devUrl: devUrl_0_0_0_6,
    prodUrl: prodUrl_0_0_0_6,
    path: '/v1/ai/knowledge-bases/{knowledgeBaseId}/documents',
    method: Method.POST,
    requestHeaders: {},
    requestBodyType: RequestBodyType.json,
    responseBodyType: ResponseBodyType.json,
    dataKey: dataKey_0_0_0_6,
    paramNames: ['knowledgeBaseId'],
    queryNames: [],
    requestDataOptional: false,
    requestDataJsonSchema: {},
    responseDataJsonSchema: {},
    requestFunctionName: 'postV1AiKnowledgeBasesByKnowledgeBaseIdDocuments',
    queryStringArrayFormat: QueryStringArrayFormat.brackets,
    extraInfo: {},
  }

/**
 * 接口 上传知识库文档 的 **请求函数**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/knowledge-bases/{knowledgeBaseId}/documents`
 */
export const postV1AiKnowledgeBasesByKnowledgeBaseIdDocuments = /*#__PURE__*/ (
  requestData: IPostV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsReq,
  ...args: UserRequestRestArgs
) => {
  return request<IPostV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsRes>(
    prepare(postV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsRequestConfig, requestData),
    ...args,
  )
}

postV1AiKnowledgeBasesByKnowledgeBaseIdDocuments.requestConfig =
  postV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsRequestConfig

/**
 * 接口 重新建立文档索引 的 **请求类型**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/reindex`
 */
export interface IPostV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdReindexReq {
  knowledgeBaseId: string
  documentId: string
}

/**
 * 接口 重新建立文档索引 的 **返回类型**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/reindex`
 */
export interface IPostV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdReindexRes {
  id?: string
  knowledgeBaseId?: string
  fileName?: string
  mimeType?: string
  fileSize?: number
  status?: string
  activeGeneration?: number
  latestGeneration?: number
  chunkCount?: number
  pageCount?: number
  createdAt?: string
  updatedAt?: string
}

/**
 * 接口 重新建立文档索引 的 **请求配置的类型**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/reindex`
 */
type PostV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdReindexRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/reindex',
    'data',
    'knowledgeBaseId' | 'documentId',
    string,
    false
  >
>

/**
 * 接口 重新建立文档索引 的 **请求配置**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/reindex`
 */
const postV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdReindexRequestConfig: PostV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdReindexRequestConfig =
  /*#__PURE__*/ {
    mockUrl: mockUrl_0_0_0_6,
    devUrl: devUrl_0_0_0_6,
    prodUrl: prodUrl_0_0_0_6,
    path: '/v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/reindex',
    method: Method.POST,
    requestHeaders: {},
    requestBodyType: RequestBodyType.raw,
    responseBodyType: ResponseBodyType.json,
    dataKey: dataKey_0_0_0_6,
    paramNames: ['knowledgeBaseId', 'documentId'],
    queryNames: [],
    requestDataOptional: false,
    requestDataJsonSchema: {},
    responseDataJsonSchema: {},
    requestFunctionName: 'postV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdReindex',
    queryStringArrayFormat: QueryStringArrayFormat.brackets,
    extraInfo: {},
  }

/**
 * 接口 重新建立文档索引 的 **请求函数**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/reindex`
 */
export const postV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdReindex = /*#__PURE__*/ (
  requestData: IPostV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdReindexReq,
  ...args: UserRequestRestArgs
) => {
  return request<IPostV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdReindexRes>(
    prepare(postV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdReindexRequestConfig, requestData),
    ...args,
  )
}

postV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdReindex.requestConfig =
  postV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdReindexRequestConfig

/**
 * 接口 查询 AI 会话列表 的 **请求类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/conversations`
 */
export interface IGetV1AiConversationsReq {
  page?: string
  pageSize?: string
  mode?: string
  knowledgeBaseId?: string
}

/**
 * 接口 查询 AI 会话列表 的 **返回类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/conversations`
 */
export interface IGetV1AiConversationsRes {
  items?: {
    conversationId?: string
    title?: string
    knowledgeBaseId?: string
    model?: string
    status?: 'active' | 'archived'
    createdAt?: string
    updatedAt?: string
  }[]
  total?: number
  page?: number
  pageSize?: number
}

/**
 * 接口 查询 AI 会话列表 的 **请求配置的类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/conversations`
 */
type GetV1AiConversationsRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/conversations',
    'data',
    string,
    'page' | 'pageSize' | 'mode' | 'knowledgeBaseId',
    false
  >
>

/**
 * 接口 查询 AI 会话列表 的 **请求配置**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/conversations`
 */
const getV1AiConversationsRequestConfig: GetV1AiConversationsRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_6,
  devUrl: devUrl_0_0_0_6,
  prodUrl: prodUrl_0_0_0_6,
  path: '/v1/ai/conversations',
  method: Method.GET,
  requestHeaders: {},
  requestBodyType: RequestBodyType.query,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_6,
  paramNames: [],
  queryNames: ['page', 'pageSize', 'mode', 'knowledgeBaseId'],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'getV1AiConversations',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 查询 AI 会话列表 的 **请求函数**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/conversations`
 */
export const getV1AiConversations = /*#__PURE__*/ (
  requestData: IGetV1AiConversationsReq,
  ...args: UserRequestRestArgs
) => {
  return request<IGetV1AiConversationsRes>(prepare(getV1AiConversationsRequestConfig, requestData), ...args)
}

getV1AiConversations.requestConfig = getV1AiConversationsRequestConfig

/**
 * 接口 创建 AI 会话 的 **请求类型**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/conversations`
 */
export interface IPostV1AiConversationsReq {
  title?: string
  knowledgeBaseId?: string
  model?: string
}

/**
 * 接口 创建 AI 会话 的 **返回类型**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/conversations`
 */
export interface IPostV1AiConversationsRes {
  conversationId?: string
  title?: string
  knowledgeBaseId?: string
  model?: string
  status?: 'active' | 'archived'
  createdAt?: string
  updatedAt?: string
}

/**
 * 接口 创建 AI 会话 的 **请求配置的类型**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/conversations`
 */
type PostV1AiConversationsRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/ai/conversations', 'data', string, string, false>
>

/**
 * 接口 创建 AI 会话 的 **请求配置**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/conversations`
 */
const postV1AiConversationsRequestConfig: PostV1AiConversationsRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_6,
  devUrl: devUrl_0_0_0_6,
  prodUrl: prodUrl_0_0_0_6,
  path: '/v1/ai/conversations',
  method: Method.POST,
  requestHeaders: {},
  requestBodyType: RequestBodyType.json,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_6,
  paramNames: [],
  queryNames: [],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'postV1AiConversations',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 创建 AI 会话 的 **请求函数**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/conversations`
 */
export const postV1AiConversations = /*#__PURE__*/ (
  requestData: IPostV1AiConversationsReq,
  ...args: UserRequestRestArgs
) => {
  return request<IPostV1AiConversationsRes>(prepare(postV1AiConversationsRequestConfig, requestData), ...args)
}

postV1AiConversations.requestConfig = postV1AiConversationsRequestConfig

/**
 * 接口 发送 AI 会话消息 的 **请求类型**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/conversations/{conversationId}/messages/stream`
 */
export interface IPostV1AiConversationsByConversationIdMessagesStreamReq {
  clientRequestId: string
  content: string
  conversationId: string
}

/**
 * 接口 发送 AI 会话消息 的 **返回类型**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/conversations/{conversationId}/messages/stream`
 */
export interface IPostV1AiConversationsByConversationIdMessagesStreamRes {
  [k: string]: unknown
}

/**
 * 接口 发送 AI 会话消息 的 **请求配置的类型**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/conversations/{conversationId}/messages/stream`
 */
type PostV1AiConversationsByConversationIdMessagesStreamRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/conversations/{conversationId}/messages/stream',
    'data',
    'conversationId',
    string,
    false
  >
>

/**
 * 接口 发送 AI 会话消息 的 **请求配置**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/conversations/{conversationId}/messages/stream`
 */
const postV1AiConversationsByConversationIdMessagesStreamRequestConfig: PostV1AiConversationsByConversationIdMessagesStreamRequestConfig =
  /*#__PURE__*/ {
    mockUrl: mockUrl_0_0_0_6,
    devUrl: devUrl_0_0_0_6,
    prodUrl: prodUrl_0_0_0_6,
    path: '/v1/ai/conversations/{conversationId}/messages/stream',
    method: Method.POST,
    requestHeaders: {},
    requestBodyType: RequestBodyType.json,
    responseBodyType: ResponseBodyType.json,
    dataKey: dataKey_0_0_0_6,
    paramNames: ['conversationId'],
    queryNames: [],
    requestDataOptional: false,
    requestDataJsonSchema: {},
    responseDataJsonSchema: {},
    requestFunctionName: 'postV1AiConversationsByConversationIdMessagesStream',
    queryStringArrayFormat: QueryStringArrayFormat.brackets,
    extraInfo: {},
  }

/**
 * 接口 发送 AI 会话消息 的 **请求函数**
 *
 * @分类 AI 知识库
 * @请求头 `POST /v1/ai/conversations/{conversationId}/messages/stream`
 */
export const postV1AiConversationsByConversationIdMessagesStream = /*#__PURE__*/ (
  requestData: IPostV1AiConversationsByConversationIdMessagesStreamReq,
  ...args: UserRequestRestArgs
) => {
  return request<IPostV1AiConversationsByConversationIdMessagesStreamRes>(
    prepare(postV1AiConversationsByConversationIdMessagesStreamRequestConfig, requestData),
    ...args,
  )
}

postV1AiConversationsByConversationIdMessagesStream.requestConfig =
  postV1AiConversationsByConversationIdMessagesStreamRequestConfig

/**
 * 接口 查询 AI 会话详情 的 **请求类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/conversations/{conversationId}`
 */
export interface IGetV1AiConversationsByConversationIdReq {
  conversationId: string
}

/**
 * 接口 查询 AI 会话详情 的 **返回类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/conversations/{conversationId}`
 */
export interface IGetV1AiConversationsByConversationIdRes {
  conversationId?: string
  title?: string
  knowledgeBaseId?: string
  model?: string
  status?: 'active' | 'archived'
  createdAt?: string
  updatedAt?: string
}

/**
 * 接口 查询 AI 会话详情 的 **请求配置的类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/conversations/{conversationId}`
 */
type GetV1AiConversationsByConversationIdRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/conversations/{conversationId}',
    'data',
    'conversationId',
    string,
    false
  >
>

/**
 * 接口 查询 AI 会话详情 的 **请求配置**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/conversations/{conversationId}`
 */
const getV1AiConversationsByConversationIdRequestConfig: GetV1AiConversationsByConversationIdRequestConfig =
  /*#__PURE__*/ {
    mockUrl: mockUrl_0_0_0_6,
    devUrl: devUrl_0_0_0_6,
    prodUrl: prodUrl_0_0_0_6,
    path: '/v1/ai/conversations/{conversationId}',
    method: Method.GET,
    requestHeaders: {},
    requestBodyType: RequestBodyType.query,
    responseBodyType: ResponseBodyType.json,
    dataKey: dataKey_0_0_0_6,
    paramNames: ['conversationId'],
    queryNames: [],
    requestDataOptional: false,
    requestDataJsonSchema: {},
    responseDataJsonSchema: {},
    requestFunctionName: 'getV1AiConversationsByConversationId',
    queryStringArrayFormat: QueryStringArrayFormat.brackets,
    extraInfo: {},
  }

/**
 * 接口 查询 AI 会话详情 的 **请求函数**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/conversations/{conversationId}`
 */
export const getV1AiConversationsByConversationId = /*#__PURE__*/ (
  requestData: IGetV1AiConversationsByConversationIdReq,
  ...args: UserRequestRestArgs
) => {
  return request<IGetV1AiConversationsByConversationIdRes>(
    prepare(getV1AiConversationsByConversationIdRequestConfig, requestData),
    ...args,
  )
}

getV1AiConversationsByConversationId.requestConfig = getV1AiConversationsByConversationIdRequestConfig

/**
 * 接口 删除 AI 会话 的 **请求类型**
 *
 * @分类 AI 知识库
 * @请求头 `DELETE /v1/ai/conversations/{conversationId}`
 */
export interface IDeleteV1AiConversationsByConversationIdReq {
  conversationId: string
}

/**
 * 接口 删除 AI 会话 的 **返回类型**
 *
 * @分类 AI 知识库
 * @请求头 `DELETE /v1/ai/conversations/{conversationId}`
 */
export type IDeleteV1AiConversationsByConversationIdRes = any

/**
 * 接口 删除 AI 会话 的 **请求配置的类型**
 *
 * @分类 AI 知识库
 * @请求头 `DELETE /v1/ai/conversations/{conversationId}`
 */
type DeleteV1AiConversationsByConversationIdRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/conversations/{conversationId}',
    'data',
    'conversationId',
    string,
    false
  >
>

/**
 * 接口 删除 AI 会话 的 **请求配置**
 *
 * @分类 AI 知识库
 * @请求头 `DELETE /v1/ai/conversations/{conversationId}`
 */
const deleteV1AiConversationsByConversationIdRequestConfig: DeleteV1AiConversationsByConversationIdRequestConfig =
  /*#__PURE__*/ {
    mockUrl: mockUrl_0_0_0_6,
    devUrl: devUrl_0_0_0_6,
    prodUrl: prodUrl_0_0_0_6,
    path: '/v1/ai/conversations/{conversationId}',
    method: Method.DELETE,
    requestHeaders: {},
    requestBodyType: RequestBodyType.raw,
    responseBodyType: ResponseBodyType.raw,
    dataKey: dataKey_0_0_0_6,
    paramNames: ['conversationId'],
    queryNames: [],
    requestDataOptional: false,
    requestDataJsonSchema: {},
    responseDataJsonSchema: {},
    requestFunctionName: 'deleteV1AiConversationsByConversationId',
    queryStringArrayFormat: QueryStringArrayFormat.brackets,
    extraInfo: {},
  }

/**
 * 接口 删除 AI 会话 的 **请求函数**
 *
 * @分类 AI 知识库
 * @请求头 `DELETE /v1/ai/conversations/{conversationId}`
 */
export const deleteV1AiConversationsByConversationId = /*#__PURE__*/ (
  requestData: IDeleteV1AiConversationsByConversationIdReq,
  ...args: UserRequestRestArgs
) => {
  return request<IDeleteV1AiConversationsByConversationIdRes>(
    prepare(deleteV1AiConversationsByConversationIdRequestConfig, requestData),
    ...args,
  )
}

deleteV1AiConversationsByConversationId.requestConfig = deleteV1AiConversationsByConversationIdRequestConfig

/**
 * 接口 更新 AI 会话 的 **请求类型**
 *
 * @分类 AI 知识库
 * @请求头 `PATCH /v1/ai/conversations/{conversationId}`
 */
export interface IPatchV1AiConversationsByConversationIdReq {
  title?: string
  status?: 'active' | 'archived'
  conversationId: string
}

/**
 * 接口 更新 AI 会话 的 **返回类型**
 *
 * @分类 AI 知识库
 * @请求头 `PATCH /v1/ai/conversations/{conversationId}`
 */
export interface IPatchV1AiConversationsByConversationIdRes {
  conversationId?: string
  title?: string
  knowledgeBaseId?: string
  model?: string
  status?: 'active' | 'archived'
  createdAt?: string
  updatedAt?: string
}

/**
 * 接口 更新 AI 会话 的 **请求配置的类型**
 *
 * @分类 AI 知识库
 * @请求头 `PATCH /v1/ai/conversations/{conversationId}`
 */
type PatchV1AiConversationsByConversationIdRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/conversations/{conversationId}',
    'data',
    'conversationId',
    string,
    false
  >
>

/**
 * 接口 更新 AI 会话 的 **请求配置**
 *
 * @分类 AI 知识库
 * @请求头 `PATCH /v1/ai/conversations/{conversationId}`
 */
const patchV1AiConversationsByConversationIdRequestConfig: PatchV1AiConversationsByConversationIdRequestConfig =
  /*#__PURE__*/ {
    mockUrl: mockUrl_0_0_0_6,
    devUrl: devUrl_0_0_0_6,
    prodUrl: prodUrl_0_0_0_6,
    path: '/v1/ai/conversations/{conversationId}',
    method: Method.PATCH,
    requestHeaders: {},
    requestBodyType: RequestBodyType.json,
    responseBodyType: ResponseBodyType.json,
    dataKey: dataKey_0_0_0_6,
    paramNames: ['conversationId'],
    queryNames: [],
    requestDataOptional: false,
    requestDataJsonSchema: {},
    responseDataJsonSchema: {},
    requestFunctionName: 'patchV1AiConversationsByConversationId',
    queryStringArrayFormat: QueryStringArrayFormat.brackets,
    extraInfo: {},
  }

/**
 * 接口 更新 AI 会话 的 **请求函数**
 *
 * @分类 AI 知识库
 * @请求头 `PATCH /v1/ai/conversations/{conversationId}`
 */
export const patchV1AiConversationsByConversationId = /*#__PURE__*/ (
  requestData: IPatchV1AiConversationsByConversationIdReq,
  ...args: UserRequestRestArgs
) => {
  return request<IPatchV1AiConversationsByConversationIdRes>(
    prepare(patchV1AiConversationsByConversationIdRequestConfig, requestData),
    ...args,
  )
}

patchV1AiConversationsByConversationId.requestConfig = patchV1AiConversationsByConversationIdRequestConfig

/**
 * 接口 查询知识库详情 的 **请求类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases/{knowledgeBaseId}`
 */
export interface IGetV1AiKnowledgeBasesByKnowledgeBaseIdReq {
  knowledgeBaseId: string
}

/**
 * 接口 查询知识库详情 的 **返回类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases/{knowledgeBaseId}`
 */
export interface IGetV1AiKnowledgeBasesByKnowledgeBaseIdRes {
  id?: string
  name?: string
  description?: string
  status?: 'active' | 'disabled' | 'deleted'
  embeddingModel?: string
  embeddingDimension?: number
  chunkSize?: number
  chunkOverlap?: number
  documentCount?: number
  readyDocumentCount?: number
  createdAt?: string
  updatedAt?: string
}

/**
 * 接口 查询知识库详情 的 **请求配置的类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases/{knowledgeBaseId}`
 */
type GetV1AiKnowledgeBasesByKnowledgeBaseIdRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/knowledge-bases/{knowledgeBaseId}',
    'data',
    'knowledgeBaseId',
    string,
    false
  >
>

/**
 * 接口 查询知识库详情 的 **请求配置**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases/{knowledgeBaseId}`
 */
const getV1AiKnowledgeBasesByKnowledgeBaseIdRequestConfig: GetV1AiKnowledgeBasesByKnowledgeBaseIdRequestConfig =
  /*#__PURE__*/ {
    mockUrl: mockUrl_0_0_0_6,
    devUrl: devUrl_0_0_0_6,
    prodUrl: prodUrl_0_0_0_6,
    path: '/v1/ai/knowledge-bases/{knowledgeBaseId}',
    method: Method.GET,
    requestHeaders: {},
    requestBodyType: RequestBodyType.query,
    responseBodyType: ResponseBodyType.json,
    dataKey: dataKey_0_0_0_6,
    paramNames: ['knowledgeBaseId'],
    queryNames: [],
    requestDataOptional: false,
    requestDataJsonSchema: {},
    responseDataJsonSchema: {},
    requestFunctionName: 'getV1AiKnowledgeBasesByKnowledgeBaseId',
    queryStringArrayFormat: QueryStringArrayFormat.brackets,
    extraInfo: {},
  }

/**
 * 接口 查询知识库详情 的 **请求函数**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases/{knowledgeBaseId}`
 */
export const getV1AiKnowledgeBasesByKnowledgeBaseId = /*#__PURE__*/ (
  requestData: IGetV1AiKnowledgeBasesByKnowledgeBaseIdReq,
  ...args: UserRequestRestArgs
) => {
  return request<IGetV1AiKnowledgeBasesByKnowledgeBaseIdRes>(
    prepare(getV1AiKnowledgeBasesByKnowledgeBaseIdRequestConfig, requestData),
    ...args,
  )
}

getV1AiKnowledgeBasesByKnowledgeBaseId.requestConfig = getV1AiKnowledgeBasesByKnowledgeBaseIdRequestConfig

/**
 * 接口 查询文档详情 的 **请求类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}`
 */
export interface IGetV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdReq {
  knowledgeBaseId: string
  documentId: string
}

/**
 * 接口 查询文档详情 的 **返回类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}`
 */
export interface IGetV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdRes {
  id?: string
  knowledgeBaseId?: string
  fileName?: string
  mimeType?: string
  fileSize?: number
  status?: string
  activeGeneration?: number
  latestGeneration?: number
  chunkCount?: number
  pageCount?: number
  createdAt?: string
  updatedAt?: string
}

/**
 * 接口 查询文档详情 的 **请求配置的类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}`
 */
type GetV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}',
    'data',
    'knowledgeBaseId' | 'documentId',
    string,
    false
  >
>

/**
 * 接口 查询文档详情 的 **请求配置**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}`
 */
const getV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdRequestConfig: GetV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdRequestConfig =
  /*#__PURE__*/ {
    mockUrl: mockUrl_0_0_0_6,
    devUrl: devUrl_0_0_0_6,
    prodUrl: prodUrl_0_0_0_6,
    path: '/v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}',
    method: Method.GET,
    requestHeaders: {},
    requestBodyType: RequestBodyType.query,
    responseBodyType: ResponseBodyType.json,
    dataKey: dataKey_0_0_0_6,
    paramNames: ['knowledgeBaseId', 'documentId'],
    queryNames: [],
    requestDataOptional: false,
    requestDataJsonSchema: {},
    responseDataJsonSchema: {},
    requestFunctionName: 'getV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentId',
    queryStringArrayFormat: QueryStringArrayFormat.brackets,
    extraInfo: {},
  }

/**
 * 接口 查询文档详情 的 **请求函数**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}`
 */
export const getV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentId = /*#__PURE__*/ (
  requestData: IGetV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdReq,
  ...args: UserRequestRestArgs
) => {
  return request<IGetV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdRes>(
    prepare(getV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdRequestConfig, requestData),
    ...args,
  )
}

getV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentId.requestConfig =
  getV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdRequestConfig

/**
 * 接口 下载文档原文 的 **请求类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/content`
 */
export interface IGetV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdContentReq {
  knowledgeBaseId: string
  documentId: string
}

/**
 * 接口 下载文档原文 的 **返回类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/content`
 */
export type IGetV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdContentRes = string

/**
 * 接口 下载文档原文 的 **请求配置的类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/content`
 */
type GetV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdContentRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/content',
    'data',
    'knowledgeBaseId' | 'documentId',
    string,
    false
  >
>

/**
 * 接口 下载文档原文 的 **请求配置**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/content`
 */
const getV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdContentRequestConfig: GetV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdContentRequestConfig =
  /*#__PURE__*/ {
    mockUrl: mockUrl_0_0_0_6,
    devUrl: devUrl_0_0_0_6,
    prodUrl: prodUrl_0_0_0_6,
    path: '/v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/content',
    method: Method.GET,
    requestHeaders: {},
    requestBodyType: RequestBodyType.query,
    responseBodyType: ResponseBodyType.json,
    dataKey: dataKey_0_0_0_6,
    paramNames: ['knowledgeBaseId', 'documentId'],
    queryNames: [],
    requestDataOptional: false,
    requestDataJsonSchema: {},
    responseDataJsonSchema: {},
    requestFunctionName: 'getV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdContent',
    queryStringArrayFormat: QueryStringArrayFormat.brackets,
    extraInfo: {},
  }

/**
 * 接口 下载文档原文 的 **请求函数**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/content`
 */
export const getV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdContent = /*#__PURE__*/ (
  requestData: IGetV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdContentReq,
  ...args: UserRequestRestArgs
) => {
  return request<IGetV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdContentRes>(
    prepare(getV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdContentRequestConfig, requestData),
    ...args,
  )
}

getV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdContent.requestConfig =
  getV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsByDocumentIdContentRequestConfig

/**
 * 接口 查询 AI 会话消息历史 的 **请求类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/conversations/{conversationId}/messages`
 */
export interface IGetV1AiConversationsByConversationIdMessagesReq {
  beforeSeq?: string
  limit?: string
  conversationId: string
}

/**
 * 接口 查询 AI 会话消息历史 的 **返回类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/conversations/{conversationId}/messages`
 */
export interface IGetV1AiConversationsByConversationIdMessagesRes {
  items?: {
    id?: string
    seq?: number
    role?: 'user' | 'assistant'
    content?: string
    status?: string
    sources?: {
      citationIndex?: number
      documentId?: string
      documentName?: string
      pageFrom?: number
      pageTo?: number
      sectionTitle?: string
      excerpt?: string
      available?: boolean
    }[]
    createdAt?: string
  }[]
  nextCursor?: string
}

/**
 * 接口 查询 AI 会话消息历史 的 **请求配置的类型**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/conversations/{conversationId}/messages`
 */
type GetV1AiConversationsByConversationIdMessagesRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/ai/conversations/{conversationId}/messages',
    'data',
    'conversationId',
    'beforeSeq' | 'limit',
    false
  >
>

/**
 * 接口 查询 AI 会话消息历史 的 **请求配置**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/conversations/{conversationId}/messages`
 */
const getV1AiConversationsByConversationIdMessagesRequestConfig: GetV1AiConversationsByConversationIdMessagesRequestConfig =
  /*#__PURE__*/ {
    mockUrl: mockUrl_0_0_0_6,
    devUrl: devUrl_0_0_0_6,
    prodUrl: prodUrl_0_0_0_6,
    path: '/v1/ai/conversations/{conversationId}/messages',
    method: Method.GET,
    requestHeaders: {},
    requestBodyType: RequestBodyType.query,
    responseBodyType: ResponseBodyType.json,
    dataKey: dataKey_0_0_0_6,
    paramNames: ['conversationId'],
    queryNames: ['beforeSeq', 'limit'],
    requestDataOptional: false,
    requestDataJsonSchema: {},
    responseDataJsonSchema: {},
    requestFunctionName: 'getV1AiConversationsByConversationIdMessages',
    queryStringArrayFormat: QueryStringArrayFormat.brackets,
    extraInfo: {},
  }

/**
 * 接口 查询 AI 会话消息历史 的 **请求函数**
 *
 * @分类 AI 知识库
 * @请求头 `GET /v1/ai/conversations/{conversationId}/messages`
 */
export const getV1AiConversationsByConversationIdMessages = /*#__PURE__*/ (
  requestData: IGetV1AiConversationsByConversationIdMessagesReq,
  ...args: UserRequestRestArgs
) => {
  return request<IGetV1AiConversationsByConversationIdMessagesRes>(
    prepare(getV1AiConversationsByConversationIdMessagesRequestConfig, requestData),
    ...args,
  )
}

getV1AiConversationsByConversationIdMessages.requestConfig = getV1AiConversationsByConversationIdMessagesRequestConfig

const mockUrl_0_0_0_7 = 'http://127.0.0.1:64948/mock/0' as any
const devUrl_0_0_0_7 = '' as any
const prodUrl_0_0_0_7 = '' as any
const dataKey_0_0_0_7 = 'data' as any

/**
 * 接口 查询菜单列表 的 **请求类型**
 *
 * @分类 菜单管理
 * @请求头 `GET /v1/menus`
 */
export interface IGetV1MenusReq {
  /**
   * 菜单名称，模糊匹配
   */
  menuName?: string
  /**
   * 菜单类型：M 目录，C 页面，B 按钮；兼容小写
   */
  menuType?: string
}

/**
 * 接口 查询菜单列表 的 **返回类型**
 *
 * @分类 菜单管理
 * @请求头 `GET /v1/menus`
 */
export interface IGetV1MenusRes {
  /**
   * 菜单列表
   */
  list?: {
    /**
     * 菜单数据库 ID，当前用户菜单接口不返回该字段
     */
    id?: number
    /**
     * 菜单业务 ID
     */
    menuId?: string
    /**
     * 父菜单业务 ID
     */
    parentId?: string
    /**
     * 菜单名称
     */
    name?: string
    /**
     * 前端路由路径
     */
    path?: string
    /**
     * 前端页面组件路径
     */
    component?: string
    /**
     * 菜单图标
     */
    icon?: string
    /**
     * 排序值
     */
    sort?: number
    /**
     * 是否隐藏：0 显示，1 隐藏
     */
    hidden?: number
    /**
     * 是否全屏：0 否，1 是
     */
    isFull?: number
    /**
     * 类型：M 目录，C 页面，B 按钮
     */
    type?: 'M' | 'C' | 'B'
    /**
     * 按钮权限标识
     */
    buttonPermission?: string
  }[]
}

/**
 * 接口 查询菜单列表 的 **请求配置的类型**
 *
 * @分类 菜单管理
 * @请求头 `GET /v1/menus`
 */
type GetV1MenusRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/menus', 'data', string, 'menuName' | 'menuType', false>
>

/**
 * 接口 查询菜单列表 的 **请求配置**
 *
 * @分类 菜单管理
 * @请求头 `GET /v1/menus`
 */
const getV1MenusRequestConfig: GetV1MenusRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_7,
  devUrl: devUrl_0_0_0_7,
  prodUrl: prodUrl_0_0_0_7,
  path: '/v1/menus',
  method: Method.GET,
  requestHeaders: {},
  requestBodyType: RequestBodyType.query,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_7,
  paramNames: [],
  queryNames: ['menuName', 'menuType'],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'getV1Menus',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 查询菜单列表 的 **请求函数**
 *
 * @分类 菜单管理
 * @请求头 `GET /v1/menus`
 */
export const getV1Menus = /*#__PURE__*/ (requestData: IGetV1MenusReq, ...args: UserRequestRestArgs) => {
  return request<IGetV1MenusRes>(prepare(getV1MenusRequestConfig, requestData), ...args)
}

getV1Menus.requestConfig = getV1MenusRequestConfig

/**
 * 接口 创建菜单 的 **请求类型**
 *
 * @分类 菜单管理
 * @请求头 `POST /v1/menus`
 */
export interface IPostV1MenusReq {
  /**
   * 菜单名称，最多 50 字符
   */
  name: string
  /**
   * 类型：M 目录，C 页面，B 按钮；兼容小写
   */
  type: 'M' | 'C' | 'B' | 'm' | 'c' | 'b'
  /**
   * 父菜单业务 ID；不传表示根菜单
   */
  parentId?: string
  /**
   * 前端路由路径，最多 100 字符，默认空字符串
   */
  path?: string
  /**
   * 前端页面组件路径，最多 100 字符
   */
  component?: string
  /**
   * 菜单图标，最多 50 字符
   */
  icon?: string
  /**
   * 排序值，非负整数，默认 0
   */
  sort?: number
  /**
   * 是否隐藏：0 显示，1 隐藏
   */
  hidden?: number
  /**
   * 是否全屏：0 否，1 是
   */
  isFull?: number
  /**
   * 按钮权限标识，最多 100 字符
   */
  buttonPermission?: string
}

/**
 * 接口 创建菜单 的 **返回类型**
 *
 * @分类 菜单管理
 * @请求头 `POST /v1/menus`
 */
export interface IPostV1MenusRes {
  /**
   * 菜单数据库 ID，当前用户菜单接口不返回该字段
   */
  id?: number
  /**
   * 菜单业务 ID
   */
  menuId?: string
  /**
   * 父菜单业务 ID
   */
  parentId?: string
  /**
   * 菜单名称
   */
  name?: string
  /**
   * 前端路由路径
   */
  path?: string
  /**
   * 前端页面组件路径
   */
  component?: string
  /**
   * 菜单图标
   */
  icon?: string
  /**
   * 排序值
   */
  sort?: number
  /**
   * 是否隐藏：0 显示，1 隐藏
   */
  hidden?: number
  /**
   * 是否全屏：0 否，1 是
   */
  isFull?: number
  /**
   * 类型：M 目录，C 页面，B 按钮
   */
  type?: 'M' | 'C' | 'B'
  /**
   * 按钮权限标识
   */
  buttonPermission?: string
}

/**
 * 接口 创建菜单 的 **请求配置的类型**
 *
 * @分类 菜单管理
 * @请求头 `POST /v1/menus`
 */
type PostV1MenusRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/menus', 'data', string, string, false>
>

/**
 * 接口 创建菜单 的 **请求配置**
 *
 * @分类 菜单管理
 * @请求头 `POST /v1/menus`
 */
const postV1MenusRequestConfig: PostV1MenusRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_7,
  devUrl: devUrl_0_0_0_7,
  prodUrl: prodUrl_0_0_0_7,
  path: '/v1/menus',
  method: Method.POST,
  requestHeaders: {},
  requestBodyType: RequestBodyType.json,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_7,
  paramNames: [],
  queryNames: [],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'postV1Menus',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 创建菜单 的 **请求函数**
 *
 * @分类 菜单管理
 * @请求头 `POST /v1/menus`
 */
export const postV1Menus = /*#__PURE__*/ (requestData: IPostV1MenusReq, ...args: UserRequestRestArgs) => {
  return request<IPostV1MenusRes>(prepare(postV1MenusRequestConfig, requestData), ...args)
}

postV1Menus.requestConfig = postV1MenusRequestConfig

/**
 * 接口 删除菜单 的 **请求类型**
 *
 * @分类 菜单管理
 * @请求头 `DELETE /v1/menus/{id}`
 */
export interface IDeleteV1MenusByIdReq {
  /**
   * 菜单数据库 ID，正整数
   */
  id: string
}

/**
 * 接口 删除菜单 的 **返回类型**
 *
 * @分类 菜单管理
 * @请求头 `DELETE /v1/menus/{id}`
 */
export type IDeleteV1MenusByIdRes = any

/**
 * 接口 删除菜单 的 **请求配置的类型**
 *
 * @分类 菜单管理
 * @请求头 `DELETE /v1/menus/{id}`
 */
type DeleteV1MenusByIdRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/menus/{id}', 'data', 'id', string, false>
>

/**
 * 接口 删除菜单 的 **请求配置**
 *
 * @分类 菜单管理
 * @请求头 `DELETE /v1/menus/{id}`
 */
const deleteV1MenusByIdRequestConfig: DeleteV1MenusByIdRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_7,
  devUrl: devUrl_0_0_0_7,
  prodUrl: prodUrl_0_0_0_7,
  path: '/v1/menus/{id}',
  method: Method.DELETE,
  requestHeaders: {},
  requestBodyType: RequestBodyType.raw,
  responseBodyType: ResponseBodyType.raw,
  dataKey: dataKey_0_0_0_7,
  paramNames: ['id'],
  queryNames: [],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'deleteV1MenusById',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 删除菜单 的 **请求函数**
 *
 * @分类 菜单管理
 * @请求头 `DELETE /v1/menus/{id}`
 */
export const deleteV1MenusById = /*#__PURE__*/ (requestData: IDeleteV1MenusByIdReq, ...args: UserRequestRestArgs) => {
  return request<IDeleteV1MenusByIdRes>(prepare(deleteV1MenusByIdRequestConfig, requestData), ...args)
}

deleteV1MenusById.requestConfig = deleteV1MenusByIdRequestConfig

/**
 * 接口 修改菜单 的 **请求类型**
 *
 * @分类 菜单管理
 * @请求头 `PATCH /v1/menus/{id}`
 */
export interface IPatchV1MenusByIdReq {
  /**
   * 菜单业务 ID，1～50 字符
   */
  menuId?: string
  /**
   * 父菜单业务 ID；传 null 表示根菜单
   */
  parentId?: string
  /**
   * 菜单名称，1～50 字符
   */
  name?: string
  /**
   * 前端路由路径，最多 100 字符，可传 null
   */
  path?: string
  /**
   * 前端页面组件路径，最多 100 字符，可传 null
   */
  component?: string
  /**
   * 菜单图标，最多 50 字符，可传 null
   */
  icon?: string
  /**
   * 排序值，非负整数
   */
  sort?: number
  /**
   * 是否隐藏：0 显示，1 隐藏
   */
  hidden?: number
  /**
   * 是否全屏：0 否，1 是
   */
  isFull?: number
  /**
   * 类型：M 目录，C 页面，B 按钮；兼容小写
   */
  type?: 'M' | 'C' | 'B' | 'm' | 'c' | 'b'
  /**
   * 按钮权限标识，最多 100 字符，可传 null
   */
  buttonPermission?: string
  /**
   * 菜单数据库 ID，正整数
   */
  id: string
}

/**
 * 接口 修改菜单 的 **返回类型**
 *
 * @分类 菜单管理
 * @请求头 `PATCH /v1/menus/{id}`
 */
export interface IPatchV1MenusByIdRes {
  /**
   * 菜单数据库 ID，当前用户菜单接口不返回该字段
   */
  id?: number
  /**
   * 菜单业务 ID
   */
  menuId?: string
  /**
   * 父菜单业务 ID
   */
  parentId?: string
  /**
   * 菜单名称
   */
  name?: string
  /**
   * 前端路由路径
   */
  path?: string
  /**
   * 前端页面组件路径
   */
  component?: string
  /**
   * 菜单图标
   */
  icon?: string
  /**
   * 排序值
   */
  sort?: number
  /**
   * 是否隐藏：0 显示，1 隐藏
   */
  hidden?: number
  /**
   * 是否全屏：0 否，1 是
   */
  isFull?: number
  /**
   * 类型：M 目录，C 页面，B 按钮
   */
  type?: 'M' | 'C' | 'B'
  /**
   * 按钮权限标识
   */
  buttonPermission?: string
}

/**
 * 接口 修改菜单 的 **请求配置的类型**
 *
 * @分类 菜单管理
 * @请求头 `PATCH /v1/menus/{id}`
 */
type PatchV1MenusByIdRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/menus/{id}', 'data', 'id', string, false>
>

/**
 * 接口 修改菜单 的 **请求配置**
 *
 * @分类 菜单管理
 * @请求头 `PATCH /v1/menus/{id}`
 */
const patchV1MenusByIdRequestConfig: PatchV1MenusByIdRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_7,
  devUrl: devUrl_0_0_0_7,
  prodUrl: prodUrl_0_0_0_7,
  path: '/v1/menus/{id}',
  method: Method.PATCH,
  requestHeaders: {},
  requestBodyType: RequestBodyType.json,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_7,
  paramNames: ['id'],
  queryNames: [],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'patchV1MenusById',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 修改菜单 的 **请求函数**
 *
 * @分类 菜单管理
 * @请求头 `PATCH /v1/menus/{id}`
 */
export const patchV1MenusById = /*#__PURE__*/ (requestData: IPatchV1MenusByIdReq, ...args: UserRequestRestArgs) => {
  return request<IPatchV1MenusByIdRes>(prepare(patchV1MenusByIdRequestConfig, requestData), ...args)
}

patchV1MenusById.requestConfig = patchV1MenusByIdRequestConfig

/**
 * 接口 查询当前用户菜单 的 **请求类型**
 *
 * @分类 菜单管理
 * @请求头 `GET /v1/me/menus`
 */
export interface IGetV1MeMenusReq {}

/**
 * 接口 查询当前用户菜单 的 **返回类型**
 *
 * @分类 菜单管理
 * @请求头 `GET /v1/me/menus`
 */
export interface IGetV1MeMenusRes {
  /**
   * 菜单列表
   */
  list?: {
    /**
     * 菜单数据库 ID，当前用户菜单接口不返回该字段
     */
    id?: number
    /**
     * 菜单业务 ID
     */
    menuId?: string
    /**
     * 父菜单业务 ID
     */
    parentId?: string
    /**
     * 菜单名称
     */
    name?: string
    /**
     * 前端路由路径
     */
    path?: string
    /**
     * 前端页面组件路径
     */
    component?: string
    /**
     * 菜单图标
     */
    icon?: string
    /**
     * 排序值
     */
    sort?: number
    /**
     * 是否隐藏：0 显示，1 隐藏
     */
    hidden?: number
    /**
     * 是否全屏：0 否，1 是
     */
    isFull?: number
    /**
     * 类型：M 目录，C 页面，B 按钮
     */
    type?: 'M' | 'C' | 'B'
    /**
     * 按钮权限标识
     */
    buttonPermission?: string
  }[]
}

/**
 * 接口 查询当前用户菜单 的 **请求配置的类型**
 *
 * @分类 菜单管理
 * @请求头 `GET /v1/me/menus`
 */
type GetV1MeMenusRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/me/menus', 'data', string, string, true>
>

/**
 * 接口 查询当前用户菜单 的 **请求配置**
 *
 * @分类 菜单管理
 * @请求头 `GET /v1/me/menus`
 */
const getV1MeMenusRequestConfig: GetV1MeMenusRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_7,
  devUrl: devUrl_0_0_0_7,
  prodUrl: prodUrl_0_0_0_7,
  path: '/v1/me/menus',
  method: Method.GET,
  requestHeaders: {},
  requestBodyType: RequestBodyType.query,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_7,
  paramNames: [],
  queryNames: [],
  requestDataOptional: true,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'getV1MeMenus',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 查询当前用户菜单 的 **请求函数**
 *
 * @分类 菜单管理
 * @请求头 `GET /v1/me/menus`
 */
export const getV1MeMenus = /*#__PURE__*/ (requestData?: IGetV1MeMenusReq, ...args: UserRequestRestArgs) => {
  return request<IGetV1MeMenusRes>(prepare(getV1MeMenusRequestConfig, requestData), ...args)
}

getV1MeMenus.requestConfig = getV1MeMenusRequestConfig

const mockUrl_0_0_0_8 = 'http://127.0.0.1:64948/mock/0' as any
const devUrl_0_0_0_8 = '' as any
const prodUrl_0_0_0_8 = '' as any
const dataKey_0_0_0_8 = 'data' as any

/**
 * 接口 退出登录 的 **请求类型**
 *
 * @分类 认证管理
 * @请求头 `POST /v1/auth/logout`
 */
export interface IPostV1AuthLogoutReq {}

/**
 * 接口 退出登录 的 **返回类型**
 *
 * @分类 认证管理
 * @请求头 `POST /v1/auth/logout`
 */
export interface IPostV1AuthLogoutRes {
  [k: string]: unknown
}

/**
 * 接口 退出登录 的 **请求配置的类型**
 *
 * @分类 认证管理
 * @请求头 `POST /v1/auth/logout`
 */
type PostV1AuthLogoutRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/auth/logout', 'data', string, string, true>
>

/**
 * 接口 退出登录 的 **请求配置**
 *
 * @分类 认证管理
 * @请求头 `POST /v1/auth/logout`
 */
const postV1AuthLogoutRequestConfig: PostV1AuthLogoutRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_8,
  devUrl: devUrl_0_0_0_8,
  prodUrl: prodUrl_0_0_0_8,
  path: '/v1/auth/logout',
  method: Method.POST,
  requestHeaders: {},
  requestBodyType: RequestBodyType.raw,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_8,
  paramNames: [],
  queryNames: [],
  requestDataOptional: true,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'postV1AuthLogout',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 退出登录 的 **请求函数**
 *
 * @分类 认证管理
 * @请求头 `POST /v1/auth/logout`
 */
export const postV1AuthLogout = /*#__PURE__*/ (requestData?: IPostV1AuthLogoutReq, ...args: UserRequestRestArgs) => {
  return request<IPostV1AuthLogoutRes>(prepare(postV1AuthLogoutRequestConfig, requestData), ...args)
}

postV1AuthLogout.requestConfig = postV1AuthLogoutRequestConfig

/**
 * 接口 账号登录 的 **请求类型**
 *
 * @分类 认证管理
 * @请求头 `POST /v1/auth/login`
 */
export interface IPostV1AuthLoginReq {
  /**
   * 登录用户名
   */
  username: string
  /**
   * 登录密码
   */
  password: string
}

/**
 * 接口 账号登录 的 **返回类型**
 *
 * @分类 认证管理
 * @请求头 `POST /v1/auth/login`
 */
export interface IPostV1AuthLoginRes {
  /**
   * JWT 访问令牌，后续请求使用 Authorization: Bearer <token>
   */
  token?: string
  /**
   * 是否首次登录
   */
  initialPasswordFlag?: boolean
}

/**
 * 接口 账号登录 的 **请求配置的类型**
 *
 * @分类 认证管理
 * @请求头 `POST /v1/auth/login`
 */
type PostV1AuthLoginRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/auth/login', 'data', string, string, false>
>

/**
 * 接口 账号登录 的 **请求配置**
 *
 * @分类 认证管理
 * @请求头 `POST /v1/auth/login`
 */
const postV1AuthLoginRequestConfig: PostV1AuthLoginRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_8,
  devUrl: devUrl_0_0_0_8,
  prodUrl: prodUrl_0_0_0_8,
  path: '/v1/auth/login',
  method: Method.POST,
  requestHeaders: {},
  requestBodyType: RequestBodyType.json,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_8,
  paramNames: [],
  queryNames: [],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'postV1AuthLogin',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 账号登录 的 **请求函数**
 *
 * @分类 认证管理
 * @请求头 `POST /v1/auth/login`
 */
export const postV1AuthLogin = /*#__PURE__*/ (requestData: IPostV1AuthLoginReq, ...args: UserRequestRestArgs) => {
  return request<IPostV1AuthLoginRes>(prepare(postV1AuthLoginRequestConfig, requestData), ...args)
}

postV1AuthLogin.requestConfig = postV1AuthLoginRequestConfig

const mockUrl_0_0_0_9 = 'http://127.0.0.1:64948/mock/0' as any
const devUrl_0_0_0_9 = '' as any
const prodUrl_0_0_0_9 = '' as any
const dataKey_0_0_0_9 = 'data' as any

/**
 * 接口 分页查询角色 的 **请求类型**
 *
 * @分类 角色管理
 * @请求头 `GET /v1/roles`
 */
export interface IGetV1RolesReq {
  /**
   * 页码，从 1 开始
   */
  page?: string
  /**
   * 每页条数，范围 1～100
   */
  pageSize?: string
  /**
   * 角色名称或代码，模糊匹配
   */
  name?: string
  /**
   * 角色状态：0 禁用，1 启用
   */
  status?: string
}

/**
 * 接口 分页查询角色 的 **返回类型**
 *
 * @分类 角色管理
 * @请求头 `GET /v1/roles`
 */
export interface IGetV1RolesRes {
  /**
   * 当前页的角色列表
   */
  list?: {
    /**
     * 角色数据库 ID
     */
    id?: number
    /**
     * 唯一角色代码
     */
    code?: string
    /**
     * 角色名称
     */
    name?: string
    /**
     * 角色描述
     */
    description?: string
    /**
     * 角色状态：0 禁用，1 启用
     */
    status?: number
    /**
     * 已授权菜单的业务 ID 列表
     */
    menuIds?: string[]
    /**
     * 创建时间（UTC）
     */
    createdAt?: string
    /**
     * 更新时间（UTC）
     */
    updatedAt?: string
  }[]
  /**
   * 角色分页信息
   */
  pagination?: {
    /**
     * 当前页码，从 1 开始
     */
    page?: number
    /**
     * 每页条数
     */
    pageSize?: number
    /**
     * 符合条件的角色总数
     */
    total?: number
    /**
     * 总页数
     */
    totalPages?: number
  }
}

/**
 * 接口 分页查询角色 的 **请求配置的类型**
 *
 * @分类 角色管理
 * @请求头 `GET /v1/roles`
 */
type GetV1RolesRequestConfig = Readonly<
  RequestConfig<
    'http://127.0.0.1:64948/mock/0',
    '',
    '',
    '/v1/roles',
    'data',
    string,
    'page' | 'pageSize' | 'name' | 'status',
    false
  >
>

/**
 * 接口 分页查询角色 的 **请求配置**
 *
 * @分类 角色管理
 * @请求头 `GET /v1/roles`
 */
const getV1RolesRequestConfig: GetV1RolesRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_9,
  devUrl: devUrl_0_0_0_9,
  prodUrl: prodUrl_0_0_0_9,
  path: '/v1/roles',
  method: Method.GET,
  requestHeaders: {},
  requestBodyType: RequestBodyType.query,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_9,
  paramNames: [],
  queryNames: ['page', 'pageSize', 'name', 'status'],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'getV1Roles',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 分页查询角色 的 **请求函数**
 *
 * @分类 角色管理
 * @请求头 `GET /v1/roles`
 */
export const getV1Roles = /*#__PURE__*/ (requestData: IGetV1RolesReq, ...args: UserRequestRestArgs) => {
  return request<IGetV1RolesRes>(prepare(getV1RolesRequestConfig, requestData), ...args)
}

getV1Roles.requestConfig = getV1RolesRequestConfig

/**
 * 接口 创建角色 的 **请求类型**
 *
 * @分类 角色管理
 * @请求头 `POST /v1/roles`
 */
export interface IPostV1RolesReq {
  /**
   * 唯一角色代码，仅允许字母、数字、下划线及连字符
   */
  code: string
  /**
   * 角色名称
   */
  name: string
  /**
   * 角色描述，默认空字符串
   */
  description?: string
  /**
   * 角色状态：0 禁用，1 启用
   */
  status?: number
  /**
   * 授权菜单的业务 ID 列表，默认为空列表
   */
  menuIds?: string[]
}

/**
 * 接口 创建角色 的 **返回类型**
 *
 * @分类 角色管理
 * @请求头 `POST /v1/roles`
 */
export interface IPostV1RolesRes {
  /**
   * 角色数据库 ID
   */
  id?: number
  /**
   * 唯一角色代码
   */
  code?: string
  /**
   * 角色名称
   */
  name?: string
  /**
   * 角色描述
   */
  description?: string
  /**
   * 角色状态：0 禁用，1 启用
   */
  status?: number
  /**
   * 已授权菜单的业务 ID 列表
   */
  menuIds?: string[]
  /**
   * 创建时间（UTC）
   */
  createdAt?: string
  /**
   * 更新时间（UTC）
   */
  updatedAt?: string
}

/**
 * 接口 创建角色 的 **请求配置的类型**
 *
 * @分类 角色管理
 * @请求头 `POST /v1/roles`
 */
type PostV1RolesRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/roles', 'data', string, string, false>
>

/**
 * 接口 创建角色 的 **请求配置**
 *
 * @分类 角色管理
 * @请求头 `POST /v1/roles`
 */
const postV1RolesRequestConfig: PostV1RolesRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_9,
  devUrl: devUrl_0_0_0_9,
  prodUrl: prodUrl_0_0_0_9,
  path: '/v1/roles',
  method: Method.POST,
  requestHeaders: {},
  requestBodyType: RequestBodyType.json,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_9,
  paramNames: [],
  queryNames: [],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'postV1Roles',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 创建角色 的 **请求函数**
 *
 * @分类 角色管理
 * @请求头 `POST /v1/roles`
 */
export const postV1Roles = /*#__PURE__*/ (requestData: IPostV1RolesReq, ...args: UserRequestRestArgs) => {
  return request<IPostV1RolesRes>(prepare(postV1RolesRequestConfig, requestData), ...args)
}

postV1Roles.requestConfig = postV1RolesRequestConfig

/**
 * 接口 查询角色详情 的 **请求类型**
 *
 * @分类 角色管理
 * @请求头 `GET /v1/roles/{id}`
 */
export interface IGetV1RolesByIdReq {
  /**
   * 角色数据库 ID，正整数
   */
  id: string
}

/**
 * 接口 查询角色详情 的 **返回类型**
 *
 * @分类 角色管理
 * @请求头 `GET /v1/roles/{id}`
 */
export interface IGetV1RolesByIdRes {
  /**
   * 角色数据库 ID
   */
  id?: number
  /**
   * 唯一角色代码
   */
  code?: string
  /**
   * 角色名称
   */
  name?: string
  /**
   * 角色描述
   */
  description?: string
  /**
   * 角色状态：0 禁用，1 启用
   */
  status?: number
  /**
   * 已授权菜单的业务 ID 列表
   */
  menuIds?: string[]
  /**
   * 创建时间（UTC）
   */
  createdAt?: string
  /**
   * 更新时间（UTC）
   */
  updatedAt?: string
}

/**
 * 接口 查询角色详情 的 **请求配置的类型**
 *
 * @分类 角色管理
 * @请求头 `GET /v1/roles/{id}`
 */
type GetV1RolesByIdRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/roles/{id}', 'data', 'id', string, false>
>

/**
 * 接口 查询角色详情 的 **请求配置**
 *
 * @分类 角色管理
 * @请求头 `GET /v1/roles/{id}`
 */
const getV1RolesByIdRequestConfig: GetV1RolesByIdRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_9,
  devUrl: devUrl_0_0_0_9,
  prodUrl: prodUrl_0_0_0_9,
  path: '/v1/roles/{id}',
  method: Method.GET,
  requestHeaders: {},
  requestBodyType: RequestBodyType.query,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_9,
  paramNames: ['id'],
  queryNames: [],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'getV1RolesById',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 查询角色详情 的 **请求函数**
 *
 * @分类 角色管理
 * @请求头 `GET /v1/roles/{id}`
 */
export const getV1RolesById = /*#__PURE__*/ (requestData: IGetV1RolesByIdReq, ...args: UserRequestRestArgs) => {
  return request<IGetV1RolesByIdRes>(prepare(getV1RolesByIdRequestConfig, requestData), ...args)
}

getV1RolesById.requestConfig = getV1RolesByIdRequestConfig

/**
 * 接口 删除角色 的 **请求类型**
 *
 * @分类 角色管理
 * @请求头 `DELETE /v1/roles/{id}`
 */
export interface IDeleteV1RolesByIdReq {
  /**
   * 角色数据库 ID，正整数
   */
  id: string
}

/**
 * 接口 删除角色 的 **返回类型**
 *
 * @分类 角色管理
 * @请求头 `DELETE /v1/roles/{id}`
 */
export type IDeleteV1RolesByIdRes = any

/**
 * 接口 删除角色 的 **请求配置的类型**
 *
 * @分类 角色管理
 * @请求头 `DELETE /v1/roles/{id}`
 */
type DeleteV1RolesByIdRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/roles/{id}', 'data', 'id', string, false>
>

/**
 * 接口 删除角色 的 **请求配置**
 *
 * @分类 角色管理
 * @请求头 `DELETE /v1/roles/{id}`
 */
const deleteV1RolesByIdRequestConfig: DeleteV1RolesByIdRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_9,
  devUrl: devUrl_0_0_0_9,
  prodUrl: prodUrl_0_0_0_9,
  path: '/v1/roles/{id}',
  method: Method.DELETE,
  requestHeaders: {},
  requestBodyType: RequestBodyType.raw,
  responseBodyType: ResponseBodyType.raw,
  dataKey: dataKey_0_0_0_9,
  paramNames: ['id'],
  queryNames: [],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'deleteV1RolesById',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 删除角色 的 **请求函数**
 *
 * @分类 角色管理
 * @请求头 `DELETE /v1/roles/{id}`
 */
export const deleteV1RolesById = /*#__PURE__*/ (requestData: IDeleteV1RolesByIdReq, ...args: UserRequestRestArgs) => {
  return request<IDeleteV1RolesByIdRes>(prepare(deleteV1RolesByIdRequestConfig, requestData), ...args)
}

deleteV1RolesById.requestConfig = deleteV1RolesByIdRequestConfig

/**
 * 接口 修改角色 的 **请求类型**
 *
 * @分类 角色管理
 * @请求头 `PATCH /v1/roles/{id}`
 */
export interface IPatchV1RolesByIdReq {
  /**
   * 唯一角色代码，仅允许字母、数字、下划线及连字符
   */
  code?: string
  /**
   * 角色名称
   */
  name?: string
  /**
   * 角色描述
   */
  description?: string
  /**
   * 角色状态：0 禁用，1 启用
   */
  status?: number
  /**
   * 替换角色菜单权限的业务 ID 列表，空列表表示清空
   */
  menuIds?: string[]
  /**
   * 角色数据库 ID，正整数
   */
  id: string
}

/**
 * 接口 修改角色 的 **返回类型**
 *
 * @分类 角色管理
 * @请求头 `PATCH /v1/roles/{id}`
 */
export interface IPatchV1RolesByIdRes {
  /**
   * 角色数据库 ID
   */
  id?: number
  /**
   * 唯一角色代码
   */
  code?: string
  /**
   * 角色名称
   */
  name?: string
  /**
   * 角色描述
   */
  description?: string
  /**
   * 角色状态：0 禁用，1 启用
   */
  status?: number
  /**
   * 已授权菜单的业务 ID 列表
   */
  menuIds?: string[]
  /**
   * 创建时间（UTC）
   */
  createdAt?: string
  /**
   * 更新时间（UTC）
   */
  updatedAt?: string
}

/**
 * 接口 修改角色 的 **请求配置的类型**
 *
 * @分类 角色管理
 * @请求头 `PATCH /v1/roles/{id}`
 */
type PatchV1RolesByIdRequestConfig = Readonly<
  RequestConfig<'http://127.0.0.1:64948/mock/0', '', '', '/v1/roles/{id}', 'data', 'id', string, false>
>

/**
 * 接口 修改角色 的 **请求配置**
 *
 * @分类 角色管理
 * @请求头 `PATCH /v1/roles/{id}`
 */
const patchV1RolesByIdRequestConfig: PatchV1RolesByIdRequestConfig = /*#__PURE__*/ {
  mockUrl: mockUrl_0_0_0_9,
  devUrl: devUrl_0_0_0_9,
  prodUrl: prodUrl_0_0_0_9,
  path: '/v1/roles/{id}',
  method: Method.PATCH,
  requestHeaders: {},
  requestBodyType: RequestBodyType.json,
  responseBodyType: ResponseBodyType.json,
  dataKey: dataKey_0_0_0_9,
  paramNames: ['id'],
  queryNames: [],
  requestDataOptional: false,
  requestDataJsonSchema: {},
  responseDataJsonSchema: {},
  requestFunctionName: 'patchV1RolesById',
  queryStringArrayFormat: QueryStringArrayFormat.brackets,
  extraInfo: {},
}

/**
 * 接口 修改角色 的 **请求函数**
 *
 * @分类 角色管理
 * @请求头 `PATCH /v1/roles/{id}`
 */
export const patchV1RolesById = /*#__PURE__*/ (requestData: IPatchV1RolesByIdReq, ...args: UserRequestRestArgs) => {
  return request<IPatchV1RolesByIdRes>(prepare(patchV1RolesByIdRequestConfig, requestData), ...args)
}

patchV1RolesById.requestConfig = patchV1RolesByIdRequestConfig

/* prettier-ignore-end */
