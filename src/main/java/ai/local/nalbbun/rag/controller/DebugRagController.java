package ai.local.nalbbun.rag.controller;

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

import ai.local.nalbbun.debug.service.DebugDatabaseInfoService;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.rag.config.RagProperties;
import ai.local.nalbbun.rag.ingest.RagDocumentIngestionService;
import ai.local.nalbbun.rag.ingest.RagIngestCommand;
import ai.local.nalbbun.rag.ingest.RagIngestionResult;
import ai.local.nalbbun.rag.ingest.RagMultiFileIngestionResult;
import ai.local.nalbbun.rag.ingest.RagUrlIngestCommand;
import ai.local.nalbbun.rag.model.RagContext;
import ai.local.nalbbun.rag.model.RagSourceFileEntry;
import ai.local.nalbbun.rag.model.RagSourceFilePurgeCommand;
import ai.local.nalbbun.rag.model.RagSourceManifest;
import ai.local.nalbbun.rag.model.RagSourcePurgeCommand;
import ai.local.nalbbun.rag.model.RagSourceReindexCommand;
import ai.local.nalbbun.rag.service.RagSourceAdminService;
import ai.local.nalbbun.rag.service.RagSourceCatalogService;
import ai.local.nalbbun.rag.service.RagSupportService;
import ai.local.nalbbun.rag.trace.DebugRagTraceService;
import lombok.RequiredArgsConstructor;

