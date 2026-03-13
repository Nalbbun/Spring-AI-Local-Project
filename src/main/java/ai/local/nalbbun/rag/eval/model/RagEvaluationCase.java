package ai.local.nalbbun.rag.eval.model;

import java.util.List;

import ai.local.nalbbun.model.category.ChatCategory;
import lombok.Data;

@Data
public class RagEvaluationCase {
    private String id;
    private ChatCategory category;
    private String query;
    private String source;
    private String version;
    private int minHits = 1;
    private List<String> expectedSources;
    private List<String> expectedVersions;
}
