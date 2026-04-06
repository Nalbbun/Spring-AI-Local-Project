package ai.local.nalbbun.admin.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ai.local.nalbbun.admin.service.DebugDatabaseInfoService;
import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.config.rag.RagProperties;
import ai.local.nalbbun.domain.rag.ingest.RagDocumentIngestionService;
import ai.local.nalbbun.domain.rag.ingest.RagIngestCommand;
import ai.local.nalbbun.domain.rag.ingest.RagIngestionResult;
import ai.local.nalbbun.domain.rag.ingest.RagMultiFileIngestionResult;
import ai.local.nalbbun.domain.rag.ingest.RagUrlIngestCommand;
import ai.local.nalbbun.domain.rag.model.RagContext;
import ai.local.nalbbun.domain.rag.model.RagSourceFileEntry;
import ai.local.nalbbun.domain.rag.model.RagSourceFilePurgeCommand;
import ai.local.nalbbun.domain.rag.model.RagSourceManifest;
import ai.local.nalbbun.domain.rag.model.RagSourcePurgeCommand;
import ai.local.nalbbun.domain.rag.model.RagSourceReindexCommand;
import ai.local.nalbbun.domain.rag.service.RagSourceAdminService;
import ai.local.nalbbun.domain.rag.service.RagSourceCatalogService;
import ai.local.nalbbun.domain.rag.service.RagSupportService;
import ai.local.nalbbun.domain.rag.trace.DebugRagTraceService;
import lombok.RequiredArgsConstructor;

import ai.local.nalbbun.domain.rag.service.EmbeddingConfigService;

/**
 * Debug Rag Controller 타입이다.
 */
