package ai.local.nalbbun.rag.eval.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.rag.config.RagProperties;
import ai.local.nalbbun.rag.eval.model.RagEvaluationCase;
import ai.local.nalbbun.rag.eval.model.RagEvaluationCaseResult;
import ai.local.nalbbun.rag.eval.model.RagEvaluationReport;
import ai.local.nalbbun.rag.model.RagRetrievedDocument;
import ai.local.nalbbun.rag.retrieve.RagDocumentRetriever;
import ai.local.nalbbun.rag.service.RagMetadataSupport;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * RagEvaluationService는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: rag evaluation service 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Service
@RequiredArgsConstructor
public class RagEvaluationService {
    /** ragProperties 값을 보관한다. */
    private final RagProperties ragProperties;
    /** ragDocumentRetriever 값을 보관한다. */
    private final RagDocumentRetriever ragDocumentRetriever;
    /** ragMetadataSupport 값을 보관한다. */
    private final RagMetadataSupport ragMetadataSupport;
    /** resourceLoader 값을 보관한다. */
    private final ResourceLoader resourceLoader;
    /** jsonMapper 값을 보관한다. */
    private final JsonMapper jsonMapper;

    /**
     * 핵심 처리 로직을 실행한다.
     * @return RagEvaluationReport 타입의 처리 결과
     */
    public RagEvaluationReport runDefaultDataset() {
        String location = ragProperties.getEvaluation().getDatasetLocation();
        return evaluateCases(location, loadDataset(location));
    }

    /**
     * 대상 정보를 조회한다.
     *
     * @param location location 값
     * @return 조회 또는 생성된 목록
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
     * evaluateCases 기능을 수행한다.
     *
     * @param datasetLocation datasetLocation 값
     * @param cases cases 목록 정보
     * @return RagEvaluationReport 타입의 처리 결과
     */
    public RagEvaluationReport evaluateCases(String datasetLocation, List<RagEvaluationCase> cases) {
        if (cases == null || cases.isEmpty()) return RagEvaluationReport.builder().datasetLocation(datasetLocation).totalCases(0).passedCases(0).passRate(0d).thresholdPassed(false).results(List.of()).build();
        List<RagEvaluationCaseResult> results = cases.stream().map(this::evaluateCase).toList();
        int passed = (int) results.stream().filter(RagEvaluationCaseResult::passed).count();
        double passRate = (double) passed / results.size();
        return RagEvaluationReport.builder().datasetLocation(datasetLocation).totalCases(results.size()).passedCases(passed).passRate(passRate).thresholdPassed(passRate >= ragProperties.getEvaluation().getMinPassRate()).results(results).build();
    }

    /**
     * evaluateCase 기능을 수행한다.
     *
     * @param testCase testCase 값
     * @return RagEvaluationCaseResult 타입의 처리 결과
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
     * matchesExpected 기능을 수행한다.
     *
     * @param actualValues actualValues 목록 정보
     * @param expectedValues expectedValues 목록 정보
     * @param source source 값
     * @return 처리 가능 여부 또는 조건 충족 여부
     */
    private boolean matchesExpected(List<String> actualValues, List<String> expectedValues, boolean source) {
        if (expectedValues == null || expectedValues.isEmpty()) return true;
        List<String> normalizedActual = actualValues == null ? Collections.emptyList() : actualValues.stream().map(v -> source ? ragMetadataSupport.normalizeSource(v) : ragMetadataSupport.normalizeVersion(v)).toList();
        return expectedValues.stream().anyMatch(normalizedActual::contains);
    }

    /**
     * normalizeList 기능을 수행한다.
     *
     * @param values values 목록 정보
     * @return 조회 또는 생성된 목록
     */
    private List<String> normalizeList(List<String> values) {
        if (values == null) return List.of();
        return values.stream().filter(Objects::nonNull).map(v -> ragMetadataSupport.normalizeKey(v.trim().toLowerCase(Locale.ROOT))).filter(v -> !v.isBlank()).toList();
    }

    /**
     * blankToDash 기능을 수행한다.
     *
     * @param value.isBlank( value.isBlank( 값
     * @return 처리 결과 문자열
     */
    private String blankToDash(String value) { return value == null || value.isBlank() ? "-" : value; }
}
