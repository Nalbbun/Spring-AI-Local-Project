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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.rag.config.RagProperties;
import ai.local.nalbbun.rag.ingest.RagDocumentIngestionService;
import ai.local.nalbbun.rag.ingest.RagIngestCommand;
import ai.local.nalbbun.rag.ingest.RagUrlIngestCommand;
import ai.local.nalbbun.rag.model.RagContext;
import ai.local.nalbbun.rag.service.RagSupportService;
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

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("enabled", ragProperties.isEnabled());
        response.put("vectorStore", ragProperties.getVectorStore());
        response.put("topK", ragProperties.getTopK());
        response.put("similarityThreshold", ragProperties.getSimilarityThreshold());
        response.put("categories", ragProperties.getCategories());
        response.put("supportedReaders", List.of("text", "file-single", "file-multi", "pdf", "markdown", "text-like", "web-url"));
        return response;
    }

    @GetMapping("/search")
    public RagContext search(@RequestParam("category") ChatCategory category,
                             @RequestParam("query") String query) {
        return ragSupportService.buildContext(category, query);
    }

    @PostMapping("/ingest-text")
    public Map<String, Object> ingestText(@RequestBody RagIngestCommand command) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("result", ragDocumentIngestionService.ingestText(command));
        response.put("status", "stored");
        return response;
    }

    @PostMapping(value = "/ingest-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> ingestFile(@RequestParam("category") ChatCategory category,
                                          @RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "source", required = false) String source,
                                          @RequestParam(value = "version", required = false) String version,
                                          @RequestParam(value = "title", required = false) String title) {
        RagIngestCommand command = new RagIngestCommand();
        command.setCategory(category);
        command.setSource(source);
        command.setVersion(version);
        command.setTitle(title);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("result", ragDocumentIngestionService.ingestFile(command, file));
        response.put("status", "stored");
        return response;
    }

    @PostMapping(value = "/ingest-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> ingestFiles(@RequestParam("category") ChatCategory category,
                                           @RequestParam("files") MultipartFile[] files,
                                           @RequestParam(value = "source", required = false) String source,
                                           @RequestParam(value = "version", required = false) String version,
                                           @RequestParam(value = "title", required = false) String title) {
        RagIngestCommand command = new RagIngestCommand();
        command.setCategory(category);
        command.setSource(source);
        command.setVersion(version);
        command.setTitle(title);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("result", ragDocumentIngestionService.ingestFiles(command, files));
        response.put("status", "stored");
        return response;
    }

    @PostMapping("/ingest-url")
    public Map<String, Object> ingestUrl(@RequestBody RagUrlIngestCommand command) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("result", ragDocumentIngestionService.ingestUrl(command));
        response.put("status", "stored");
        return response;
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
