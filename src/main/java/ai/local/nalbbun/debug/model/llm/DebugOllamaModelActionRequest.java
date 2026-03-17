package ai.local.nalbbun.debug.model.llm;

import lombok.Data;

@Data
public class DebugOllamaModelActionRequest {
    private String model;
    /**
     * true  -> /api/pull 로 영구 등록만 수행
     * false -> pull 없이 keep_alive 기반으로 PS 로드만 수행
     */
    private Boolean pull;
    private String keepAlive;
}
