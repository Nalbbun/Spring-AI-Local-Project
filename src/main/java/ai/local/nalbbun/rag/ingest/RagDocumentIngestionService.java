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

import ai.local.nalbbun.category.model.ChatCategory;
import ai.local.nalbbun.rag.config.RagProperties;
import ai.local.nalbbun.rag.reader.RagDocumentReaderService;
import ai.local.nalbbun.rag.service.RuntimeOllamaVectorStoreFactory;
import lombok.RequiredArgsConstructor;

/**
 * Rag Document Ingestion Service 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
@Service
@RequiredArgsConstructor
public class RagDocumentIngestionService {

    private final RuntimeOllamaVectorStoreFactory runtimeVectorStoreFactory;
    private final RagProperties ragProperties;
    private final RagDocumentReaderService ragDocumentReaderService;

    /**
     * ingest Text 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * ingest File 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * ingest Files 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * ingest Reconstructed File 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * ingest Url 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * store Documents 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * create Base Metadata 객체를 생성한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * validate Category 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    private void validateCategory(Object category) {
        if (category == null) {
            throw new IllegalArgumentException("category는 필수입니다.");
        }
    }

    /**
     * resolve Version 결과를 계산한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String resolveVersion(String version) {
        return blankToDefault(version, "v1");
    }

    /**
     * resolve Source 결과를 계산한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String resolveSource(String source, String title, String fallback) {
        String candidate = firstNonBlank(source, title, fallback, "manual");
        return slugify(candidate);
    }

    /**
     * first Available Base Name 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * base Name 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String baseName(String originalFilename) {
        String fileName = safeFileName(originalFilename);
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(0, idx) : fileName;
    }

    /**
     * safe File Name 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String safeFileName(String originalFilename) {
        return blankToDefault(originalFilename, "unknown-file");
    }

    /**
     * first Non Blank 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * blank To Default 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
