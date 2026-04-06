package ai.local.nalbbun.admin.model.llm;

import java.util.List;
import lombok.Data;

@Data
public class VllmRerankTestRequest {
    private String query;
    private List<String> documents;
    private Integer topK;
}
