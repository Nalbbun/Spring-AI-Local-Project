package ai.local.nalbbun.debug.model.llm;

import lombok.Data;

@Data
public class DebugOllamaModelConfig {

    private String modelSource;       // RUNNING / INSTALLED

    private String generalModel;
    private String devModel;
    private String miceModel;

    private String travelSearchModel; // tool calling 쓰는 쪽
    private String travelPlanModel;   // 일정 생성용

    // 쉼표/개행 구분 상주 모델 목록
    private String residentModelList;

    // 예: -1, 24h, 10m
    private String residentKeepAlive;
}