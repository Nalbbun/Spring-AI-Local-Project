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
import ai.local.nalbbun.rag.model.RagSourceFilePurgeCommand;
import ai.local.nalbbun.rag.model.RagSourcePurgeCommand;
import ai.local.nalbbun.rag.model.RagSourceReindexCommand;
import ai.local.nalbbun.rag.service.RagSourceAdminService;
import ai.local.nalbbun.rag.service.RagSourceCatalogService;
import ai.local.nalbbun.rag.service.RagSupportService;
import ai.local.nalbbun.rag.trace.DebugRagTraceService;
import lombok.RequiredArgsConstructor;

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

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("enabled", ragProperties.isEnabled());
        response.put("vectorStore", ragProperties.getVectorStore());
        response.put("topK", ragProperties.getTopK());
        response.put("similarityThreshold", ragProperties.getSimilarityThreshold());
        response.put("categories", ragProperties.getCategories());
        response.put("registryBaseDir", ragProperties.getRegistry().getBaseDir());
        response.put("maxUploadFileCount", ragProperties.getIngest().getMaxUploadFileCount());
        response.put("supportedReaders", List.of("pdf", "markdown", "html", "text-like", "web-url"));
        return response;
    }

    @GetMapping("/db-info")
    public Map<String, Object> dbInfo() {
        return debugDatabaseInfoService.getInfo();
    }

    @GetMapping("/sources")
    public List<?> sources(@RequestParam("category") ChatCategory category,
                           @RequestParam(value = "source", required = false) String source,
                           @RequestParam(value = "version", required = false) String version) {
        return ragSourceCatalogService.listSources(category, source, version);
    }

    @GetMapping("/source/files")
    public List<?> sourceFiles(@RequestParam("category") ChatCategory category,
                               @RequestParam("source") String source,
                               @RequestParam("version") String version) {
        return ragSourceCatalogService.listFiles(category, source, version);
    }

    @PostMapping("/source/purge")
    public Map<String, Object> purgeSource(@RequestBody RagSourcePurgeCommand command) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("result", ragSourceAdminService.purgeSource(command));
        response.put("sources", ragSourceCatalogService.listSources(command.getCategory(), null, null));
        return response;
    }

    @PostMapping("/source/file/purge")
    public Map<String, Object> purgeSourceFile(@RequestBody RagSourceFilePurgeCommand command) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("result", ragSourceAdminService.purgeSourceFile(command));
        response.put("files", ragSourceCatalogService.listFiles(command.getCategory(), command.getSource(), command.getVersion()));
        return response;
    }

    @PostMapping("/source/reindex")
    public Map<String, Object> reindexSource(@RequestBody RagSourceReindexCommand command) {
        Map<String, Object> response = new LinkedHashMap<>();
        var result = ragSourceAdminService.reindexSource(command);
        response.put("result", result);
        response.put("sources", ragSourceCatalogService.listSources(command.getCategory(), command.getSource(), null));
        response.put("files", ragSourceCatalogService.listFiles(command.getCategory(), command.getSource(), result.targetVersion()));
        return response;
    }

    @GetMapping("/source/compare")
    public Object compareSourceVersions(@RequestParam("category") ChatCategory category,
                                        @RequestParam("source") String source,
                                        @RequestParam("leftVersion") String leftVersion,
                                        @RequestParam("rightVersion") String rightVersion,
                                        @RequestParam(value = "query", required = false) String query) {
        return ragSourceAdminService.compare(category, source, leftVersion, rightVersion, query);
    }

    @GetMapping("/search")
    public RagContext search(@RequestParam("category") ChatCategory category,
                             @RequestParam("query") String query,
                             @RequestParam(value = "source", required = false) String source,
                             @RequestParam(value = "version", required = false) String version) {
        return ragSupportService.buildContext(category, query, source, version);
    }

    @GetMapping("/traces")
    public Map<String, Object> traces(@RequestParam(value = "limit", defaultValue = "150") int limit) {
        return debugRagTraceService.latest(limit);
    }

    @GetMapping("/traces/{traceId}")
    public Map<String, Object> traceById(@PathVariable("traceId") String traceId) {
        return debugRagTraceService.byTraceId(traceId);
    }

    @PostMapping("/traces/clear")
    public Map<String, Object> clearTraces() {
        debugRagTraceService.clear();
        return Map.of("status", "cleared");
    }

    @PostMapping("/ingest-text")
    public Map<String, Object> ingestText(@RequestBody RagIngestCommand command) {
        RagIngestionResult result = ragDocumentIngestionService.ingestText(command);
        return ingestResponse(command.getCategory(), result.source(), result.version(), result);
    }

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
        command.setMetadata(parseMetadataJson(metadataJson));

        RagIngestionResult result = ragDocumentIngestionService.ingestFile(command, file);
        return ingestResponse(category, result.source(), result.version(), result);
    }

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
        command.setMetadata(parseMetadataJson(metadataJson));

        RagMultiFileIngestionResult result = ragDocumentIngestionService.ingestFiles(command, files);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "stored");
        response.put("result", result);
        response.put("manifest", ragSourceCatalogService.listSources(category, result.source(), result.version()).stream().findFirst().orElse(null));
        response.put("files", ragSourceCatalogService.listFiles(category, result.source(), result.version()));
        return response;
    }

    @PostMapping("/ingest-url")
    public Map<String, Object> ingestUrl(@RequestBody RagUrlIngestCommand command) {
        RagIngestionResult result = ragDocumentIngestionService.ingestUrl(command);
        return ingestResponse(command.getCategory(), result.source(), result.version(), result);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(RuntimeException e) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", "bad_request");
        response.put("message", e.getMessage());
        response.put("hint", hintFor(e.getMessage()));
        return response;
    }

    private Map<String, Object> ingestResponse(ChatCategory category, String source, String version, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "stored");
        response.put("result", result);
        response.put("manifest", ragSourceCatalogService.listSources(category, source, version).stream().findFirst().orElse(null));
        response.put("files", ragSourceCatalogService.listFiles(category, source, version));
        response.put("sources", ragSourceCatalogService.listSources(category, null, null));
        return response;
    }

    private Map<String, Object> parseMetadataJson(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> value = new tools.jackson.databind.json.JsonMapper().readValue(metadataJson, LinkedHashMap.class);
            return value == null ? Map.of() : value;
        } catch (Exception e) {
            throw new IllegalArgumentException("metadataJson 파싱 실패: " + e.getMessage(), e);
        }
    }

    private String hintFor(String message) {
        if (message == null) {
            return "입력값과 설정을 다시 확인하세요.";
        }
        if (message.contains("지원하지 않는 파일 형식")) {
            return "pdf, md, markdown, txt, log, yaml, yml, json, xml, csv, tsv, html, htm 및 주요 코드/설정 파일을 사용하세요.";
        }
        if (message.contains("업로드할 파일을 선택")) {
            return "파일 선택 후 다시 업로드하세요.";
        }
        if (message.contains("멀티파일 업로드 개수 제한")) {
            return "debug 설정의 RAG Max Upload File Count 값을 확인하세요.";
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
