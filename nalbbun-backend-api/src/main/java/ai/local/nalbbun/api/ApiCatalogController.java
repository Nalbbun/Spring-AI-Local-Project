package ai.local.nalbbun.api;

import ai.local.nalbbun.api.dto.catalog.*;
import ai.local.nalbbun.api.dto.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class ApiCatalogController {

    @GetMapping
    public ApiResponse<ApiCatalogDto> getCatalog() {
        return ApiResponse.ok(new ApiCatalogDto(
            "nalbbun-backend-api",
            "Backend API catalog for the separated frontend project.",
            List.of(
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
                    endpoint("POST", "/api/prompt-entries/seed", "프롬프트 초기 데이터 반영", "seed")
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
                ))
            )
        ));
    }

    private ApiGroupDto group(String name, List<ApiEndpointDto> endpoints) { return new ApiGroupDto(name, endpoints); }
    private ApiEndpointDto endpoint(String method, String path, String title, String notes) { return new ApiEndpointDto(method, path, title, notes); }
}
