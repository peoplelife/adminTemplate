package com.admin.template.common;

import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.common.exception.GlobalExceptionHandler;
import com.admin.template.common.response.Result;
import com.admin.template.common.web.TraceIdFilter;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.Filter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import org.slf4j.LoggerFactory;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.junit.jupiter.api.extension.ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = ApiContractTest.TestApplication.class)
class ApiContractTest {
    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private TraceIdFilter traceIdFilter;

    private MockMvc mockMvc;

    @Configuration
    @EnableWebMvc
    @Import({GlobalExceptionHandler.class, TraceIdFilter.class, Probe.class,
            ValidatedProbe.class})
    static class TestApplication {
        @Bean
        static MethodValidationPostProcessor methodValidationPostProcessor() {
            return new MethodValidationPostProcessor();
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .addFilters((Filter) traceIdFilter)
                .build();
    }

    @Test
    void returnsExplicitResultAndPropagatesRequestId() throws Exception {
        mockMvc.perform(get("/v1/test/success").header("X-Trace-Id", "client-request-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "client-request-123"))
                .andExpect(header().doesNotExist("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.value").value("ok"))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.traceId").value("client-request-123"))
                .andExpect(jsonPath("$.requestId").doesNotExist())
                .andExpect(jsonPath("$.timestamp", matchesPattern(".+")));
    }

    @Test
    void usesEndpointSpecificSuccessMessageWhenDeclared() throws Exception {
        mockMvc.perform(get("/v1/test/custom-success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("自定义成功"));
    }

    @Test
    void generatesRequestIdWhenHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/v1/test/success"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.traceId", matchesPattern(".+")));
    }

    @Test
    void leavesNonVersionedJsonUnwrapped() throws Exception {
        mockMvc.perform(get("/test/plain"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"value\":\"plain\"}"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void replacesUnsafeTraceIdsWithoutEchoingThem() throws Exception {
        for (String supplied : List.of("", "unsafe value", "bad/value", "x".repeat(129))) {
            var response = mockMvc.perform(get("/v1/test/success").header("X-Trace-Id", supplied))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Trace-Id", matchesPattern("[A-Za-z0-9._:-]{1,128}")))
                    .andReturn().getResponse();
            assertThat(response.getHeader("X-Trace-Id")).isNotEqualTo(supplied);
            assertThat(new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(response.getContentAsString()).path("traceId").asText())
                    .isEqualTo(response.getHeader("X-Trace-Id"));
        }
    }

    @Test
    void doesNotImplicitlyWrapVersionedJson() throws Exception {
        mockMvc.perform(get("/v1/test/unwrapped"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"value\":\"plain\"}"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void normalizesApplicationErrorsWithStableFields() throws Exception {
        mockMvc.perform(get("/v1/test/error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(120002))
                .andExpect(jsonPath("$.message").value("Conflict"))
                .andExpect(jsonPath("$.details.reason").value("duplicate"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.traceId", matchesPattern(".+")))
                .andExpect(jsonPath("$.timestamp", matchesPattern(".+")));
    }

    @Test
    void usesTheUnifiedResultEnvelopeForErrors() throws Exception {
        mockMvc.perform(get("/v1/test/error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(120002))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.details.reason").value("duplicate"));
    }

    @Test
    void keepsNoContentResponseBodyEmpty() throws Exception {
        mockMvc.perform(get("/v1/test/no-content"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void reportsBodyValidationAs422WithZodIssueFields() throws Exception {
        mockMvc.perform(post("/v1/test/validated-body").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100001))
                .andExpect(jsonPath("$.message").value("请求参数不正确"))
                .andExpect(jsonPath("$.details[0].path").value("username"))
                .andExpect(jsonPath("$.details[0].message", matchesPattern(".+")))
                .andExpect(jsonPath("$.details[0].type").value("too_small"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void reportsMissingBodyPropertyAsAnInvalidTypeIssue() throws Exception {
        mockMvc.perform(post("/v1/test/validated-body").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100001))
                .andExpect(jsonPath("$.details[0].path").value("username"))
                .andExpect(jsonPath("$.details[0].type").value("invalid_type"));
    }

    @Test
    void reportsInvalidPathParameterAsAValidationIssue() throws Exception {
        mockMvc.perform(get("/v1/test/typed-path/not-a-number"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100001))
                .andExpect(jsonPath("$.details[0].path").value("id"))
                .andExpect(jsonPath("$.details[0].type").value("invalid_type"));
    }

    @Test
    void reportsInvalidQueryParameterAsAValidationIssue() throws Exception {
        mockMvc.perform(get("/v1/test/native-validation").param("page", "bad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100001))
                .andExpect(jsonPath("$.details[0].path").value("page"))
                .andExpect(jsonPath("$.details[0].type").value("invalid_type"));
    }

    @Test
    void reportsNativeMvcConstraintViolationsAsValidationIssues() throws Exception {
        mockMvc.perform(get("/v1/test/native-validation").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100001))
                .andExpect(jsonPath("$.details[0].path").value("page"))
                .andExpect(jsonPath("$.details[0].type").value("too_small"));
    }

    @Test
    void reportsProxiedMethodConstraintViolationsWithoutJavaMethodNames() throws Exception {
        mockMvc.perform(get("/v1/test/proxied-validation").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100001))
                .andExpect(jsonPath("$.details[0].path").value("page"))
                .andExpect(jsonPath("$.details[0].type").value("too_small"));
    }

    @Test
    void keepsMalformedJsonAsABadRequest() throws Exception {
        mockMvc.perform(post("/v1/test/validated-body").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.details").value(nullValue()))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void normalizesMissingRoutesAsNotFound() throws Exception {
        mockMvc.perform(get("/v1/test/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Not Found"))
                .andExpect(jsonPath("$.details").value(nullValue()));
    }

    @Test
    void preservesUnsupportedHttpMethodStatusInTheErrorEnvelope() throws Exception {
        mockMvc.perform(post("/v1/test/success"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"))
                .andExpect(jsonPath("$.code").value(405))
                .andExpect(jsonPath("$.details").value(nullValue()));
    }

    @Test
    void distinguishesUnsupportedContentTypeFromServerErrors() throws Exception {
        mockMvc.perform(post("/v1/test/validated-body").contentType(MediaType.TEXT_PLAIN).content("username=alice"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(415))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void distinguishesAnUnacceptableResponseTypeFromServerErrors() throws Exception {
        mockMvc.perform(get("/v1/test/unsupported-response-type"))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.code").value(406));
    }

    @Test
    void normalizesMultipartSizeLimitsAsValidationErrors() throws Exception {
        mockMvc.perform(get("/v1/test/upload-size-limit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100001))
                .andExpect(jsonPath("$.message").value("文件大小超过限制（最大10MB）"))
                .andExpect(jsonPath("$.details").value(nullValue()));
    }

    @Test
    void normalizesMultipartCountLimitsAsValidationErrors() throws Exception {
        mockMvc.perform(get("/v1/test/upload-count-limit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100001))
                .andExpect(jsonPath("$.message").value("文件数量超过限制（最多10个）"))
                .andExpect(jsonPath("$.details").value(nullValue()));
    }

    @Test
    void hidesUnexpectedExceptionDetailsWithTheOriginalInternalErrorCode() throws Exception {
        mockMvc.perform(get("/v1/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500000))
                .andExpect(jsonPath("$.message").value("服务内部错误"))
                .andExpect(jsonPath("$.details").value(nullValue()));
    }

    @Test
    void logsUnexpectedExceptionsWhileReturningGenericInternalError() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        LoggerContext loggerContext = logger.getLoggerContext();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext(loggerContext);
        appender.start();
        logger.addAppender(appender);

        try {
            mockMvc.perform(get("/v1/test/unexpected").header("X-Trace-Id", "unexpected-log-contract"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(500000))
                    .andExpect(jsonPath("$.message").value("服务内部错误"))
                    .andExpect(jsonPath("$.details").value(nullValue()));

            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                assertThat(event.getThrowableProxy()).isNotNull();
                assertThat(event.getThrowableProxy().getClassName())
                        .isEqualTo(IllegalStateException.class.getName());
                assertThat(event.getFormattedMessage()).contains("unexpected-log-contract");
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void preservesValidationIssuesThrownWhileBindingRequestFields() throws Exception {
        mockMvc.perform(post("/v1/test/constructor-validation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100001))
                .andExpect(jsonPath("$.details[0].path").value("status"))
                .andExpect(jsonPath("$.details[0].type").value("too_big"));
    }

    record ValidatedBody(@NotBlank String username) {
    }

    record ConstructorValidatedBody(int status) {
        ConstructorValidatedBody {
            if (status > 1) {
                throw new BizException(BusinessError.VALIDATION_ERROR,
                        java.util.List.of(Map.of("path", "status", "message", "状态只能为0或1", "type", "too_big")));
            }
        }
    }

    @Validated
    @RestController
    static class ValidatedProbe {
        @GetMapping("/v1/test/proxied-validation")
        public Map<String, Integer> page(@RequestParam @Min(1) int page) {
            return Map.of("page", page);
        }
    }

    @RestController
    static class Probe {
        @GetMapping("/v1/test/success")
        Result<Map<String, String>> success(@RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
            return Result.ok(Map.of("value", "ok"), traceId);
        }

        @GetMapping("/test/plain")
        Map<String, String> plain() {
            return Map.of("value", "plain");
        }

        @GetMapping("/v1/test/unwrapped")
        Map<String, String> unwrapped() {
            return Map.of("value", "plain");
        }

        @GetMapping("/v1/test/custom-success")
        Result<Map<String, String>> customSuccess(@RequestAttribute(TraceIdFilter.ATTRIBUTE_NAME) String traceId) {
            return Result.ok(Map.of("value", "custom"), "自定义成功", traceId);
        }

        @GetMapping("/v1/test/error")
        Map<String, String> error() {
            throw new BizException(BusinessError.USERNAME_EXISTS, "Conflict", Map.of("reason", "duplicate"));
        }

        @GetMapping("/v1/test/no-content")
        org.springframework.http.ResponseEntity<Void> noContent() {
            return org.springframework.http.ResponseEntity.noContent().build();
        }

        @PostMapping("/v1/test/validated-body")
        Map<String, String> validatedBody(@Valid @RequestBody ValidatedBody body) {
            return Map.of("username", body.username());
        }

        @PostMapping("/v1/test/constructor-validation")
        ConstructorValidatedBody constructorValidation(@RequestBody ConstructorValidatedBody body) {
            return body;
        }

        @GetMapping("/v1/test/typed-path/{id}")
        Map<String, Long> typedPath(@PathVariable long id) {
            return Map.of("id", id);
        }

        @GetMapping("/v1/test/native-validation")
        Map<String, Integer> nativeValidation(@RequestParam @Min(1) int page) {
            return Map.of("page", page);
        }

        @GetMapping("/v1/test/upload-size-limit")
        Map<String, String> sizeLimit() {
            throw new MaxUploadSizeExceededException(10 * 1024 * 1024);
        }

        @GetMapping("/v1/test/unsupported-response-type")
        Map<String, String> unsupportedResponseType() throws org.springframework.web.HttpMediaTypeNotAcceptableException {
            throw new org.springframework.web.HttpMediaTypeNotAcceptableException(java.util.List.of(MediaType.APPLICATION_JSON));
        }

        @GetMapping("/v1/test/upload-count-limit")
        Map<String, String> countLimit() {
            throw new MultipartException("Too many files");
        }

        @GetMapping("/v1/test/unexpected")
        Map<String, String> unexpected() {
            throw new IllegalStateException("private database information");
        }
    }
}
