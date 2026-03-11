package ai.local.nalbbun.category.mice.model;

import ai.local.nalbbun.model.common.CategoryContext;
import lombok.Data;

@Data
public class MiceContext implements CategoryContext {
    private String eventType;
    private String deliverableType;
    private String targetRegion;
    private String parserMode;
}