package ai.local.nalbbun.category.dev.model;

import ai.local.nalbbun.model.common.CategoryContext;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * DevContext는 계층 간에 전달되는 입력 및 출력 데이터를 표현하는 모델이다.
 * <p>주요 기능: dev context 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Data
public class DevContext implements CategoryContext {
    /** taskType 값을 보관한다. */
    private String taskType;
    /** topic 값을 보관한다. */
    private String topic;
    /** stackKeywords 값을 보관한다. */
    private List<String> stackKeywords = new ArrayList<>();
    /** parserMode 값을 보관한다. */
    private String parserMode;
}