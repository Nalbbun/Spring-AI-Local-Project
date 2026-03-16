package ai.local.nalbbun.debug.service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.debug.model.llm.DebugOllamaModelConfig;
import ai.local.nalbbun.debug.model.llm.OllamaModelSource;

@Service
public class DebugRuntimeModelConfigService {

    private final OllamaModelSource defaultModelSource;
    private final String defaultGeneralModel;
    private final String defaultDevModel;
    private final String defaultMiceModel;
    private final String defaultTravelSearchModel;
    private final String defaultTravelPlanModel;
    private final String defaultResidentModels;
    private final String defaultResidentKeepAlive;
    private final boolean defaultAutoWarmupWhenNoRunningModels;

    private final AtomicReference<OllamaModelSource> modelSource;
    private final AtomicReference<String> generalModel = new AtomicReference<>("");
    private final AtomicReference<String> devModel = new AtomicReference<>("");
    private final AtomicReference<String> miceModel = new AtomicReference<>("");
    private final AtomicReference<String> travelSearchModel = new AtomicReference<>("");
    private final AtomicReference<String> travelPlanModel = new AtomicReference<>("");
    private final AtomicReference<String> residentModels = new AtomicReference<>("");
    private final AtomicReference<String> residentKeepAlive = new AtomicReference<>("-1");
    private final AtomicReference<Boolean> autoWarmupWhenNoRunningModels = new AtomicReference<>(true);

    public DebugRuntimeModelConfigService(
            @Value("${app.ollama.model-source:RUNNING}") String modelSource,
            @Value("${app.ollama.default-general-model:}") String generalModel,
            @Value("${app.ollama.default-dev-model:}") String devModel,
            @Value("${app.ollama.default-mice-model:}") String miceModel,
            @Value("${app.ollama.default-travel-search-model:}") String travelSearchModel,
            @Value("${app.ollama.default-travel-plan-model:}") String travelPlanModel,
            @Value("${app.ollama.resident-models:}") String residentModels,
            @Value("${app.ollama.resident-keep-alive:-1}") String residentKeepAlive,
            @Value("${app.ollama.auto-warmup-when-no-running-models:true}") boolean autoWarmupWhenNoRunningModels
    ) {
        this.defaultModelSource = safeSource(modelSource);
        this.defaultGeneralModel = safe(generalModel);
        this.defaultDevModel = safe(devModel);
        this.defaultMiceModel = safe(miceModel);
        this.defaultTravelSearchModel = safe(travelSearchModel);
        this.defaultTravelPlanModel = safe(travelPlanModel);
        this.defaultResidentModels = normalizeResidentModels(residentModels);
        this.defaultResidentKeepAlive = safeKeepAlive(residentKeepAlive);
        this.defaultAutoWarmupWhenNoRunningModels = autoWarmupWhenNoRunningModels;

        this.modelSource = new AtomicReference<>(this.defaultModelSource);
        this.generalModel.set(this.defaultGeneralModel);
        this.devModel.set(this.defaultDevModel);
        this.miceModel.set(this.defaultMiceModel);
        this.travelSearchModel.set(this.defaultTravelSearchModel);
        this.travelPlanModel.set(this.defaultTravelPlanModel);
        this.residentModels.set(this.defaultResidentModels);
        this.residentKeepAlive.set(this.defaultResidentKeepAlive);
        this.autoWarmupWhenNoRunningModels.set(this.defaultAutoWarmupWhenNoRunningModels);
    }

    public DebugOllamaModelConfig getCurrentConfig() {
        DebugOllamaModelConfig config = new DebugOllamaModelConfig();
        config.setModelSource(modelSource.get().name());
        config.setGeneralModel(generalModel.get());
        config.setDevModel(devModel.get());
        config.setMiceModel(miceModel.get());
        config.setTravelSearchModel(travelSearchModel.get());
        config.setTravelPlanModel(travelPlanModel.get());
        config.setResidentModels(residentModels.get());
        config.setResidentKeepAlive(residentKeepAlive.get());
        config.setAutoWarmupWhenNoRunningModels(autoWarmupWhenNoRunningModels.get());
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
        if (request.getResidentModels() != null) {
            residentModels.set(normalizeResidentModels(request.getResidentModels()));
        }
        if (request.getResidentKeepAlive() != null) {
            residentKeepAlive.set(safeKeepAlive(request.getResidentKeepAlive()));
        }
        if (request.getAutoWarmupWhenNoRunningModels() != null) {
            autoWarmupWhenNoRunningModels.set(request.getAutoWarmupWhenNoRunningModels());
        }

        return getCurrentConfig();
    }

    public DebugOllamaModelConfig reset() {
        modelSource.set(defaultModelSource);
        generalModel.set(defaultGeneralModel);
        devModel.set(defaultDevModel);
        miceModel.set(defaultMiceModel);
        travelSearchModel.set(defaultTravelSearchModel);
        travelPlanModel.set(defaultTravelPlanModel);
        residentModels.set(defaultResidentModels);
        residentKeepAlive.set(defaultResidentKeepAlive);
        autoWarmupWhenNoRunningModels.set(defaultAutoWarmupWhenNoRunningModels);
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

    public String getResidentModels() {
        return residentModels.get();
    }

    public List<String> getResidentModelList() {
        String raw = residentModels.get();
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("[\\r\\n,]+"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    public String getResidentKeepAlive() {
        return residentKeepAlive.get();
    }

    public boolean isAutoWarmupWhenNoRunningModels() {
        return autoWarmupWhenNoRunningModels.get();
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

    private String safeKeepAlive(String value) {
        String normalized = safe(value);
        return normalized.isBlank() ? "-1" : normalized;
    }

    private String normalizeResidentModels(String value) {
        return getNormalizedResidentModels(value);
    }

    private String getNormalizedResidentModels(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Arrays.stream(value.split("[\\r\\n,]+"))
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .distinct()
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }
}
