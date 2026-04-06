package ai.local.nalbbun.domain.rag.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import ai.local.nalbbun.domain.runtime.port.RuntimeVllmConnectionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class VllmGatewayEmbeddingModel implements EmbeddingModel {

    private final RuntimeVllmConnectionPort connectionPort;
    private final int dimensions;
    private final long connectTimeoutMs;
    private final long readTimeoutMs;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> texts = request == null || request.getInstructions() == null ? List.of() : request.getInstructions();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("texts", texts);
        String requestUrl = connectionPort.getResolvedEmbeddingRequestUrl();
        Map<String, Object> response = restClient().post().uri(requestUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);
        List<float[]> vectors = extractVectors(response);
        List<Embedding> embeddings = new ArrayList<>();
        for (int i = 0; i < vectors.size(); i++) {
            embeddings.add(new Embedding(vectors.get(i), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        if (document == null) {
            return new float[0];
        }
        return embed(getEmbeddingContent(document));
    }

    @Override
    public int dimensions() {
        return dimensions > 0 ? dimensions : EmbeddingModel.super.dimensions();
    }


    private String getEmbeddingContent(Document document) {
        if (document == null) {
            return "";
        }
        for (String methodName : List.of("getFormattedContent", "getText", "getContent")) {
            try {
                Object value = document.getClass().getMethod(methodName).invoke(document);
                if (value instanceof String s && !s.isBlank()) {
                    return s;
                }
            } catch (ReflectiveOperationException ignored) {
                // try next known accessor
            }
        }
        return String.valueOf(document);
    }

    private RestClient restClient() {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(Math.max(1000, connectTimeoutMs))).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(Math.max(1000, readTimeoutMs)));
        RestClient.Builder builder = RestClient.builder().requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        String apiKey = connectionPort.getResolvedApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim());
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private List<float[]> extractVectors(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return List.of();
        }
        Object embeddings = firstNonNull(response.get("embeddings"), response.get("vectors"), response.get("data"), response.get("result"));
        if (embeddings instanceof List<?> list) {
            List<float[]> vectors = new ArrayList<>();
            int idx = 0;
            for (Object item : list) {
                float[] vector = extractSingleVector(item);
                if (vector.length > 0) {
                    vectors.add(vector);
                } else {
                    log.warn("vLLM embedding response item {} could not be parsed. type={}", idx, item == null ? "null" : item.getClass().getName());
                }
                idx++;
            }
            return vectors;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private float[] extractSingleVector(Object item) {
        if (item instanceof List<?> values) {
            return toFloatArray(values);
        }
        if (item instanceof Map<?, ?> map) {
            Object candidate = firstNonNull(map.get("embedding"), map.get("vector"), map.get("values"));
            if (candidate instanceof List<?> values) {
                return toFloatArray(values);
            }
        }
        return new float[0];
    }

    private float[] toFloatArray(List<?> values) {
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            if (value instanceof Number n) {
                result[i] = n.floatValue();
            } else {
                result[i] = 0f;
            }
        }
        return result;
    }

    private Object firstNonNull(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
