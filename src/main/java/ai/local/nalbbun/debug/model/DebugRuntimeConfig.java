package ai.local.nalbbun.debug.model;

import lombok.Data;

@Data
public class DebugRuntimeConfig {
    private String resolverMode;
    private String generalParserMode;
    private String travelParserMode;
    private String devParserMode;
    private String miceParserMode;
}