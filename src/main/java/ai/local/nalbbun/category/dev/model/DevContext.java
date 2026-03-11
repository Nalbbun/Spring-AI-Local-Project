package ai.local.nalbbun.category.dev.model;

import ai.local.nalbbun.model.common.CategoryContext;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DevContext implements CategoryContext {
    private String taskType;
    private String topic;
    private List<String> stackKeywords = new ArrayList<>();
    private String parserMode;
}