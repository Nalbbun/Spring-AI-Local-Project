package ai.local.nalbbun.domain.category.memory;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Category Memory Update 타입이다.
 *
 * <p>기능 설명: 애플리케이션 기능을 이루는 재사용 가능한 구성 요소다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Data
@Builder
public class CategoryMemoryUpdate {
    private String summary;

    @Builder.Default
    private List<String> importantNotes = new ArrayList<>();
}