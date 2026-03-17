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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.rag.config.RagProperties;
import ai.local.nalbbun.rag.reader.RagDocumentReaderService;
import ai.local.nalbbun.rag.service.RuntimeOllamaVectorStoreFactory;
import lombok.RequiredArgsConstructor;

/**
 * RagDocumentIngestionService는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: rag document ingestion service 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Service
@RequiredArgsConstructor
public class RagDocumentIngestionService {

    /** runtimeVectorStoreFactory 값을 보관한다. */
    private final RuntimeOllamaVectorStoreFactory runtimeVectorStoreFactory;
    /** ragProperties 값을 보관한다. */
    private final RagProperties ragProperties;
    /** ragDocumentReaderService 값을 보관한다. */
    private final RagDocumentReaderService ragDocumentReaderService;

    /**
     * ingestText 기능을 수행한다.
     *
     * @param command 실행 명령 정보
     * @return RagIngestionResult 타입의 처리 결과
     */
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

    /**
     * ingestFile 기능을 수행한다.
     *
     * @param command 실행 명령 정보
     * @param file 처리 대상 파일 정보
     * @return RagIngestionResult 타입의 처리 결과
     */
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

    /**
     * ingestFiles 기능을 수행한다.
     *
     * @param command 실행 명령 정보
     * @param files 처리 대상 파일 정보
     * @return RagMultiFileIngestionResult 타입의 처리 결과
     */
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


    /**
     * ingestReconstructedFile 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     * @param source source 값
     * @param version version 값
     * @param title title 값
     * @param fileId fileId 식별자 값
     * @param fileName fileName 값
     * @param originalFileName originalFileName 값
     * @param contentType contentType 값
     * @param text 본문 또는 텍스트 내용
     * @param metadata metadata 매핑 정보
     * @return RagIngestionResult 타입의 처리 결과
     */
    public RagIngestionResult ingestReconstructedFile(
            ChatCategory category,
            String source,
            String version,
            String title,
            String fileId,
            String fileName,
            String originalFileName,
            String contentType,
            String text,
            Map<String, Object> metadata
    ) {
        validateCategory(category);
        String resolvedVersion = resolveVersion(version);
        String resolvedSource = resolveSource(source, title, fileName);
        String resolvedTitle = blankToDefault(title, originalFileName);

        Map<String, Object> baseMetadata = createBaseMetadata(category.name(), resolvedSource, resolvedVersion, resolvedTitle, "reconstructed-file", metadata);
        baseMetadata.put("fileId", blankToDefault(fileId, UUID.randomUUID().toString().substring(0, 8)));
        baseMetadata.put("fileName", safeFileName(fileName));
        baseMetadata.put("originalFileName", safeFileName(originalFileName));
        baseMetadata.put("contentType", blankToDefault(contentType, "text/plain"));

        List<Document> seedDocuments = List.of(new Document(blankToDefault(text, ""), baseMetadata));
        return storeDocuments(category.name(), resolvedSource, resolvedVersion, resolvedTitle, seedDocuments);
    }

    /**
     * ingestUrl 기능을 수행한다.
     *
     * @param command 실행 명령 정보
     * @return RagIngestionResult 타입의 처리 결과
     */
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

    /**
     * storeDocuments 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     * @param source source 값
     * @param version version 값
     * @param title title 값
     * @param seedDocuments seedDocuments 목록 정보
     * @return RagIngestionResult 타입의 처리 결과
     */
    private RagIngestionResult storeDocuments(String category, String source, String version, String title, List<Document> seedDocuments) {
        VectorStore vectorStore = runtimeVectorStoreFactory.create();

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

    /**
     * 새 항목 또는 결과를 생성한다.
     *
     * @param category 대상 카테고리 정보
     * @param source source 값
     * @param version version 값
     * @param title title 값
     * @param ingestType ingestType 값
     * @param metadata metadata 매핑 정보
     * @return 키와 값으로 구성된 결과 매핑
     */
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

    /**
     * validateCategory 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     */
    private void validateCategory(Object category) {
        if (category == null) {
            throw new IllegalArgumentException("category는 필수입니다.");
        }
    }

    /**
     * 입력 정보를 해석하여 결과를 결정한다.
     *
     * @param version version 값
     * @return 처리 결과 문자열
     */
    private String resolveVersion(String version) {
        return blankToDefault(version, "v1");
    }

    /**
     * 입력 정보를 해석하여 결과를 결정한다.
     *
     * @param source source 값
     * @param title title 값
     * @param fallback fallback 값
     * @return 처리 결과 문자열
     */
    private String resolveSource(String source, String title, String fallback) {
        String candidate = firstNonBlank(source, title, fallback, "manual");
        return slugify(candidate);
    }

    /**
     * firstAvailableBaseName 기능을 수행한다.
     *
     * @param files 처리 대상 파일 정보
     * @return 처리 결과 문자열
     */
    private String firstAvailableBaseName(MultipartFile[] files) {
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                return baseName(file.getOriginalFilename());
            }
        }
        return "batch-upload";
    }

    /**
     * baseName 기능을 수행한다.
     *
     * @param originalFilename originalFilename 값
     * @return 처리 결과 문자열
     */
    private String baseName(String originalFilename) {
        String fileName = safeFileName(originalFilename);
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(0, idx) : fileName;
    }

    /**
     * safeFileName 기능을 수행한다.
     *
     * @param originalFilename originalFilename 값
     * @return 처리 결과 문자열
     */
    private String safeFileName(String originalFilename) {
        return blankToDefault(originalFilename, "unknown-file");
    }

    /**
     * firstNonBlank 기능을 수행한다.
     *
     * @param values values 값
     * @return 처리 결과 문자열
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "manual";
    }

    /**
     * slugify 기능을 수행한다.
     *
     * @param value value 값
     * @return 처리 결과 문자열
     */
    private String slugify(String value) {
        String normalized = Normalizer.normalize(blankToDefault(value, "manual"), Normalizer.Form.NFKC)
                .toLowerCase()
                .replaceAll("[^a-z0-9가-힣._-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "manual" : normalized;
    }

    /**
     * 값이 비어 있을 때 기본값으로 대체한다.
     *
     * @param value value 값
     * @param defaultValue defaultValue 값
     * @return 처리 결과 문자열
     */
    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
