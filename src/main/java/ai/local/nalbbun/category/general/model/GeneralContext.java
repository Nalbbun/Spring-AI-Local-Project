package ai.local.nalbbun.category.general.model;

import ai.local.nalbbun.model.common.CategoryContext;
import lombok.Data;

@Data
public class GeneralContext implements CategoryContext {
    private String intent;
    private String parserMode;
}