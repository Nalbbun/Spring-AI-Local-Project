package ai.local.nalbbun.api.dto.catalog;

import java.util.List;

public record ApiGroupDto(String name, List<ApiEndpointDto> endpoints) {}
