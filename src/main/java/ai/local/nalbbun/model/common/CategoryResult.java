package ai.local.nalbbun.model.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryResult {
    private String finalResponse;
    private Object payload;
}