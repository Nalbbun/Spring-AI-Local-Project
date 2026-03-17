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

/**
 * DebugRagTraceService는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: debug rag trace service 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Service
public class DebugRagTraceService {

    /** MAX_ENTRIES 값을 보관한다. */
    private static final int MAX_ENTRIES = 2000;

    /** sequence 값을 보관한다. */
    private final AtomicLong sequence = new AtomicLong(0);
    /** entries 값을 보관한다. */
    private final LinkedList<RagTraceEntry> entries = new LinkedList<>();

    /**
     * startTrace 기능을 수행한다.
     *
     * @param operation operation 값
     * @param details details 매핑 정보
     * @return 처리 결과 문자열
     */
    public synchronized String startTrace(String operation, Map<String, Object> details) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log(traceId, operation, "START", "RUNNING", "RAG 작업 시작", details);
        return traceId;
    }

    /**
     * info 기능을 수행한다.
     *
     * @param traceId traceId 식별자 값
     * @param operation operation 값
     * @param stage stage 값
     * @param message 사용자 입력 또는 질의 내용
     * @param details details 매핑 정보
     */
    public void info(String traceId, String operation, String stage, String message, Map<String, Object> details) {
        log(traceId, operation, stage, "RUNNING", message, details);
    }

    /**
     * success 기능을 수행한다.
     *
     * @param traceId traceId 식별자 값
     * @param operation operation 값
     * @param stage stage 값
     * @param message 사용자 입력 또는 질의 내용
     * @param details details 매핑 정보
     */
    public void success(String traceId, String operation, String stage, String message, Map<String, Object> details) {
        log(traceId, operation, stage, "SUCCESS", message, details);
    }

    /**
     * warn 기능을 수행한다.
     *
     * @param traceId traceId 식별자 값
     * @param operation operation 값
     * @param stage stage 값
     * @param message 사용자 입력 또는 질의 내용
     * @param details details 매핑 정보
     */
    public void warn(String traceId, String operation, String stage, String message, Map<String, Object> details) {
        log(traceId, operation, stage, "WARN", message, details);
    }

    /**
     * error 기능을 수행한다.
     *
     * @param traceId traceId 식별자 값
     * @param operation operation 값
     * @param stage stage 값
     * @param message 사용자 입력 또는 질의 내용
     * @param details details 매핑 정보
     */
    public void error(String traceId, String operation, String stage, String message, Map<String, Object> details) {
        log(traceId, operation, stage, "ERROR", message, details);
    }

    /**
     * clear 기능을 수행한다.
     */
    public synchronized void clear() {
        entries.clear();
    }

    /**
     * latest 기능을 수행한다.
     *
     * @param limit limit 값
     * @return 키와 값으로 구성된 결과 매핑
     */
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

    /**
     * byTraceId 기능을 수행한다.
     *
     * @param traceId traceId 식별자 값
     * @return 키와 값으로 구성된 결과 매핑
     */
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

    /**
     * log 기능을 수행한다.
     *
     * @param traceId traceId 식별자 값
     * @param operation operation 값
     * @param stage stage 값
     * @param status status 값
     * @param message 사용자 입력 또는 질의 내용
     * @param details details 매핑 정보
     */
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

    /**
     * summarize 기능을 수행한다.
     *
     * @param targetEntries targetEntries 목록 정보
     * @return 조회 또는 생성된 목록
     */
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

    /**
     * summarizeOne 기능을 수행한다.
     *
     * @param traceEntries traceEntries 목록 정보
     * @return RagTraceSummary 타입의 처리 결과
     */
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
