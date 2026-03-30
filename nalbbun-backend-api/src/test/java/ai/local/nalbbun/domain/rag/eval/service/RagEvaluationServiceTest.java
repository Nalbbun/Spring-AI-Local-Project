package ai.local.nalbbun.domain.rag.eval.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import tools.jackson.databind.json.JsonMapper;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.config.rag.RagProperties;
import ai.local.nalbbun.domain.rag.eval.model.RagEvaluationCase;
import ai.local.nalbbun.domain.rag.eval.model.RagEvaluationReport;
import ai.local.nalbbun.domain.rag.model.RagRetrievedDocument;
import ai.local.nalbbun.domain.rag.retrieve.RagDocumentRetriever;
import ai.local.nalbbun.domain.rag.service.RagMetadataSupport;

/**
 * Rag Evaluation Service Test 타입이다.
 *
 * <p>기능 설명: 대상 컴포넌트의 기대 동작과 회귀 여부를 검증한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 테스트 데이터, 목 객체, 검증 조건</p>
 * <p>출력: 검증 결과와 회귀 방지용 보장</p>
 */
class RagEvaluationServiceTest {
    /**
     * Evaluate Dataset Against Expected Source And Version 기대 동작을 검증한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
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
