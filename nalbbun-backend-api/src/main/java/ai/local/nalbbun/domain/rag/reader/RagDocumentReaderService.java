package ai.local.nalbbun.domain.rag.reader;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Rag Document Reader Service 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
@Service
@RequiredArgsConstructor
public class RagDocumentReaderService {

    private static final Logger log = LoggerFactory.getLogger(RagDocumentReaderService.class);

    private final RagFileTypeDetector ragFileTypeDetector;

    /**
     * read Multipart File 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * read Stored Resource 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * read Pdf 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public ReadResult readPdf(Resource resource, String title, String source, Map<String, Object> metadata) {
        return new ReadResult(RagFileType.PDF, readPdf(resource, enrichMetadata(metadata, title, source, "pdf")), title);
    }

    /**
     * read Markdown 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public ReadResult readMarkdown(Resource resource, String title, String source, Map<String, Object> metadata) {
        return new ReadResult(RagFileType.MARKDOWN, readMarkdown(resource, enrichMetadata(metadata, title, source, "markdown")), title);
    }

    /**
     * read Web Url 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * read Pdf 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private List<Document> readPdf(Resource resource, Map<String, Object> metadata) {
        PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                .withPagesPerDocument(1)
                .build();

        try {
            List<Document> documents = new PagePdfDocumentReader(resource, config).read();
            return applyMetadata(documents, metadata);
        }
        catch (RuntimeException e) {
            log.warn("PDF 레이아웃 추출 실패. 기본 PDF 파서로 재시도합니다. message={}", e.getMessage(), e);
            try {
                List<Document> fallbackDocuments = readPdfWithBasicStripper(resource, metadata, e);
                if (!fallbackDocuments.isEmpty()) {
                    return fallbackDocuments;
                }
            }
            catch (Exception fallbackException) {
                throw buildPdfReadException(e, fallbackException);
            }
            throw buildPdfReadException(e, null);
        }
    }

    private List<Document> readPdfWithBasicStripper(Resource resource,
                                                    Map<String, Object> metadata,
                                                    RuntimeException layoutException) throws IOException {
    	
        try (PDDocument pdDocument = Loader.loadPDF(resource.getInputStream().readAllBytes())) {
        	
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = pdDocument.getNumberOfPages();
            List<Document> documents = new ArrayList<>();

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(pdDocument);
                if (text == null || text.isBlank()) {
                    continue;
                }

                Map<String, Object> pageMetadata = new LinkedHashMap<>();
                if (metadata != null) {
                    pageMetadata.putAll(metadata);
                }
                pageMetadata.put("readerType", "pdf-basic-fallback");
                pageMetadata.put("pdfFallbackUsed", true);
                pageMetadata.put("pdfFallbackReason", safeMessage(layoutException));
                pageMetadata.put("pageNumber", page);
                pageMetadata.put("totalPages", totalPages);
                documents.add(new Document(text, pageMetadata));
            }

            log.info("PDF 기본 파서 fallback 완료. pages={}, extractedDocuments={}", totalPages, documents.size());
            return documents;
        }
    }

    private IllegalStateException buildPdfReadException(RuntimeException layoutException, Exception fallbackException) {
        String baseMessage = "PDF 문서 분석 중 오류가 발생했습니다. 스캔본/복잡한 레이아웃 PDF일 수 있습니다. "
                + "텍스트 추출이 가능한 PDF로 다시 저장하거나, TXT/Markdown으로 변환 후 업로드해 주세요.";

        if (fallbackException == null) {
            return new IllegalStateException(baseMessage + " (layout-reader=" + safeMessage(layoutException) + ")", layoutException);
        }

        IllegalStateException exception = new IllegalStateException(
                baseMessage + " (layout-reader=" + safeMessage(layoutException) + ", basic-reader=" + safeMessage(fallbackException) + ")",
                fallbackException
        );
        exception.addSuppressed(layoutException);
        return exception;
    }

    private String safeMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return throwable == null ? "unknown" : throwable.getClass().getSimpleName();
        }
        return throwable.getMessage();
    }

    /**
     * read Markdown 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * read Text 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * enrich Metadata 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * apply Metadata 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * validate Url 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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

    public record ReadResult(RagFileType fileType, List<Document> documents, String displayName) {
    }
}
