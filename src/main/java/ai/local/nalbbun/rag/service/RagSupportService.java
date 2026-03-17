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

/**
 * RagSupportService는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: rag support service 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Service
@RequiredArgsConstructor
public class RagSupportService {

    /** ragProperties 값을 보관한다. */
    private final RagProperties ragProperties;
    /** ragDocumentRetriever 값을 보관한다. */
    private final RagDocumentRetriever ragDocumentRetriever;
    /** ragPromptComposer 값을 보관한다. */
    private final RagPromptComposer ragPromptComposer;
    /** vectorStoreProvider 값을 보관한다. */
    private final ObjectProvider<VectorStore> vectorStoreProvider;

    /**
     * 필요한 결과 객체를 구성한다.
     *
     * @param category 대상 카테고리 정보
     * @param userQuery 사용자 입력 또는 질의 내용
     * @return RagContext 타입의 처리 결과
     */
    public RagContext buildContext(ChatCategory category, String userQuery) {
        return buildContext(category, userQuery, null, null);
    }

    /**
     * 필요한 결과 객체를 구성한다.
     *
     * @param category 대상 카테고리 정보
     * @param userQuery 사용자 입력 또는 질의 내용
     * @param sourceFilter sourceFilter 값
     * @param versionFilter versionFilter 값
     * @return RagContext 타입의 처리 결과
     */
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
