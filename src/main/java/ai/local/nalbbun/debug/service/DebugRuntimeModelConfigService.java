package ai.local.nalbbun.debug.service;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.debug.model.llm.DebugOllamaModelConfig;
import ai.local.nalbbun.debug.model.llm.OllamaModelSource;

@Service
public class DebugRuntimeModelConfigService {

    private final AtomicReference<OllamaModelSource> modelSource;
    private final AtomicReference<String> generalModel = new AtomicReference<>("");
    private final AtomicReference<String> devModel = new AtomicReference<>("");
    private final AtomicReference<String> miceModel = new AtomicReference<>("");
    private final AtomicReference<String> travelSearchModel = new AtomicReference<>("");
    private final AtomicReference<String> travelPlanModel = new AtomicReference<>("");

    public DebugRuntimeModelConfigService(
            @Value("${app.ollama.model-source:RUNNING}") String modelSource,
            @Value("${app.ollama.default-general-model:}") String generalModel,
            @Value("${app.ollama.default-dev-model:}") String devModel,
            @Value("${app.ollama.default-mice-model:}") String miceModel,
            @Value("${app.ollama.default-travel-search-model:}") String travelSearchModel,
            @Value("${app.ollama.default-travel-plan-model:}") String travelPlanModel
    ) {
        this.modelSource = new AtomicReference<>(safeSource(modelSource));
        this.generalModel.set(safe(generalModel));
        this.devModel.set(safe(devModel));
        this.miceModel.set(safe(miceModel));
        this.travelSearchModel.set(safe(travelSearchModel));
        this.travelPlanModel.set(safe(travelPlanModel));
    }

    public DebugOllamaModelConfig getCurrentConfig() {
        DebugOllamaModelConfig config = new DebugOllamaModelConfig();
        config.setModelSource(modelSource.get().name());
        config.setGeneralModel(generalModel.get());
        config.setDevModel(devModel.get());
        config.setMiceModel(miceModel.get());
        config.setTravelSearchModel(travelSearchModel.get());
        config.setTravelPlanModel(travelPlanModel.get());
        return config;
    }

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

        return getCurrentConfig();
    }

    public DebugOllamaModelConfig reset() {
        modelSource.set(OllamaModelSource.RUNNING);
        generalModel.set("");
        devModel.set("");
        miceModel.set("");
        travelSearchModel.set("");
        travelPlanModel.set("");
        return getCurrentConfig();
    }

    public OllamaModelSource getModelSource() {
        return modelSource.get();
    }

    public String getGeneralModel() {
        return generalModel.get();
    }

    public String getDevModel() {
        return devModel.get();
    }

    public String getMiceModel() {
        return miceModel.get();
    }

    public String getTravelSearchModel() {
        return travelSearchModel.get();
    }

    public String getTravelPlanModel() {
        return travelPlanModel.get();
    }

    private OllamaModelSource safeSource(String value) {
        try {
            return OllamaModelSource.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return OllamaModelSource.RUNNING;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}