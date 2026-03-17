package ai.local.nalbbun.rag.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ai.local.nalbbun.model.category.ChatCategory;

/**
 * RagMetadataSupportTest는 대상 기능의 동작을 검증하는 테스트 클래스이다.
 * <p>주요 기능: rag metadata support test 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
class RagMetadataSupportTest {
    /** support 값을 보관한다. */
    private final RagMetadataSupport support = new RagMetadataSupport();

    /**
     * 대상 기능의 동작을 검증한다.
     */
    @Test
    void shouldNormalizeSourceAndVersionForFiltering() {
        assertEquals("project-readme", support.normalizeSource("Project README"));
        assertEquals("v1.0-kr", support.normalizeVersion("V1.0 / KR"));
    }

    /**
     * 대상 기능의 동작을 검증한다.
     */
    @Test
    void shouldBuildCategorySourceVersionFilterExpression() {
        String expr = support.buildFilterExpression(ChatCategory.DEV, "Project README", "v1");
        assertTrue(expr.contains("category == 'DEV'"));
        assertTrue(expr.contains("sourceKey == 'project-readme'"));
        assertTrue(expr.contains("versionKey == 'v1'"));
    }
}
