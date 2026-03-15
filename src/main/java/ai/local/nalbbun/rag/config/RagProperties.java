package ai.local.nalbbun.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import ai.local.nalbbun.model.category.ChatCategory;
import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    private boolean enabled = false;
    private String vectorStore = "pgvector";
    private int topK = 4;
    private double similarityThreshold = 0.72d;
    private boolean includeCitations = true;
    private int citationMaxSources = 3;
    private Categories categories = new Categories();
    private Ingest ingest = new Ingest();
    private Registry registry = new Registry();
    private Evaluation evaluation = new Evaluation();

    public boolean isCategoryEnabled(ChatCategory category) {
        if (category == null) {
            return false;
        }
        return switch (category) {
            case GENERAL -> categories.isGeneral();
            case DEV -> categories.isDev();
            case MICE -> categories.isMice();
            case TRAVEL -> categories.isTravel();
        };
    }

    @Data
    public static class Categories {
        private boolean general = false;
        private boolean dev = true;
        private boolean mice = true;
        private boolean travel = false;
    }

    @Data
    public static class Ingest {
        private int chunkSize = 350;
        private int minChunkSizeChars = 120;
        private int minChunkLengthToEmbed = 10;
        private int maxNumChunks = 128;
        private int maxUploadFileCount = 20;
    }

    @Data
    public static class Registry {
        private String baseDir = "data/rag-registry";
    }

    @Data
    public static class Evaluation {
        private String datasetLocation = "classpath:rag/eval/default-eval-set.json";
        private double minPassRate = 0.7d;
    }
}
