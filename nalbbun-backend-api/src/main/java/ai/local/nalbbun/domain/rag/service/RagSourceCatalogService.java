package ai.local.nalbbun.domain.rag.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.rag.model.RagSourceFileEntry;
import ai.local.nalbbun.domain.rag.model.RagSourceManifest;
import lombok.RequiredArgsConstructor;

/**
 * Rag Source Catalog Service 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
@Service
@RequiredArgsConstructor
public class RagSourceCatalogService {

    private final RagSourceRegistryService registryService;
    private final ObjectProvider<DataSource> dataSourceProvider;

    /**
     * list Sources 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public List<RagSourceManifest> listSources(ChatCategory category, String source, String version) {
        Map<String, RagSourceManifest> merged = new LinkedHashMap<>();
        for (RagSourceManifest manifest : discoverVectorSources(category, source, version)) {
            merged.put(keyOf(manifest.getCategory(), manifest.getSource(), manifest.getVersion()), manifest);
        }
        for (RagSourceManifest manifest : registryService.listManifests(category, source, version)) {
            merged.put(keyOf(manifest.getCategory(), manifest.getSource(), manifest.getVersion()), manifest);
        }
        List<RagSourceManifest> items = new ArrayList<>(merged.values());
        items.sort(Comparator.comparing(RagSourceManifest::getLastIndexedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RagSourceManifest::getSource, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(RagSourceManifest::getVersion, Comparator.nullsLast(String::compareToIgnoreCase)));
        return items;
    }

    /**
     * list Files 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public List<RagSourceFileEntry> listFiles(ChatCategory category, String source, String version) {
        List<RagSourceFileEntry> files = registryService.listFiles(category, source, version);
        if (!files.isEmpty()) {
            return files;
        }
        return discoverVectorFiles(category, source, version);
    }

    /**
     * discover Vector Sources 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private List<RagSourceManifest> discoverVectorSources(ChatCategory category, String source, String version) {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) return List.of();
        try (Connection connection = dataSource.getConnection()) {
            String tableName = findExistingTable(connection, List.of("vector_store", "spring_ai_vector_store", "rag_vector_store"));
            if (tableName == null) return List.of();
            StringBuilder sql = new StringBuilder("""
                    SELECT
                      COALESCE(metadata->>'category','GENERAL') AS category,
                      COALESCE(metadata->>'source','manual') AS source,
                      COALESCE(metadata->>'version','v1') AS version,
                      COALESCE(MAX(metadata->>'title'), MAX(metadata->>'source'), 'manual') AS title,
                      COALESCE(MAX(metadata->>'ingestType'),'vector-db') AS ingest_type,
                      COALESCE(MAX(metadata->>'ingestedAt'),'') AS ingested_at,
                      COALESCE(MAX(metadata->>'ingestedAt'),'') AS last_indexed_at,
                      COUNT(*) AS chunk_count
                    FROM %s
                    WHERE metadata IS NOT NULL
                    """.formatted(tableName));
            List<Object> params = new ArrayList<>();
            appendSourceFilters(sql, params, category, source, version);
            sql.append(" GROUP BY 1,2,3 ORDER BY COALESCE(MAX(metadata->>'ingestedAt'),'') DESC, 2,3");

            try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    List<RagSourceManifest> items = new ArrayList<>();
                    while (rs.next()) {
                        RagSourceManifest manifest = new RagSourceManifest();
                        manifest.setCategory(parseCategory(rs.getString("category")));
                        manifest.setSource(rs.getString("source"));
                        manifest.setSourceKey(rs.getString("source"));
                        manifest.setVersion(rs.getString("version"));
                        manifest.setVersionKey(rs.getString("version"));
                        manifest.setTitle(rs.getString("title"));
                        manifest.setIngestType(rs.getString("ingest_type"));
                        manifest.setStorageKind("vector-db");
                        manifest.setStoragePath(tableName);
                        manifest.setIngestedAt(rs.getString("ingested_at"));
                        manifest.setLastIndexedAt(rs.getString("last_indexed_at"));
                        manifest.setChunkCount(rs.getInt("chunk_count"));
                        manifest.setFileCount(0);
                        items.add(manifest);
                    }
                    return items;
                }
            }
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * discover Vector Files 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private List<RagSourceFileEntry> discoverVectorFiles(ChatCategory category, String source, String version) {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) return List.of();
        try (Connection connection = dataSource.getConnection()) {
            String tableName = findExistingTable(connection, List.of("vector_store", "spring_ai_vector_store", "rag_vector_store"));
            if (tableName == null) return List.of();
            StringBuilder sql = new StringBuilder("""
                    SELECT
                      COALESCE(metadata->>'category','GENERAL') AS category,
                      COALESCE(metadata->>'source','manual') AS source,
                      COALESCE(metadata->>'version','v1') AS version,
                      COALESCE(metadata->>'fileId', md5(COALESCE(metadata->>'originalFileName', COALESCE(metadata->>'title','unknown')))) AS file_id,
                      COALESCE(MAX(metadata->>'fileName'), MAX(metadata->>'originalFileName'), MAX(metadata->>'title'), 'unknown') AS file_name,
                      COALESCE(MAX(metadata->>'originalFileName'), MAX(metadata->>'fileName'), MAX(metadata->>'title'), 'unknown') AS original_file_name,
                      COALESCE(MAX(metadata->>'title'), MAX(metadata->>'source'), 'untitled') AS title,
                      COALESCE(MAX(metadata->>'ingestType'),'vector-db') AS ingest_type,
                      COALESCE(MAX(metadata->>'contentType'),'') AS content_type,
                      COALESCE(MAX(metadata->>'ingestedAt'),'') AS last_indexed_at,
                      COUNT(*) AS chunk_count
                    FROM %s
                    WHERE metadata IS NOT NULL
                    """.formatted(tableName));
            List<Object> params = new ArrayList<>();
            appendSourceFilters(sql, params, category, source, version);
            sql.append(" GROUP BY 1,2,3,4 ORDER BY COALESCE(MAX(metadata->>'ingestedAt'),'') DESC, 6");
            try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    List<RagSourceFileEntry> items = new ArrayList<>();
                    while (rs.next()) {
                        RagSourceFileEntry file = new RagSourceFileEntry();
                        file.setCategory(parseCategory(rs.getString("category")));
                        file.setSource(rs.getString("source"));
                        file.setSourceKey(rs.getString("source"));
                        file.setVersion(rs.getString("version"));
                        file.setVersionKey(rs.getString("version"));
                        file.setFileId(rs.getString("file_id"));
                        file.setFileName(rs.getString("file_name"));
                        file.setOriginalFileName(rs.getString("original_file_name"));
                        file.setTitle(rs.getString("title"));
                        file.setIngestType(rs.getString("ingest_type"));
                        file.setStorageKind("vector-db");
                        file.setStoragePath(tableName);
                        file.setContentType(rs.getString("content_type"));
                        file.setLastIndexedAt(rs.getString("last_indexed_at"));
                        file.setChunkCount(rs.getInt("chunk_count"));
                        items.add(file);
                    }
                    return items;
                }
            }
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * append Source Filters 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    private void appendSourceFilters(StringBuilder sql, List<Object> params, ChatCategory category, String source, String version) {
        if (category != null) { sql.append(" AND COALESCE(metadata->>'category','GENERAL') = ?"); params.add(category.name()); }
        if (source != null && !source.isBlank()) { sql.append(" AND COALESCE(metadata->>'source','manual') = ?"); params.add(source); }
        if (version != null && !version.isBlank()) { sql.append(" AND COALESCE(metadata->>'version','v1') = ?"); params.add(version); }
    }

    /**
     * bind 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    private void bind(PreparedStatement ps, List<Object> params) throws Exception {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    /**
     * parse Category 처리를 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private ChatCategory parseCategory(String raw) {
        try { return ChatCategory.valueOf(Objects.requireNonNullElse(raw, "GENERAL")); } catch (Exception e) { return ChatCategory.GENERAL; }
    }

    /**
     * key Of 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String keyOf(ChatCategory category, String source, String version) {
        return (category == null ? "GENERAL" : category.name()) + '|' + source + '|' + version;
    }

    /**
     * list Tables 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private List<String> listTables(Connection connection) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema NOT IN ('information_schema', 'pg_catalog')
                ORDER BY table_name
                """); ResultSet rs = ps.executeQuery()) {
            List<String> tables = new ArrayList<>();
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
            return tables;
        }
    }

    /**
     * find Existing Table 대상을 조회한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String findExistingTable(Connection connection, List<String> candidates) throws Exception {
        List<String> tables = listTables(connection);
        return candidates.stream().filter(tables::contains).findFirst().orElse(null);
    }
}
