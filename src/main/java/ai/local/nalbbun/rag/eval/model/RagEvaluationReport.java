package ai.local.nalbbun.rag.eval.model;

import java.util.List;

import lombok.Builder;

@Builder
public record RagEvaluationReport(String datasetLocation, int totalCases, int passedCases, double passRate, boolean thresholdPassed, List<RagEvaluationCaseResult> results) {}
