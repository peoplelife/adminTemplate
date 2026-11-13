package com.admin.template.service.ai;

import com.admin.template.dto.ai.pageagent.PageAgentDtos.MessageRequest;
import com.admin.template.dto.ai.pageagent.PageAgentDtos.Workspace;
import com.admin.template.security.AuthenticatedUser;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Service
public class AiPageAgentService {
    private static final String ROOT = "/api/v1/ai/page-agent";
    private final AiServiceClient client;

    public AiPageAgentService(AiServiceClient client) {
        this.client = client;
    }

    public Workspace workspace(AuthenticatedUser user, String traceId) {
        return client.jsonRequest(HttpMethod.GET, ROOT + "/workspace", null, user, traceId, Workspace.class);
    }

    public Workspace reset(AuthenticatedUser user, String traceId) {
        return client.jsonRequest(HttpMethod.POST, ROOT + "/reset", null, user, traceId, Workspace.class);
    }

    public StreamingResponseBody stream(MessageRequest request, AuthenticatedUser user, String traceId) {
        return client.streamRequest(ROOT + "/messages/stream", request, user, traceId);
    }
}
