<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
 getV1AiEvaluationDatasets, postV1AiEvaluationDatasets, putV1AiEvaluationDatasetsByDatasetId,
 deleteV1AiEvaluationDatasetsByDatasetId, getV1AiEvaluationDatasetsByDatasetIdCases,
 postV1AiEvaluationDatasetsByDatasetIdCases, putV1AiEvaluationDatasetsByDatasetIdCasesByCaseId,
 deleteV1AiEvaluationDatasetsByDatasetIdCasesByCaseId, getV1AiEvaluationDatasetsByDatasetIdRuns,
 postV1AiEvaluationDatasetsByDatasetIdRuns, getV1AiEvaluationDatasetsByDatasetIdRunsByRunId,
 getV1AiKnowledgeBases, getV1AiKnowledgeBasesByKnowledgeBaseIdDocuments,
 type IGetV1AiEvaluationDatasetsRes, type IPostV1AiEvaluationDatasetsReq,
 type IPostV1AiEvaluationDatasetsByDatasetIdCasesReq, type IGetV1AiEvaluationDatasetsByDatasetIdCasesRes,
 type IGetV1AiEvaluationDatasetsByDatasetIdRunsRes, type IGetV1AiEvaluationDatasetsByDatasetIdRunsByRunIdRes,
 type IGetV1AiKnowledgeBasesRes, type IGetV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsRes
} from '@/api';

type Dataset = NonNullable<IGetV1AiEvaluationDatasetsRes['items']>[number];
type DatasetInput = IPostV1AiEvaluationDatasetsReq;
type CaseInput = Omit<IPostV1AiEvaluationDatasetsByDatasetIdCasesReq, 'datasetId'>;
type EvaluationCase = NonNullable<IGetV1AiEvaluationDatasetsByDatasetIdCasesRes['items']>[number];
type EvaluationRun = NonNullable<IGetV1AiEvaluationDatasetsByDatasetIdRunsRes['items']>[number];
type EvaluationReport = IGetV1AiEvaluationDatasetsByDatasetIdRunsByRunIdRes;
type KnowledgeBase = NonNullable<IGetV1AiKnowledgeBasesRes['items']>[number];
type Document = NonNullable<IGetV1AiKnowledgeBasesByKnowledgeBaseIdDocumentsRes['items']>[number];

const datasets = ref<Dataset[]>([]), datasetTotal = ref(0), datasetPage = ref(1);
const selected = ref<Dataset>();
const cases = ref<EvaluationCase[]>([]), caseTotal = ref(0), casePage = ref(1);
const runs = ref<EvaluationRun[]>([]), runTotal = ref(0), runPage = ref(1);
const report = ref<EvaluationReport>(), running = ref(false), topK = ref(5);
const generateAnswers = ref(false);
const knowledgeBases = ref<KnowledgeBase[]>([]), kbTotal = ref(0), kbPage = ref(0);
const documents = ref<Document[]>([]), docTotal = ref(0), docPage = ref(0);
const datasetDialog = ref(false), caseDialog = ref(false), saving = ref(false);
const editingDataset = ref<string>(), editingCase = ref<string>();
const datasetForm = reactive<DatasetInput>({ name: '', knowledgeBaseId: '', description: '' });
const emptyCase = (): CaseInput => ({ question: '', category: '', difficulty: 'easy', answerable: true,
 expectedEvidence: [{ documentId: '', text: '' }], expectedAnswer: '', notes: '' });
