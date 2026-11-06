package com.admin.template.service.ai;

import com.admin.template.dto.ai.rag.EvaluationDtos.*;
import com.admin.template.security.AuthenticatedUser;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class AiEvaluationService {
    private static final String ROOT = "/api/v1/rag/evaluation-datasets";
    private final AiRagService transport;
    public AiEvaluationService(AiRagService transport) { this.transport = transport; }

    public Dataset createDataset(DatasetRequest request, AuthenticatedUser user, String traceId) {
        return transport.jsonRequest(HttpMethod.POST, ROOT, request, user, traceId, Dataset.class);
    }

    public DatasetPage listDatasets(int page, int pageSize, AuthenticatedUser user, String traceId) {
        return transport.jsonRequest(HttpMethod.GET, ROOT + "?page=" + page + "&page_size=" + pageSize, null, user, traceId, DatasetPage.class);
    }

    public Dataset updateDataset(UUID datasetId, DatasetRequest request, AuthenticatedUser user, String traceId) {
        return transport.jsonRequest(HttpMethod.PUT, ROOT + "/" + datasetId, request, user, traceId, Dataset.class);
    }

    public CaseData saveCase(UUID datasetId, CaseRequest request, AuthenticatedUser user, String traceId) {
        return transport.jsonRequest(HttpMethod.POST, ROOT + "/" + datasetId + "/cases", request, user, traceId, CaseData.class);
    }

    public CaseData updateCase(UUID datasetId, UUID caseId, CaseRequest request, AuthenticatedUser user, String traceId) {
        return transport.jsonRequest(HttpMethod.PUT, ROOT + "/" + datasetId + "/cases/" + caseId, request, user, traceId, CaseData.class);
    }

    public CasePage listCases(UUID datasetId, int page, int pageSize, AuthenticatedUser user, String traceId) {
        return transport.jsonRequest(HttpMethod.GET, ROOT + "/" + datasetId + "/cases?page=" + page + "&page_size=" + pageSize, null, user, traceId, CasePage.class);
    }

    public Run run(UUID datasetId, RunRequest request, AuthenticatedUser user, String traceId) {
        return transport.jsonRequest(HttpMethod.POST, ROOT + "/" + datasetId + "/runs", request, user, traceId, Run.class);
    }

    public RunPage listRuns(UUID datasetId, int page, int pageSize, AuthenticatedUser user, String traceId) {
        return transport.jsonRequest(HttpMethod.GET, ROOT + "/" + datasetId + "/runs?page=" + page + "&page_size=" + pageSize, null, user, traceId, RunPage.class);
    }

    public Run report(UUID datasetId, UUID runId, AuthenticatedUser user, String traceId) {
        return transport.jsonRequest(HttpMethod.GET, ROOT + "/" + datasetId + "/runs/" + runId, null, user, traceId, Run.class);
    }

    public void deleteDataset(UUID datasetId, AuthenticatedUser user, String traceId) {
        transport.deleteResource(ROOT + "/" + datasetId, user, traceId);
    }

    public void deleteCase(UUID datasetId, UUID caseId, AuthenticatedUser user, String traceId) {
        transport.deleteResource(ROOT + "/" + datasetId + "/cases/" + caseId, user, traceId);
    }
}
