package ai.local.nalbbun.debug.service;
 
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import ai.local.nalbbun.debug.model.llm.OllamaModelInfo;
import ai.local.nalbbun.debug.model.llm.OllamaModelListResponse;
import ai.local.nalbbun.debug.model.llm.OllamaModelSource;

@Service
public class OllamaModelDiscoveryService {

    private final RestClient restClient;

    public OllamaModelDiscoveryService(
            @Value("${app.ollama.base-url:http://192.168.1.10:11434}") String baseUrl
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public List<OllamaModelInfo> getRunningModels() {
        OllamaModelListResponse response = restClient.get()
                .uri("/api/ps")
                .retrieve()
                .body(OllamaModelListResponse.class);

        return response == null || response.getModels() == null
                ? List.of()
                : response.getModels();
    }

    public List<OllamaModelInfo> getInstalledModels() {
        OllamaModelListResponse response = restClient.get()
                .uri("/api/tags")
                .retrieve()
                .body(OllamaModelListResponse.class);

        return response == null || response.getModels() == null
                ? List.of()
                : response.getModels();
    }

    public List<OllamaModelInfo> getModels(OllamaModelSource source) {
        if (source == OllamaModelSource.RUNNING) {
            return getRunningModels();
        }
        return getInstalledModels();
    }
}