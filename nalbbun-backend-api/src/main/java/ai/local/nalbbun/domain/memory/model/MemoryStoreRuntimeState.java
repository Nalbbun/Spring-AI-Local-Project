package ai.local.nalbbun.domain.memory.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryStoreRuntimeState {
    private String activeStore;
    private String requestedStore;
    private LocalDateTime restartRequestedAt;
    private LocalDateTime lastAppliedAt;
    private Integer redisSessionTtlMinutes;
    private Integer redisMemoryTtlMinutes;
    private String lastAction;
}
