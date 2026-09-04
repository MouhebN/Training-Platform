package com.training.platform.ml.dto;

import java.util.List;

public record MlProfilePayload(
        String level,
        List<String> skills,
        String goals
) {
}
