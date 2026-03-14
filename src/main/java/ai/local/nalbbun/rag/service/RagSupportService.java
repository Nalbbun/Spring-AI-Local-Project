package ai.local.nalbbun.rag.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.rag.config.RagProperties;
import ai.local.nalbbun.rag.model.RagContext;
import ai.local.nalbbun.rag.model.RagRetrievedDocument;
import ai.local.nalbbun.rag.retrieve.RagDocumentRetriever;
import ai.local.nalbbun.rag.trace.DebugRagTraceService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RagSupportService {

    private final RagProperties ragProperties;
    private final RagDocumentRetriever ragDocumentRetriever;
    private final RagPromptComposer ragPromptComposer;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final ObjectProvider<DebugRagTraceService> debugRagTraceServiceProvider;

    public RagContext buildContext(ChatCategory category, String userQuery) {
        return buildContext(category, userQuery, null, null);
    }

    public RagContext buildContext(ChatCategory category, String userQuery, String sourceFilter, String versionFilter) {
        String traceId = startTrace(Map.of(
                "category", category == null ? "-" : category.name(),
                "sourceFilter", safeValue(sourceFilter),
                "versionFilter", safeValue(versionFilter)
        ));

        if (!ragProperties.isEnabled()) {
            warn(traceId, "RAG_CONTEXT", "DISABLED", "RAG 전체 설정 비활성화", Map.of());
            return RagContext.builder()
                    .enabled(false)
                    .applied(false)
                    .reason("disabled-config")
                    .traceId(traceId)
                    .traceMessage("rag=off, reason=disabled-config")
                    .promptBlock("")
                    .documents(List.of())
                    .sourceFilter(safeValue(sourceFilter))
                    .versionFilter(safeValue(versionFilter))
                    .build();
        }
        if (!ragProperties.isCategoryEnabled(category)) {
            warn(traceId, "RAG_CONTEXT", "CATEGORY_DISABLED", "카테고리별 RAG 비활성화", Map.of("category", category.name()));
            return RagContext.builder()
                    .enabled(false)
                    .applied(false)
                    .reason("category-disabled")
                    .traceId(traceId)
                    .traceMessage("rag=off, reason=category-disabled")
                    .promptBlock("")
                    .documents(List.of())
                    .sourceFilter(safeValue(sourceFilter))
                    .versionFilter(safeValue(versionFilter))
                    .build();
        }
        if (vectorStoreProvider.getIfAvailable() == null) {
            warn(traceId, "RAG_CONTEXT", "VECTOR_STORE_UNAVAILABLE", "VectorStore 미연결", Map.of());
            return RagContext.builder()
                    .enabled(false)
                    .applied(false)
                    .reason("vector-store-unavailable")
                    .traceId(traceId)
                    .traceMessage("rag=off, reason=vector-store-unavailable")
                    .promptBlock("")
                    .documents(List.of())
                    .sourceFilter(safeValue(sourceFilter))
                    .versionFilter(safeValue(versionFilter))
                    .build();
        }

        info(traceId, "RAG_CONTEXT", "RETRIEVE_BEGIN", "RAG 문맥 구성 시작", Map.of(
                "category", category.name(),
                "queryLength", userQuery == null ? 0 : userQuery.length(),
                "sourceFilter", safeValue(sourceFilter),
                "versionFilter", safeValue(versionFilter)
        ));

        List<RagRetrievedDocument> documents = ragDocumentRetriever.retrieve(category, userQuery, sourceFilter, versionFilter, traceId);
        if (documents.isEmpty()) {
            warn(traceId, "RAG_CONTEXT", "NO_MATCH", "검색 결과 없음", Map.of());
            return RagContext.builder()
                    .enabled(true)
                    .applied(false)
                    .reason("no-matching-documents")
                    .traceId(traceId)
                    .traceMessage("rag=on, hits=0, reason=no-matching-documents")
                    .promptBlock("")
                    .documents(List.of())
                    .sourceFilter(safeValue(sourceFilter))
                    .versionFilter(safeValue(versionFilter))
                    .build();
        }

        String sources = documents.stream()
                .map(RagRetrievedDocument::source)
                .distinct()
                .collect(Collectors.joining(", "));

        String promptBlock = ragPromptComposer.compose(documents);
        success(traceId, "RAG_CONTEXT", "PROMPT_COMPOSE_COMPLETE", "프롬프트 블록 구성 완료", Map.of(
                "hitCount", documents.size(),
                "sources", sources
        ));

        return RagContext.builder()
                .enabled(true)
                .applied(true)
                .reason("ok")
                .traceId(traceId)
                .documents(documents)
                .promptBlock(promptBlock)
                .traceMessage("rag=on, hits=" + documents.size() + ", sources=" + sources)
                .sourceFilter(safeValue(sourceFilter))
                .versionFilter(safeValue(versionFilter))
                .build();
    }

    private String startTrace(Map<String, Object> details) {
        DebugRagTraceService service = debugRagTraceServiceProvider.getIfAvailable();
        if (service == null) {
            return "";
        }
        return service.startTrace("RAG_CONTEXT", details);
    }

    private void info(String traceId, String operation, String stage, String message, Map<String, Object> details) {
        DebugRagTraceService service = debugRagTraceServiceProvider.getIfAvailable();
        if (service != null) {
            service.info(traceId, operation, stage, message, details);
        }
    }

    private void warn(String traceId, String operation, String stage, String message, Map<String, Object> details) {
        DebugRagTraceService service = debugRagTraceServiceProvider.getIfAvailable();
        if (service != null) {
            service.warn(traceId, operation, stage, message, details);
        }
    }

    private void success(String traceId, String operation, String stage, String message, Map<String, Object> details) {
        DebugRagTraceService service = debugRagTraceServiceProvider.getIfAvailable();
        if (service != null) {
            service.success(traceId, operation, stage, message, details);
        }
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "" : value;
    }
}
