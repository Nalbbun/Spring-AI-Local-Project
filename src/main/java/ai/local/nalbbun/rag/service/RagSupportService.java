package ai.local.nalbbun.rag.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.rag.config.RagProperties;
import ai.local.nalbbun.rag.model.RagContext;
import ai.local.nalbbun.rag.model.RagRetrievedDocument;
import ai.local.nalbbun.rag.retrieve.RagDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RagSupportService {

    private final RagProperties ragProperties;
    private final RagDocumentRetriever ragDocumentRetriever;
    private final RagPromptComposer ragPromptComposer;
    private final ObjectProvider<VectorStore> vectorStoreProvider;

    public RagContext buildContext(ChatCategory category, String userQuery) {
        return buildContext(category, userQuery, null, null);
    }

    public RagContext buildContext(ChatCategory category, String userQuery, String sourceFilter, String versionFilter) {
        if (!ragProperties.isEnabled()) {
            return RagContext.disabled("disabled-config");
        }
        if (!ragProperties.isCategoryEnabled(category)) {
            return RagContext.disabled("category-disabled");
        }
        if (vectorStoreProvider.getIfAvailable() == null) {
            return RagContext.disabled("vector-store-unavailable");
        }

        List<RagRetrievedDocument> documents = ragDocumentRetriever.retrieve(category, userQuery, sourceFilter, versionFilter);
        if (documents.isEmpty()) {
            return RagContext.builder()
                    .enabled(true)
                    .applied(false)
                    .reason("no-matching-documents")
                    .traceMessage("rag=on, hits=0, reason=no-matching-documents")
                    .promptBlock("")
                    .documents(List.of())
                    .sourceFilter(sourceFilter == null ? "" : sourceFilter)
                    .versionFilter(versionFilter == null ? "" : versionFilter)
                    .build();
        }

        String sources = documents.stream()
                .map(RagRetrievedDocument::source)
                .distinct()
                .collect(Collectors.joining(", "));

        return RagContext.builder()
                .enabled(true)
                .applied(true)
                .reason("ok")
                .documents(documents)
                .promptBlock(ragPromptComposer.compose(documents))
                .traceMessage("rag=on, hits=" + documents.size() + ", sources=" + sources)
                .sourceFilter(sourceFilter == null ? "" : sourceFilter)
                .versionFilter(versionFilter == null ? "" : versionFilter)
                .build();
    }
}
