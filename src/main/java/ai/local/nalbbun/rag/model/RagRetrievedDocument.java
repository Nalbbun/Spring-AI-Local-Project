package ai.local.nalbbun.rag.model;

import ai.local.nalbbun.model.category.ChatCategory;
import lombok.Builder;

/**
 * RagRetrievedDocument는 계층 간에 전달되는 입력 및 출력 데이터를 표현하는 모델이다.
 * <p>주요 기능: rag retrieved document 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 * @param id 식별자 값
 * @param title title 값
 * @param source source 값
 * @param version version 값
 * @param category 대상 카테고리 정보
 * @param text 본문 또는 텍스트 내용
 * @param score score 값
 */
@Builder
public record RagRetrievedDocument(
        String id,
        String title,
        String source,
        String version,
        ChatCategory category,
        String text,
        Double score
) {
}
