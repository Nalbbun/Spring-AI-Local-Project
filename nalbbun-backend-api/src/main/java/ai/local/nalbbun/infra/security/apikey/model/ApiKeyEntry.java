package ai.local.nalbbun.infra.security.apikey.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API 키 엔트리.
 * keyValue 는 AES 암호화 후 저장되며 API 응답 시 마스킹 처리됩니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyEntry {

    private String id;

    /**
     * 프로바이더 식별자.
     * 예: OPENAI, TAVILY, ANTHROPIC
     */
    private String provider;

    /** 표시용 레이블 */
    private String label;

    /** 설명 */
    private String description;

    /**
     * 실제 API 키 값 (DB에는 AES 암호화 저장).
     * 응답 시 마스킹 처리 — 뷰 API에서만 복호화 반환.
     */
    private String keyValue;

    /** 활성화 여부 */
    private boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 마스킹된 키 반환 (앞 4자 + *** + 끝 4자) */
    public String maskedKey() {
        if (keyValue == null || keyValue.length() < 8) return "****";
        return keyValue.substring(0, 4) + "****" + keyValue.substring(keyValue.length() - 4);
    }
}
