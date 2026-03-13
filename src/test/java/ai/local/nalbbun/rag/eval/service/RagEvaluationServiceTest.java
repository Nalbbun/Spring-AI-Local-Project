package ai.local.nalbbun.rag.eval.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import tools.jackson.databind.json.JsonMapper;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.rag.config.RagProperties;
import ai.local.nalbbun.rag.eval.model.RagEvaluationCase;
import ai.local.nalbbun.rag.eval.model.RagEvaluationReport;
import ai.local.nalbbun.rag.model.RagRetrievedDocument;
import ai.local.nalbbun.rag.retrieve.RagDocumentRetriever;
import ai.local.nalbbun.rag.service.RagMetadataSupport;

class RagEvaluationServiceTest {
    @Test
    void shouldEvaluateDatasetAgainstExpectedSourceAndVersion() {
        RagProperties properties = new RagProperties();
        RagDocumentRetriever retriever = mock(RagDocumentRetriever.class);
        when(retriever.retrieve(eq(ChatCategory.DEV), eq("README 구조와 실행 흐름을 설명해줘"), eq("project-readme"), eq("v1")))
                .thenReturn(List.of(RagRetrievedDocument.builder().id("1").title("README").source("project-readme").version("v1").category(ChatCategory.DEV).text("..." ).score(0.9d).build()));
        RagEvaluationService service = new RagEvaluationService(properties, retriever, new RagMetadataSupport(), new DefaultResourceLoader(), JsonMapper.builder().build());
        RagEvaluationCase testCase = new RagEvaluationCase();
        testCase.setId("dev-readme-v1");
        testCase.setCategory(ChatCategory.DEV);
        testCase.setQuery("README 구조와 실행 흐름을 설명해줘");
        testCase.setSource("project-readme");
        testCase.setVersion("v1");
        testCase.setExpectedSources(List.of("project-readme"));
        testCase.setExpectedVersions(List.of("v1"));
        RagEvaluationReport report = service.evaluateCases("classpath:rag/eval/default-eval-set.json", List.of(testCase));
        assertEquals(1, report.passedCases());
        assertTrue(report.thresholdPassed());
    }
}
