package com.admin.template.common.exception;

import com.admin.template.common.response.Result;
import com.admin.template.common.web.TraceIdFilter;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Object> handleBizException(BizException exception, WebRequest request) {
        String traceId = traceId(request);
        LOGGER.warn("Business exception httpStatus=200 resultCode={} reason={} message={} traceId={}",
                exception.code(), exception.error().name(), exception.getMessage(), traceId);
        return businessError(exception.code(), exception.getMessage(), exception.details(), new HttpHeaders(), traceId);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Object> handleAppException(AppException exception, WebRequest request) {
        String traceId = traceId(request);
        LOGGER.error("Application exception httpStatus={} resultCode={} reason={} traceId={}",
                exception.error().status().value(), exception.code(), exception.error().name(), traceId, exception);
        return error(exception.error().status(), exception.code(), exception.getMessage(),
                exception.details(), new HttpHeaders(), traceId);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ValidationIssue> details = new ArrayList<>();
        for (ObjectError error : exception.getBindingResult().getAllErrors()) {
            String path = error instanceof FieldError fieldError ? fieldError.getField() : "";
            if (error.contains(ConstraintViolation.class)) {
                details.add(issue(path, error.unwrap(ConstraintViolation.class)));
            } else {
                details.add(new ValidationIssue(path, error.getDefaultMessage(), "invalid_type"));
            }
        }
        return validation(details, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (exception.isForReturnValue()) return handleUnexpected(exception, request);
        List<ValidationIssue> details = new ArrayList<>();
        for (var result : exception.getParameterValidationResults()) {
            var parameter = result.getMethodParameter();
            RequestParam query = parameter.getParameterAnnotation(RequestParam.class);
            PathVariable path = parameter.getParameterAnnotation(PathVariable.class);
            String name = query != null && !query.name().isEmpty() ? query.name()
                    : path != null && !path.name().isEmpty() ? path.name() : parameter.getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                details.add(issue(name, result.unwrap(error, ConstraintViolation.class)));
            }
        }
        return validation(details, headers, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolations(ConstraintViolationException exception, WebRequest request) {
        List<ValidationIssue> details = new ArrayList<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            StringBuilder path = new StringBuilder();
            for (var node : violation.getPropertyPath()) {
                if (node.getKind() == ElementKind.RETURN_VALUE) return handleUnexpected(exception, request);
                if (node.getKind() != ElementKind.PARAMETER && node.getKind() != ElementKind.PROPERTY) continue;
                if (!path.isEmpty()) path.append('.');
                path.append(node.getName());
            }
            details.add(issue(path.toString(), violation));
        }
        return validation(details, new HttpHeaders(), request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String path = exception instanceof MethodArgumentTypeMismatchException argument ? argument.getName()
                : exception.getPropertyName();
        return validation(List.of(new ValidationIssue(path, "请求参数类型不正确", "invalid_type")), headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        for (Throwable cause = exception.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof BizException bizException) {
                return handleBizException(bizException, request);
            }
            if (cause instanceof AppException appException) {
                return handleAppException(appException, request);
            }
        }
        return error(HttpStatus.BAD_REQUEST, "请求体格式不正确", null, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        long maxBytes = exception.getMaxUploadSize();
        long maxMiB = maxBytes > 0 ? Math.max(1, Math.round(maxBytes / 1024d / 1024d)) : 20;
        String limit = maxBytes > 0 && maxBytes % (1024L * 1024L) == 0
                ? maxMiB == 10 ? "10MB" : maxMiB + " MiB"
                : maxBytes > 0 ? maxBytes + " 字节" : "20 MiB";
        String message = maxMiB == 10
                ? "文件大小超过限制（最大10MB）"
                : "文件大小超过服务接收限制（最大" + limit + "）";
        return businessError(BusinessError.VALIDATION_ERROR.code(), message, null,
                headers, request);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Object> handleMultipart(MultipartException exception, WebRequest request) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            String description = (cause.getClass().getSimpleName() + " " + cause.getMessage()).toLowerCase(Locale.ROOT);
            if (description.contains("too many files") || description.contains("unexpected field")
                    || description.contains("filecountlimit")) {
                return businessError(BusinessError.VALIDATION_ERROR.code(),
                        "文件数量超过限制（最多10个）", null,
                        new HttpHeaders(), request);
            }
        }
        return error(HttpStatus.BAD_REQUEST, "上传请求格式不正确", null, new HttpHeaders(), request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String traceId = traceId(request);
        if (status.is5xxServerError()) {
            LOGGER.error("HTTP exception traceId={}", traceId, exception);
        }
        String message = status.value() == 404 ? "Not Found"
                : exception instanceof org.springframework.web.ErrorResponse error ? error.getBody().getDetail()
                : HttpStatus.valueOf(status.value()).getReasonPhrase();
        return error(status, message, null, headers, traceId);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception exception, WebRequest request) {
        String traceId = traceId(request);
        LOGGER.error("Unexpected server exception traceId={}", traceId, exception);
        return error(SystemError.INTERNAL_ERROR.status(), SystemError.INTERNAL_ERROR.code(),
                SystemError.INTERNAL_ERROR.message(), null, new HttpHeaders(), traceId);
    }

    private ResponseEntity<Object> validation(List<ValidationIssue> details, HttpHeaders headers, WebRequest request) {
        return businessError(BusinessError.VALIDATION_ERROR.code(),
                BusinessError.VALIDATION_ERROR.message(), details, headers, request);
    }

    private ValidationIssue issue(String path, ConstraintViolation<?> violation) {
        var descriptor = violation.getConstraintDescriptor();
        String constraint = descriptor.getAnnotation().annotationType().getSimpleName();
        Object value = violation.getInvalidValue();
        String type = switch (constraint) {
            case "NotNull", "Null" -> "invalid_type";
            case "NotBlank", "NotEmpty" -> value == null ? "invalid_type" : "too_small";
            case "Min", "DecimalMin", "Positive", "PositiveOrZero" -> "too_small";
            case "Max", "DecimalMax", "Negative", "NegativeOrZero" -> "too_big";
            case "Size" -> size(value) > ((Number) descriptor.getAttributes().get("max")).intValue()
                    ? "too_big" : "too_small";
            case "Email", "Pattern" -> "invalid_format";
            default -> "custom";
        };
        return new ValidationIssue(path == null ? "" : path.replaceAll("\\[(\\d+)]", ".$1"), violation.getMessage(), type);
    }

    private int size(Object value) {
        if (value instanceof CharSequence text) return text.length();
        if (value instanceof Collection<?> collection) return collection.size();
        if (value instanceof Map<?, ?> map) return map.size();
        return value != null && value.getClass().isArray() ? Array.getLength(value) : 0;
    }

    private ResponseEntity<Object> error(HttpStatusCode status, String message, Object details,
            HttpHeaders headers, WebRequest request) {
        return error(status, status.value(), message, details, headers, traceId(request));
    }

    private ResponseEntity<Object> error(HttpStatusCode status, String message, Object details,
            HttpHeaders headers, String traceId) {
        return error(status, status.value(), message, details, headers, traceId);
    }

    private ResponseEntity<Object> error(HttpStatusCode status, int code, String message, Object details,
            HttpHeaders headers, String traceId) {
        return ResponseEntity.status(status).headers(headers)
                .body(Result.fail(code, message, details, traceId));
    }

    private ResponseEntity<Object> businessError(int code, String message, Object details, HttpHeaders headers,
            WebRequest request) {
        return businessError(code, message, details, headers, traceId(request));
    }

    private ResponseEntity<Object> businessError(int code, String message, Object details, HttpHeaders headers,
            String traceId) {
        return ResponseEntity.ok().headers(headers).body(Result.fail(code, message, details, traceId));
    }

    private String traceId(WebRequest request) {
        Object traceId = request.getAttribute(TraceIdFilter.ATTRIBUTE_NAME, WebRequest.SCOPE_REQUEST);
        return traceId == null ? UUID.randomUUID().toString() : traceId.toString();
    }

    private record ValidationIssue(String path, String message, String type) {
    }
}