/**
 * DebugRagController는 HTTP 요청과 응답을 처리하는 컨트롤러이다.
 * <p>주요 기능: debug rag controller 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@RestController
@Profile("local")
@ConditionalOnProperty(prefix = "app.debug", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@RequestMapping("/debug/api/rag")
public class DebugRagController {

    /** ragProperties 값을 보관한다. */
    private final RagProperties ragProperties;
    /** ragSupportService 값을 보관한다. */
    private final RagSupportService ragSupportService;
    /** ragDocumentIngestionService 값을 보관한다. */
    private final RagDocumentIngestionService ragDocumentIngestionService;
    /** ragSourceCatalogService 값을 보관한다. */
    private final RagSourceCatalogService ragSourceCatalogService;
    /** ragSourceAdminService 값을 보관한다. */
    private final RagSourceAdminService ragSourceAdminService;
    /** debugDatabaseInfoService 값을 보관한다. */
    private final DebugDatabaseInfoService debugDatabaseInfoService;
    /** debugRagTraceService 값을 보관한다. */
    private final DebugRagTraceService debugRagTraceService;

    /**
     * status 기능을 수행한다.
     * @return 키와 값으로 구성된 결과 매핑
     */
    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("enabled", ragProperties.isEnabled());
        response.put("vectorStore", ragProperties.getVectorStore());
        response.put("topK", ragProperties.getTopK());
        response.put("similarityThreshold", ragProperties.getSimilarityThreshold());
        response.put("categories", ragProperties.getCategories());
        response.put("registryBaseDir", ragProperties.getRegistry().getBaseDir());
        response.put("supportedReaders", List.of("text", "file-single", "file-multi", "pdf", "markdown", "text-like", "web-url"));
        return response;
    }

    /**
     * dbInfo 기능을 수행한다.
     * @return 키와 값으로 구성된 결과 매핑
     */
    @GetMapping("/db-info")
    public Map<String, Object> dbInfo() {
        return debugDatabaseInfoService.getInfo();
    }

    /**
     * 대상 정보를 조회한다.
     *
     * @param category 대상 카테고리 정보
     * @param query 사용자 입력 또는 질의 내용
     * @param source source 값
     * @param version version 값
     * @return RagContext 타입의 처리 결과
     */
    @GetMapping("/search")
    public RagContext search(@RequestParam("category") ChatCategory category,
                             @RequestParam("query") String query,
                             @RequestParam(value = "source", required = false) String source,
                             @RequestParam(value = "version", required = false) String version) {
        return ragSupportService.buildContext(category, query, source, version);
    }

    /**
     * sources 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     * @param source source 값
     * @param version version 값
     * @return 조회 또는 생성된 목록
     */
    @GetMapping("/sources")
    public List<RagSourceManifest> sources(@RequestParam("category") ChatCategory category,
                                           @RequestParam(value = "source", required = false) String source,
                                           @RequestParam(value = "version", required = false) String version) {
        return ragSourceCatalogService.listSources(category, source, version);
    }

    /**
     * sourceFiles 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     * @param source source 값
     * @param version version 값
     * @return 조회 또는 생성된 목록
     */
    @GetMapping("/source/files")
    public List<RagSourceFileEntry> sourceFiles(@RequestParam("category") ChatCategory category,
                                                @RequestParam("source") String source,
                                                @RequestParam("version") String version) {
        return ragSourceCatalogService.listFiles(category, source, version);
    }

    /**
     * 대상 데이터를 제거한다.
     *
     * @param command 실행 명령 정보
     * @return 키와 값으로 구성된 결과 매핑
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
     * 대상 데이터를 제거한다.
     *
     * @param command 실행 명령 정보
     * @return 키와 값으로 구성된 결과 매핑
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
     * reindexSource 기능을 수행한다.
     *
     * @param command 실행 명령 정보
     * @return 키와 값으로 구성된 결과 매핑
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
     * compare 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     * @param source source 값
     * @param leftVersion leftVersion 값
     * @param rightVersion rightVersion 값
     * @param query 사용자 입력 또는 질의 내용
     * @return 키와 값으로 구성된 결과 매핑
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
     * traces 기능을 수행한다.
     *
     * @param limit limit 값
     * @return 키와 값으로 구성된 결과 매핑
     */
    @GetMapping("/traces")
    public Map<String, Object> traces(@RequestParam(value = "limit", defaultValue = "150") int limit) {
        return debugRagTraceService.latest(limit);
    }

    /**
     * traceById 기능을 수행한다.
     *
     * @param traceId traceId 식별자 값
     * @return 키와 값으로 구성된 결과 매핑
     */
    @GetMapping("/traces/{traceId}")
    public Map<String, Object> traceById(@PathVariable("traceId") String traceId) {
        return debugRagTraceService.byTraceId(traceId);
    }

    /**
     * clearTraces 기능을 수행한다.
     * @return 키와 값으로 구성된 결과 매핑
     */
    @PostMapping("/traces/clear")
    public Map<String, Object> clearTraces() {
        debugRagTraceService.clear();
        return Map.of("status", "cleared");
    }

    /**
     * ingestText 기능을 수행한다.
     *
     * @param command 실행 명령 정보
     * @return 키와 값으로 구성된 결과 매핑
     */
    @PostMapping("/ingest-text")
    public Map<String, Object> ingestText(@RequestBody RagIngestCommand command) {
        RagIngestionResult result = ragDocumentIngestionService.ingestText(command);
        return ingestResponse(command.getCategory(), result.source(), result.version(), result);
    }

    /**
     * ingestFile 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     * @param file 처리 대상 파일 정보
     * @param source source 값
     * @param version version 값
     * @param title title 값
     * @param metadataJson metadataJson 값
     * @return 키와 값으로 구성된 결과 매핑
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
     * ingestFiles 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     * @param files 처리 대상 파일 정보
     * @param source source 값
     * @param version version 값
     * @param title title 값
     * @param metadataJson metadataJson 값
     * @return 키와 값으로 구성된 결과 매핑
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
     * ingestUrl 기능을 수행한다.
     *
     * @param command 실행 명령 정보
     * @return 키와 값으로 구성된 결과 매핑
     */
    @PostMapping("/ingest-url")
    public Map<String, Object> ingestUrl(@RequestBody RagUrlIngestCommand command) {
        RagIngestionResult result = ragDocumentIngestionService.ingestUrl(command);
        return ingestResponse(command.getCategory(), result.source(), result.version(), result);
    }

    /**
     * 요청 또는 상태를 처리한다.
     *
     * @param e e 값
     * @return 키와 값으로 구성된 결과 매핑
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
     * ingestResponse 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     * @param source source 값
     * @param version version 값
     * @param result 처리 결과 객체
     * @return 키와 값으로 구성된 결과 매핑
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

    /**
     * 대상 정보를 조회한다.
     *
     * @param category 대상 카테고리 정보
     * @param source source 값
     * @param version version 값
     * @return RagSourceManifest 타입의 처리 결과
     */
    private RagSourceManifest findManifest(ChatCategory category, String source, String version) {
        return ragSourceCatalogService.listSources(category, source, version)
                .stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * hintFor 기능을 수행한다.
     *
     * @param message 사용자 입력 또는 질의 내용
     * @return 처리 결과 문자열
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
        if (message.contains("category")) {
            return "카테고리를 선택한 후 다시 시도하세요.";
        }
        return "입력값과 설정을 다시 확인하세요.";
    }
}
