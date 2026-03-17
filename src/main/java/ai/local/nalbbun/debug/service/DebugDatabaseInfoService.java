package ai.local.nalbbun.debug.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;

/**
 * DebugDatabaseInfoService는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: debug database info service 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Service
@RequiredArgsConstructor
public class DebugDatabaseInfoService {

    /** dataSourceProvider 값을 보관한다. */
    private final ObjectProvider<DataSource> dataSourceProvider;
    /** redisTemplateProvider 값을 보관한다. */
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    /** ragProperties 값을 보관한다. */
    private final RagProperties ragProperties;

    /**
     * 지정된 정보를 조회한다.
     * @return 키와 값으로 구성된 결과 매핑
     */
    public Map<String, Object> getInfo() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ragEnabled", ragProperties.isEnabled());
        response.put("vectorStoreType", ragProperties.getVectorStore());
        response.put("registryBaseDir", ragProperties.getRegistry().getBaseDir());
        response.put("jdbc", jdbcInfo());
        response.put("vectorDb", vectorDbInfo());
        response.put("memoryDb", memoryDbInfo());
        response.put("redis", redisInfo());
        response.put("registry", registryInfo());
        return response;
    }

    /**
     * jdbcInfo 기능을 수행한다.
     * @return 키와 값으로 구성된 결과 매핑
     */
    private Map<String, Object> jdbcInfo() {
        Map<String, Object> result = new LinkedHashMap<>();
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            result.put("connected", false);
            result.put("message", "DataSource bean 없음");
            return result;
        }

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            result.put("connected", true);
            result.put("productName", metaData.getDatabaseProductName());
            result.put("productVersion", metaData.getDatabaseProductVersion());
            result.put("url", metaData.getURL());
            result.put("userName", metaData.getUserName());
            result.put("catalog", connection.getCatalog());
            result.put("schema", connection.getSchema());
            result.put("tables", listTables(connection));
        } catch (Exception e) {
            result.put("connected", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * vectorDbInfo 기능을 수행한다.
     * @return 키와 값으로 구성된 결과 매핑
     */
    private Map<String, Object> vectorDbInfo() {
        Map<String, Object> result = new LinkedHashMap<>();
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            result.put("tableExists", false);
            result.put("message", "DataSource bean 없음");
            return result;
        }

        try (Connection connection = dataSource.getConnection()) {
            String tableName = findExistingTable(connection, List.of("vector_store", "spring_ai_vector_store", "rag_vector_store"));
            result.put("tableName", tableName);
            result.put("tableExists", tableName != null);
            if (tableName == null) {
                result.put("rowCount", 0);
                return result;
            }

            result.put("rowCount", queryForLong(connection, "SELECT COUNT(*) FROM " + tableName));
            result.put("distinctSources", queryJsonDistinctCount(connection, tableName, "source"));
            result.put("distinctVersions", queryJsonDistinctCount(connection, tableName, "version"));
            result.put("distinctCategories", queryJsonDistinctCount(connection, tableName, "category"));
        } catch (Exception e) {
            result.put("tableExists", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * memoryDbInfo 기능을 수행한다.
     * @return 키와 값으로 구성된 결과 매핑
     */
    private Map<String, Object> memoryDbInfo() {
        Map<String, Object> result = new LinkedHashMap<>();
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            result.put("available", false);
            result.put("message", "DataSource bean 없음");
            return result;
        }

        try (Connection connection = dataSource.getConnection()) {
            result.put("available", true);
            result.put("conversationMessageRows", tableCount(connection, "conversation_message"));
            result.put("conversationSummaryRows", tableCount(connection, "conversation_summary"));
            result.put("conversationNoteRows", tableCount(connection, "conversation_note"));
            result.put("distinctConversations", distinctConversationCount(connection));
        } catch (Exception e) {
            result.put("available", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * redisInfo 기능을 수행한다.
     * @return 키와 값으로 구성된 결과 매핑
     */
    private Map<String, Object> redisInfo() {
        Map<String, Object> result = new LinkedHashMap<>();
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            result.put("connected", false);
            result.put("message", "StringRedisTemplate bean 없음");
            return result;
        }

        try {
            String pong = redisTemplate.execute((RedisCallback<String>) this::ping);
            result.put("connected", true);
            result.put("ping", pong);
        } catch (Exception e) {
            result.put("connected", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * registryInfo 기능을 수행한다.
     * @return 키와 값으로 구성된 결과 매핑
     */
    private Map<String, Object> registryInfo() {
        Map<String, Object> result = new LinkedHashMap<>();
        Path baseDir = Path.of(ragProperties.getRegistry().getBaseDir());
        result.put("baseDir", baseDir.toString());
        result.put("exists", Files.exists(baseDir));

        if (!Files.exists(baseDir)) {
            result.put("totalFiles", 0);
            result.put("totalDirs", 0);
            result.put("totalBytes", 0L);
            result.put("manifestCount", 0);
            result.put("filesJsonCount", 0);
            result.put("byCategory", Map.of());
            return result;
        }

        long totalFiles = 0;
        long totalDirs = 0;
        long totalBytes = 0;
        long manifestCount = 0;
        long filesJsonCount = 0;
        Map<String, Integer> byCategory = new LinkedHashMap<>();
        try (Stream<Path> stream = Files.walk(baseDir)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                if (Files.isDirectory(path)) {
                    totalDirs++;
                    continue;
                }
                totalFiles++;
                totalBytes += Files.size(path);
                String name = path.getFileName().toString();
                if ("manifest.json".equalsIgnoreCase(name)) {
                    manifestCount++;
                    Path rel = baseDir.relativize(path);
                    if (rel.getNameCount() > 0) {
                        String category = rel.getName(0).toString();
                        byCategory.merge(category, 1, Integer::sum);
                    }
                }
                if ("files.json".equalsIgnoreCase(name)) {
                    filesJsonCount++;
                }
            }
            result.put("totalFiles", totalFiles);
            result.put("totalDirs", totalDirs);
            result.put("totalBytes", totalBytes);
            result.put("manifestCount", manifestCount);
            result.put("filesJsonCount", filesJsonCount);
            result.put("byCategory", byCategory);
        } catch (IOException e) {
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 대상 정보를 조회한다.
     *
     * @param connection connection 값
     * @return 조회 또는 생성된 목록
     */
    private List<String> listTables(Connection connection) {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema NOT IN ('information_schema', 'pg_catalog')
                ORDER BY table_name
                """);
             ResultSet rs = ps.executeQuery()) {
            java.util.ArrayList<String> tables = new java.util.ArrayList<>();
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
            return tables;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 대상 정보를 조회한다.
     *
     * @param connection connection 값
     * @param candidates candidates 목록 정보
     * @return 처리 결과 문자열
     */
    private String findExistingTable(Connection connection, List<String> candidates) {
        List<String> tables = listTables(connection);
        return candidates.stream().filter(tables::contains).findFirst().orElse(null);
    }

    /**
     * tableCount 기능을 수행한다.
     *
     * @param connection connection 값
     * @param tableName tableName 값
     * @return long 타입의 처리 결과
     */
    private long tableCount(Connection connection, String tableName) {
        if (!listTables(connection).contains(tableName)) {
            return 0L;
        }
        return queryForLong(connection, "SELECT COUNT(*) FROM " + tableName);
    }

    /**
     * distinctConversationCount 기능을 수행한다.
     *
     * @param connection connection 값
     * @return long 타입의 처리 결과
     */
    private long distinctConversationCount(Connection connection) {
        if (!listTables(connection).contains("conversation_message")) {
            return 0L;
        }
        return queryForLong(connection, "SELECT COUNT(DISTINCT conversation_id) FROM conversation_message");
    }

    /**
     * queryForLong 기능을 수행한다.
     *
     * @param connection connection 값
     * @param sql sql 값
     * @return long 타입의 처리 결과
     */
    private long queryForLong(Connection connection, String sql) {
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * queryJsonDistinctCount 기능을 수행한다.
     *
     * @param connection connection 값
     * @param tableName tableName 값
     * @param key key 값
     * @return long 타입의 처리 결과
     */
    private long queryJsonDistinctCount(Connection connection, String tableName, String key) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(DISTINCT metadata->>?) FROM " + tableName + " WHERE metadata IS NOT NULL")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * ping 기능을 수행한다.
     *
     * @param connection connection 값
     * @return 처리 결과 문자열
     */
    private String ping(RedisConnection connection) {
        return Optional.ofNullable(connection.ping()).orElse("PONG");
    }
}
