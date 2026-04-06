package ai.local.nalbbun.admin.model.llm;

import java.util.List;
import lombok.Data;

@Data
public class VllmEmbeddingTestRequest {
    private List<String> texts;
}
