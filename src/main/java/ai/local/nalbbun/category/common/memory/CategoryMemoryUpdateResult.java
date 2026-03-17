package ai.local.nalbbun.category.common.memory;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * CategoryMemoryUpdateResult는 대화 메모리 규칙 또는 저장 처리를 담당하는 컴포넌트이다.
 * <p>주요 기능: category memory update result 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Data
@Builder
public class CategoryMemoryUpdateResult {

    /** summaryUpdated 값을 보관한다. */
    private boolean summaryUpdated;

    /** addedNotes 값을 보관한다. */
    @Builder.Default
    private List<String> addedNotes = new ArrayList<>();

    /**
     * addedNoteCount 기능을 수행한다.
     * @return int 타입의 처리 결과
     */
    public int addedNoteCount() {
        return addedNotes == null ? 0 : addedNotes.size();
    }

    /**
     * 현재 상태를 다른 표현 형태로 변환한다.
     * @return 처리 결과 문자열
     */
    public String toDebugMessage() {
        return String.format(
                "summary=%s, notes=%d",
                summaryUpdated ? "updated" : "unchanged",
                addedNoteCount()
        );
    }
}