const caseForm = reactive<CaseInput>(emptyCase());
const percent = (value: number | null | undefined) => value == null ? '未评分' : `${(value * 100).toFixed(1)}%`;
async function loadDatasets() { const page = await getV1AiEvaluationDatasets({ page: String(datasetPage.value), pageSize: '20' }); datasets.value = page.items; datasetTotal.value = page.total; }
async function loadCases() { if (!selected.value) return; const page = await getV1AiEvaluationDatasetsByDatasetIdCases({ datasetId: selected.value.id, page: String(casePage.value), pageSize: '20' }); cases.value = page.items; caseTotal.value = page.total; }
async function loadRuns() { if (!selected.value) return; const page = await getV1AiEvaluationDatasetsByDatasetIdRuns({ datasetId: selected.value.id, page: String(runPage.value), pageSize: '10' }); runs.value = page.items; runTotal.value = page.total; }
async function moreKnowledgeBases() { const page = await getV1AiKnowledgeBases({ page: String(kbPage.value + 1), pageSize: '100' }); kbPage.value++; knowledgeBases.value.push(...page.items); kbTotal.value = page.total; }
async function moreDocuments() { if (!selected.value) return; const page = await getV1AiKnowledgeBasesByKnowledgeBaseIdDocuments({ knowledgeBaseId: selected.value.knowledgeBaseId, page: String(docPage.value + 1), pageSize: '100' }); docPage.value++; documents.value.push(...page.items); docTotal.value = page.total; }
async function selectDataset(row: Dataset) {
 selected.value = row; report.value = undefined; casePage.value = runPage.value = 1;
 documents.value = []; docPage.value = 0;
 await Promise.all([loadCases(), loadRuns(), moreDocuments()]);
}
function openDataset(row?: Dataset) {
 editingDataset.value = row?.id;
 Object.assign(datasetForm, row ? { name: row.name, knowledgeBaseId: row.knowledgeBaseId, description: row.description } : { name: '', knowledgeBaseId: '', description: '' });
 datasetDialog.value = true;
}
async function saveDataset() {
 if (!datasetForm.name.trim() || !datasetForm.knowledgeBaseId) return void ElMessage.warning('请填写名称并选择知识库');
 saving.value = true;
 try { const row = editingDataset.value ? await putV1AiEvaluationDatasetsByDatasetId({ ...datasetForm, datasetId: editingDataset.value }) : await postV1AiEvaluationDatasets(datasetForm); datasetDialog.value = false; await loadDatasets(); await selectDataset(row); }
 finally { saving.value = false; }
}
function openCase(row?: EvaluationCase) {
 editingCase.value = row?.id;
 Object.assign(caseForm, row ? { question: row.question, category: row.category, difficulty: row.difficulty,
  answerable: row.answerable, expectedEvidence: row.expectedEvidence.map(item => ({ ...item })), expectedAnswer: row.expectedAnswer, notes: row.notes } : emptyCase());
 caseDialog.value = true;
}
async function saveCase() {
 if (!selected.value) return;
 if (!caseForm.question.trim()) return void ElMessage.warning('请填写问题');
 const evidence = caseForm.answerable ? caseForm.expectedEvidence : [];
 if (caseForm.answerable && (!evidence.length || evidence.some(item => !item.documentId || !item.text.trim()))) return void ElMessage.warning('请选择文档并填写原文依据');
 saving.value = true;
 try { editingCase.value ? await putV1AiEvaluationDatasetsByDatasetIdCasesByCaseId({ ...caseForm, expectedEvidence: evidence, datasetId: selected.value.id, caseId: editingCase.value }) : await postV1AiEvaluationDatasetsByDatasetIdCases({ ...caseForm, expectedEvidence: evidence, datasetId: selected.value.id }); caseDialog.value = false; await loadCases(); }
 finally { saving.value = false; }
}
async function removeDataset(row: Dataset) {
 if (await ElMessageBox.confirm('删除后样本和历史报告也会删除，是否继续？', '删除评测集', { type: 'warning' }).catch(() => false) !== 'confirm') return;
 await deleteV1AiEvaluationDatasetsByDatasetId({ datasetId: row.id });
 if (selected.value?.id === row.id) { selected.value = undefined; report.value = undefined; }
 await loadDatasets();
}
async function removeCase(row: EvaluationCase) {
 if (!selected.value || await ElMessageBox.confirm('删除样本？历史报告快照会保留。', '删除样本').catch(() => false) !== 'confirm') return;
 await deleteV1AiEvaluationDatasetsByDatasetIdCasesByCaseId({ datasetId: selected.value.id, caseId: row.id }); await loadCases();
}
async function run() {
 if (!selected.value) return;
 running.value = true;
 try { report.value = await postV1AiEvaluationDatasetsByDatasetIdRuns({ datasetId: selected.value.id, topK: topK.value, generateAnswers: generateAnswers.value }, { timeout: 60000 }); await loadRuns(); }
 finally { running.value = false; }
}
async function showReport(row: EvaluationRun) { if (selected.value) report.value = await getV1AiEvaluationDatasetsByDatasetIdRunsByRunId({ datasetId: selected.value.id, runId: row.id }); }
onMounted(async () => { await Promise.all([loadDatasets(), moreKnowledgeBases()]); });
</script>

