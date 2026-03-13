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
}