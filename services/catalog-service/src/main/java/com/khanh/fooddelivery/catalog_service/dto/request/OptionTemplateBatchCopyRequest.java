package com.khanh.fooddelivery.catalog_service.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record OptionTemplateBatchCopyRequest(@NotEmpty List<@NotNull UUID> templateIds) {}
