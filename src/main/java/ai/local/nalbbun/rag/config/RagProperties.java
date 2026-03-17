package ai.local.nalbbun.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import ai.local.nalbbun.model.category.ChatCategory;
import lombok.Data;

/**
 * RagProperties는 애플리케이션 설정과 빈 구성을 담당하는 설정 타입이다.
 * <p>주요 기능: rag properties 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    /** enabled 값을 보관한다. */
    private boolean enabled = false;
    /** vectorStore 값을 보관한다. */
    private String vectorStore = "pgvector";
    /** topK 값을 보관한다. */
    private int topK = 4;
    /** similarityThreshold 값을 보관한다. */
    private double similarityThreshold = 0.72d;
    /** includeCitations 값을 보관한다. */
    private boolean includeCitations = true;
    /** citationMaxSources 값을 보관한다. */
    private int citationMaxSources = 3;
    /** categories 값을 보관한다. */
    private Categories categories = new Categories();
    /** ingest 값을 보관한다. */
    private Ingest ingest = new Ingest();
    /** registry 값을 보관한다. */
    private Registry registry = new Registry();
    /** evaluation 값을 보관한다. */
    private Evaluation evaluation = new Evaluation();

    /**
     * 조건 충족 여부를 확인한다.
     *
     * @param category 대상 카테고리 정보
     * @return 처리 가능 여부 또는 조건 충족 여부
     */
    public boolean isCategoryEnabled(ChatCategory category) {
        return switch (category) {
            case GENERAL -> categories.isGeneral();
            case DEV -> categories.isDev();
            case MICE -> categories.isMice();
            case TRAVEL -> categories.isTravel();
        };
    }

    /**
     * Categories는 애플리케이션 설정과 빈 구성을 담당하는 설정 타입이다.
     * <p>주요 기능: categories 관련 책임을 수행한다.</p>
     * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
     */
    @Data
    public static class Categories {
        /** general 값을 보관한다. */
        private boolean general = false;
        /** dev 값을 보관한다. */
        private boolean dev = true;
        /** mice 값을 보관한다. */
        private boolean mice = true;
        /** travel 값을 보관한다. */
        private boolean travel = false;
    }

    /**
     * Ingest는 애플리케이션 설정과 빈 구성을 담당하는 설정 타입이다.
     * <p>주요 기능: ingest 관련 책임을 수행한다.</p>
     * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
     */
    @Data
    public static class Ingest {
        /** chunkSize 값을 보관한다. */
        private int chunkSize = 350;
        /** minChunkSizeChars 값을 보관한다. */
        private int minChunkSizeChars = 120;
        /** minChunkLengthToEmbed 값을 보관한다. */
        private int minChunkLengthToEmbed = 10;
        /** maxNumChunks 값을 보관한다. */
        private int maxNumChunks = 128;
    }

    /**
     * Registry는 애플리케이션 설정과 빈 구성을 담당하는 설정 타입이다.
     * <p>주요 기능: registry 관련 책임을 수행한다.</p>
     * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
     */
    @Data
    public static class Registry {
        /** baseDir 값을 보관한다. */
        private String baseDir = "data/rag-registry";
    }

    /**
     * Evaluation는 애플리케이션 설정과 빈 구성을 담당하는 설정 타입이다.
     * <p>주요 기능: evaluation 관련 책임을 수행한다.</p>
     * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
     */
    @Data
    public static class Evaluation {
        /** datasetLocation 값을 보관한다. */
        private String datasetLocation = "classpath:rag/eval/default-eval-set.json";
        /** minPassRate 값을 보관한다. */
        private double minPassRate = 0.7d;
    }
}
