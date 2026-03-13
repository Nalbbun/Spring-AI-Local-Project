package ai.local.nalbbun.rag.eval.model;

import java.util.List;

import lombok.Builder;

@Builder
public record RagEvaluationCaseResult(String id, boolean passed, int hitCount, List<String> actualSources, List<String> actualVersions, String detail) {}
