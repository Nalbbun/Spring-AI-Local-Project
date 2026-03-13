package ai.local.nalbbun.rag.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import ai.local.nalbbun.model.category.ChatCategory;

@Component
public class RagMetadataSupport {

    private static final Pattern NON_ALLOWED = Pattern.compile("[^a-z0-9._-]");
    private static final Pattern MULTI_DASH = Pattern.compile("-{2,}");

    public String normalizeSource(String value) {
        return normalizeKey(value);
    }

    public String normalizeVersion(String value) {
        return normalizeKey(value);
    }

    public String normalizeKey(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replace(' ', '-')
                .replace('_', '-')
                .replace('/', '-')
                .replace('\\', '-');
        normalized = NON_ALLOWED.matcher(normalized).replaceAll("");
        normalized = MULTI_DASH.matcher(normalized).replaceAll("-");
        return trimDash(normalized);
    }

    public String escapeFilterValue(String value) {
        return value == null ? "" : value.replace("'", "\\'");
    }

    public String buildFilterExpression(ChatCategory category, String sourceFilter, String versionFilter) {
        List<String> clauses = new ArrayList<>();
        clauses.add("category == '" + escapeFilterValue(category.name()) + "'");
        String sourceKey = normalizeSource(sourceFilter);
        if (!sourceKey.isBlank()) {
            clauses.add("sourceKey == '" + escapeFilterValue(sourceKey) + "'");
        }
        String versionKey = normalizeVersion(versionFilter);
        if (!versionKey.isBlank()) {
            clauses.add("versionKey == '" + escapeFilterValue(versionKey) + "'");
        }
        return String.join(" && ", clauses);
    }

    public String displayOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String trimDash(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '-') start++;
        while (end > start && value.charAt(end - 1) == '-') end--;
        return value.substring(start, end);
    }
}
