package com.training.platform.ml.dto;

import java.util.List;

public record MlSuggestResponse(
        String source,
        String modelVersion,
        List<MlSuggestionItem> suggestions
) {
}
