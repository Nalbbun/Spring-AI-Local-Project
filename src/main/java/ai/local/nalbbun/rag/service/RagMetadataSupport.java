package ai.local.nalbbun.rag.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import ai.local.nalbbun.model.category.ChatCategory;

/**
 * RagMetadataSupport는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: rag metadata support 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class RagMetadataSupport {

    /** NON_ALLOWED 값을 보관한다. */
    private static final Pattern NON_ALLOWED = Pattern.compile("[^a-z0-9._-]");
    /** MULTI_DASH 값을 보관한다. */
    private static final Pattern MULTI_DASH = Pattern.compile("-{2,}");

    /**
     * normalizeSource 기능을 수행한다.
     *
     * @param value value 값
     * @return 처리 결과 문자열
     */
    public String normalizeSource(String value) {
        return normalizeKey(value);
    }

    /**
     * normalizeVersion 기능을 수행한다.
     *
     * @param value value 값
     * @return 처리 결과 문자열
     */
    public String normalizeVersion(String value) {
        return normalizeKey(value);
    }

    /**
     * normalizeKey 기능을 수행한다.
     *
     * @param value value 값
     * @return 처리 결과 문자열
     */
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

    /**
     * escapeFilterValue 기능을 수행한다.
     *
     * @param value value 값
     * @return 처리 결과 문자열
     */
    public String escapeFilterValue(String value) {
        return value == null ? "" : value.replace("'", "\\'");
    }

    /**
     * 필요한 결과 객체를 구성한다.
     *
     * @param category 대상 카테고리 정보
     * @param sourceFilter sourceFilter 값
     * @param versionFilter versionFilter 값
     * @return 처리 결과 문자열
     */
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

    /**
     * displayOrDash 기능을 수행한다.
     *
     * @param value value 값
     * @return 처리 결과 문자열
     */
    public String displayOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /**
     * trimDash 기능을 수행한다.
     *
     * @param value value 값
     * @return 처리 결과 문자열
     */
    private String trimDash(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '-') start++;
        while (end > start && value.charAt(end - 1) == '-') end--;
        return value.substring(start, end);
    }
}
