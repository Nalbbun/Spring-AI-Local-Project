package ai.local.nalbbun.api.dto.catalog;

import java.util.List;

public record ApiCatalogDto(String application, String description, List<ApiGroupDto> groups) {}
