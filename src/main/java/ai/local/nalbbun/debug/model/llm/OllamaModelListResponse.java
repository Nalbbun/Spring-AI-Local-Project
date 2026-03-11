package ai.local.nalbbun.debug.model.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaModelListResponse {
    private List<OllamaModelInfo> models = new ArrayList<>();
}