<template>
 <main class="evaluation-page">
  <header><div><h1>知识库检索评测</h1><p>维护问题与原文依据，用同一组题检验检索效果。</p></div><el-button type="primary" @click="openDataset()">新建评测集</el-button></header>
  <el-table :data="datasets" highlight-current-row @row-click="selectDataset">
   <el-table-column prop="name" label="评测集"/><el-table-column prop="description" label="说明"/>
   <el-table-column label="操作" width="170"><template #default="{ row }"><el-button link @click.stop="openDataset(row)">编辑</el-button><el-button link type="danger" @click.stop="removeDataset(row)">删除</el-button></template></el-table-column>
  </el-table>
  <el-pagination v-model:current-page="datasetPage" :page-size="20" :total="datasetTotal" layout="prev, pager, next, total" @current-change="loadDatasets"/>
  <section v-if="selected" :key="selected.id">
   <header><h2>{{ selected.name }}</h2><div class="actions"><el-button @click="openCase()">新增样本</el-button><span>Top K</span><el-input-number v-model="topK" :min="1" :max="20"/><el-switch v-model="generateAnswers" active-text="评估答案生成"/><el-button type="primary" :loading="running" :disabled="caseTotal < 1 || caseTotal > 50" @click="run">运行评测</el-button></div></header>
   <el-alert v-if="generateAnswers" title="开启后会调用模型生成答案并评审，使用公共默认模型，会增加耗时和调用费用。先用少量样本；模型评分需人工复核。" type="info" :closable="false"/>
   <el-alert title="每次同步评测 1～50 题。依据请选简短完整原文；跨片段依据可能漏判。无答案题仅保留召回，不参与质量均值。" type="info" :closable="false"/>
   <el-table :data="cases"><el-table-column prop="question" label="问题"/><el-table-column prop="category" label="分类" width="130"/>
    <el-table-column label="标准" width="150"><template #default="{ row }">{{ row.answerable ? `${row.expectedEvidence.length} 条依据` : '无答案题' }}</template></el-table-column>
    <el-table-column label="操作" width="150"><template #default="{ row }"><el-button link @click="openCase(row)">编辑</el-button><el-button link type="danger" @click="removeCase(row)">删除</el-button></template></el-table-column>
   </el-table>
   <el-pagination v-model:current-page="casePage" :page-size="20" :total="caseTotal" layout="prev, pager, next, total" @current-change="loadCases"/>
   <h2>运行历史</h2>
   <el-table :data="runs"><el-table-column prop="createdAt" label="开始时间"/><el-table-column prop="status" label="状态" width="120"/><el-table-column prop="totalCases" label="题数" width="80"/>
    <el-table-column label="依据 Recall@K"><template #default="{ row }">{{ percent(row.metrics.recallAtK) }}</template></el-table-column>
    <el-table-column label="操作" width="100"><template #default="{ row }"><el-button link @click="showReport(row)">查看报告</el-button></template></el-table-column>
   </el-table>
   <el-pagination v-model:current-page="runPage" :page-size="10" :total="runTotal" layout="prev, pager, next, total" @current-change="loadRuns"/>
  </section>
  <section v-if="report">
   <h2>评测报告</h2>
   <p>有答案题 {{ report.metrics.evaluatedCases }} · 无答案题 {{ report.metrics.unanswerableCases }} · 失败 {{ report.metrics.failedCases }}</p>
   <div class="metrics">
    <el-tooltip content="前 K 条召回结果中，至少命中一条标准原文依据的题目比例。" placement="top"><span class="metric-help">Hit@{{ report.topK }}：{{ percent(report.metrics.hitAtK) }} ⓘ</span></el-tooltip>
    <el-tooltip content="前 K 条结果找回的标准依据数量，占该题标准依据总数的比例，再对有答案题取平均。" placement="top"><span class="metric-help">依据 Recall@{{ report.topK }}：{{ percent(report.metrics.recallAtK) }} ⓘ</span></el-tooltip>
    <el-tooltip content="第一条命中依据的排名倒数：第 1 名为 1，第 2 名为 0.5；越大表示正确依据越靠前。" placement="top"><span class="metric-help">MRR：{{ report.metrics.mrr?.toFixed(3) ?? '未评分' }} ⓘ</span></el-tooltip>
   </div>
   <p>Embedding：{{ report.config.embedding_model }} · 评分规则：{{ report.config.scorer_version }}</p>
   <div v-if="report.metrics.generatedCases || report.metrics.generationFailedCases" class="metrics">
    <span>生成成功 {{ report.metrics.generatedCases }} · 生成失败 {{ report.metrics.generationFailedCases }}</span>
    <el-tooltip content="评审模型判断回答是否切题，越高越好。" placement="top"><span>答案相关性：{{ percent(report.metrics.answerRelevance) }} ⓘ</span></el-tooltip>
    <el-tooltip content="评审模型对照参考答案判断事实、数值和条件是否正确；未填参考答案时不评分。" placement="top"><span>答案正确性：{{ percent(report.metrics.answerCorrectness) }} ⓘ</span></el-tooltip>
    <el-tooltip content="评审模型判断回答声明是否得到实际发送给模型的上下文支持。" placement="top"><span>忠实性：{{ percent(report.metrics.faithfulness) }} ⓘ</span></el-tooltip>
   </div>
   <el-collapse><el-collapse-item v-for="result in report.results" :key="result.id" :name="result.id" :title="`${result.caseSnapshot.question} · ${result.status === 'failed' ? '执行失败' : percent(result.recallAtK)}`">
    <p v-if="result.errorCode">失败码：{{ result.errorCode }}</p><p>耗时 {{ result.elapsedMs.toFixed(0) }} ms · 首个命中排名：{{ result.mrr == null ? '未评分' : result.mrr === 0 ? '未命中' : `第 ${Math.round(1 / result.mrr)} 名` }}</p>
    <template v-if="result.generationStatus && result.generationStatus !== 'not_requested'">
     <h3>最终生成答案</h3><p>状态：{{ result.generationStatus }} · 模型：{{ result.generationModel || '未调用' }} · Prompt：{{ result.promptVersion || '未使用' }}</p>
     <pre>{{ result.generatedAnswer || '生成失败，未获得完整答案' }}</pre>
     <h3>参考答案</h3><pre>{{ result.caseSnapshot.expectedAnswer || '未填写，不评答案正确性' }}</pre>
     <p>相关性 {{ percent(result.answerRelevance) }} · 正确性 {{ percent(result.answerCorrectness) }} · 忠实性 {{ percent(result.faithfulness) }}</p>
     <p v-if="result.generationDetails?.judge">评审状态：{{ result.generationDetails.judge.status }} · {{ result.generationDetails.judge.reason || '本题尚未获得有效评分' }}</p>
     <ul v-if="result.unsupportedClaims?.length"><li v-for="claim in result.unsupportedClaims" :key="claim">无依据声明：{{ claim }}</li></ul>
    </template>
    <h3>标准依据（运行时快照）</h3><p v-if="!result.caseSnapshot.answerable">无答案题</p><blockquote v-for="(evidence, i) in result.caseSnapshot.expectedEvidence" :key="i">{{ evidence.text }}</blockquote>
    <h3>实际召回</h3><article v-for="(item, index) in result.retrievedItems" :key="item.chunkId"><strong>{{ index + 1 }}. {{ item.fileName }} · 索引版本 {{ item.generation }} · 相似度 {{ item.similarity.toFixed(3) }}</strong><p v-if="item.sectionTitle">{{ item.sectionTitle }}</p><pre>{{ item.content }}</pre></article>
   </el-collapse-item></el-collapse>
  </section>
  <el-dialog v-model="datasetDialog" :title="editingDataset ? '编辑评测集' : '新建评测集'" width="560px">
   <el-form label-position="top"><el-form-item label="名称"><el-input v-model="datasetForm.name" maxlength="100"/></el-form-item>
    <el-form-item label="知识库"><el-select v-model="datasetForm.knowledgeBaseId" :disabled="!!editingDataset" placeholder="选择知识库"><el-option v-for="kb in knowledgeBases" :key="kb.id" :label="kb.name" :value="kb.id"/></el-select><el-button v-if="knowledgeBases.length < kbTotal" link @click="moreKnowledgeBases">加载更多知识库</el-button></el-form-item>
    <el-form-item label="说明"><el-input v-model="datasetForm.description" type="textarea" maxlength="1000"/></el-form-item>
   </el-form><template #footer><el-button @click="datasetDialog=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveDataset">保存</el-button></template>
  </el-dialog>
  <el-dialog v-model="caseDialog" :title="editingCase ? '编辑样本' : '新增样本'" width="700px">
   <el-form label-position="top"><el-form-item label="问题"><el-input v-model="caseForm.question" type="textarea" maxlength="2000"/></el-form-item>
    <div class="actions"><el-input v-model="caseForm.category" placeholder="自定义分类" maxlength="100"/><el-select v-model="caseForm.difficulty"><el-option label="简单" value="easy"/><el-option label="中等" value="medium"/><el-option label="困难" value="hard"/></el-select><el-switch v-model="caseForm.answerable" active-text="有答案"/></div>
    <div v-if="caseForm.answerable"><h3>标准原文依据</h3><div v-for="(evidence, index) in caseForm.expectedEvidence" :key="index" class="evidence">
     <el-select v-model="evidence.documentId" placeholder="选择预期文档"><el-option v-for="doc in documents" :key="doc.id" :label="doc.fileName" :value="doc.id"/></el-select><el-button link type="danger" @click="caseForm.expectedEvidence.splice(index,1)">移除</el-button>
     <el-input v-model="evidence.text" type="textarea" :rows="3" maxlength="4000" placeholder="粘贴文档中的简短完整原文"/>
    </div><el-button :disabled="caseForm.expectedEvidence.length >= 20" @click="caseForm.expectedEvidence.push({documentId:'',text:''})">添加依据</el-button><el-button v-if="documents.length < docTotal" link @click="moreDocuments">加载更多文档</el-button></div>
    <el-form-item label="参考答案（可选，用于答案正确性评分）"><el-input v-model="caseForm.expectedAnswer" type="textarea" maxlength="4000"/></el-form-item>
    <el-form-item label="标注说明"><el-input v-model="caseForm.notes" type="textarea" maxlength="2000"/></el-form-item>
   </el-form><template #footer><el-button @click="caseDialog=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveCase">保存</el-button></template>
  </el-dialog>
 </main>
</template>

<style scoped>
.evaluation-page { width: 100%; height: 100%; box-sizing: border-box; overflow-y: auto; padding: 24px; max-width: 1400px; margin: 0 auto; color: #303133; }
header, .actions, .metrics { display: flex; align-items: center; gap: 12px; }
header { justify-content: space-between; margin-bottom: 18px; }
h1 { margin: 0; font-size: 24px; } h2 { font-size: 19px; } p { color: #606266; }
section { margin-top: 28px; padding-top: 20px; border-top: 1px solid #ebeef5; }
.el-pagination { margin-top: 14px; } .metrics { flex-wrap: wrap; padding: 16px; background: #f5f7fa; }
.evidence { margin-bottom: 16px; } .evidence .el-textarea { margin-top: 8px; }
pre { white-space: pre-wrap; overflow-wrap: anywhere; background: #f5f7fa; padding: 12px; }
blockquote { border-left: 3px solid #409eff; padding-left: 12px; white-space: pre-wrap; }
@media (max-width: 760px) { header { align-items: flex-start; flex-direction: column; } .actions { flex-wrap: wrap; } }
</style>
