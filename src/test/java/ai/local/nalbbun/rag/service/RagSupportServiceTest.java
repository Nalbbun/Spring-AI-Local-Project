package ai.local.nalbbun.rag.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.rag.config.RagProperties;
import ai.local.nalbbun.rag.model.RagContext;
import ai.local.nalbbun.rag.model.RagRetrievedDocument;
import ai.local.nalbbun.rag.retrieve.RagDocumentRetriever;

import static org.mockito.Mockito.mock;

/**
 * Rag Support Service Test 타입이다.
 *
 * <p>기능 설명: 대상 컴포넌트의 기대 동작과 회귀 여부를 검증한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 테스트 데이터, 목 객체, 검증 조건</p>
 * <p>출력: 검증 결과와 회귀 방지용 보장</p>
 */
class RagSupportServiceTest {

    /**
     * Return Disabled When Category Is Not Enabled 기대 동작을 검증한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    @Test
    void shouldReturnDisabledWhenCategoryIsNotEnabled() {
        RagProperties properties = new RagProperties();
        properties.setEnabled(true);
        properties.getCategories().setDev(false);

        RagDocumentRetriever retriever = mock(RagDocumentRetriever.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> vectorStoreProvider = (ObjectProvider<VectorStore>) mock(ObjectProvider.class);
        when(vectorStoreProvider.getIfAvailable()).thenReturn(mock(VectorStore.class));

        RagSupportService service = new RagSupportService(
                properties,
                retriever,
                new RagPromptComposer(properties),
                vectorStoreProvider
        );

        RagContext context = service.buildContext(ChatCategory.DEV, "배포 구조 설명해줘");

        assertFalse(context.isApplied());
        assertTrue(context.getTraceMessage().contains("category-disabled"));
    }

    /**
     * Build Prompt Block When Documents Exist 기대 동작을 검증한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    @Test
    void shouldBuildPromptBlockWhenDocumentsExist() {
        RagProperties properties = new RagProperties();
        properties.setEnabled(true);

        RagDocumentRetriever retriever = mock(RagDocumentRetriever.class);
        when(retriever.retrieve(any(), any())).thenReturn(List.of(
                RagRetrievedDocument.builder()
                        .id("doc-1")
                        .title("README")
                        .source("README.md")
                        .category(ChatCategory.DEV)
                        .text("애플리케이션 배포 순서")
                        .score(0.88d)
                        .build()
        ));

        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> vectorStoreProvider = (ObjectProvider<VectorStore>) mock(ObjectProvider.class);
        when(vectorStoreProvider.getIfAvailable()).thenReturn(mock(VectorStore.class));

        RagSupportService service = new RagSupportService(
                properties,
                retriever,
                new RagPromptComposer(properties),
                vectorStoreProvider
        );

        RagContext context = service.buildContext(ChatCategory.DEV, "배포 흐름 설명");

        assertTrue(context.isApplied());
        assertTrue(context.getPromptBlock().contains("README.md"));
    }
}
