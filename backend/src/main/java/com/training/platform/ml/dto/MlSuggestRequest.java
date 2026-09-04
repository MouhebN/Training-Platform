package com.training.platform.ml.dto;

import java.util.List;

public record MlSuggestRequest(
        MlProfilePayload profile,
        List<MlFormationPayload> formations
) {
}
