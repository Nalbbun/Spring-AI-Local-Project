package ai.local.nalbbun.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import ai.local.nalbbun.category.model.ChatCategory;
import lombok.Data;

/**
 * Rag Properties 타입이다.
 *
 * <p>기능 설명: 스프링 빈과 런타임 설정을 구성한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 프로퍼티 값, 환경 변수, 스프링 컨텍스트 정보</p>
 * <p>출력: 빈 등록 결과 또는 런타임 설정 상태</p>
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
    private Categories categories = new Categories();
    private Ingest ingest = new Ingest();
    private Registry registry = new Registry();
    private Evaluation evaluation = new Evaluation();

    /**
     * Category Enabled 여부를 판별한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public boolean isCategoryEnabled(ChatCategory category) {
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
