package ai.local.nalbbun.rag.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import ai.local.nalbbun.category.model.ChatCategory;

/**
 * Rag Metadata Support 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
@Component
public class RagMetadataSupport {

    private static final Pattern NON_ALLOWED = Pattern.compile("[^a-z0-9._-]");
    private static final Pattern MULTI_DASH = Pattern.compile("-{2,}");

    /**
     * normalize Source 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String normalizeSource(String value) {
        return normalizeKey(value);
    }

    /**
     * normalize Version 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String normalizeVersion(String value) {
        return normalizeKey(value);
    }

    /**
     * normalize Key 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * escape Filter Value 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String escapeFilterValue(String value) {
        return value == null ? "" : value.replace("'", "\\'");
    }

    /**
     * build Filter Expression 결과를 구성한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * display Or Dash 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String displayOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /**
     * trim Dash 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String trimDash(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '-') start++;
        while (end > start && value.charAt(end - 1) == '-') end--;
        return value.substring(start, end);
    }
}
