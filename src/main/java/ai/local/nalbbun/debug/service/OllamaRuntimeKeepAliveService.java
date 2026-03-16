package ai.local.nalbbun.debug.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import ai.local.nalbbun.debug.model.llm.DebugOllamaWarmupResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaRuntimeKeepAliveService {

    private final DebugRuntimeOllamaConnectionService ollamaConnectionService;
    private final DebugRuntimeModelConfigService modelConfigService;

    public DebugOllamaWarmupResult warmupConfiguredResidentModels() {
        return warmupModels(modelConfigService.getResidentModelList(), modelConfigService.getResidentKeepAlive());
    }

    public DebugOllamaWarmupResult warmupModels(List<String> models, String keepAlive) {
        DebugOllamaWarmupResult result = new DebugOllamaWarmupResult();
        result.setBaseUrl(ollamaConnectionService.getBaseUrl());
        result.setKeepAlive((keepAlive == null || keepAlive.isBlank()) ? "-1" : keepAlive.trim());
        result.getRequestedModels().addAll(models);

        if (models == null || models.isEmpty()) {
            result.setApplied(false);
            result.setMessage("상주 모델이 설정되지 않았습니다.");
            return result;
        }

        for (String model : models) {
            try {
                warmupModel(model, result.getKeepAlive());
                result.getWarmedModels().add(model);
            } catch (Exception e) {
                result.getFailedModels().add(model);
                log.warn("Resident model warmup failed. model={}, baseUrl={}, reason={}", model, result.getBaseUrl(), e.getMessage());
            }
        }

        result.setApplied(!result.getWarmedModels().isEmpty());
        if (result.getFailedModels().isEmpty()) {
            result.setMessage("상주 모델 로드 완료");
        } else if (result.getWarmedModels().isEmpty()) {
            result.setMessage("상주 모델 로드 실패");
        } else {
            result.setMessage("일부 상주 모델만 로드 완료");
        }
        return result;
    }

    private void warmupModel(String model, String keepAlive) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("prompt", "");
            body.put("stream", false);
            body.put("keep_alive", keepAlive);
            restClient().post()
                    .uri("/api/generate")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return;
        } catch (Exception generateFailure) {
            log.debug("Generate warmup fallback to embed. model={}, reason={}", model, generateFailure.getMessage());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", "warmup");
        body.put("keep_alive", keepAlive);
        restClient().post()
                .uri("/api/embed")
                .body(body)
                .retrieve()
                .body(String.class);
    }

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl(ollamaConnectionService.getBaseUrl())
                .build();
    }
}
