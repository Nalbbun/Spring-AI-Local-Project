package ai.local.nalbbun.category.common.memory;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class CategoryMemoryUpdateResult {

    private boolean summaryUpdated;

    @Builder.Default
    private List<String> addedNotes = new ArrayList<>();

    public int addedNoteCount() {
        return addedNotes == null ? 0 : addedNotes.size();
    }

    public String toDebugMessage() {
        return String.format(
                "summary=%s, notes=%d",
                summaryUpdated ? "updated" : "unchanged",
                addedNoteCount()
        );
    }
}