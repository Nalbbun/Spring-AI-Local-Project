package ai.local.nalbbun.admin.model.llm;

import lombok.Data;

/**
 * Debug Ollama Model Config 타입이다.
 *
 * <p>기능 설명: 스프링 빈과 런타임 설정을 구성한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 프로퍼티 값, 환경 변수, 스프링 컨텍스트 정보</p>
 * <p>출력: 빈 등록 결과 또는 런타임 설정 상태</p>
 */
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