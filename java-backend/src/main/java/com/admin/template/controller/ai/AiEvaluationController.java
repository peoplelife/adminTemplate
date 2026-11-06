package com.admin.template.controller.ai;

import com.admin.template.common.response.Result;
import com.admin.template.common.web.TraceIdFilter;
import com.admin.template.dto.ai.rag.EvaluationDtos.*;
import com.admin.template.security.AuthenticatedUser;
import com.admin.template.service.ai.AiEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/v1/ai/evaluation-datasets")
@Tag(name="RAG 评测", description="私有评测集、样本与检索报告")
public class AiEvaluationController {
    private final AiEvaluationService service;
    public AiEvaluationController(AiEvaluationService service) { this.service = service; }

    @PostMapping("")
    @Operation(summary="创建评测集")
    public ResponseEntity<Result<Dataset>> createDataset(@Valid @RequestBody DatasetRequest request, @AuthenticationPrincipal AuthenticatedUser user, @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return ResponseEntity.status(201).body(Result.ok(service.createDataset(request, user, traceId), traceId));
    }

    @GetMapping("")
    @Operation(summary="查询评测集")
    public Result<DatasetPage> listDatasets(@RequestParam(defaultValue="1") @Min(1) int page, @RequestParam(defaultValue="20") @Min(1) @Max(100) int pageSize, @AuthenticationPrincipal AuthenticatedUser user, @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(service.listDatasets(page, pageSize, user, traceId), traceId);
    }

    @PutMapping("/{datasetId}")
    @Operation(summary="修改评测集")
    public Result<Dataset> updateDataset(@PathVariable UUID datasetId, @Valid @RequestBody DatasetRequest request, @AuthenticationPrincipal AuthenticatedUser user, @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(service.updateDataset(datasetId, request, user, traceId), traceId);
    }

    @PostMapping("/{datasetId}/cases")
    @Operation(summary="新增评测样本")
    public ResponseEntity<Result<CaseData>> saveCase(@PathVariable UUID datasetId, @Valid @RequestBody CaseRequest request, @AuthenticationPrincipal AuthenticatedUser user, @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return ResponseEntity.status(201).body(Result.ok(service.saveCase(datasetId, request, user, traceId), traceId));
    }

    @PutMapping("/{datasetId}/cases/{caseId}")
    @Operation(summary="修改评测样本")
    public Result<CaseData> updateCase(@PathVariable UUID datasetId, @PathVariable UUID caseId, @Valid @RequestBody CaseRequest request, @AuthenticationPrincipal AuthenticatedUser user, @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(service.updateCase(datasetId, caseId, request, user, traceId), traceId);
    }

    @GetMapping("/{datasetId}/cases")
    @Operation(summary="查询评测样本")
    public Result<CasePage> listCases(@PathVariable UUID datasetId, @RequestParam(defaultValue="1") @Min(1) int page, @RequestParam(defaultValue="20") @Min(1) @Max(100) int pageSize, @AuthenticationPrincipal AuthenticatedUser user, @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(service.listCases(datasetId, page, pageSize, user, traceId), traceId);
    }

    @PostMapping("/{datasetId}/runs")
    @Operation(summary="执行检索评测（同步 1 至 50 题）")
    public ResponseEntity<Result<Run>> run(@PathVariable UUID datasetId, @Valid @RequestBody RunRequest request, @AuthenticationPrincipal AuthenticatedUser user, @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return ResponseEntity.status(201).body(Result.ok(service.run(datasetId, request, user, traceId), traceId));
    }

    @GetMapping("/{datasetId}/runs")
    @Operation(summary="查询运行历史")
    public Result<RunPage> listRuns(@PathVariable UUID datasetId, @RequestParam(defaultValue="1") @Min(1) int page, @RequestParam(defaultValue="20") @Min(1) @Max(100) int pageSize, @AuthenticationPrincipal AuthenticatedUser user, @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(service.listRuns(datasetId, page, pageSize, user, traceId), traceId);
    }

    @GetMapping("/{datasetId}/runs/{runId}")
    @Operation(summary="查询完整评测报告")
    public Result<Run> report(@PathVariable UUID datasetId, @PathVariable UUID runId, @AuthenticationPrincipal AuthenticatedUser user, @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(service.report(datasetId, runId, user, traceId), traceId);
    }

    @DeleteMapping("/{datasetId}")
    @Operation(summary="删除评测数据")
    public ResponseEntity<Void> deleteDataset(@PathVariable UUID datasetId, @AuthenticationPrincipal AuthenticatedUser user, @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        service.deleteDataset(datasetId, user, traceId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{datasetId}/cases/{caseId}")
    @Operation(summary="删除评测数据")
    public ResponseEntity<Void> deleteCase(@PathVariable UUID datasetId, @PathVariable UUID caseId, @AuthenticationPrincipal AuthenticatedUser user, @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        service.deleteCase(datasetId, caseId, user, traceId);
        return ResponseEntity.noContent().build();
    }
}
