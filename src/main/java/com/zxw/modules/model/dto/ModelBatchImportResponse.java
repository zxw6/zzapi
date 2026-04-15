package com.zxw.modules.model.dto;

import java.util.List;

public record ModelBatchImportResponse(
        int importedCount,
        int skippedCount,
        List<String> importedModels,
        List<String> skippedModels
) {
}
