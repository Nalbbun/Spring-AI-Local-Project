package ai.local.nalbbun.rag.reader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
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

@Service
@RequiredArgsConstructor
public class RagDocumentReaderService {

    private final RagFileTypeDetector ragFileTypeDetector;

    public ReadResult readMultipartFile(MultipartFile file, Map<String, Object> metadata) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드 파일이 비어 있습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        RagFileType fileType = ragFileTypeDetector.detect(originalFilename);

        try {
            Resource resource = new NamedByteArrayResource(file.getBytes(), originalFilename);
            return readByFileType(fileType, resource, originalFilename, metadata);
        }
        catch (IOException e) {
            throw new IllegalStateException("업로드 파일을 읽지 못했습니다.", e);
        }
    }

    public ReadResult readStoredResource(Resource resource, String originalFilename, Map<String, Object> metadata) {
        RagFileType fileType = ragFileTypeDetector.detect(originalFilename);
        return readByFileType(fileType, resource, originalFilename, metadata);
    }

    public ReadResult readPdf(Resource resource, String title, String source, Map<String, Object> metadata) {
        return new ReadResult(RagFileType.PDF, readPdf(resource, enrichMetadata(metadata, title, source, "pdf")), title);
    }

    public ReadResult readMarkdown(Resource resource, String title, String source, Map<String, Object> metadata) {
        return new ReadResult(RagFileType.MARKDOWN, readMarkdown(resource, enrichMetadata(metadata, title, source, "markdown")), title);
    }

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

    private ReadResult readByFileType(RagFileType fileType, Resource resource, String displayName, Map<String, Object> metadata) {
        return switch (fileType) {
            case PDF -> new ReadResult(fileType, readPdf(resource, metadata), displayName);
            case MARKDOWN -> new ReadResult(fileType, readMarkdown(resource, metadata), displayName);
            case HTML -> new ReadResult(fileType, readHtml(resource, metadata), displayName);
            case TEXT -> new ReadResult(fileType, readText(resource, metadata), displayName);
        };
    }

    private List<Document> readPdf(Resource resource, Map<String, Object> metadata) {
        PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                .withPagesPerDocument(1)
                .build();

        List<Document> documents = new PagePdfDocumentReader(resource, config).read();
        return applyMetadata(documents, metadata);
    }

    private List<Document> readMarkdown(Resource resource, Map<String, Object> metadata) {
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(true)
                .withIncludeBlockquote(true)
                .withAdditionalMetadata(metadata)
                .build();

        return new MarkdownDocumentReader(resource, config).read();
    }

    private List<Document> readHtml(Resource resource, Map<String, Object> metadata) {
        try (InputStream inputStream = resource.getInputStream()) {
            String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            String text = Jsoup.parse(html).text();
            return singleDocument(text, metadata);
        }
        catch (IOException e) {
            throw new IllegalStateException("HTML 파일을 읽지 못했습니다.", e);
        }
    }

    private List<Document> readText(Resource resource, Map<String, Object> metadata) {
        try (InputStream inputStream = resource.getInputStream()) {
            String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return singleDocument(text, metadata);
        }
        catch (IOException e) {
            throw new IllegalStateException("텍스트 파일을 읽지 못했습니다.", e);
        }
    }

    private List<Document> singleDocument(String text, Map<String, Object> metadata) {
        List<Document> documents = new ArrayList<>(1);
        documents.add(new Document(text, metadata));
        return documents;
    }

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

    private List<Document> applyMetadata(List<Document> documents, Map<String, Object> metadata) {
        List<Document> mergedDocuments = new ArrayList<>();
        for (Document document : documents) {
            Map<String, Object> merged = new LinkedHashMap<>();
            if (document.getMetadata() != null) {
                merged.putAll(document.getMetadata());
            }
            if (metadata != null) {
                merged.putAll(metadata);
            }
            mergedDocuments.add(new Document(document.getText(), merged));
        }
        return mergedDocuments;
    }

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
