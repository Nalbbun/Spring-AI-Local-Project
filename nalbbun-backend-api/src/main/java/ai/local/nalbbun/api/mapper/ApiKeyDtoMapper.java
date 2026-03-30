package ai.local.nalbbun.api.mapper;

import ai.local.nalbbun.api.dto.apikey.*;

import java.time.LocalDateTime;
import java.util.Map;

public final class ApiKeyDtoMapper {
    private ApiKeyDtoMapper() {}

    public static ApiKeyEntryDto toEntryDto(Map<String, Object> map) {
        return new ApiKeyEntryDto(
                str(map, "id"),
                str(map, "provider"),
                str(map, "label"),
                str(map, "description"),
                str(map, "maskedKey"),
                bool(map, "active"),
                (LocalDateTime) map.get("createdAt"),
                (LocalDateTime) map.get("updatedAt")
        );
    }

    public static ApiKeyProviderDto toProviderDto(Map<String, Object> map) {
        return new ApiKeyProviderDto(
                str(map, "provider"),
                str(map, "displayName"),
                str(map, "description"),
                str(map, "keyIssueUrl"),
                bool(map, "hasActiveKey"),
                str(map, "maskedKey")
        );
    }

    private static String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? null : v.toString();
    }
    private static boolean bool(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null && Boolean.parseBoolean(v.toString());
    }
}
