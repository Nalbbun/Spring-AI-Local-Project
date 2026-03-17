package ai.local.nalbbun.rag.reader;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.jsoup.config.JsoupDocumentReaderConfig;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

/**
 * RagDocumentReaderService는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: rag document reader service 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Service
@RequiredArgsConstructor
public class RagDocumentReaderService {

    /** ragFileTypeDetector 값을 보관한다. */
    private final RagFileTypeDetector ragFileTypeDetector;

    /**
     * 대상 정보를 조회한다.
     *
     * @param file 처리 대상 파일 정보
     * @param metadata metadata 매핑 정보
     * @return ReadResult 타입의 처리 결과
     */
    public ReadResult readMultipartFile(MultipartFile file, Map<String, Object> metadata) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드 파일이 비어 있습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        RagFileType fileType = ragFileTypeDetector.detect(originalFilename);

        try {
            Resource resource = new NamedByteArrayResource(file.getBytes(), originalFilename);
            return switch (fileType) {
                case PDF -> new ReadResult(fileType, readPdf(resource, metadata), originalFilename);
                case MARKDOWN -> new ReadResult(fileType, readMarkdown(resource, metadata), originalFilename);
                case TEXT -> new ReadResult(fileType, readText(resource, metadata), originalFilename);
            };
        }
        catch (IOException e) {
            throw new IllegalStateException("업로드 파일을 읽지 못했습니다.", e);
        }
    }


    /**
     * 대상 정보를 조회한다.
     *
     * @param resource resource 값
     * @param originalFilename originalFilename 값
     * @param metadata metadata 매핑 정보
     * @return ReadResult 타입의 처리 결과
     */
    public ReadResult readStoredResource(Resource resource, String originalFilename, Map<String, Object> metadata) {
        RagFileType fileType = ragFileTypeDetector.detect(originalFilename);
        return switch (fileType) {
            case PDF -> new ReadResult(fileType, readPdf(resource, metadata), originalFilename);
            case MARKDOWN -> new ReadResult(fileType, readMarkdown(resource, metadata), originalFilename);
            case TEXT -> new ReadResult(fileType, readText(resource, metadata), originalFilename);
        };
    }

    /**
     * 대상 정보를 조회한다.
     *
     * @param resource resource 값
     * @param title title 값
     * @param source source 값
     * @param metadata metadata 매핑 정보
     * @return ReadResult 타입의 처리 결과
     */
    public ReadResult readPdf(Resource resource, String title, String source, Map<String, Object> metadata) {
        return new ReadResult(RagFileType.PDF, readPdf(resource, enrichMetadata(metadata, title, source, "pdf")), title);
    }

    /**
     * 대상 정보를 조회한다.
     *
     * @param resource resource 값
     * @param title title 값
     * @param source source 값
     * @param metadata metadata 매핑 정보
     * @return ReadResult 타입의 처리 결과
     */
    public ReadResult readMarkdown(Resource resource, String title, String source, Map<String, Object> metadata) {
        return new ReadResult(RagFileType.MARKDOWN, readMarkdown(resource, enrichMetadata(metadata, title, source, "markdown")), title);
    }

    /**
     * 대상 정보를 조회한다.
     *
     * @param url 대상 URL
     * @param title title 값
     * @param source source 값
     * @param metadata metadata 매핑 정보
     * @return 조회 또는 생성된 목록
     */
    public List<Document> readWebUrl(String url, String title, String source, Map<String, Object> metadata) {
        validateUrl(url);

        JsoupDocumentReaderConfig config = JsoupDocumentReaderConfig.builder()
                .selector("body")
                .includeLinkUrls(true)
                .metadataTags(List.of("description", "keywords"))
                .additionalMetadata(enrichMetadata(metadata, title, source == null || source.isBlank() ? url : source, "web-url"))
                .build();

        return new JsoupDocumentReader(url, config).read();
    }

    /**
     * 대상 정보를 조회한다.
     *
     * @param resource resource 값
     * @param metadata metadata 매핑 정보
     * @return 조회 또는 생성된 목록
     */
    private List<Document> readPdf(Resource resource, Map<String, Object> metadata) {
        PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                .withPagesPerDocument(1)
                .build();

        List<Document> documents = new PagePdfDocumentReader(resource, config).read();
        return applyMetadata(documents, metadata);
    }

    /**
     * 대상 정보를 조회한다.
     *
     * @param resource resource 값
     * @param metadata metadata 매핑 정보
     * @return 조회 또는 생성된 목록
     */
    private List<Document> readMarkdown(Resource resource, Map<String, Object> metadata) {
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(true)
                .withIncludeBlockquote(true)
                .withAdditionalMetadata(metadata)
                .build();

        return new MarkdownDocumentReader(resource, config).read();
    }

    /**
     * 대상 정보를 조회한다.
     *
     * @param resource resource 값
     * @param metadata metadata 매핑 정보
     * @return 조회 또는 생성된 목록
     */
    private List<Document> readText(Resource resource, Map<String, Object> metadata) {
        try {
            String text = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return List.of(new Document(text, metadata));
        } catch (IOException e) {
            throw new IllegalStateException("텍스트 파일을 읽지 못했습니다.", e);
        }
    }

    /**
     * enrichMetadata 기능을 수행한다.
     *
     * @param metadata metadata 매핑 정보
     * @param title title 값
     * @param source source 값
     * @param readerType readerType 값
     * @return 키와 값으로 구성된 결과 매핑
     */
    private Map<String, Object> enrichMetadata(Map<String, Object> metadata, String title, String source, String readerType) {
        Map<String, Object> enriched = new LinkedHashMap<>();
        if (metadata != null) {
            enriched.putAll(metadata);
        }
        if (title != null && !title.isBlank()) {
            enriched.put("title", title);
        }
        if (source != null && !source.isBlank()) {
            enriched.put("source", source);
        }
        enriched.put("readerType", readerType);
        return enriched;
    }

    /**
     * applyMetadata 기능을 수행한다.
     *
     * @param documents documents 목록 정보
     * @param metadata metadata 매핑 정보
     * @return 조회 또는 생성된 목록
     */
    private List<Document> applyMetadata(List<Document> documents, Map<String, Object> metadata) {
        return documents.stream()
                .map(document -> {
                    Map<String, Object> merged = new LinkedHashMap<>();
                    if (document.getMetadata() != null) {
                        merged.putAll(document.getMetadata());
                    }
                    merged.putAll(metadata);
                    return new Document(document.getText(), merged);
                })
                .toList();
    }

    /**
     * validateUrl 기능을 수행한다.
     *
     * @param url 대상 URL
     */
    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url은 비어 있을 수 없습니다.");
        }
        URI uri = URI.create(url);
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("웹 URL은 http 또는 https 이어야 합니다.");
        }
    }

    /**
     * ReadResult는 RAG 관련 처리와 관리 기능을 담당하는 컴포넌트이다.
     * <p>주요 기능: read result 관련 책임을 수행한다.</p>
     * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
     * @param fileType fileType 값
     * @param documents documents 값
     * @param displayName displayName 값
     */
    public record ReadResult(RagFileType fileType, List<Document> documents, String displayName) {
    }
}
