package ai.local.nalbbun.rag.eval.model;

import java.util.List;

import lombok.Builder;

/**
 * RagEvaluationReport는 계층 간에 전달되는 입력 및 출력 데이터를 표현하는 모델이다.
 * <p>주요 기능: rag evaluation report 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 * @param datasetLocation datasetLocation 값
 * @param totalCases totalCases 값
 * @param passedCases passedCases 값
 * @param passRate passRate 값
 * @param thresholdPassed thresholdPassed 값
 * @param results results 값
 */
@Builder
public record RagEvaluationReport(String datasetLocation, int totalCases, int passedCases, double passRate, boolean thresholdPassed, List<RagEvaluationCaseResult> results) {}
