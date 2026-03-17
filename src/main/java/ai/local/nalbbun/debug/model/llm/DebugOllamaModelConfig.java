package ai.local.nalbbun.debug.model.llm;

import lombok.Data;

/**
 * DebugOllamaModelConfig는 애플리케이션 설정과 빈 구성을 담당하는 설정 타입이다.
 * <p>주요 기능: debug ollama model config 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Data
public class DebugOllamaModelConfig {

    /** modelSource 값을 보관한다. */
    private String modelSource;       // RUNNING / INSTALLED

    /** generalModel 값을 보관한다. */
    private String generalModel;
    /** devModel 값을 보관한다. */
    private String devModel;
    /** miceModel 값을 보관한다. */
    private String miceModel;

    /** travelSearchModel 값을 보관한다. */
    private String travelSearchModel; // tool calling 쓰는 쪽
    /** travelPlanModel 값을 보관한다. */
    private String travelPlanModel;   // 일정 생성용

    // 쉼표/개행 구분 상주 모델 목록
    /** residentModelList 값을 보관한다. */
    private String residentModelList;

    // 예: -1, 24h, 10m
    /** residentKeepAlive 값을 보관한다. */
    private String residentKeepAlive;
}