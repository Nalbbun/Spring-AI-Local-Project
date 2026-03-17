package ai.local.nalbbun.debug.model.llm;

import lombok.Data;

@Data
public class DebugOllamaModelRegisterRequest {

    private String model;
    private Boolean pullPermanent;
    private Boolean loadToPs;
    private String keepAlive;
}
