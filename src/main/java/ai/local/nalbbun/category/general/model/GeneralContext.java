package ai.local.nalbbun.category.general.model;

import ai.local.nalbbun.model.common.CategoryContext;
import lombok.Data;

/**
 * GeneralContext는 계층 간에 전달되는 입력 및 출력 데이터를 표현하는 모델이다.
 * <p>주요 기능: general context 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Data
public class GeneralContext implements CategoryContext {
    /** intent 값을 보관한다. */
    private String intent;
    /** parserMode 값을 보관한다. */
    private String parserMode;
}