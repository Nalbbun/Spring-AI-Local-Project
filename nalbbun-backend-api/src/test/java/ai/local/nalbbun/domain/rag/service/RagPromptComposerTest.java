package ai.local.nalbbun.domain.rag.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.config.rag.RagProperties;
import ai.local.nalbbun.domain.rag.model.RagRetrievedDocument;

/**
 * Rag Prompt Composer Test 타입이다.
 *
 * <p>기능 설명: 대상 컴포넌트의 기대 동작과 회귀 여부를 검증한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 테스트 데이터, 목 객체, 검증 조건</p>
 * <p>출력: 검증 결과와 회귀 방지용 보장</p>
 */
class RagPromptComposerTest {

    /**
     * Compose Citation Friendly Prompt Block 기대 동작을 검증한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
