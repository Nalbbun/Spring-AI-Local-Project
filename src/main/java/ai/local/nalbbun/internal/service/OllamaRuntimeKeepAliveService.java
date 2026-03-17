package ai.local.nalbbun.internal.service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import ai.local.nalbbun.internal.model.llm.DebugOllamaWarmupResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ollama Runtime Keep Alive Service 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaRuntimeKeepAliveService {

    private final DebugRuntimeOllamaConnectionService debugRuntimeOllamaConnectionService;
    private final DebugRuntimeModelConfigService debugRuntimeModelConfigService;

    /**
     * warmup Configured Resident Models 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public DebugOllamaWarmupResult warmupConfiguredResidentModels() {
    	return warmupModels(
    	        parseResidentModels(debugRuntimeModelConfigService.getResidentModelList()),
    	        debugRuntimeModelConfigService.getResidentKeepAlive()
    	);
    }

    /**
     * warmup Models 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * warmup Model 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     * rest Client 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl(debugRuntimeOllamaConnectionService.getBaseUrl())
                .build();
    }
    
    /**
     * parse Resident Models 처리를 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
