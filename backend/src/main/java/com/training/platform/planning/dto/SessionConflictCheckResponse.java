package com.training.platform.planning.dto;

import java.util.List;

public record SessionConflictCheckResponse(
        boolean hasBlockingConflicts,
        boolean hasWarnings,
        List<SessionConflictItem> conflicts
) {
}
