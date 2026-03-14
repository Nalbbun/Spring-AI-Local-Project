package ai.local.nalbbun.rag.ingest;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ai.local.nalbbun.rag.config.RagProperties;
import ai.local.nalbbun.rag.reader.RagDocumentReaderService;
import ai.local.nalbbun.rag.trace.DebugRagTraceService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RagDocumentIngestionService {

    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RagProperties ragProperties;
    private final RagDocumentReaderService ragDocumentReaderService;
    private final ObjectProvider<DebugRagTraceService> debugRagTraceServiceProvider;

    public RagIngestionResult ingestText(RagIngestCommand command) {
        String traceId = startTrace("INGEST_TEXT", Map.of(
                "category", command.getCategory() == null ? "-" : command.getCategory().name(),
                "title", blankToDefault(command.getTitle(), "manual-text")
        ));
        try {
            validateCategory(command.getCategory());
            if (command.getText() == null || command.getText().isBlank()) {
                throw new IllegalArgumentException("text는 비어 있을 수 없습니다.");
            }

            String version = resolveVersion(command.getVersion());
            String source = resolveSource(command.getSource(), command.getTitle(), "manual-text");
            String title = blankToDefault(command.getTitle(), source);
            trace(traceId, "INGEST_TEXT", "BUILD_METADATA", "텍스트 문서 메타데이터 구성", Map.of(
                    "source", source,
                    "version", version,
                    "title", title
            ));

            Map<String, Object> metadata = createBaseMetadata(command.getCategory().name(), source, version, title, "manual-text", command.getMetadata());
            Document seed = new Document(command.getText(), metadata);
            RagIngestionResult result = storeDocuments(traceId, "INGEST_TEXT", command.getCategory().name(), source, version, title, List.of(seed));
            success(traceId, "INGEST_TEXT", "COMPLETE", "텍스트 문서 등록 완료", Map.of(
                    "chunkCount", result.chunkCount(),
                    "source", result.source()
            ));
            return result;
        } catch (RuntimeException e) {
            error(traceId, "INGEST_TEXT", "FAILED", "텍스트 문서 등록 실패", Map.of("reason", e.getMessage()));
            throw e;
        }
    }

    public RagIngestionResult ingestFile(RagIngestCommand command, MultipartFile file) {
        String traceId = startTrace("INGEST_FILE", Map.of(
                "category", command.getCategory() == null ? "-" : command.getCategory().name(),
                "fileName", file == null ? "-" : safeFileName(file.getOriginalFilename())
        ));
        try {
            validateCategory(command.getCategory());
            String version = resolveVersion(command.getVersion());
            String fallbackName = file == null ? "upload" : baseName(file.getOriginalFilename());
            String source = resolveSource(command.getSource(), command.getTitle(), fallbackName);
            String title = blankToDefault(command.getTitle(), fallbackName);
            trace(traceId, "INGEST_FILE", "PREPARE_REQUEST", "단일 파일 등록 요청 준비", Map.of(
                    "source", source,
                    "version", version,
                    "title", title
            ));

            RagDocumentReaderService.ReadResult readResult = ragDocumentReaderService.readMultipartFile(
                    file,
                    createBaseMetadata(command.getCategory().name(), source, version, title, "uploaded-file", command.getMetadata()),
                    traceId
            );
            trace(traceId, "INGEST_FILE", "READ_RESULT", "파일 파싱 결과 생성", Map.of(
                    "fileType", readResult.fileType().name(),
                    "documentCount", readResult.documents().size()
            ));

            RagIngestionResult result = storeDocuments(traceId, "INGEST_FILE", command.getCategory().name(), source, version, title, readResult.documents());
            success(traceId, "INGEST_FILE", "COMPLETE", "단일 파일 등록 완료", Map.of(
                    "chunkCount", result.chunkCount(),
                    "source", result.source()
            ));
            return result;
        } catch (RuntimeException e) {
            error(traceId, "INGEST_FILE", "FAILED", "단일 파일 등록 실패", Map.of("reason", e.getMessage()));
            throw e;
        }
    }

    public RagMultiFileIngestionResult ingestFiles(RagIngestCommand command, MultipartFile[] files) {
        String batchTraceId = startTrace("INGEST_FILES", Map.of(
                "category", command.getCategory() == null ? "-" : command.getCategory().name(),
                "requestedFileCount", files == null ? 0 : files.length
        ));
        try {
            validateCategory(command.getCategory());
            if (files == null || files.length == 0) {
                throw new IllegalArgumentException("업로드할 파일을 선택하세요.");
            }

            String version = resolveVersion(command.getVersion());
            String fallbackName = firstAvailableBaseName(files);
            String source = resolveSource(command.getSource(), command.getTitle(), fallbackName);
            String groupTitle = blankToDefault(command.getTitle(), source);
            trace(batchTraceId, "INGEST_FILES", "PREPARE_BATCH", "멀티파일 등록 요청 준비", Map.of(
                    "source", source,
                    "version", version,
                    "title", groupTitle,
                    "requestedFileCount", files.length
            ));

            List<RagFileIngestionItemResult> items = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;
            int totalChunkCount = 0;

            for (MultipartFile file : files) {
                String fileTraceId = startTrace("INGEST_FILE_ITEM", Map.of(
                        "batchTraceId", batchTraceId,
                        "fileName", file == null ? "-" : safeFileName(file.getOriginalFilename())
                ));

                if (file == null || file.isEmpty()) {
                    failCount++;
                    warn(fileTraceId, "INGEST_FILE_ITEM", "VALIDATE_FILE", "빈 파일 건너뜀", Map.of());
                    items.add(new RagFileIngestionItemResult(null, "(empty)", "(empty)", null, source, version, groupTitle, 0, false, fileTraceId, "빈 파일입니다."));
                    continue;
                }

                String fileTitle = blankToDefault(baseName(file.getOriginalFilename()), groupTitle);
                String fileId = UUID.randomUUID().toString().substring(0, 8);
                try {
                    Map<String, Object> perFileMetadata = new LinkedHashMap<>();
                    if (command.getMetadata() != null) {
                        perFileMetadata.putAll(command.getMetadata());
                    }
                    perFileMetadata.put("fileId", fileId);
                    perFileMetadata.put("fileName", safeFileName(file.getOriginalFilename()));
                    perFileMetadata.put("originalFileName", safeFileName(file.getOriginalFilename()));
                    perFileMetadata.put("contentType", blankToDefault(file.getContentType(), "application/octet-stream"));
                    trace(fileTraceId, "INGEST_FILE_ITEM", "PREPARE_FILE", "파일별 메타데이터 구성", Map.of(
                            "fileId", fileId,
                            "title", fileTitle
                    ));

                    RagDocumentReaderService.ReadResult readResult = ragDocumentReaderService.readMultipartFile(
                            file,
                            createBaseMetadata(command.getCategory().name(), source, version, fileTitle, "uploaded-file", perFileMetadata),
                            fileTraceId
                    );
                    trace(fileTraceId, "INGEST_FILE_ITEM", "READ_RESULT", "파일 파싱 결과 생성", Map.of(
                            "fileType", readResult.fileType().name(),
                            "documentCount", readResult.documents().size()
                    ));

                    RagIngestionResult result = storeDocuments(fileTraceId, "INGEST_FILE_ITEM", command.getCategory().name(), source, version, fileTitle, readResult.documents());
                    successCount++;
                    totalChunkCount += result.chunkCount();
                    success(fileTraceId, "INGEST_FILE_ITEM", "COMPLETE", "멀티파일 내 단건 등록 완료", Map.of(
                            "chunkCount", result.chunkCount(),
                            "source", result.source()
                    ));
                    items.add(new RagFileIngestionItemResult(
                            fileId,
                            safeFileName(file.getOriginalFilename()),
                            safeFileName(file.getOriginalFilename()),
                            blankToDefault(file.getContentType(), "application/octet-stream"),
                            result.source(),
                            result.version(),
                            result.title(),
                            result.chunkCount(),
                            result.stored(),
                            fileTraceId,
                            "stored"
                    ));
                } catch (Exception e) {
                    failCount++;
                    error(fileTraceId, "INGEST_FILE_ITEM", "FAILED", "멀티파일 내 단건 등록 실패", Map.of("reason", e.getMessage()));
                    items.add(new RagFileIngestionItemResult(
                            fileId,
                            safeFileName(file.getOriginalFilename()),
                            safeFileName(file.getOriginalFilename()),
                            blankToDefault(file.getContentType(), "application/octet-stream"),
                            source,
                            version,
                            fileTitle,
                            0,
                            false,
                            fileTraceId,
                            e.getMessage()
                    ));
                }
            }

            success(batchTraceId, "INGEST_FILES", "COMPLETE", "멀티파일 등록 완료", Map.of(
                    "successCount", successCount,
                    "failCount", failCount,
                    "totalChunkCount", totalChunkCount
            ));
            return new RagMultiFileIngestionResult(
                    command.getCategory().name(),
                    source,
                    version,
                    files.length,
                    successCount,
                    failCount,
                    totalChunkCount,
                    batchTraceId,
                    items
            );
        } catch (RuntimeException e) {
            error(batchTraceId, "INGEST_FILES", "FAILED", "멀티파일 등록 실패", Map.of("reason", e.getMessage()));
            throw e;
        }
    }

    public RagIngestionResult ingestUrl(RagUrlIngestCommand command) {
        String traceId = startTrace("INGEST_URL", Map.of(
                "category", command.getCategory() == null ? "-" : command.getCategory().name(),
                "url", blankToDefault(command.getUrl(), "-")
        ));
        try {
            validateCategory(command.getCategory());
            String version = resolveVersion(command.getVersion());
            String source = resolveSource(command.getSource(), command.getTitle(), command.getUrl());
            String title = blankToDefault(command.getTitle(), command.getUrl());
            trace(traceId, "INGEST_URL", "PREPARE_REQUEST", "URL 등록 요청 준비", Map.of(
                    "source", source,
                    "version", version,
                    "title", title
            ));
            List<Document> documents = ragDocumentReaderService.readWebUrl(
                    command.getUrl(),
                    title,
                    source,
                    createBaseMetadata(command.getCategory().name(), source, version, title, "web-url", command.getMetadata()),
                    traceId
            );

            RagIngestionResult result = storeDocuments(traceId, "INGEST_URL", command.getCategory().name(), source, version, title, documents);
            success(traceId, "INGEST_URL", "COMPLETE", "URL 문서 등록 완료", Map.of(
                    "chunkCount", result.chunkCount(),
                    "source", result.source()
            ));
            return result;
        } catch (RuntimeException e) {
            error(traceId, "INGEST_URL", "FAILED", "URL 문서 등록 실패", Map.of("reason", e.getMessage()));
            throw e;
        }
    }

    private RagIngestionResult storeDocuments(String traceId, String operation, String category, String source, String version, String title, List<Document> seedDocuments) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            throw new IllegalStateException("VectorStore bean을 찾을 수 없습니다. app.rag.enabled 및 pgvector 설정을 확인하세요.");
        }

        trace(traceId, operation, "SEED_DOCUMENTS_READY", "원본 문서 준비 완료", Map.of(
                "seedDocumentCount", seedDocuments.size(),
                "chunkSize", ragProperties.getIngest().getChunkSize(),
                "maxNumChunks", ragProperties.getIngest().getMaxNumChunks()
        ));

        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(ragProperties.getIngest().getChunkSize())
                .withMinChunkSizeChars(ragProperties.getIngest().getMinChunkSizeChars())
                .withMinChunkLengthToEmbed(ragProperties.getIngest().getMinChunkLengthToEmbed())
                .withMaxNumChunks(ragProperties.getIngest().getMaxNumChunks())
                .withKeepSeparator(true)
                .build();

        trace(traceId, operation, "SPLIT_BEGIN", "청킹 시작", Map.of());
        List<Document> splitDocuments = splitter.apply(seedDocuments);
        trace(traceId, operation, "SPLIT_COMPLETE", "청킹 완료", Map.of("chunkCount", splitDocuments.size()));

        List<Document> enriched = new ArrayList<>();
        for (int i = 0; i < splitDocuments.size(); i++) {
            Document splitDocument = splitDocuments.get(i);
            Map<String, Object> splitMetadata = new LinkedHashMap<>(splitDocument.getMetadata());
            splitMetadata.put("chunkIndex", i);
            splitMetadata.put("source", source);
            splitMetadata.put("version", version);
            splitMetadata.put("title", title);
            enriched.add(new Document(splitDocument.getText(), splitMetadata));
        }

        trace(traceId, operation, "VECTOR_STORE_BEGIN", "벡터 저장 시작", Map.of("chunkCount", enriched.size()));
        vectorStore.accept(enriched);
        success(traceId, operation, "VECTOR_STORE_COMPLETE", "벡터 저장 완료", Map.of("chunkCount", enriched.size()));

        return new RagIngestionResult(
                category,
                blankToDefault(source, "manual"),
                blankToDefault(version, "v1"),
                blankToDefault(title, "manual-ingest"),
                enriched.size(),
                true,
                traceId
        );
    }

    private Map<String, Object> createBaseMetadata(String category, String source, String version, String title, String ingestType, Map<String, Object> metadata) {
        Map<String, Object> base = new LinkedHashMap<>();
        if (metadata != null) {
            base.putAll(metadata);
        }
        base.put("category", category);
        base.put("source", blankToDefault(source, "manual"));
        base.put("version", blankToDefault(version, "v1"));
        base.put("title", blankToDefault(title, "manual-ingest"));
        base.put("ingestType", ingestType);
        base.put("ingestedAt", LocalDateTime.now().toString());
        return base;
    }

    private void validateCategory(Object category) {
        if (category == null) {
            throw new IllegalArgumentException("category는 필수입니다.");
        }
    }

    private String resolveVersion(String version) {
        return blankToDefault(version, "v1");
    }

    private String resolveSource(String source, String title, String fallback) {
        String candidate = firstNonBlank(source, title, fallback, "manual");
        return slugify(candidate);
    }

    private String firstAvailableBaseName(MultipartFile[] files) {
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                return baseName(file.getOriginalFilename());
            }
        }
        return "batch-upload";
    }

    private String baseName(String originalFilename) {
        String fileName = safeFileName(originalFilename);
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(0, idx) : fileName;
    }

    private String safeFileName(String originalFilename) {
        return blankToDefault(originalFilename, "unknown-file");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "manual";
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(blankToDefault(value, "manual"), Normalizer.Form.NFKC)
                .toLowerCase()
                .replaceAll("[^a-z0-9가-힣._-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "manual" : normalized;
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String startTrace(String operation, Map<String, Object> details) {
        DebugRagTraceService service = debugRagTraceServiceProvider.getIfAvailable();
        if (service == null) {
            return UUID.randomUUID().toString().substring(0, 8);
        }
        return service.startTrace(operation, details);
    }

    private void trace(String traceId, String operation, String stage, String message, Map<String, Object> details) {
        DebugRagTraceService service = debugRagTraceServiceProvider.getIfAvailable();
        if (service != null) {
            service.info(traceId, operation, stage, message, details);
        }
    }

    private void success(String traceId, String operation, String stage, String message, Map<String, Object> details) {
        DebugRagTraceService service = debugRagTraceServiceProvider.getIfAvailable();
        if (service != null) {
            service.success(traceId, operation, stage, message, details);
        }
    }

    private void warn(String traceId, String operation, String stage, String message, Map<String, Object> details) {
        DebugRagTraceService service = debugRagTraceServiceProvider.getIfAvailable();
        if (service != null) {
            service.warn(traceId, operation, stage, message, details);
        }
    }

    private void error(String traceId, String operation, String stage, String message, Map<String, Object> details) {
        DebugRagTraceService service = debugRagTraceServiceProvider.getIfAvailable();
        if (service != null) {
            service.error(traceId, operation, stage, message, details);
        }
    }
}
