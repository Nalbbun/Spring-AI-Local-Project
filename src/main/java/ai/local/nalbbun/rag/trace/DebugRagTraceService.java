package ai.local.nalbbun.rag.trace;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class DebugRagTraceService {

    private static final int MAX_ENTRIES = 2000;

    private final AtomicLong sequence = new AtomicLong(0);
    private final LinkedList<RagTraceEntry> entries = new LinkedList<>();

    public synchronized String startTrace(String operation, Map<String, Object> details) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log(traceId, operation, "START", "RUNNING", "RAG 작업 시작", details);
        return traceId;
    }

    public void info(String traceId, String operation, String stage, String message, Map<String, Object> details) {
        log(traceId, operation, stage, "RUNNING", message, details);
    }

    public void success(String traceId, String operation, String stage, String message, Map<String, Object> details) {
        log(traceId, operation, stage, "SUCCESS", message, details);
    }

    public void warn(String traceId, String operation, String stage, String message, Map<String, Object> details) {
        log(traceId, operation, stage, "WARN", message, details);
    }

    public void error(String traceId, String operation, String stage, String message, Map<String, Object> details) {
        log(traceId, operation, stage, "ERROR", message, details);
    }

    public synchronized void clear() {
        entries.clear();
    }

    public synchronized Map<String, Object> latest(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        List<RagTraceEntry> latestEntries = entries.stream()
                .skip(Math.max(0, entries.size() - safeLimit))
                .toList();
        return Map.of(
                "count", latestEntries.size(),
                "traceCount", latestEntries.stream().map(RagTraceEntry::traceId).distinct().count(),
                "entries", latestEntries,
                "summaries", summarize(latestEntries)
        );
    }

    public synchronized Map<String, Object> byTraceId(String traceId) {
        List<RagTraceEntry> traceEntries = entries.stream()
                .filter(entry -> entry.traceId().equals(traceId))
                .toList();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("traceId", traceId);
        response.put("entryCount", traceEntries.size());
        response.put("summary", summarizeOne(traceEntries));
        response.put("entries", traceEntries);
        return response;
    }

    private synchronized void log(String traceId, String operation, String stage, String status, String message, Map<String, Object> details) {
        Map<String, Object> safeDetails = new LinkedHashMap<>();
        if (details != null) {
            safeDetails.putAll(details);
        }
        RagTraceEntry entry = RagTraceEntry.builder()
                .sequence(sequence.incrementAndGet())
                .traceId(traceId)
                .operation(operation)
                .stage(stage)
                .status(status)
                .message(message)
                .details(Collections.unmodifiableMap(safeDetails))
                .timestamp(LocalDateTime.now())
                .build();
        entries.add(entry);
        while (entries.size() > MAX_ENTRIES) {
            entries.removeFirst();
        }
    }

    private List<RagTraceSummary> summarize(List<RagTraceEntry> targetEntries) {
        Map<String, List<RagTraceEntry>> grouped = targetEntries.stream()
                .collect(Collectors.groupingBy(RagTraceEntry::traceId, LinkedHashMap::new, Collectors.toList()));
        List<RagTraceSummary> summaries = new ArrayList<>();
        for (List<RagTraceEntry> traceEntries : grouped.values()) {
            RagTraceSummary summary = summarizeOne(traceEntries);
            if (summary != null) {
                summaries.add(summary);
            }
        }
        return summaries;
    }

    private RagTraceSummary summarizeOne(List<RagTraceEntry> traceEntries) {
        if (traceEntries == null || traceEntries.isEmpty()) {
            return null;
        }
        RagTraceEntry first = traceEntries.get(0);
        RagTraceEntry last = traceEntries.get(traceEntries.size() - 1);
        return RagTraceSummary.builder()
                .traceId(first.traceId())
                .operation(first.operation())
                .finalStatus(last.status())
                .lastStage(last.stage())
                .lastMessage(last.message())
                .entryCount(traceEntries.size())
                .startedAt(first.timestamp())
                .endedAt(last.timestamp())
                .build();
    }
}
