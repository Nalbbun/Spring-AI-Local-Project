package ai.local.nalbbun.debug.model.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * OllamaModelInfo는 계층 간에 전달되는 입력 및 출력 데이터를 표현하는 모델이다.
 * <p>주요 기능: ollama model info 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaModelInfo {
    /** name 값을 보관한다. */
    private String name;
    /** model 값을 보관한다. */
    private String model;
    /** size 값을 보관한다. */
    private Long size;
    /** state 값을 보관한다. */
    private String state;
    /** displayName 값을 보관한다. */
    private String displayName;
    /** modifiedAt 값을 보관한다. */
    private String modifiedAt;
}
