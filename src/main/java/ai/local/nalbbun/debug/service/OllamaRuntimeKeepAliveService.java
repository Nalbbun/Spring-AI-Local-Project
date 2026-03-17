package ai.local.nalbbun.debug.service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import ai.local.nalbbun.debug.model.llm.DebugOllamaWarmupResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OllamaRuntimeKeepAliveService는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: ollama runtime keep alive service 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaRuntimeKeepAliveService {

    /** debugRuntimeOllamaConnectionService 값을 보관한다. */
    private final DebugRuntimeOllamaConnectionService debugRuntimeOllamaConnectionService;
    /** debugRuntimeModelConfigService 값을 보관한다. */
    private final DebugRuntimeModelConfigService debugRuntimeModelConfigService;

    /**
     * warmupConfiguredResidentModels 기능을 수행한다.
     * @return DebugOllamaWarmupResult 타입의 처리 결과
     */
    public DebugOllamaWarmupResult warmupConfiguredResidentModels() {
    	return warmupModels(
    	        parseResidentModels(debugRuntimeModelConfigService.getResidentModelList()),
    	        debugRuntimeModelConfigService.getResidentKeepAlive()
    	);
    }

    /**
     * warmupModels 기능을 수행한다.
     *
     * @param models models 목록 정보
     * @param keepAlive keepAlive 값
     * @return DebugOllamaWarmupResult 타입의 처리 결과
     */
    public DebugOllamaWarmupResult warmupModels(List<String> models, String keepAlive) {
        DebugOllamaWarmupResult result = new DebugOllamaWarmupResult();
        result.setBaseUrl(debugRuntimeOllamaConnectionService.getBaseUrl());
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

    /**
     * warmupModel 기능을 수행한다.
     *
     * @param model 대상 모델 이름
     * @param keepAlive keepAlive 값
     */
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

    /**
     * restClient 기능을 수행한다.
     * @return RestClient 타입의 처리 결과
     */
    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl(debugRuntimeOllamaConnectionService.getBaseUrl())
                .build();
    }
    
    /**
     * 입력 데이터를 파싱하여 구조화한다.
     *
     * @param value value 값
     * @return 조회 또는 생성된 목록
     */
    private List<String> parseResidentModels(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }
}
