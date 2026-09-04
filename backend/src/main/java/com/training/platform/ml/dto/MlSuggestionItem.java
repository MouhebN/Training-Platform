package com.training.platform.ml.dto;

import java.util.List;

public record MlSuggestionItem(
        Long formationId,
        double score,
        List<String> reasons
) {
}
