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
 * Rag Support Service 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
@Service
@RequiredArgsConstructor
public class RagSupportService {

    private final RagProperties ragProperties;
    private final RagDocumentRetriever ragDocumentRetriever;
    private final RagPromptComposer ragPromptComposer;
    private final ObjectProvider<VectorStore> vectorStoreProvider;

    /**
     * build Context 결과를 구성한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public RagContext buildContext(ChatCategory category, String userQuery) {
        return buildContext(category, userQuery, null, null);
    }

    /**
     * build Context 결과를 구성한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
