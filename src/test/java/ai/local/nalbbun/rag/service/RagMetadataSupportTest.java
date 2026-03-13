package ai.local.nalbbun.rag.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ai.local.nalbbun.model.category.ChatCategory;

class RagMetadataSupportTest {
    private final RagMetadataSupport support = new RagMetadataSupport();

    @Test
    void shouldNormalizeSourceAndVersionForFiltering() {
        assertEquals("project-readme", support.normalizeSource("Project README"));
        assertEquals("v1.0-kr", support.normalizeVersion("V1.0 / KR"));
    }

    @Test
    void shouldBuildCategorySourceVersionFilterExpression() {
        String expr = support.buildFilterExpression(ChatCategory.DEV, "Project README", "v1");
        assertTrue(expr.contains("category == 'DEV'"));
        assertTrue(expr.contains("sourceKey == 'project-readme'"));
        assertTrue(expr.contains("versionKey == 'v1'"));
    }
}
