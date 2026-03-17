package ai.local.nalbbun.debug.service;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.debug.model.llm.DebugOllamaModelConfig;
import ai.local.nalbbun.debug.model.llm.OllamaModelSource;

/**
 * DebugRuntimeModelConfigService는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: debug runtime model config service 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Service
public class DebugRuntimeModelConfigService {

    /** modelSource 값을 보관한다. */
    private final AtomicReference<OllamaModelSource> modelSource;
    /** generalModel 값을 보관한다. */
    private final AtomicReference<String> generalModel = new AtomicReference<>("");
    /** devModel 값을 보관한다. */
    private final AtomicReference<String> devModel = new AtomicReference<>("");
    /** miceModel 값을 보관한다. */
    private final AtomicReference<String> miceModel = new AtomicReference<>("");
    /** travelSearchModel 값을 보관한다. */
    private final AtomicReference<String> travelSearchModel = new AtomicReference<>("");
    /** travelPlanModel 값을 보관한다. */
    private final AtomicReference<String> travelPlanModel = new AtomicReference<>("");
    /** residentModelList 값을 보관한다. */
    private final AtomicReference<String> residentModelList = new AtomicReference<>("");
    /** residentKeepAlive 값을 보관한다. */
    private final AtomicReference<String> residentKeepAlive = new AtomicReference<>("24h");

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param modelSource modelSource 값
     * @param generalModel generalModel 값
     * @param devModel devModel 값
     * @param miceModel miceModel 값
     * @param travelSearchModel travelSearchModel 값
     * @param travelPlanModel travelPlanModel 값
     * @param residentModelList residentModelList 값
     * @param residentKeepAlive residentKeepAlive 값
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
     * 지정된 정보를 조회한다.
     * @return DebugOllamaModelConfig 타입의 처리 결과
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
     * 대상 값을 갱신한다.
     *
     * @param request HTTP 요청 객체
     * @return DebugOllamaModelConfig 타입의 처리 결과
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
     * @return DebugOllamaModelConfig 타입의 처리 결과
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
     * 지정된 정보를 조회한다.
     * @return OllamaModelSource 타입의 처리 결과
     */
    public OllamaModelSource getModelSource() {
        return modelSource.get();
    }

    /**
     * 지정된 정보를 조회한다.
     * @return 처리 결과 문자열
     */
    public String getGeneralModel() {
        return generalModel.get();
    }

    /**
     * 지정된 정보를 조회한다.
     * @return 처리 결과 문자열
     */
    public String getDevModel() {
        return devModel.get();
    }

    /**
     * 지정된 정보를 조회한다.
     * @return 처리 결과 문자열
     */
    public String getMiceModel() {
        return miceModel.get();
    }

    /**
     * 지정된 정보를 조회한다.
     * @return 처리 결과 문자열
     */
    public String getTravelSearchModel() {
        return travelSearchModel.get();
    }

    /**
     * 지정된 정보를 조회한다.
     * @return 처리 결과 문자열
     */
    public String getTravelPlanModel() {
        return travelPlanModel.get();
    }

    /**
     * 지정된 정보를 조회한다.
     * @return 처리 결과 문자열
     */
    public String getResidentModelList() {
        return residentModelList.get();
    }

    /**
     * 지정된 정보를 조회한다.
     * @return 처리 결과 문자열
     */
    public String getResidentKeepAlive() {
        return residentKeepAlive.get();
    }

    /**
     * safeSource 기능을 수행한다.
     *
     * @param value value 값
     * @return OllamaModelSource 타입의 처리 결과
     */
    private OllamaModelSource safeSource(String value) {
        try {
            return OllamaModelSource.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return OllamaModelSource.RUNNING;
        }
    }

    /**
     * 조건 충족 여부를 확인한다.
     *
     * @param value value 값
     * @return 처리 가능 여부 또는 조건 충족 여부
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * safe 기능을 수행한다.
     *
     * @param value value 값
     * @return 처리 결과 문자열
     */
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * defaultIfBlank 기능을 수행한다.
     *
     * @param value value 값
     * @param defaultValue defaultValue 값
     * @return 처리 결과 문자열
     */
    private String defaultIfBlank(String value, String defaultValue) {
        String safeValue = safe(value);
        return safeValue.isBlank() ? defaultValue : safeValue;
    }
}