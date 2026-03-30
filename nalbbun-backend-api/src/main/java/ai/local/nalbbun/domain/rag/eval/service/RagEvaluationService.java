package ai.local.nalbbun.domain.rag.eval.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.config.rag.RagProperties;
import ai.local.nalbbun.domain.rag.eval.model.RagEvaluationCase;
import ai.local.nalbbun.domain.rag.eval.model.RagEvaluationCaseResult;
import ai.local.nalbbun.domain.rag.eval.model.RagEvaluationReport;
import ai.local.nalbbun.domain.rag.model.RagRetrievedDocument;
import ai.local.nalbbun.domain.rag.retrieve.RagDocumentRetriever;
import ai.local.nalbbun.domain.rag.service.RagMetadataSupport;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Rag Evaluation Service 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
@Service
@RequiredArgsConstructor
public class RagEvaluationService {
    private final RagProperties ragProperties;
    private final RagDocumentRetriever ragDocumentRetriever;
    private final RagMetadataSupport ragMetadataSupport;
    private final ResourceLoader resourceLoader;
    private final JsonMapper jsonMapper;

    /**
     * run Default Dataset 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public RagEvaluationReport runDefaultDataset() {
        String location = ragProperties.getEvaluation().getDatasetLocation();
        return evaluateCases(location, loadDataset(location));
    }

    /**
     * load Dataset 데이터를 로드한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public List<RagEvaluationCase> loadDataset(String location) {
        try {
            Resource resource = resourceLoader.getResource(location);
            if (!resource.exists()) throw new IllegalArgumentException("RAG evaluation dataset not found: " + location);
            try (InputStream is = resource.getInputStream()) {
                return jsonMapper.readValue(is, new TypeReference<List<RagEvaluationCase>>(){});
            }
        } catch (IOException e) {
            throw new IllegalStateException("RAG evaluation dataset load failed: " + location, e);
        }
    }

    /**
     * evaluate Cases 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public RagEvaluationReport evaluateCases(String datasetLocation, List<RagEvaluationCase> cases) {
        if (cases == null || cases.isEmpty()) return RagEvaluationReport.builder().datasetLocation(datasetLocation).totalCases(0).passedCases(0).passRate(0d).thresholdPassed(false).results(List.of()).build();
        List<RagEvaluationCaseResult> results = cases.stream().map(this::evaluateCase).toList();
        int passed = (int) results.stream().filter(RagEvaluationCaseResult::passed).count();
        double passRate = (double) passed / results.size();
        return RagEvaluationReport.builder().datasetLocation(datasetLocation).totalCases(results.size()).passedCases(passed).passRate(passRate).thresholdPassed(passRate >= ragProperties.getEvaluation().getMinPassRate()).results(results).build();
    }

    /**
     * evaluate Case 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private RagEvaluationCaseResult evaluateCase(RagEvaluationCase testCase) {
        List<RagRetrievedDocument> hits = ragDocumentRetriever.retrieve(testCase.getCategory(), testCase.getQuery(), testCase.getSource(), testCase.getVersion());
        boolean hitCountOk = hits.size() >= Math.max(testCase.getMinHits(), 1);
        boolean sourceOk = matchesExpected(hits.stream().map(RagRetrievedDocument::source).filter(Objects::nonNull).toList(), normalizeList(testCase.getExpectedSources()), true);
        boolean versionOk = matchesExpected(hits.stream().map(RagRetrievedDocument::version).filter(Objects::nonNull).toList(), normalizeList(testCase.getExpectedVersions()), false);
        return RagEvaluationCaseResult.builder()
                .id(testCase.getId())
                .passed(hitCountOk && sourceOk && versionOk)
                .hitCount(hits.size())
                .actualSources(hits.stream().map(RagRetrievedDocument::source).distinct().toList())
                .actualVersions(hits.stream().map(RagRetrievedDocument::version).distinct().toList())
                .detail("hits=" + hits.size() + ", sourceFilter=" + blankToDash(testCase.getSource()) + ", versionFilter=" + blankToDash(testCase.getVersion()))
                .build();
    }

    /**
     * matches Expected 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private boolean matchesExpected(List<String> actualValues, List<String> expectedValues, boolean source) {
        if (expectedValues == null || expectedValues.isEmpty()) return true;
        List<String> normalizedActual = actualValues == null ? Collections.emptyList() : actualValues.stream().map(v -> source ? ragMetadataSupport.normalizeSource(v) : ragMetadataSupport.normalizeVersion(v)).toList();
        return expectedValues.stream().anyMatch(normalizedActual::contains);
    }

    /**
     * normalize List 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private List<String> normalizeList(List<String> values) {
        if (values == null) return List.of();
        return values.stream().filter(Objects::nonNull).map(v -> ragMetadataSupport.normalizeKey(v.trim().toLowerCase(Locale.ROOT))).filter(v -> !v.isBlank()).toList();
    }

    /**
     * blank To Dash 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String blankToDash(String value) { return value == null || value.isBlank() ? "-" : value; }
}
