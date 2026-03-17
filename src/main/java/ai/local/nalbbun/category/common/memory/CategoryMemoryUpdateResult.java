package ai.local.nalbbun.category.common.memory;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Category Memory Update Result 타입이다.
 *
 * <p>기능 설명: 계층 간에 전달되는 도메인 데이터와 상태를 표현한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다. 주요 속성 예시는 summaryUpdated 이다.</p>
 * <p>입력: 상위 계층에서 전달한 속성 값</p>
 * <p>출력: 직렬화/역직렬화 가능한 데이터 객체</p>
 */
@Data
@Builder
public class CategoryMemoryUpdateResult {

    private boolean summaryUpdated;

    @Builder.Default
    private List<String> addedNotes = new ArrayList<>();

    /**
     * added Note Count 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public int addedNoteCount() {
        return addedNotes == null ? 0 : addedNotes.size();
    }

    /**
     * to Debug Message 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String toDebugMessage() {
        return String.format(
                "summary=%s, notes=%d",
                summaryUpdated ? "updated" : "unchanged",
                addedNoteCount()
        );
    }
}