@RestController
@Profile("local")
@ConditionalOnProperty(prefix = "app.debug", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@RequestMapping("/debug/api/rag")
public class DebugRagController {

    private final RagProperties ragProperties;
    private final RagSupportService ragSupportService;
    private final RagDocumentIngestionService ragDocumentIngestionService;
    private final RagSourceCatalogService ragSourceCatalogService;
    private final RagSourceAdminService ragSourceAdminService;
    private final DebugDatabaseInfoService debugDatabaseInfoService;
    private final DebugRagTraceService debugRagTraceService;
    private final EmbeddingConfigService embeddingConfigService;

    /**
     * status 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", ragProperties.isEnabled() ? "UP" : "DISABLED");
        response.put("details", status());
        return response;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("enabled", ragProperties.isEnabled());
        response.put("vectorStore", ragProperties.getVectorStore());
        response.put("topK", ragProperties.getTopK());
        response.put("similarityThreshold", ragProperties.getSimilarityThreshold());
        response.put("includeCitations", ragProperties.isIncludeCitations());
        Map<String, Object> categories = new LinkedHashMap<>();
        categories.put("GENERAL", ragProperties.getCategories().isGeneral());
        categories.put("DEV", ragProperties.getCategories().isDev());
        categories.put("MICE", ragProperties.getCategories().isMice());
        categories.put("TRAVEL", ragProperties.getCategories().isTravel());
        response.put("categories", categories);
        Map<String, Object> ingest = new LinkedHashMap<>();
        ingest.put("chunkSize", ragProperties.getIngest().getChunkSize());
        ingest.put("minChunkSizeChars", ragProperties.getIngest().getMinChunkSizeChars());
        ingest.put("minChunkLengthToEmbed", ragProperties.getIngest().getMinChunkLengthToEmbed());
        ingest.put("maxNumChunks", ragProperties.getIngest().getMaxNumChunks());
        ingest.put("maxUploadFileCount", ragProperties.getIngest().getMaxUploadFileCount());
        response.put("ingest", ingest);
        response.put("registryBaseDir", ragProperties.getRegistry().getBaseDir());
        response.put("supportedReaders", List.of("text", "file-single", "file-multi", "pdf", "markdown", "text-like", "web-url"));
        return response;
    }

    /**
     * db Info 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @GetMapping("/db-info")
    public Map<String, Object> dbInfo() {
        return debugDatabaseInfoService.getInfo();
    }

    /**
     * search 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @GetMapping("/search")
    public RagContext search(@RequestParam("category") ChatCategory category,
                             @RequestParam("query") String query,
                             @RequestParam(value = "source", required = false) String source,
                             @RequestParam(value = "version", required = false) String version) {
        return ragSupportService.buildContext(category, query, source, version);
    }

    /**
     * sources 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @GetMapping("/sources")
    public List<RagSourceManifest> sources(@RequestParam(value = "category", required = false) ChatCategory category,
                                           @RequestParam(value = "source", required = false) String source,
                                           @RequestParam(value = "version", required = false) String version) {
        return ragSourceCatalogService.listSources(category, source, version);
    }

    /**
     * source Files 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @GetMapping("/source/files")
    public List<RagSourceFileEntry> sourceFiles(@RequestParam("category") ChatCategory category,
                                                @RequestParam("source") String source,
                                                @RequestParam("version") String version) {
        return ragSourceCatalogService.listFiles(category, source, version);
    }

    /**
     * purge Source 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @PostMapping("/source/purge")
    public Map<String, Object> purgeSource(@RequestBody RagSourcePurgeCommand command) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("result", ragSourceAdminService.purgeSource(command));
        response.put("status", "purged");
        response.put("sources", ragSourceCatalogService.listSources(command.getCategory(), command.getSource(), command.getVersion()));
        response.put("files", ragSourceCatalogService.listFiles(command.getCategory(), command.getSource(), command.getVersion()));
        return response;
    }

    /**
     * purge Source File 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @PostMapping("/source/file/purge")
    public Map<String, Object> purgeSourceFile(@RequestBody RagSourceFilePurgeCommand command) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("result", ragSourceAdminService.purgeSourceFile(command));
        response.put("status", "purged");
        response.put("sources", ragSourceCatalogService.listSources(command.getCategory(), command.getSource(), command.getVersion()));
        response.put("files", ragSourceCatalogService.listFiles(command.getCategory(), command.getSource(), command.getVersion()));
        return response;
    }

    /**
     * reindex Source 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @PostMapping("/source/reindex")
    public Map<String, Object> reindexSource(@RequestBody RagSourceReindexCommand command) {
        Map<String, Object> response = new LinkedHashMap<>();
        var result = ragSourceAdminService.reindexSource(command);
        response.put("result", result);
        response.put("status", "reindexed");
        response.put("manifest", findManifest(command.getCategory(), command.getSource(), result.targetVersion()));
        response.put("files", ragSourceCatalogService.listFiles(command.getCategory(), command.getSource(), result.targetVersion()));
        return response;
    }

    /**
     * compare 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @GetMapping("/source/compare")
    public Map<String, Object> compare(@RequestParam("category") ChatCategory category,
                                       @RequestParam("source") String source,
                                       @RequestParam("leftVersion") String leftVersion,
                                       @RequestParam("rightVersion") String rightVersion,
                                       @RequestParam(value = "query", required = false) String query) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("result", ragSourceAdminService.compare(category, source, leftVersion, rightVersion, query));
        response.put("status", "compared");
        return response;
    }

    /**
     * traces 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @GetMapping("/traces")
    public Map<String, Object> traces(@RequestParam(value = "limit", defaultValue = "150") int limit) {
        return debugRagTraceService.latest(limit);
    }

    /**
     * trace By Id 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @GetMapping("/traces/{traceId}")
    public Map<String, Object> traceById(@PathVariable("traceId") String traceId) {
        return debugRagTraceService.byTraceId(traceId);
    }

    /**
     * clear Traces 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @PostMapping("/traces/clear")
    public Map<String, Object> clearTraces() {
        debugRagTraceService.clear();
        return Map.of("status", "cleared");
    }

    /**
     * ingest Text 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @PostMapping("/ingest-text")
    public Map<String, Object> ingestText(@RequestBody RagIngestCommand command) {
        RagIngestionResult result = ragDocumentIngestionService.ingestText(command);
        return ingestResponse(command.getCategory(), result.source(), result.version(), result);
    }

    /**
     * ingest File 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @PostMapping(value = "/ingest-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> ingestFile(@RequestParam("category") ChatCategory category,
                                          @RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "source", required = false) String source,
                                          @RequestParam(value = "version", required = false) String version,
                                          @RequestParam(value = "title", required = false) String title,
                                          @RequestParam(value = "metadataJson", required = false) String metadataJson) {
        RagIngestCommand command = new RagIngestCommand();
        command.setCategory(category);
        command.setSource(source);
        command.setVersion(version);
        command.setTitle(title);

        RagIngestionResult result = ragDocumentIngestionService.ingestFile(command, file);
        return ingestResponse(category, result.source(), result.version(), result);
    }

    /**
     * ingest Files 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @PostMapping(value = "/ingest-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> ingestFiles(@RequestParam("category") ChatCategory category,
                                           @RequestParam("files") MultipartFile[] files,
                                           @RequestParam(value = "source", required = false) String source,
                                           @RequestParam(value = "version", required = false) String version,
                                           @RequestParam(value = "title", required = false) String title,
                                           @RequestParam(value = "metadataJson", required = false) String metadataJson) {
        RagIngestCommand command = new RagIngestCommand();
        command.setCategory(category);
        command.setSource(source);
        command.setVersion(version);
        command.setTitle(title);

        RagMultiFileIngestionResult result = ragDocumentIngestionService.ingestFiles(command, files);
        return ingestResponse(category, result.source(), result.version(), result);
    }

    /**
     * ingest Url 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @PostMapping("/ingest-url")
    public Map<String, Object> ingestUrl(@RequestBody RagUrlIngestCommand command) {
        RagIngestionResult result = ragDocumentIngestionService.ingestUrl(command);
        return ingestResponse(command.getCategory(), result.source(), result.version(), result);
    }

    /**
     * handle Bad Request 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(RuntimeException e) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", "bad_request");
        response.put("message", e.getMessage());
        response.put("hint", hintFor(e.getMessage()));
        return response;
    }

    /**
     * ingest Response 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private Map<String, Object> ingestResponse(ChatCategory category, String source, String version, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("result", result);
        response.put("status", "stored");
        if (category != null && source != null && !source.isBlank() && version != null && !version.isBlank()) {
            response.put("manifest", findManifest(category, source, version));
            response.put("files", ragSourceCatalogService.listFiles(category, source, version));
        }
        return response;
    }

    // ── RAG 런타임 설정 변경 ─────────────────────────────────────────────────
    /**
     * update Config 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @PostMapping("/config")
    public Map<String, Object> updateConfig(@RequestBody Map<String, Object> body) {
        if (body.containsKey("enabled")) {
            ragProperties.setEnabled(Boolean.parseBoolean(String.valueOf(body.get("enabled"))));
        }
        if (body.containsKey("topK")) {
            ragProperties.setTopK(Integer.parseInt(String.valueOf(body.get("topK"))));
        }
        if (body.containsKey("similarityThreshold")) {
            ragProperties.setSimilarityThreshold(Double.parseDouble(String.valueOf(body.get("similarityThreshold"))));
        }
        if (body.containsKey("includeCitations")) {
            ragProperties.setIncludeCitations(Boolean.parseBoolean(String.valueOf(body.get("includeCitations"))));
        }

        Object categories = body.get("categories");
        if (categories instanceof Map<?, ?> categoryMap) {
            if (categoryMap.containsKey("GENERAL")) {
                ragProperties.getCategories().setGeneral(Boolean.parseBoolean(String.valueOf(categoryMap.get("GENERAL"))));
            }
            if (categoryMap.containsKey("DEV")) {
                ragProperties.getCategories().setDev(Boolean.parseBoolean(String.valueOf(categoryMap.get("DEV"))));
            }
            if (categoryMap.containsKey("MICE")) {
                ragProperties.getCategories().setMice(Boolean.parseBoolean(String.valueOf(categoryMap.get("MICE"))));
            }
            if (categoryMap.containsKey("TRAVEL")) {
                ragProperties.getCategories().setTravel(Boolean.parseBoolean(String.valueOf(categoryMap.get("TRAVEL"))));
            }
        }
        if (body.containsKey("generalEnabled")) {
            ragProperties.getCategories().setGeneral(Boolean.parseBoolean(String.valueOf(body.get("generalEnabled"))));
        }
        if (body.containsKey("devEnabled")) {
            ragProperties.getCategories().setDev(Boolean.parseBoolean(String.valueOf(body.get("devEnabled"))));
        }
        if (body.containsKey("miceEnabled")) {
            ragProperties.getCategories().setMice(Boolean.parseBoolean(String.valueOf(body.get("miceEnabled"))));
        }
        if (body.containsKey("travelEnabled")) {
            ragProperties.getCategories().setTravel(Boolean.parseBoolean(String.valueOf(body.get("travelEnabled"))));
        }

        Object ingest = body.get("ingest");
        if (ingest instanceof Map<?, ?> ingestMap) {
            if (ingestMap.containsKey("chunkSize")) {
                ragProperties.getIngest().setChunkSize(Integer.parseInt(String.valueOf(ingestMap.get("chunkSize"))));
            }
            if (ingestMap.containsKey("minChunkSizeChars")) {
                ragProperties.getIngest().setMinChunkSizeChars(Integer.parseInt(String.valueOf(ingestMap.get("minChunkSizeChars"))));
            }
            if (ingestMap.containsKey("minChunkLengthToEmbed")) {
                ragProperties.getIngest().setMinChunkLengthToEmbed(Integer.parseInt(String.valueOf(ingestMap.get("minChunkLengthToEmbed"))));
            }
            if (ingestMap.containsKey("maxNumChunks")) {
                ragProperties.getIngest().setMaxNumChunks(Integer.parseInt(String.valueOf(ingestMap.get("maxNumChunks"))));
            }
            if (ingestMap.containsKey("maxUploadFileCount")) {
                ragProperties.getIngest().setMaxUploadFileCount(Integer.parseInt(String.valueOf(ingestMap.get("maxUploadFileCount"))));
            }
        }
        if (body.containsKey("chunkSize")) {
            ragProperties.getIngest().setChunkSize(Integer.parseInt(String.valueOf(body.get("chunkSize"))));
        }
        if (body.containsKey("minChunkSizeChars")) {
            ragProperties.getIngest().setMinChunkSizeChars(Integer.parseInt(String.valueOf(body.get("minChunkSizeChars"))));
        }
        if (body.containsKey("minChunkLengthToEmbed")) {
            ragProperties.getIngest().setMinChunkLengthToEmbed(Integer.parseInt(String.valueOf(body.get("minChunkLengthToEmbed"))));
        }
        if (body.containsKey("maxNumChunks")) {
            ragProperties.getIngest().setMaxNumChunks(Integer.parseInt(String.valueOf(body.get("maxNumChunks"))));
        }
        if (body.containsKey("maxUploadFileCount")) {
            ragProperties.getIngest().setMaxUploadFileCount(Integer.parseInt(String.valueOf(body.get("maxUploadFileCount"))));
        }
        return status();
    }

    /**
     * find Manifest 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    // ── 임베딩 모델 설정 ──────────────────────────────────────────────────────

    /** 현재 임베딩 설정 조회 */
    @GetMapping("/embedding/config")
    public Map<String, Object> getEmbeddingConfig() {
        return embeddingConfigService.getCurrentConfig();
    }

    /** 임베딩 설정 변경 (model, keepAlive, dimensions) */
    @PostMapping("/embedding/config")
    public Map<String, Object> updateEmbeddingConfig(@RequestBody Map<String, Object> body) {
        String model     = body.containsKey("model")     ? String.valueOf(body.get("model"))     : null;
        String keepAlive = body.containsKey("keepAlive") ? String.valueOf(body.get("keepAlive")) : null;
        Integer dims     = body.containsKey("dimensions")
                ? Integer.parseInt(String.valueOf(body.get("dimensions"))) : null;
        return embeddingConfigService.applyConfig(model, keepAlive, dims);
    }

    /** 임베딩 설정 기본값으로 초기화 */
    @PostMapping("/embedding/config/reset")
    public Map<String, Object> resetEmbeddingConfig() {
        return embeddingConfigService.resetConfig();
    }

    /** Ollama 설치된 모델 목록 (임베딩 모델 선택용) */
    @GetMapping("/embedding/models")
    public Map<String, Object> listEmbeddingModels() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentModel", embeddingConfigService.getModel());
        result.put("models", embeddingConfigService.listAvailableModels());
        return result;
    }

    private RagSourceManifest findManifest(ChatCategory category, String source, String version) {
        return ragSourceCatalogService.listSources(category, source, version)
                .stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * hint For 요청을 처리한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String hintFor(String message) {
        if (message == null) {
            return "입력값과 설정을 다시 확인하세요.";
        }
        if (message.contains("지원하지 않는 파일 형식")) {
            return "pdf, md, markdown, txt, log, yaml, yml, json, xml, csv 형식을 사용하세요.";
        }
        if (message.contains("업로드할 파일을 선택")) {
            return "파일 선택 후 다시 업로드하세요.";
        }
        if (message.contains("VectorStore")) {
            return "APP_RAG_ENABLED, SPRING_AI_VECTORSTORE_TYPE, PGVector 연결 상태를 확인하세요.";
        }
        if (message.contains("PDF 문서 분석 중 오류")) {
            return "해당 PDF는 레이아웃이 복잡하거나 스캔본일 수 있습니다. PDF를 다시 저장하거나 TXT/Markdown으로 변환 후 업로드해 보세요.";
        }
        if (message.contains("Comparison method violates its general contract")) {
            return "PDF 내부 텍스트 좌표 정렬 오류입니다. 서버 fallback 파서 적용 여부를 확인하고, 문제 파일은 텍스트 기반 PDF로 다시 저장해 업로드하세요.";
        }
        if (message.contains("category")) {
            return "카테고리를 선택한 후 다시 시도하세요.";
        }
        return "입력값과 설정을 다시 확인하세요.";
    }
}
