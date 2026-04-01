package ai.local.nalbbun.domain.rag.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.config.rag.RagProperties;
import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.rag.model.RagContext;
import ai.local.nalbbun.domain.rag.model.RagRetrievalResult;
import ai.local.nalbbun.domain.rag.model.RagRetrievedDocument;
import ai.local.nalbbun.domain.rag.model.RagStepTrace;
import ai.local.nalbbun.domain.rag.retrieve.RagDocumentRetriever;
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

    public boolean isConfiguredEnabled() {
        return ragProperties.isEnabled();
    }

    public boolean isRuntimeAvailable() {
        return ragProperties.isEnabled() && vectorStoreProvider.getIfAvailable() != null;
    }

    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        boolean configuredEnabled = ragProperties.isEnabled();
        boolean vectorStoreBeanAvailable = vectorStoreProvider.getIfAvailable() != null;
        boolean runtimeAvailable = configuredEnabled && vectorStoreBeanAvailable;

        result.put("configuredEnabled", configuredEnabled);
        result.put("vectorStoreBeanAvailable", vectorStoreBeanAvailable);
        result.put("runtimeAvailable", runtimeAvailable);
        result.put("vectorStore", ragProperties.getVectorStore());
        result.put("topK", ragProperties.getTopK());
        result.put("similarityThreshold", ragProperties.getSimilarityThreshold());
        result.put("categories", ragProperties.getCategories());

        if (!configuredEnabled) {
            result.put("status", "DISABLED");
            result.put("reason", "app.rag.enabled=false");
            return result;
        }
        if (!vectorStoreBeanAvailable) {
            result.put("status", "DEGRADED");
            result.put("reason", "vector-store-bean-unavailable");
            return result;
        }

        result.put("status", "UP");
        result.put("reason", "ok");
        return result;
    }

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
        List<RagStepTrace> steps = new ArrayList<>();
        steps.add(new RagStepTrace("RAG 요청 수신", "running", "category=" + category + ", query=" + summarize(userQuery)));

        if (!ragProperties.isEnabled()) {
            steps.add(new RagStepTrace("RAG 설정 확인", "disabled", "app.rag.enabled=false"));
            return RagContext.builder()
                    .enabled(false)
                    .applied(false)
                    .reason("disabled-config")
                    .traceMessage("rag=off, reason=disabled-config")
                    .promptBlock("")
                    .documents(List.of())
                    .sourceFilter(sourceFilter == null ? "" : sourceFilter)
                    .versionFilter(versionFilter == null ? "" : versionFilter)
                    .steps(steps)
                    .candidateCount(0)
                    .hitCount(0)
                    .retrievalElapsedMs(0L)
                    .filterExpression("")
                    .similarityThreshold(0.0d)
                    .topK(0)
                    .rerankApplied(false)
                    .retrievalMode("disabled")
                    .build();
        }
        steps.add(new RagStepTrace("RAG 설정 확인", "ok", "enabled=true"));

        if (!ragProperties.isCategoryEnabled(category)) {
            steps.add(new RagStepTrace("카테고리 정책", "disabled", "category=" + category + " 는 RAG 비활성"));
            return RagContext.builder()
                    .enabled(false)
                    .applied(false)
                    .reason("category-disabled")
                    .traceMessage("rag=off, reason=category-disabled")
                    .promptBlock("")
                    .documents(List.of())
                    .sourceFilter(sourceFilter == null ? "" : sourceFilter)
                    .versionFilter(versionFilter == null ? "" : versionFilter)
                    .steps(steps)
                    .candidateCount(0)
                    .hitCount(0)
                    .retrievalElapsedMs(0L)
                    .filterExpression("")
                    .similarityThreshold(0.0d)
                    .topK(0)
                    .rerankApplied(false)
                    .retrievalMode("category-disabled")
                    .build();
        }
        steps.add(new RagStepTrace("카테고리 정책", "ok", "category=" + category + " 사용 가능"));

        if (vectorStoreProvider.getIfAvailable() == null) {
            steps.add(new RagStepTrace("Vector Store 확인", "disabled", "vector-store bean 없음"));
            return RagContext.builder()
                    .enabled(false)
                    .applied(false)
                    .reason("vector-store-unavailable")
                    .traceMessage("rag=off, reason=vector-store-unavailable")
                    .promptBlock("")
                    .documents(List.of())
                    .sourceFilter(sourceFilter == null ? "" : sourceFilter)
                    .versionFilter(versionFilter == null ? "" : versionFilter)
                    .steps(steps)
                    .candidateCount(0)
                    .hitCount(0)
                    .retrievalElapsedMs(0L)
                    .filterExpression("")
                    .similarityThreshold(0.0d)
                    .topK(0)
                    .rerankApplied(false)
                    .retrievalMode("vector-store-unavailable")
                    .build();
        }
        steps.add(new RagStepTrace("Vector Store 확인", "ok", "runtime vector store 사용 가능"));
        steps.add(new RagStepTrace("검색 필터 준비", "running", "source=" + blankToAll(sourceFilter) + ", version=" + blankToAll(versionFilter)));

        RagRetrievalResult retrieval = ragDocumentRetriever.retrieveDetailed(category, userQuery, sourceFilter, versionFilter);
        steps.add(new RagStepTrace(
                "문서 검색",
                retrieval.returnedCount() > 0 ? "ok" : "empty",
                "candidates=" + retrieval.candidateCount() + ", hits=" + retrieval.returnedCount() + ", elapsed=" + retrieval.elapsedMs() + "ms"
        ));
        steps.add(new RagStepTrace(
                "검색 전략",
                retrieval.rerankApplied() ? "reranked" : "basic",
                "mode=" + retrieval.retrievalMode() + ", topK=" + retrieval.topK() + ", threshold=" + retrieval.similarityThreshold()
        ));

        List<RagRetrievedDocument> documents = retrieval.documents();
        if (documents.isEmpty()) {
            steps.add(new RagStepTrace("프롬프트 구성", "skipped", "검색 문서가 없어 prompt block 생략"));
            return RagContext.builder()
                    .enabled(true)
                    .applied(false)
                    .reason("no-matching-documents")
                    .traceMessage("rag=on, hits=0, reason=no-matching-documents")
                    .promptBlock("")
                    .documents(List.of())
                    .sourceFilter(sourceFilter == null ? "" : sourceFilter)
                    .versionFilter(versionFilter == null ? "" : versionFilter)
                    .steps(steps)
                    .candidateCount(retrieval.candidateCount())
                    .hitCount(0)
                    .retrievalElapsedMs(retrieval.elapsedMs())
                    .filterExpression(retrieval.filterExpression())
                    .similarityThreshold(retrieval.similarityThreshold())
                    .topK(retrieval.topK())
                    .rerankApplied(retrieval.rerankApplied())
                    .retrievalMode(retrieval.retrievalMode())
                    .build();
        }

        String sources = documents.stream()
                .map(RagRetrievedDocument::source)
                .distinct()
                .collect(Collectors.joining(", "));
        steps.add(new RagStepTrace("프롬프트 구성", "ok", "documents=" + documents.size() + ", sources=" + sources));

        return RagContext.builder()
                .enabled(true)
                .applied(true)
                .reason("ok")
                .documents(documents)
                .promptBlock(ragPromptComposer.compose(documents))
                .traceMessage("rag=on, hits=" + documents.size() + ", sources=" + sources + ", elapsed=" + retrieval.elapsedMs() + "ms")
                .sourceFilter(sourceFilter == null ? "" : sourceFilter)
                .versionFilter(versionFilter == null ? "" : versionFilter)
                .steps(steps)
                .candidateCount(retrieval.candidateCount())
                .hitCount(documents.size())
                .retrievalElapsedMs(retrieval.elapsedMs())
                .filterExpression(retrieval.filterExpression())
                .similarityThreshold(retrieval.similarityThreshold())
                .topK(retrieval.topK())
                .rerankApplied(retrieval.rerankApplied())
                .retrievalMode(retrieval.retrievalMode())
                .build();
    }

    private String summarize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace("\n", " ").replace("\r", " ").trim();
        return normalized.length() > 80 ? normalized.substring(0, 80) + "..." : normalized;
    }

    private String blankToAll(String value) {
        return value == null || value.isBlank() ? "ALL" : value;
    }
}
