package com.training.platform.session.dto;

import java.util.List;

public record SessionCompletionRequest(
        List<Long> presentEnrollmentIds
) {
}
