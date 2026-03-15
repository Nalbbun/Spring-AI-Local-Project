package ai.local.nalbbun.rag.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.rag.ingest.RagDocumentIngestionService;
import ai.local.nalbbun.rag.ingest.RagIngestionResult;
import ai.local.nalbbun.rag.model.RagRetrievedDocument;
import ai.local.nalbbun.rag.model.RagSourceFileEntry;
import ai.local.nalbbun.rag.model.RagSourceFilePurgeCommand;
import ai.local.nalbbun.rag.model.RagSourceFilePurgeResult;
import ai.local.nalbbun.rag.model.RagSourceManifest;
import ai.local.nalbbun.rag.model.RagSourcePurgeCommand;
import ai.local.nalbbun.rag.model.RagSourcePurgeResult;
import ai.local.nalbbun.rag.model.RagSourceReindexCommand;
import ai.local.nalbbun.rag.model.RagSourceReindexItemResult;
import ai.local.nalbbun.rag.model.RagSourceReindexResult;
import ai.local.nalbbun.rag.model.RagSourceVersionCompareResult;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RagSourceAdminService {

    private final ObjectProvider<DataSource> dataSourceProvider;
    private final RagSourceRegistryService ragSourceRegistryService;
    private final RagSourceCatalogService ragSourceCatalogService;
    private final RagDocumentIngestionService ragDocumentIngestionService;
    private final RagSupportService ragSupportService;

    public RagSourcePurgeResult purgeSource(RagSourcePurgeCommand command) {
        validate(command.getCategory(), command.getSource(), command.getVersion());
        int before = countVectorRows(command.getCategory(), command.getSource(), command.getVersion(), null, null);
        int after = deleteVectorRows(command.getCategory(), command.getSource(), command.getVersion(), null, null);
        int registryEntriesRemoved = command.isDeleteRegistry()
                ? ragSourceRegistryService.removeSource(command.getCategory(), command.getSource(), command.getVersion(), true)
                : 0;

        return RagSourcePurgeResult.builder()
                .category(command.getCategory().name())
                .source(command.getSource())
                .version(command.getVersion())
                .estimatedVectorRowsBefore(before)
                .estimatedVectorRowsAfter(after)
                .registryEntriesRemoved(registryEntriesRemoved)
                .registryDeleted(command.isDeleteRegistry())
                .build();
    }

    public RagSourceFilePurgeResult purgeSourceFile(RagSourceFilePurgeCommand command) {
        validate(command.getCategory(), command.getSource(), command.getVersion());
        if (command.getFileId() == null || command.getFileId().isBlank()) {
            throw new IllegalArgumentException("fileId는 필수입니다.");
        }
        RagSourceFileEntry target = resolveFile(command.getCategory(), command.getSource(), command.getVersion(), command.getFileId());
        if (target == null) {
            throw new IllegalArgumentException("삭제할 fileId를 찾을 수 없습니다.");
        }

        int before = countVectorRows(command.getCategory(), command.getSource(), command.getVersion(), target, command.getFileId());
        int after = deleteVectorRows(command.getCategory(), command.getSource(), command.getVersion(), target, command.getFileId());
        boolean registryDeleted = command.isDeleteRegistry() && ragSourceRegistryService.removeFile(command.getCategory(), command.getSource(), command.getVersion(), command.getFileId());

        return RagSourceFilePurgeResult.builder()
                .category(command.getCategory().name())
                .source(command.getSource())
                .version(command.getVersion())
                .fileId(command.getFileId())
                .estimatedVectorRowsBefore(before)
                .estimatedVectorRowsAfter(after)
                .registryDeleted(registryDeleted)
                .build();
    }

    public RagSourceReindexResult reindexSource(RagSourceReindexCommand command) {
        validate(command.getCategory(), command.getSource(), command.getVersion());
        String targetVersion = resolveTargetVersion(command);

        List<RagSourceFileEntry> files = new ArrayList<>(ragSourceCatalogService.listFiles(command.getCategory(), command.getSource(), command.getVersion()));
        files.sort(Comparator.comparing(RagSourceFileEntry::getOriginalFileName, Comparator.nullsLast(String::compareToIgnoreCase)));
        List<ReindexCandidate> candidates = new ArrayList<>();
        for (RagSourceFileEntry file : files) {
            String text = reconstructFileText(command.getCategory(), command.getSource(), command.getVersion(), file);
            candidates.add(new ReindexCandidate(file, text));
        }

        if (command.isPurgeBeforeReindex()) {
            RagSourcePurgeCommand purgeCommand = new RagSourcePurgeCommand();
            purgeCommand.setCategory(command.getCategory());
            purgeCommand.setSource(command.getSource());
            purgeCommand.setVersion(targetVersion);
            purgeCommand.setDeleteRegistry(true);
            purgeSource(purgeCommand);
        }

        List<RagSourceReindexItemResult> results = new ArrayList<>();
        int successCount = 0;
        for (ReindexCandidate candidate : candidates) {
            RagSourceFileEntry file = candidate.file();
            if (candidate.text() == null || candidate.text().isBlank()) {
                results.add(RagSourceReindexItemResult.builder()
                        .source(command.getSource())
                        .version(command.getVersion())
                        .targetVersion(targetVersion)
                        .title(file.getTitle())
                        .storageKind(file.getStorageKind())
                        .success(false)
                        .detail("재색인할 본문을 복원하지 못했습니다.")
                        .build());
                continue;
            }
            try {
                Map<String, Object> metadata = new LinkedHashMap<>();
                if (file.getMetadata() != null) {
                    metadata.putAll(file.getMetadata());
                }
                metadata.put("reindexFromVersion", command.getVersion());
                metadata.put("reindexedAt", java.time.LocalDateTime.now().toString());
                RagIngestionResult ingestResult = ragDocumentIngestionService.ingestReconstructedFile(
                        command.getCategory(),
                        command.getSource(),
                        targetVersion,
                        nonBlank(file.getTitle(), file.getOriginalFileName(), command.getSource()),
                        file.getFileId(),
                        nonBlank(file.getFileName(), file.getOriginalFileName(), file.getTitle()),
                        nonBlank(file.getOriginalFileName(), file.getFileName(), file.getTitle()),
                        nonBlank(file.getContentType(), "text/plain"),
                        candidate.text(),
                        metadata
                );
                successCount++;
                results.add(RagSourceReindexItemResult.builder()
                        .source(command.getSource())
                        .version(command.getVersion())
                        .targetVersion(targetVersion)
                        .title(file.getTitle())
                        .storageKind(file.getStorageKind())
                        .success(true)
                        .detail("reindexed")
                        .ingestionResult(ingestResult)
                        .build());
            } catch (Exception e) {
                results.add(RagSourceReindexItemResult.builder()
                        .source(command.getSource())
                        .version(command.getVersion())
                        .targetVersion(targetVersion)
                        .title(file.getTitle())
                        .storageKind(file.getStorageKind())
                        .success(false)
                        .detail(e.getMessage())
                        .build());
            }
        }

        return RagSourceReindexResult.builder()
                .category(command.getCategory().name())
                .source(command.getSource())
                .version(command.getVersion())
                .targetVersion(targetVersion)
                .totalCandidates(candidates.size())
                .successCount(successCount)
                .results(results)
                .build();
    }

    public RagSourceVersionCompareResult compare(ChatCategory category, String source, String leftVersion, String rightVersion, String query) {
        validate(category, source, leftVersion);
        if (rightVersion == null || rightVersion.isBlank()) {
            throw new IllegalArgumentException("rightVersion은 필수입니다.");
        }
        RagSourceManifest left = ragSourceCatalogService.listSources(category, source, leftVersion).stream().findFirst().orElse(null);
        RagSourceManifest right = ragSourceCatalogService.listSources(category, source, rightVersion).stream().findFirst().orElse(null);
        int leftRows = countVectorRows(category, source, leftVersion, null, null);
        int rightRows = countVectorRows(category, source, rightVersion, null, null);
        List<RagRetrievedDocument> leftHits = query == null || query.isBlank()
                ? List.of()
                : ragSupportService.buildContext(category, query, source, leftVersion).getDocuments();
        List<RagRetrievedDocument> rightHits = query == null || query.isBlank()
                ? List.of()
                : ragSupportService.buildContext(category, query, source, rightVersion).getDocuments();

        String summary = "leftRows=" + leftRows
                + ", rightRows=" + rightRows
                + ", leftChunks=" + (left == null ? 0 : left.getChunkCount())
                + ", rightChunks=" + (right == null ? 0 : right.getChunkCount())
                + ", leftHits=" + leftHits.size()
                + ", rightHits=" + rightHits.size();

        return RagSourceVersionCompareResult.builder()
                .category(category.name())
                .source(source)
                .leftVersion(leftVersion)
                .rightVersion(rightVersion)
                .left(left)
                .right(right)
                .leftVectorRows(leftRows)
                .rightVectorRows(rightRows)
                .query(query)
                .leftHits(leftHits)
                .rightHits(rightHits)
                .summary(summary)
                .build();
    }

    private String reconstructFileText(ChatCategory category, String source, String version, RagSourceFileEntry file) {
        List<VectorChunkRow> rows = selectChunkRows(category, source, version, file, file == null ? null : file.getFileId());
        if (rows.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (VectorChunkRow row : rows) {
            if (row.content() == null || row.content().isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(row.content().trim());
        }
        return builder.toString();
    }

    private RagSourceFileEntry resolveFile(ChatCategory category, String source, String version, String fileId) {
        RagSourceFileEntry registryFile = ragSourceRegistryService.findFile(category, source, version, fileId);
        if (registryFile != null) {
            return registryFile;
        }
        return ragSourceCatalogService.listFiles(category, source, version).stream()
                .filter(item -> fileId.equals(item.getFileId()))
                .findFirst()
                .orElse(null);
    }

    private int countVectorRows(ChatCategory category, String source, String version, RagSourceFileEntry file, String fileId) {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            return 0;
        }
        try (Connection connection = dataSource.getConnection()) {
            String tableName = findExistingTable(connection);
            if (tableName == null) {
                return 0;
            }
            List<Object> params = new ArrayList<>();
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM " + tableName + " WHERE metadata IS NOT NULL");
            appendSourceWhere(sql, params, category, source, version);
            appendFileWhere(sql, params, file, fileId);
            try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private int deleteVectorRows(ChatCategory category, String source, String version, RagSourceFileEntry file, String fileId) {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            return 0;
        }
        int before = countVectorRows(category, source, version, file, fileId);
        try (Connection connection = dataSource.getConnection()) {
            String tableName = findExistingTable(connection);
            if (tableName == null) {
                return 0;
            }
            List<Object> params = new ArrayList<>();
            StringBuilder sql = new StringBuilder("DELETE FROM " + tableName + " WHERE metadata IS NOT NULL");
            appendSourceWhere(sql, params, category, source, version);
            appendFileWhere(sql, params, file, fileId);
            try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
                bind(ps, params);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            return before;
        }
        return countVectorRows(category, source, version, file, fileId);
    }

    private List<VectorChunkRow> selectChunkRows(ChatCategory category, String source, String version, RagSourceFileEntry file, String fileId) {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            return List.of();
        }
        try (Connection connection = dataSource.getConnection()) {
            String tableName = findExistingTable(connection);
            if (tableName == null) {
                return List.of();
            }
            List<Object> params = new ArrayList<>();
            StringBuilder sql = new StringBuilder(
                    "SELECT content, COALESCE((metadata->>'chunkIndex')::int, 0) AS chunk_index FROM " + tableName + " WHERE metadata IS NOT NULL");
            appendSourceWhere(sql, params, category, source, version);
            appendFileWhere(sql, params, file, fileId);
            sql.append(" ORDER BY COALESCE((metadata->>'chunkIndex')::int, 0), content");
            try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    List<VectorChunkRow> rows = new ArrayList<>();
                    while (rs.next()) {
                        rows.add(new VectorChunkRow(rs.getString("content"), rs.getInt("chunk_index")));
                    }
                    return rows;
                }
            }
        } catch (Exception e) {
            return List.of();
        }
    }

    private void appendSourceWhere(StringBuilder sql, List<Object> params, ChatCategory category, String source, String version) {
        sql.append(" AND COALESCE(metadata->>'category','GENERAL') = ?");
        params.add(category.name());
        sql.append(" AND COALESCE(metadata->>'source','manual') = ?");
        params.add(source);
        sql.append(" AND COALESCE(metadata->>'version','v1') = ?");
        params.add(version);
    }

    private void appendFileWhere(StringBuilder sql, List<Object> params, RagSourceFileEntry file, String fileId) {
        if ((fileId == null || fileId.isBlank()) && file == null) {
            return;
        }
        String originalFileName = file == null ? "" : nonBlank(file.getOriginalFileName(), file.getFileName(), file.getTitle());
        String title = file == null ? "" : nonBlank(file.getTitle(), file.getOriginalFileName(), file.getFileName());
        sql.append(" AND (COALESCE(metadata->>'fileId','') = ? OR (COALESCE(metadata->>'fileId','') = '' AND (COALESCE(metadata->>'originalFileName', COALESCE(metadata->>'fileName', COALESCE(metadata->>'title',''))) = ? OR COALESCE(metadata->>'title','') = ?)))");
        params.add(nonBlank(fileId, ""));
        params.add(originalFileName);
        params.add(title);
    }

    private void bind(PreparedStatement ps, List<Object> params) throws Exception {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private String findExistingTable(Connection connection) throws Exception {
        List<String> candidates = List.of("vector_store", "spring_ai_vector_store", "rag_vector_store");
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT table_name FROM information_schema.tables WHERE table_schema NOT IN ('information_schema', 'pg_catalog') ORDER BY table_name");
             ResultSet rs = ps.executeQuery()) {
            List<String> tables = new ArrayList<>();
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
            return candidates.stream().filter(tables::contains).findFirst().orElse(null);
        }
    }

    private void validate(ChatCategory category, String source, String version) {
        if (category == null) {
            throw new IllegalArgumentException("category는 필수입니다.");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source는 필수입니다.");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("version은 필수입니다.");
        }
    }

    private String resolveTargetVersion(RagSourceReindexCommand command) {
        if (!command.isCopyToNewVersion()) {
            return command.getVersion();
        }
        if (command.getTargetVersion() != null && !command.getTargetVersion().isBlank()) {
            return command.getTargetVersion().trim();
        }
        return command.getVersion() + "-reindexed";
    }

    private String nonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private record VectorChunkRow(String content, int chunkIndex) { }
    private record ReindexCandidate(RagSourceFileEntry file, String text) { }
}
