package com.admin.template.dto.ai.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class EvaluationDtos {
    private EvaluationDtos() {}

    @Schema(description = "评测集请求，建立后不更换知识库")
    public record DatasetRequest(@Schema(title="名称") @NotBlank @Size(max=100) String name,
        @Schema(title="知识库 ID") @NotNull UUID knowledgeBaseId,
        @Schema(title="说明") @Size(max=1000) String description) {
        public DatasetRequest { description = description == null ? "" : description; }
    }

    public record Dataset(@Schema(title="评测集 ID") UUID id, @Schema(title="名称") String name,
        @Schema(title="知识库 ID") UUID knowledgeBaseId, @Schema(title="说明") String description,
        @Schema(title="创建时间") Instant createdAt) {}

    public record Evidence(@Schema(title="文档 ID") @NotNull UUID documentId,
        @Schema(title="原文依据") @NotBlank @Size(max=4000) String text) {}

    public record CaseRequest(@Schema(title="问题") @NotBlank @Size(max=2000) String question,
        @Schema(title="分类") @Size(max=100) String category,
        @Schema(title="难度", allowableValues={"easy","medium","hard"}) @Pattern(regexp="easy|medium|hard") String difficulty,
        @Schema(title="是否有答案") @NotNull Boolean answerable,
        @Schema(title="标准依据") @NotNull @Size(max=20) List<@Valid Evidence> expectedEvidence,
        @Schema(title="参考答案") @Size(max=4000) String expectedAnswer,
        @Schema(title="备注") @Size(max=2000) String notes) {
        public CaseRequest {
            category = category == null ? "" : category;
            difficulty = difficulty == null ? "easy" : difficulty;
            notes = notes == null ? "" : notes;
        }
    }

    public record CaseData(@Schema(title="样本 ID") UUID id, @Schema(title="评测集 ID") UUID datasetId,
        @Schema(title="创建时间") Instant createdAt, @Schema(title="问题") String question,
        @Schema(title="分类") String category, @Schema(title="难度") String difficulty,
        @Schema(title="是否有答案") boolean answerable, @Schema(title="标准依据") List<Evidence> expectedEvidence,
        @Schema(title="参考答案") String expectedAnswer, @Schema(title="备注") String notes) {}

    public record RunRequest(@Schema(title="召回数量", defaultValue="5") @Min(1) @Max(20) Integer topK,
        @Schema(title="是否生成答案") Boolean generateAnswers) {
        public RunRequest { topK = topK == null ? 5 : topK; generateAnswers = generateAnswers != null && generateAnswers; }
    }

    public record Metrics(@Schema(title="Hit@K") Double hitAtK, @Schema(title="Recall@K") Double recallAtK,
        @Schema(title="MRR") Double mrr, @Schema(title="成功有答案题数") int evaluatedCases,
        @Schema(title="无答案题数") int unanswerableCases, @Schema(title="失败题数") int failedCases,
        @Schema(title="生成答案题数") int generatedCases, @Schema(title="生成失败题数") int generationFailedCases,
        @Schema(title="答案相关性") Double answerRelevance, @Schema(title="答案正确性") Double answerCorrectness,
        @Schema(title="答案忠实性") Double faithfulness) {}

    public record DocumentVersion(@Schema(title="文档 ID") UUID document_id, @Schema(title="索引版本") int generation,
        @Schema(title="文件哈希") String file_hash, @Schema(title="解析版本") String parser_version,
        @Schema(title="切分版本") String splitter_version) {}

    public record RunConfig(@Schema(title="评测集名称") String dataset_name,
        @Schema(title="知识库 ID") UUID knowledge_base_id, @Schema(title="Embedding 模型") String embedding_model,
        @Schema(title="切块大小") int chunk_size, @Schema(title="重叠长度") int chunk_overlap,
        @Schema(title="评分规则") String scorer_version, @Schema(title="文档快照") List<DocumentVersion> documents) {}

    public record RetrievedItem(@Schema(title="片段 ID") UUID chunkId, @Schema(title="文档 ID") UUID documentId,
        @Schema(title="索引版本") int generation, @Schema(title="原文") String content,
        @Schema(title="文件名") String fileName, @Schema(title="相似度") double similarity,
        @Schema(title="起始页") Integer pageFrom, @Schema(title="结束页") Integer pageTo,
        @Schema(title="章节") String sectionTitle) {}

    public record JudgeDetails(@Schema(title="评审状态") String status, @Schema(title="评审模型") String model,
        @Schema(title="评审 Prompt 版本") String prompt_version, @Schema(title="评分理由") String reason) {}

    public record GenerationDetails(@Schema(title="实际模型上下文") String context,
        @Schema(title="评审结果") JudgeDetails judge) {}

    public record CaseResult(@Schema(title="结果 ID") UUID id, @Schema(title="样本 ID") UUID caseId,
        @Schema(title="样本快照") CaseRequest caseSnapshot, @Schema(title="状态") String status,
        @Schema(title="召回结果") List<RetrievedItem> retrievedItems, @Schema(title="Hit@K") Double hitAtK,
        @Schema(title="Recall@K") Double recallAtK, @Schema(title="MRR") Double mrr,
        @Schema(title="耗时毫秒") double elapsedMs, @Schema(title="失败码") Integer errorCode,
        @Schema(title="生成答案") String generatedAnswer, @Schema(title="生成模型") String generationModel,
        @Schema(title="Prompt 版本") String promptVersion, @Schema(title="生成状态") String generationStatus,
        @Schema(title="答案相关性") Double answerRelevance, @Schema(title="答案正确性") Double answerCorrectness,
        @Schema(title="答案忠实性") Double faithfulness, @Schema(title="未支持声明") List<String> unsupportedClaims,
        @Schema(title="生成详情") GenerationDetails generationDetails) {}

    public record Run(@Schema(title="运行 ID") UUID id, @Schema(title="评测集 ID") UUID datasetId,
        @Schema(title="状态") String status, @Schema(title="召回数量") int topK,
        @Schema(title="样本数") int totalCases, @Schema(title="汇总指标") Metrics metrics,
        @Schema(title="运行配置") RunConfig config, @Schema(title="开始时间") Instant createdAt,
        @Schema(title="结束时间") Instant completedAt, @Schema(title="单题结果，列表中为空") List<CaseResult> results) {}

    public record DatasetPage(@Schema(title="当前页数据") List<Dataset> items,
        @Schema(title="总数") long total, @Schema(title="页码") int page,
        @Schema(title="每页数量") int pageSize) {}

    public record CasePage(@Schema(title="当前页数据") List<CaseData> items,
        @Schema(title="总数") long total, @Schema(title="页码") int page,
        @Schema(title="每页数量") int pageSize) {}

    public record RunPage(@Schema(title="当前页数据") List<Run> items,
        @Schema(title="总数") long total, @Schema(title="页码") int page,
        @Schema(title="每页数量") int pageSize) {}
}
