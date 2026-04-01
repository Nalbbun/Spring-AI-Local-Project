package ai.local.nalbbun.config.rag;

import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import lombok.Data;

/**
 * RAG runtime configuration.
 */
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
    private boolean deduplicateResults = true;
    private int maxResultsPerSource = 2;
    private String duplicatePolicy = "REPLACE";
    private Categories categories = new Categories();
    private Ingest ingest = new Ingest();
    private Registry registry = new Registry();
    private Evaluation evaluation = new Evaluation();
    private Retrieval retrieval = new Retrieval();

    public boolean isCategoryEnabled(ChatCategory category) {
        return switch (category) {
            case GENERAL -> categories.isGeneral();
            case DEV -> categories.isDev();
            case MICE -> categories.isMice();
            case TRAVEL -> categories.isTravel();
        };
    }

    public int resolveTopK(ChatCategory category) {
        CategoryTuning tuning = retrieval.tuningFor(category);
        return tuning.getTopK() != null && tuning.getTopK() > 0 ? tuning.getTopK() : topK;
    }

    public double resolveSimilarityThreshold(ChatCategory category) {
        CategoryTuning tuning = retrieval.tuningFor(category);
        return tuning.getSimilarityThreshold() != null && tuning.getSimilarityThreshold() > 0
                ? tuning.getSimilarityThreshold()
                : similarityThreshold;
    }

    public int resolveMaxResultsPerSource(ChatCategory category) {
        CategoryTuning tuning = retrieval.tuningFor(category);
        return tuning.getMaxResultsPerSource() != null && tuning.getMaxResultsPerSource() > 0
                ? tuning.getMaxResultsPerSource()
                : maxResultsPerSource;
    }

    public boolean isNormalizePdfEnabled() {
        return ingest.isNormalizePdf();
    }

    public boolean isNormalizeTextEnabled() {
        return ingest.isNormalizeText();
    }

    public boolean isReplaceDuplicateIngest() {
        return "REPLACE".equalsIgnoreCase(blankToDefault(duplicatePolicy, "REPLACE"));
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
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
        private boolean normalizePdf = true;
        private boolean normalizeText = true;
        private boolean includeChunkPreview = true;
        private int chunkPreviewLength = 180;
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

    @Data
    public static class Retrieval {
        private CategoryTuning general = new CategoryTuning();
        private CategoryTuning dev = new CategoryTuning();
        private CategoryTuning mice = new CategoryTuning();
        private CategoryTuning travel = new CategoryTuning();

        public CategoryTuning tuningFor(ChatCategory category) {
            return switch (category) {
                case GENERAL -> general;
                case DEV -> dev;
                case MICE -> mice;
                case TRAVEL -> travel;
            };
        }
    }

    @Data
    public static class CategoryTuning {
        private Integer topK;
        private Double similarityThreshold;
        private Integer maxResultsPerSource;
        private String queryMode;

        public String normalizedQueryMode() {
            return queryMode == null ? "DEFAULT" : queryMode.trim().toUpperCase(Locale.ROOT);
        }
    }
}
