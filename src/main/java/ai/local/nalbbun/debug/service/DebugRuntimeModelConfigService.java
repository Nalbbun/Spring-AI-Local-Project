package ai.local.nalbbun.debug.service;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.debug.model.llm.DebugOllamaModelConfig;
import ai.local.nalbbun.debug.model.llm.OllamaModelSource;

/**
 * Debug Runtime Model Config Service 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
@Service
public class DebugRuntimeModelConfigService {

    private final AtomicReference<OllamaModelSource> modelSource;
    private final AtomicReference<String> generalModel = new AtomicReference<>("");
    private final AtomicReference<String> devModel = new AtomicReference<>("");
    private final AtomicReference<String> miceModel = new AtomicReference<>("");
    private final AtomicReference<String> travelSearchModel = new AtomicReference<>("");
    private final AtomicReference<String> travelPlanModel = new AtomicReference<>("");
    private final AtomicReference<String> residentModelList = new AtomicReference<>("");
    private final AtomicReference<String> residentKeepAlive = new AtomicReference<>("24h");

    /**
     * Debug Runtime Model Config Service 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public DebugRuntimeModelConfigService(
            @Value("${app.ollama.model-source:RUNNING}") String modelSource,
            @Value("${app.ollama.default-general-model:}") String generalModel,
            @Value("${app.ollama.default-dev-model:}") String devModel,
            @Value("${app.ollama.default-mice-model:}") String miceModel,
            @Value("${app.ollama.default-travel-search-model:}") String travelSearchModel,
            @Value("${app.ollama.default-travel-plan-model:}") String travelPlanModel,
            @Value("${app.ollama.resident-model-list:}") String residentModelList,
            @Value("${app.ollama.resident-keep-alive:24h}") String residentKeepAlive
    ) {
        this.modelSource = new AtomicReference<>(safeSource(modelSource));
        this.generalModel.set(safe(generalModel));
        this.devModel.set(safe(devModel));
        this.miceModel.set(safe(miceModel));
        this.travelSearchModel.set(safe(travelSearchModel));
        this.travelPlanModel.set(safe(travelPlanModel));
        this.residentModelList.set(safe(residentModelList));
        this.residentKeepAlive.set(defaultIfBlank(residentKeepAlive, "24h"));
    }

    /**
     * Current Config 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public DebugOllamaModelConfig getCurrentConfig() {
        DebugOllamaModelConfig config = new DebugOllamaModelConfig();
        config.setModelSource(modelSource.get().name());
        config.setGeneralModel(generalModel.get());
        config.setDevModel(devModel.get());
        config.setMiceModel(miceModel.get());
        config.setTravelSearchModel(travelSearchModel.get());
        config.setTravelPlanModel(travelPlanModel.get());
        config.setResidentModelList(residentModelList.get());
        config.setResidentKeepAlive(residentKeepAlive.get());
        return config;
    }

    /**
     * update 작업을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public DebugOllamaModelConfig update(DebugOllamaModelConfig request) {
        if (request == null) {
            return getCurrentConfig();
        }

        if (hasText(request.getModelSource())) {
            modelSource.set(safeSource(request.getModelSource()));
        }
        if (request.getGeneralModel() != null) {
            generalModel.set(safe(request.getGeneralModel()));
        }
        if (request.getDevModel() != null) {
            devModel.set(safe(request.getDevModel()));
        }
        if (request.getMiceModel() != null) {
            miceModel.set(safe(request.getMiceModel()));
        }
        if (request.getTravelSearchModel() != null) {
            travelSearchModel.set(safe(request.getTravelSearchModel()));
        }
        if (request.getTravelPlanModel() != null) {
            travelPlanModel.set(safe(request.getTravelPlanModel()));
        }
        if (request.getResidentModelList() != null) {
            residentModelList.set(safe(request.getResidentModelList()));
        }
        if (request.getResidentKeepAlive() != null) {
            residentKeepAlive.set(defaultIfBlank(request.getResidentKeepAlive(), "24h"));
        }

        return getCurrentConfig();
    }

    /**
     * reset 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public DebugOllamaModelConfig reset() {
        modelSource.set(OllamaModelSource.RUNNING);
        generalModel.set("");
        devModel.set("");
        miceModel.set("");
        travelSearchModel.set("");
        travelPlanModel.set("");
        residentModelList.set("");
        residentKeepAlive.set("24h");
        return getCurrentConfig();
    }

    /**
     * Model Source 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public OllamaModelSource getModelSource() {
        return modelSource.get();
    }

    /**
     * General Model 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String getGeneralModel() {
        return generalModel.get();
    }

    /**
     * Dev Model 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String getDevModel() {
        return devModel.get();
    }

    /**
     * Mice Model 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String getMiceModel() {
        return miceModel.get();
    }

    /**
     * Travel Search Model 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String getTravelSearchModel() {
        return travelSearchModel.get();
    }

    /**
     * Travel Plan Model 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String getTravelPlanModel() {
        return travelPlanModel.get();
    }

    /**
     * Resident Model List 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String getResidentModelList() {
        return residentModelList.get();
    }

    /**
     * Resident Keep Alive 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String getResidentKeepAlive() {
        return residentKeepAlive.get();
    }

    /**
     * safe Source 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private OllamaModelSource safeSource(String value) {
        try {
            return OllamaModelSource.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return OllamaModelSource.RUNNING;
        }
    }

    /**
     * has Text 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * safe 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * default If Blank 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String defaultIfBlank(String value, String defaultValue) {
        String safeValue = safe(value);
        return safeValue.isBlank() ? defaultValue : safeValue;
    }
}