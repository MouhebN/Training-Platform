package com.training.platform.ml.dto;

import java.util.List;

public record MlFormationPayload(
        Long id,
        String title,
        String level,
        List<String> requiredSkills,
        String category
) {
}
