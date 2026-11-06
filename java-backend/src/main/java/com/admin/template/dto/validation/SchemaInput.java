package com.admin.template.dto.validation;

import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** JSON coercion rules shared by the migrated request schemas. */
public final class SchemaInput {
    private final JsonNode body;

    public SchemaInput(JsonNode body) {
        if (body == null || !body.isObject()) throw invalid("", "请求体必须是对象", "invalid_type");
        this.body = body;
    }

    public boolean has(String field) {
        return body.has(field);
    }

    public String requiredString(String field, int max) {
        String value = string(body.get(field), field);
        return length(value.trim(), field, 1, max);
    }

    public String optionalString(String field) {
        JsonNode value = body.get(field);
        if (value == null || value.isNull()) return null;
        return optionalValue(string(value, field));
    }

    public String nullableString(String field, int max) {
        JsonNode value = body.get(field);
        if (value == null || value.isNull()) return null;
        return length(string(value, field).trim(), field, 0, max);
    }

    public String patchRequiredString(String field, int max) {
        return has(field) ? requiredString(field, max) : null;
    }

    public String email(String field) {
        String value = optionalString(field);
        if (value != null && value.isEmpty()) throw invalid(field, "邮箱格式不正确", "invalid_format");
        return value;
    }

    public String menuType(String field, boolean required) {
        String value = required ? requiredString(field, Integer.MAX_VALUE) : optionalString(field);
        if (value == null) return null;
        value = value.toUpperCase(Locale.ROOT);
        if (!List.of("M", "C", "B").contains(value)) throw invalid(field, "菜单类型必须是 M、C 或 B", "invalid_value");
        return value;
    }

    public String parentId(String field) {
        JsonNode value = body.get(field);
        if (value == null || value.isNull()) return null;
        if (value.isTextual()) return value.textValue().trim();
        if (value.isNumber()) {
            try {
                return value.decimalValue().toBigIntegerExact().toString();
            } catch (ArithmeticException exception) {
                throw invalid(field, "父菜单 ID 必须是整数或字符串", "invalid_union");
            }
        }
        throw invalid(field, "父菜单 ID 必须是整数或字符串", "invalid_union");
    }

    public Integer integer(String field, Integer defaultValue, boolean emptyUsesDefault, int min, int max) {
        JsonNode value = body.get(field);
        if (value == null || emptyUsesDefault && (value.isNull() || value.isTextual() && value.textValue().isEmpty())) {
            return defaultValue;
        }
        String text = value.isNull() ? "0" : value.isBoolean() ? (value.booleanValue() ? "1" : "0")
                : coercedString(value);
        return parseInteger(text, field, min, max);
    }

    public List<String> stringList(String field, List<String> defaultValue) {
        JsonNode value = body.get(field);
        if (value == null) return defaultValue;
        if (!value.isArray()) throw invalid(field, "必须是数组", "invalid_type");
        List<String> values = new ArrayList<>();
        for (int index = 0; index < value.size(); index++) {
            String item = coercedString(value.get(index)).trim();
            if (item.isEmpty()) throw invalid(field + "." + index, "不能为空", "too_small");
            if (!values.contains(item)) values.add(item);
        }
        return List.copyOf(values);
    }

    public static String optionalValue(String value) {
        return value == null || value.isEmpty() ? null : value.trim();
    }

    public static String defaultEmpty(String value) {
        return value == null ? "" : value;
    }

    public static Integer queryInteger(String value, String field, Integer defaultValue, int min, int max) {
        if (value == null) return defaultValue;
        return Integer.valueOf(parseInteger(value, field, min, max));
    }

    public static long positiveId(String value) {
        try {
            long id = decimal(value).longValueExact();
            if (id <= 0) throw invalid("id", "必须大于 0", "too_small");
            return id;
        } catch (ArithmeticException | NumberFormatException exception) {
            throw invalid("id", "必须是正整数", "invalid_type");
        }
    }

    public static String queryMenuType(String value) {
        value = optionalValue(value);
        if (value == null) return null;
        value = value.toUpperCase(Locale.ROOT);
        if (!List.of("M", "C", "B").contains(value)) throw invalid("menuType", "菜单类型必须是 M、C 或 B", "invalid_value");
        return value;
    }

    public static BizException invalid(String path, String message, String type) {
        return new BizException(BusinessError.VALIDATION_ERROR,
                List.of(Map.of("path", path, "message", message, "type", type)));
    }

    private String string(JsonNode value, String field) {
        if (value == null || !value.isTextual()) throw invalid(field, "必须是字符串", "invalid_type");
        return value.textValue();
    }

    private String length(String value, String field, int min, int max) {
        if (value.length() < min) throw invalid(field, "不能为空", "too_small");
        if (value.length() > max) throw invalid(field, "长度不能超过 " + max, "too_big");
        return value;
    }

    private static int parseInteger(String text, String field, int min, int max) {
        BigDecimal value;
        try {
            value = decimal(text);
            value.toBigIntegerExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw invalid(field, "必须是整数", "invalid_type");
        }
        if (value.compareTo(BigDecimal.valueOf(min)) < 0) throw invalid(field, "不能小于 " + min, "too_small");
        if (value.compareTo(BigDecimal.valueOf(max)) > 0) throw invalid(field, "不能大于 " + max, "too_big");
        return value.intValueExact();
    }

    private static BigDecimal decimal(String text) {
        text = text.trim();
        if (text.isEmpty()) return BigDecimal.ZERO;
        if (text.matches("0[xX][0-9a-fA-F]+")) return new BigDecimal(new BigInteger(text.substring(2), 16));
        if (text.matches("0[bB][01]+")) return new BigDecimal(new BigInteger(text.substring(2), 2));
        if (text.matches("0[oO][0-7]+")) return new BigDecimal(new BigInteger(text.substring(2), 8));
        return new BigDecimal(text);
    }

    private static String coercedString(JsonNode value) {
        if (value.isTextual()) return value.textValue();
        if (value.isNull()) return "null";
        if (value.isObject()) return "[object Object]";
        if (value.isArray()) {
            List<String> values = new ArrayList<>();
            for (JsonNode child : value) values.add(child.isNull() ? "" : coercedString(child));
            return String.join(",", values);
        }
        return value.asText();
    }
}
