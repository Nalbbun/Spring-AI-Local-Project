package ai.local.nalbbun.rag.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.rag.config.RagProperties;
import ai.local.nalbbun.rag.model.RagSourceFileEntry;
import ai.local.nalbbun.rag.model.RagSourceManifest;
import lombok.RequiredArgsConstructor;

/**
 * RagSourceRegistryService는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: rag source registry service 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Service
@RequiredArgsConstructor
public class RagSourceRegistryService {

    /** FILE_LIST_TYPE 값을 보관한다. */
    private static final TypeReference<List<RagSourceFileEntry>> FILE_LIST_TYPE = new TypeReference<>() {};
    /** ragProperties 값을 보관한다. */
    private final RagProperties ragProperties;
    /** objectMapper 값을 보관한다. */
    private final ObjectMapper objectMapper;

    /**
     * upsertText 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     * @param source source 값
     * @param version version 값
     * @param title title 값
     * @param chunkCount chunkCount 값
     * @param metadata metadata 매핑 정보
     * @return RagSourceManifest 타입의 처리 결과
     */
    public RagSourceManifest upsertText(ChatCategory category, String source, String version, String title, int chunkCount, Map<String, Object> metadata) {
        return upsertEntry(category, source, version, title, chunkCount, "manual-text", "manual-text", "text/plain", null, metadata);
    }

    /**
     * upsertUploadedFile 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     * @param source source 값
     * @param version version 값
     * @param title title 값
     * @param fileId fileId 식별자 값
     * @param fileName fileName 값
     * @param originalFileName originalFileName 값
     * @param contentType contentType 값
     * @param chunkCount chunkCount 값
     * @param metadata metadata 매핑 정보
     * @return RagSourceManifest 타입의 처리 결과
     */
    public RagSourceManifest upsertUploadedFile(ChatCategory category, String source, String version, String title, String fileId, String fileName, String originalFileName, String contentType, int chunkCount, Map<String, Object> metadata) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("fileName", blankToDefault(fileName, blankToDefault(originalFileName, title)));
        extra.put("originalFileName", blankToDefault(originalFileName, blankToDefault(fileName, title)));
        return upsertEntry(category, source, version, title, chunkCount, safeFileId(fileId), "uploaded-file", contentType, null, enrich(metadata, extra));
    }

    /**
     * upsertUrl 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     * @param source source 값
     * @param version version 값
     * @param title title 값
     * @param url 대상 URL
     * @param chunkCount chunkCount 값
     * @param metadata metadata 매핑 정보
     * @return RagSourceManifest 타입의 처리 결과
     */
    public RagSourceManifest upsertUrl(ChatCategory category, String source, String version, String title, String url, int chunkCount, Map<String, Object> metadata) {
        return upsertEntry(category, source, version, title, chunkCount, safeFileId(null), "web-url", "text/html", url, metadata);
    }

    /**
     * 대상 정보를 조회한다.
     *
     * @param category 대상 카테고리 정보
     * @param source source 값
     * @param version version 값
     * @return 조회 또는 생성된 목록
     */
    public List<RagSourceManifest> listManifests(ChatCategory category, String source, String version) {
        List<RagSourceManifest> items = new ArrayList<>();
        Path base = baseDir();
        if (!Files.exists(base)) {
            return items;
        }
        try (Stream<Path> stream = Files.walk(base)) {
            stream.filter(path -> path.getFileName().toString().equals("manifest.json"))
                    .forEach(path -> {
                        RagSourceManifest manifest = readManifestFile(path);
                        if (manifest != null && matches(manifest, category, source, version)) {
                            items.add(manifest);
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        items.sort(Comparator.comparing(RagSourceManifest::getLastIndexedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RagSourceManifest::getSource, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(RagSourceManifest::getVersion, Comparator.nullsLast(String::compareToIgnoreCase)));
        return items;
    }

    /**
     * 대상 정보를 조회한다.
     *
     * @param category 대상 카테고리 정보
     * @param source source 값
     * @param version version 값
     * @return 조회 또는 생성된 목록
     */
    public List<RagSourceFileEntry> listFiles(ChatCategory category, String source, String version) {
        Path files = filesPath(category, source, version);
        if (!Files.exists(files)) {
            return List.of();
        }
        List<RagSourceFileEntry> list = objectMapper.readValue(files.toFile(), FILE_LIST_TYPE);
		list.sort(Comparator.comparing(RagSourceFileEntry::getLastIndexedAt, Comparator.nullsLast(Comparator.reverseOrder()))
		        .thenComparing(RagSourceFileEntry::getOriginalFileName, Comparator.nullsLast(String::compareToIgnoreCase)));
		return list;
    }

    /**
     * 대상 데이터를 제거한다.
     *
     * @param category 대상 카테고리 정보
     * @param source source 값
     * @param version version 값
     * @param deleteRegistryDir deleteRegistryDir 값
     * @return int 타입의 처리 결과
     */
    public int removeSource(ChatCategory category, String source, String version, boolean deleteRegistryDir) {
        List<RagSourceFileEntry> files = listFiles(category, source, version);
        int removedEntries = files.size();
        Path manifestPath = manifestPath(category, source, version);
        Path filesPath = filesPath(category, source, version);
        try {
            Files.deleteIfExists(manifestPath);
            Files.deleteIfExists(filesPath);
            if (deleteRegistryDir) {
                deleteRecursively(versionDir(category, source, version));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return removedEntries;
    }

    /**
     * 대상 데이터를 제거한다.
     *
     * @param category 대상 카테고리 정보
     * @param source source 값
     * @param version version 값
     * @param fileId fileId 식별자 값
     * @return 처리 가능 여부 또는 조건 충족 여부
     */
    public boolean removeFile(ChatCategory category, String source, String version, String fileId) {
        Path filesPath = filesPath(category, source, version);
        if (!Files.exists(filesPath)) {
            return false;
        }
        List<RagSourceFileEntry> files = objectMapper.readValue(filesPath.toFile(), FILE_LIST_TYPE);
		boolean removed = files.removeIf(it -> fileId != null && fileId.equals(it.getFileId()));
		if (!removed) {
		    return false;
		}
		if (files.isEmpty()) {
		    removeSource(category, source, version, true);
		    return true;
		}
		
		RagSourceManifest manifest = Files.exists(manifestPath(category, source, version))
		        ? objectMapper.readValue(manifestPath(category, source, version).toFile(), RagSourceManifest.class)
		        : new RagSourceManifest();
		manifest.setCategory(category);
		manifest.setSource(source);
		manifest.setVersion(version);
		manifest.setFileCount(files.size());
		manifest.setChunkCount(files.stream().mapToInt(RagSourceFileEntry::getChunkCount).sum());
		manifest.setLastIndexedAt(LocalDateTime.now().toString());
		objectMapper.writerWithDefaultPrettyPrinter().writeValue(filesPath.toFile(), files);
		objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestPath(category, source, version).toFile(), manifest);
		return true;
    }

    /**
     * 대상 정보를 조회한다.
     *
     * @param category 대상 카테고리 정보
     * @param source source 값
     * @param version version 값
     * @param fileId fileId 식별자 값
     * @return RagSourceFileEntry 타입의 처리 결과
     */
    public RagSourceFileEntry findFile(ChatCategory category, String source, String version, String fileId) {
        return listFiles(category, source, version).stream()
                .filter(item -> fileId != null && fileId.equals(item.getFileId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * upsertEntry 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     * @param source source 값
     * @param version version 값
     * @param title title 값
     * @param chunkCount chunkCount 값
     * @param fileId fileId 식별자 값
     * @param ingestType ingestType 값
     * @param contentType contentType 값
     * @param url 대상 URL
     * @param metadata metadata 매핑 정보
     * @return RagSourceManifest 타입의 처리 결과
     */
    private RagSourceManifest upsertEntry(ChatCategory category, String source, String version, String title, int chunkCount, String fileId, String ingestType, String contentType, String url, Map<String, Object> metadata) {
        Path manifestPath = manifestPath(category, source, version);
        Path filesPath = filesPath(category, source, version);
        try {
            Files.createDirectories(manifestPath.getParent());
            RagSourceManifest manifest = Files.exists(manifestPath) ? objectMapper.readValue(manifestPath.toFile(), RagSourceManifest.class) : new RagSourceManifest();
            List<RagSourceFileEntry> files = Files.exists(filesPath) ? objectMapper.readValue(filesPath.toFile(), FILE_LIST_TYPE) : new ArrayList<>();

            String now = LocalDateTime.now().toString();
            RagSourceFileEntry entry = files.stream().filter(it -> fileId.equals(it.getFileId())).findFirst().orElseGet(RagSourceFileEntry::new);
            entry.setCategory(category);
            entry.setSource(source);
            entry.setSourceKey(source);
            entry.setVersion(version);
            entry.setVersionKey(version);
            entry.setFileId(fileId);
            entry.setFileName(stringValue(metadata, "fileName", title));
            entry.setOriginalFileName(stringValue(metadata, "originalFileName", entry.getFileName()));
            entry.setTitle(title);
            entry.setIngestType(ingestType);
            entry.setStorageKind(ragProperties.getVectorStore());
            entry.setStoragePath(manifestPath.getParent().toString());
            entry.setContentType(blankToDefault(contentType, entry.getContentType()));
            entry.setUrl(blankToDefault(url, entry.getUrl()));
            entry.setIngestedAt(entry.getIngestedAt() == null ? now : entry.getIngestedAt());
            entry.setLastIndexedAt(now);
            entry.setChunkCount(chunkCount);
            entry.setMetadata(new LinkedHashMap<>());
            if (metadata != null) entry.getMetadata().putAll(metadata);
            if (files.stream().noneMatch(it -> fileId.equals(it.getFileId()))) {
                files.add(entry);
            }

            manifest.setCategory(category);
            manifest.setSource(source);
            manifest.setSourceKey(source);
            manifest.setVersion(version);
            manifest.setVersionKey(version);
            manifest.setTitle(title);
            manifest.setIngestType(ingestType);
            manifest.setStorageKind(ragProperties.getVectorStore());
            manifest.setStoragePath(manifestPath.getParent().toString());
            manifest.setOriginalFilename(entry.getOriginalFileName());
            manifest.setContentType(entry.getContentType());
            manifest.setUrl(entry.getUrl());
            manifest.setIngestedAt(manifest.getIngestedAt() == null ? now : manifest.getIngestedAt());
            manifest.setLastIndexedAt(now);
            manifest.setChunkCount(files.stream().mapToInt(RagSourceFileEntry::getChunkCount).sum());
            manifest.setFileCount(files.size());
            manifest.setMetadata(new LinkedHashMap<>());
            if (metadata != null) manifest.getMetadata().putAll(metadata);

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestPath.toFile(), manifest);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filesPath.toFile(), files);
            return manifest;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * matches 기능을 수행한다.
     *
     * @param manifest manifest 값
     * @param category 대상 카테고리 정보
     * @param source source 값
     * @param version version 값
     * @return 처리 가능 여부 또는 조건 충족 여부
     */
    private boolean matches(RagSourceManifest manifest, ChatCategory category, String source, String version) {
        if (manifest == null) return false;
        if (category != null && manifest.getCategory() != category) return false;
        if (source != null && !source.isBlank() && !source.equalsIgnoreCase(blankToDefault(manifest.getSource(), ""))) return false;
        if (version != null && !version.isBlank() && !version.equalsIgnoreCase(blankToDefault(manifest.getVersion(), ""))) return false;
        return true;
    }

    /**
     * 대상 정보를 조회한다.
     *
     * @param path path 값
     * @return RagSourceManifest 타입의 처리 결과
     */
    private RagSourceManifest readManifestFile(Path path) {
        return objectMapper.readValue(path.toFile(), RagSourceManifest.class);
    }

    /**
     * baseDir 기능을 수행한다.
     * @return Path 타입의 처리 결과
     */
    private Path baseDir() {
        return Path.of(ragProperties.getRegistry().getBaseDir());
    }

    /**
     * versionDir 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     * @param source source 값
     * @param version version 값
     * @return Path 타입의 처리 결과
     */
    private Path versionDir(ChatCategory category, String source, String version) {
        return baseDir().resolve(category.name().toLowerCase()).resolve(source).resolve(version);
    }

    /**
     * manifestPath 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     * @param source source 값
     * @param version version 값
     * @return Path 타입의 처리 결과
     */
    private Path manifestPath(ChatCategory category, String source, String version) {
        return versionDir(category, source, version).resolve("manifest.json");
    }

    /**
     * filesPath 기능을 수행한다.
     *
     * @param category 대상 카테고리 정보
     * @param source source 값
     * @param version version 값
     * @return Path 타입의 처리 결과
     */
    private Path filesPath(ChatCategory category, String source, String version) {
        return versionDir(category, source, version).resolve("files.json");
    }

    /**
     * 대상 데이터를 제거한다.
     *
     * @param path path 값
     */
    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(target -> {
                try {
                    Files.deleteIfExists(target);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * safeFileId 기능을 수행한다.
     *
     * @param fileId fileId 식별자 값
     * @return 처리 결과 문자열
     */
    private String safeFileId(String fileId) {
        return blankToDefault(fileId, UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * enrich 기능을 수행한다.
     *
     * @param left left 매핑 정보
     * @param right right 매핑 정보
     * @return 키와 값으로 구성된 결과 매핑
     */
    private Map<String, Object> enrich(Map<String, Object> left, Map<String, Object> right) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (left != null) merged.putAll(left);
        merged.putAll(right);
        return merged;
    }

    /**
     * 값이 비어 있을 때 기본값으로 대체한다.
     *
     * @param value value 값
     * @param defaultValue defaultValue 값
     * @return 처리 결과 문자열
     */
    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /**
     * stringValue 기능을 수행한다.
     *
     * @param metadata metadata 매핑 정보
     * @param key key 값
     * @param defaultValue defaultValue 값
     * @return 처리 결과 문자열
     */
    private String stringValue(Map<String, Object> metadata, String key, String defaultValue) {
        if (metadata == null) return defaultValue;
        Object value = metadata.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }
}
