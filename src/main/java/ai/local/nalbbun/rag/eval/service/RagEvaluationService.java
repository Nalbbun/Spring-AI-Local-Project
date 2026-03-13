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

@Service
@RequiredArgsConstructor
public class RagEvaluationService {
    private final RagProperties ragProperties;
    private final RagDocumentRetriever ragDocumentRetriever;
    private final RagMetadataSupport ragMetadataSupport;
    private final ResourceLoader resourceLoader;
    private final JsonMapper jsonMapper;

    public RagEvaluationReport runDefaultDataset() {
        String location = ragProperties.getEvaluation().getDatasetLocation();
        return evaluateCases(location, loadDataset(location));
    }

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

    public RagEvaluationReport evaluateCases(String datasetLocation, List<RagEvaluationCase> cases) {
        if (cases == null || cases.isEmpty()) return RagEvaluationReport.builder().datasetLocation(datasetLocation).totalCases(0).passedCases(0).passRate(0d).thresholdPassed(false).results(List.of()).build();
        List<RagEvaluationCaseResult> results = cases.stream().map(this::evaluateCase).toList();
        int passed = (int) results.stream().filter(RagEvaluationCaseResult::passed).count();
        double passRate = (double) passed / results.size();
        return RagEvaluationReport.builder().datasetLocation(datasetLocation).totalCases(results.size()).passedCases(passed).passRate(passRate).thresholdPassed(passRate >= ragProperties.getEvaluation().getMinPassRate()).results(results).build();
    }

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

    private boolean matchesExpected(List<String> actualValues, List<String> expectedValues, boolean source) {
        if (expectedValues == null || expectedValues.isEmpty()) return true;
        List<String> normalizedActual = actualValues == null ? Collections.emptyList() : actualValues.stream().map(v -> source ? ragMetadataSupport.normalizeSource(v) : ragMetadataSupport.normalizeVersion(v)).toList();
        return expectedValues.stream().anyMatch(normalizedActual::contains);
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) return List.of();
        return values.stream().filter(Objects::nonNull).map(v -> ragMetadataSupport.normalizeKey(v.trim().toLowerCase(Locale.ROOT))).filter(v -> !v.isBlank()).toList();
    }

    private String blankToDash(String value) { return value == null || value.isBlank() ? "-" : value; }
}
