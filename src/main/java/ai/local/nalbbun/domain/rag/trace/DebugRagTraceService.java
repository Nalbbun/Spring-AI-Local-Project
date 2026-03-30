package ai.local.nalbbun.domain.rag.trace;

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
 * Debug Rag Trace Service 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
@Service
public class DebugRagTraceService {

    private static final int MAX_ENTRIES = 2000;

    private final AtomicLong sequence = new AtomicLong(0);
    private final LinkedList<RagTraceEntry> entries = new LinkedList<>();

    /**
     * start Trace 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public synchronized String startTrace(String operation, Map<String, Object> details) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log(traceId, operation, "START", "RUNNING", "RAG 작업 시작", details);
        return traceId;
    }

    /**
     * info 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public void info(String traceId, String operation, String stage, String message, Map<String, Object> details) {
        log(traceId, operation, stage, "RUNNING", message, details);
    }

    /**
     * success 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public void success(String traceId, String operation, String stage, String message, Map<String, Object> details) {
        log(traceId, operation, stage, "SUCCESS", message, details);
    }

    /**
     * warn 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public void warn(String traceId, String operation, String stage, String message, Map<String, Object> details) {
        log(traceId, operation, stage, "WARN", message, details);
    }

    /**
     * error 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public void error(String traceId, String operation, String stage, String message, Map<String, Object> details) {
        log(traceId, operation, stage, "ERROR", message, details);
    }

    /**
     * clear 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public synchronized void clear() {
        entries.clear();
    }

    /**
     * latest 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * by Trace Id 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * summarize One 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
