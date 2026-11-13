package com.admin.template.controller.ai;

import com.admin.template.common.response.Result;
import com.admin.template.common.web.TraceIdFilter;
import com.admin.template.dto.ai.pageagent.PageAgentDtos.MessageRequest;
import com.admin.template.dto.ai.pageagent.PageAgentDtos.Workspace;
import com.admin.template.security.AuthenticatedUser;
import com.admin.template.service.ai.AiPageAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/v1/ai/page-agent")
@Tag(name = "AI 页面生成", description = "当前用户的页面生成工作区、对话及预览")
public class AiPageAgentController {
    private final AiPageAgentService service;

    public AiPageAgentController(AiPageAgentService service) {
        this.service = service;
    }

    @GetMapping(value = "/workspace", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "恢复页面生成工作区", description = "读取当前已认证用户的消息、页面结构和预览。")
    public Result<Workspace> workspace(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(hidden = true) @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(service.workspace(user, traceId), traceId);
    }

    @PostMapping(value = "/reset", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "重置页面生成工作区", description = "清空当前已认证用户的消息和页面状态，返回空工作区。")
    public Result<Workspace> reset(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(hidden = true) @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        return Result.ok(service.reset(user, traceId), traceId);
    }

    @PostMapping(value = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发送页面生成消息", description = """
            使用公共默认模型，客户端仅提交 content。SSE 事件：
            status {stage,message,traceId}；reasoning {content} 为模型推理增量；
            message {content} 为回复正文增量；clarification {question}；
            page_spec 为 PageAgentPageSpec；preview 为 PageAgentPreview；
            done {status:'completed',pageChanged:boolean}；error {code,message,traceId}。
            流中可包含注释心跳；开流前业务失败返回 Result JSON，外部服务故障返回 HTTP 502。
            """)
    @ApiResponse(responseCode = "200", description = "SSE 流或开流前的业务失败 JSON",
            content = {
                    @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE, schema = @Schema(type = "string")),
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Result.class))
            })
    @ApiResponse(responseCode = "502", description = "AI 服务不可用",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Result.class)))
    public ResponseEntity<StreamingResponseBody> stream(
            @Valid @RequestBody MessageRequest request,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(hidden = true) @RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
        StreamingResponseBody body = service.stream(request, user, traceId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_EVENT_STREAM);
        headers.setCacheControl(CacheControl.noCache());
        headers.set("X-Accel-Buffering", "no");
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
