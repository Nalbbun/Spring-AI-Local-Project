package ai.local.nalbbun.rag.service;

import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.debug.service.DebugRuntimeOllamaConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuntimeOllamaVectorStoreFactory {

    private final JdbcTemplate jdbcTemplate;
    private final DebugRuntimeOllamaConnectionService ollamaConnectionService;

    @Value("${spring.ai.ollama.embedding.options.model:nomic-embed-text}")
    private String embeddingModelName;

    @Value("${spring.ai.vectorstore.pgvector.initialize-schema:true}")
    private boolean initializeSchema;

    @Value("${spring.ai.vectorstore.pgvector.dimensions:768}")
    private int dimensions;

    public VectorStore create() {
        String baseUrl = ollamaConnectionService.getBaseUrl();
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(baseUrl)
                .build();
        OllamaEmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaEmbeddingOptions.builder()
                        .model(embeddingModelName)
                        .build())
                .build();

        log.debug("Creating runtime PGVector store. baseUrl={}, embeddingModel={}, dimensions={}", baseUrl, embeddingModelName, dimensions);

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(dimensions)
                .initializeSchema(initializeSchema)
                .build();
    }
}
