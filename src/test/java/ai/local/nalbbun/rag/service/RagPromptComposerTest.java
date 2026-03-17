package ai.local.nalbbun.rag.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.rag.config.RagProperties;
import ai.local.nalbbun.rag.model.RagRetrievedDocument;

/**
 * RagPromptComposerTest는 대상 기능의 동작을 검증하는 테스트 클래스이다.
 * <p>주요 기능: rag prompt composer test 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
class RagPromptComposerTest {

    /**
     * 대상 기능의 동작을 검증한다.
     */
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
