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
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RagDocumentIngestionService {

    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RagProperties ragProperties;
    private final RagDocumentReaderService ragDocumentReaderService;

    public RagIngestionResult ingestText(RagIngestCommand command) {
        validateCategory(command.getCategory());
        if (command.getText() == null || command.getText().isBlank()) {
            throw new IllegalArgumentException("text는 비어 있을 수 없습니다.");
        }

        String version = resolveVersion(command.getVersion());
        String source = resolveSource(command.getSource(), command.getTitle(), "manual-text");
        String title = blankToDefault(command.getTitle(), source);
        Map<String, Object> metadata = createBaseMetadata(command.getCategory().name(), source, version, title, "manual-text", command.getMetadata());
        Document seed = new Document(command.getText(), metadata);
        return storeDocuments(command.getCategory().name(), source, version, title, List.of(seed));
    }

    public RagIngestionResult ingestFile(RagIngestCommand command, MultipartFile file) {
        validateCategory(command.getCategory());
        String version = resolveVersion(command.getVersion());
        String fallbackName = file == null ? "upload" : baseName(file.getOriginalFilename());
        String source = resolveSource(command.getSource(), command.getTitle(), fallbackName);
        String title = blankToDefault(command.getTitle(), fallbackName);

        RagDocumentReaderService.ReadResult readResult = ragDocumentReaderService.readMultipartFile(
                file,
                createBaseMetadata(command.getCategory().name(), source, version, title, "uploaded-file", command.getMetadata())
        );

        return storeDocuments(command.getCategory().name(), source, version, title, readResult.documents());
    }

    public RagMultiFileIngestionResult ingestFiles(RagIngestCommand command, MultipartFile[] files) {
        validateCategory(command.getCategory());
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("업로드할 파일을 선택하세요.");
        }

        String version = resolveVersion(command.getVersion());
        String fallbackName = firstAvailableBaseName(files);
        String source = resolveSource(command.getSource(), command.getTitle(), fallbackName);
        String groupTitle = blankToDefault(command.getTitle(), source);

        List<RagFileIngestionItemResult> items = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        int totalChunkCount = 0;

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                failCount++;
                items.add(new RagFileIngestionItemResult("(empty)", source, version, groupTitle, 0, false, "빈 파일입니다."));
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
                RagDocumentReaderService.ReadResult readResult = ragDocumentReaderService.readMultipartFile(
                        file,
                        createBaseMetadata(command.getCategory().name(), source, version, fileTitle, "uploaded-file", perFileMetadata)
                );
                RagIngestionResult result = storeDocuments(command.getCategory().name(), source, version, fileTitle, readResult.documents());
                successCount++;
                totalChunkCount += result.chunkCount();
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
                        "stored"
                ));
            } catch (Exception e) {
                failCount++;
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
                        e.getMessage()
                ));
            }
        }

        return new RagMultiFileIngestionResult(
                command.getCategory().name(),
                source,
                version,
                files.length,
                successCount,
                failCount,
                totalChunkCount,
                items
        );
    }

    public RagIngestionResult ingestUrl(RagUrlIngestCommand command) {
        validateCategory(command.getCategory());
        String version = resolveVersion(command.getVersion());
        String source = resolveSource(command.getSource(), command.getTitle(), command.getUrl());
        String title = blankToDefault(command.getTitle(), command.getUrl());
        List<Document> documents = ragDocumentReaderService.readWebUrl(
                command.getUrl(),
                title,
                source,
                createBaseMetadata(command.getCategory().name(), source, version, title, "web-url", command.getMetadata())
        );

        return storeDocuments(command.getCategory().name(), source, version, title, documents);
    }

    private RagIngestionResult storeDocuments(String category, String source, String version, String title, List<Document> seedDocuments) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            throw new IllegalStateException("VectorStore bean을 찾을 수 없습니다. app.rag.enabled 및 pgvector 설정을 확인하세요.");
        }

        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(ragProperties.getIngest().getChunkSize())
                .withMinChunkSizeChars(ragProperties.getIngest().getMinChunkSizeChars())
                .withMinChunkLengthToEmbed(ragProperties.getIngest().getMinChunkLengthToEmbed())
                .withMaxNumChunks(ragProperties.getIngest().getMaxNumChunks())
                .withKeepSeparator(true)
                .build();

        List<Document> splitDocuments = splitter.apply(seedDocuments);
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

        vectorStore.accept(enriched);

        return new RagIngestionResult(
                category,
                blankToDefault(source, "manual"),
                blankToDefault(version, "v1"),
                blankToDefault(title, "manual-ingest"),
                enriched.size(),
                true
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
}
