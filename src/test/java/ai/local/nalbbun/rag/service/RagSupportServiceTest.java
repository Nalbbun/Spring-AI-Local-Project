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

class RagSupportServiceTest {

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
