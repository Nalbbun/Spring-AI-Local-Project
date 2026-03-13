package ai.local.nalbbun.rag.reader;

import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class RagFileTypeDetector {

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "txt", "log", "yaml", "yml", "json", "xml", "csv", "tsv",
            "properties", "conf", "cfg", "ini", "env", "sql",
            "java", "kt", "kts", "groovy", "gradle", "sh", "bash", "zsh",
            "bat", "cmd", "ps1", "py", "js", "jsx", "ts", "tsx",
            "css", "scss", "less", "c", "cpp", "cc", "h", "hpp",
            "cs", "go", "rs", "php", "rb", "swift", "scala", "r",
            "vue", "svelte", "toml", "lock", "gitignore", "editorconfig",
            "mf", "tex", "adoc", "asciidoc", "rst"
    );

    public RagFileType detect(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("업로드 파일명은 비어 있을 수 없습니다.");
        }

        String normalized = filename.trim().toLowerCase(Locale.ROOT);
        String ext = extensionOf(normalized);

        if (normalized.equals("dockerfile") || normalized.endsWith(".env")) {
            return RagFileType.TEXT;
        }
        if (normalized.endsWith(".pdf")) {
            return RagFileType.PDF;
        }
        if (normalized.endsWith(".md") || normalized.endsWith(".markdown")) {
            return RagFileType.MARKDOWN;
        }
        if (normalized.endsWith(".html") || normalized.endsWith(".htm")) {
            return RagFileType.HTML;
        }
        if (TEXT_EXTENSIONS.contains(ext)) {
            return RagFileType.TEXT;
        }

        throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. pdf, md, markdown, txt, log, yaml, yml, json, xml, csv, tsv, html, htm 및 주요 코드/설정 파일을 지원합니다.");
    }

    private String extensionOf(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return filename;
        }
        return filename.substring(idx + 1);
    }
}
