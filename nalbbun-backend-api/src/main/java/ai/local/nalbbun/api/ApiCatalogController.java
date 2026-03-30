package ai.local.nalbbun.api;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
public class ApiCatalogController {

    @GetMapping
    public Map<String, Object> getCatalog() {
        return Map.of(
            "application", "nalbbun-backend-api",
            "description", "Backend API catalog for the separated frontend project.",
            "groups", List.of(
                group("Chat", List.of(
                    endpoint("GET", "/api/chat/stream", "일반/RAG 채팅 SSE 스트림", "message, conversationId, category, useRag 등 query 기반"),
                    endpoint("POST", "/api/agent/execute", "에이전트 실행", "AgentRequestBody JSON")
                )),
                group("Conversation / Memory", List.of(
                    endpoint("GET", "/api/memory/conversations", "대화 목록 조회", "conversation list"),
                    endpoint("GET", "/api/memory/conversations/{conversationId}", "대화 상세 조회", "messages by conversationId"),
                    endpoint("DELETE", "/api/memory/conversations/{conversationId}", "대화 삭제", "delete conversation"),
                    endpoint("GET", "/api/memory/summary", "메모리 요약 조회", "memory summary")
                )),
                group("Prompt", List.of(
                    endpoint("GET", "/api/prompt-entries/summary", "프롬프트 엔트리 요약", "summary"),
                    endpoint("GET", "/api/prompt-entries", "프롬프트 엔트리 목록", "list"),
                    endpoint("GET", "/api/prompt-entries/{id}", "프롬프트 엔트리 상세", "detail"),
                    endpoint("GET", "/api/prompt-entries/default", "기본 프롬프트 조회", "default entry"),
                    endpoint("POST", "/api/prompt-entries", "프롬프트 엔트리 생성", "create"),
                    endpoint("PUT", "/api/prompt-entries/{id}", "프롬프트 엔트리 수정", "update"),
                    endpoint("DELETE", "/api/prompt-entries/{id}", "프롬프트 엔트리 삭제", "delete"),
                    endpoint("POST", "/api/prompt-entries/{id}/default", "기본 프롬프트 지정", "set default"),
                    endpoint("POST", "/api/prompt-entries/seed", "프롬프트 초기 데이터 반영", "seed"),
                    endpoint("GET", "/api/prompts", "프롬프트 템플릿 목록", "template list"),
                    endpoint("GET", "/api/prompts/{id}", "프롬프트 템플릿 상세", "template detail"),
                    endpoint("POST", "/api/prompts", "프롬프트 템플릿 생성", "create template"),
                    endpoint("PUT", "/api/prompts/{id}", "프롬프트 템플릿 수정", "update template"),
                    endpoint("DELETE", "/api/prompts/{id}", "프롬프트 템플릿 삭제", "delete template")
                )),
                group("Runtime / Model", List.of(
                    endpoint("GET", "/api/runtime/ollama", "런타임 Ollama 정보", "runtime info"),
                    endpoint("GET", "/api/model-priority", "모델 우선순위 조회", "priority list"),
                    endpoint("POST", "/api/model-priority", "모델 우선순위 저장", "save priorities"),
                    endpoint("POST", "/api/model-priority/reset", "모델 우선순위 초기화", "reset priorities")
                )),
                group("API Key", List.of(
                    endpoint("GET", "/api/api-keys/providers", "API 키 제공자 목록", "providers"),
                    endpoint("GET", "/api/api-keys/runtime-status", "API 키 런타임 상태", "runtime status"),
                    endpoint("GET", "/api/api-keys", "API 키 목록", "list"),
                    endpoint("GET", "/api/api-keys/{id}", "API 키 상세", "detail"),
                    endpoint("GET", "/api/api-keys/{id}/reveal", "API 키 복호화 조회", "admin only"),
                    endpoint("POST", "/api/api-keys", "API 키 생성", "create"),
                    endpoint("PUT", "/api/api-keys/{id}", "API 키 수정", "update"),
                    endpoint("DELETE", "/api/api-keys/{id}", "API 키 삭제", "delete"),
                    endpoint("POST", "/api/api-keys/{id}/activate", "API 키 활성화", "activate")
                )),
                group("Debug / Admin", List.of(
                    endpoint("GET", "/debug", "디버그 홈 JSON", "debug home"),
                    endpoint("GET", "/debug/api/memory", "메모리 디버그", "debug memory"),
                    endpoint("POST", "/debug/api/memory/clear", "메모리 초기화", "clear memory"),
                    endpoint("GET", "/debug/api/search", "검색 디버그", "search debug"),
                    endpoint("GET", "/debug/api/search/fetch", "검색 fetch", "search fetch"),
                    endpoint("GET", "/debug/api/config", "런타임 설정 조회", "runtime config"),
                    endpoint("POST", "/debug/api/config", "런타임 설정 저장", "save runtime config"),
                    endpoint("POST", "/debug/api/config/reset", "런타임 설정 초기화", "reset runtime config"),
                    endpoint("GET", "/debug/api/ollama/connection", "Ollama 연결 조회", "connection info"),
                    endpoint("POST", "/debug/api/ollama/connection", "Ollama 연결 저장", "save connection"),
                    endpoint("POST", "/debug/api/ollama/connection/reset", "Ollama 연결 초기화", "reset connection"),
                    endpoint("GET", "/debug/api/ollama/models", "Ollama 모델 목록", "model list"),
                    endpoint("POST", "/debug/api/ollama/models/action", "Ollama 모델 액션", "pull/load/unload"),
                    endpoint("GET", "/debug/api/ollama/config", "Ollama 설정 조회", "ollama config"),
                    endpoint("POST", "/debug/api/ollama/config", "Ollama 설정 저장", "save config"),
                    endpoint("POST", "/debug/api/ollama/config/reset", "Ollama 설정 초기화", "reset config"),
                    endpoint("GET", "/debug/api/rag/status", "RAG 상태 조회", "rag status"),
                    endpoint("GET", "/debug/api/rag/db-info", "RAG DB 정보", "rag db info"),
                    endpoint("GET", "/debug/api/rag/search", "RAG 검색 테스트", "rag search"),
                    endpoint("GET", "/debug/api/rag/sources", "RAG 소스 목록", "source list")
                ))
            )
        );
    }

    private Map<String, Object> group(String name, List<Map<String, String>> endpoints) {
        return Map.of("name", name, "endpoints", endpoints);
    }

    private Map<String, String> endpoint(String method, String path, String title, String notes) {
        return Map.of("method", method, "path", path, "title", title, "notes", notes);
    }
}
