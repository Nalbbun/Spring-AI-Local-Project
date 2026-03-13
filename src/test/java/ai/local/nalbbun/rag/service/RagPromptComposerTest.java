package ai.local.nalbbun.rag.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.rag.config.RagProperties;
import ai.local.nalbbun.rag.model.RagRetrievedDocument;

class RagPromptComposerTest {

    @Test
    void shouldComposeCitationFriendlyPromptBlock() {
        RagProperties properties = new RagProperties();
        properties.setIncludeCitations(true);
        RagPromptComposer composer = new RagPromptComposer(properties);

        String block = composer.compose(List.of(
                RagRetrievedDocument.builder()
                        .id("doc-1")
                        .title("운영 매뉴얼")
                        .source("manual.pdf")
                        .category(ChatCategory.MICE)
                        .text("행사 운영 체크리스트와 동선 운영 기준")
                        .score(0.91d)
                        .build()
        ));

        assertTrue(block.contains("[검색된 참고 문서]"));
        assertTrue(block.contains("manual.pdf"));
        assertTrue(block.contains("[1]"));
    }
}
