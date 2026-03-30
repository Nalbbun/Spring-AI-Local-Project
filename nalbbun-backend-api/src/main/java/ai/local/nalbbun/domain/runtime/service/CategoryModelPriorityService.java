package ai.local.nalbbun.domain.runtime.service;

import ai.local.nalbbun.domain.runtime.model.RuntimeModelTarget;
import ai.local.nalbbun.domain.runtime.model.ModelPriority;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 카테고리(RuntimeModelTarget)별 모델 우선순위를 런타임으로 관리합니다.
 * 기본값: OLLAMA_FIRST (모든 카테고리)
 */
@Slf4j
@Service
public class CategoryModelPriorityService {

    private final Map<RuntimeModelTarget, ModelPriority> priorities =
            new ConcurrentHashMap<>();

    public CategoryModelPriorityService() {
        // 기본값 설정
        for (RuntimeModelTarget t : RuntimeModelTarget.values()) {
            priorities.put(t, ModelPriority.OLLAMA_FIRST);
        }
    }

    public ModelPriority get(RuntimeModelTarget target) {
        return priorities.getOrDefault(target, ModelPriority.OLLAMA_FIRST);
    }

    public void set(RuntimeModelTarget target, ModelPriority priority) {
        priorities.put(target, priority);
        log.info("ModelPriority 변경: {} → {}", target, priority);
    }

    public Map<String, Object> getAll() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (RuntimeModelTarget t : RuntimeModelTarget.values()) {
            Map<String, String> item = new LinkedHashMap<>();
            ModelPriority p = get(t);
            item.put("priority",    p.name());
            item.put("description", p.description);
            result.put(t.name(), item);
        }
        return result;
    }

    public Map<String, Object> updateAll(Map<String, String> updates) {
        updates.forEach((targetStr, priorityStr) -> {
            try {
                RuntimeModelTarget target   = RuntimeModelTarget.valueOf(targetStr.toUpperCase());
                ModelPriority      priority = ModelPriority.from(priorityStr);
                set(target, priority);
            } catch (Exception e) {
                log.warn("우선순위 업데이트 실패: {} → {}", targetStr, priorityStr);
            }
        });
        return getAll();
    }

    public Map<String, Object> reset() {
        for (RuntimeModelTarget t : RuntimeModelTarget.values()) {
            priorities.put(t, ModelPriority.OLLAMA_FIRST);
        }
        log.info("ModelPriority 전체 초기화 (OLLAMA_FIRST)");
        return getAll();
    }
}
