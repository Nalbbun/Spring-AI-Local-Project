package ai.local.nalbbun.rag.eval.model;

import java.util.List;

import lombok.Builder;

/**
 * RagEvaluationCaseResult는 계층 간에 전달되는 입력 및 출력 데이터를 표현하는 모델이다.
 * <p>주요 기능: rag evaluation case result 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 * @param id 식별자 값
 * @param passed passed 값
 * @param hitCount hitCount 값
 * @param actualSources actualSources 값
 * @param actualVersions actualVersions 값
 * @param detail detail 값
 */
@Builder
public record RagEvaluationCaseResult(String id, boolean passed, int hitCount, List<String> actualSources, List<String> actualVersions, String detail) {}
