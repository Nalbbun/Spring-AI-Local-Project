package ai.local.nalbbun.debug.model.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaModelInfo {
    private String name;
    private String model;
    private Long size;
}