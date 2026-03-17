package ai.local.nalbbun.rag.model;

import java.util.List;

import lombok.Builder;

/**
 * RagSourceVersionCompareResult는 계층 간에 전달되는 입력 및 출력 데이터를 표현하는 모델이다.
 * <p>주요 기능: rag source version compare result 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 * @param category 대상 카테고리 정보
 * @param source source 값
 * @param leftVersion leftVersion 값
 * @param rightVersion rightVersion 값
 * @param left left 값
 * @param right right 값
 * @param leftVectorRows leftVectorRows 값
 * @param rightVectorRows rightVectorRows 값
 * @param query 사용자 입력 또는 질의 내용
 * @param leftHits leftHits 값
 * @param rightHits rightHits 값
 * @param summary summary 값
 */
@Builder
public record RagSourceVersionCompareResult(
        String category,
        String source,
        String leftVersion,
        String rightVersion,
        RagSourceManifest left,
        RagSourceManifest right,
        int leftVectorRows,
        int rightVectorRows,
        String query,
        List<RagRetrievedDocument> leftHits,
        List<RagRetrievedDocument> rightHits,
        String summary
) {
}
