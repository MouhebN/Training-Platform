package com.training.platform.enrollment.service;

import com.training.platform.enrollment.dto.EnrollmentResponse;
import com.training.platform.enrollment.entity.Enrollment;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentMapper {

    public EnrollmentResponse toResponse(Enrollment enrollment) {
        String learnerFullName = enrollment.getLearner().getUser().getFirstName()
                + " "
                + enrollment.getLearner().getUser().getLastName();
        String trainerFullName = enrollment.getSession().getTrainer().getUser().getFirstName()
                + " "
                + enrollment.getSession().getTrainer().getUser().getLastName();

        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getLearner().getId(),
                learnerFullName,
                enrollment.getSession().getId(),
                enrollment.getSession().getTitle(),
                enrollment.getSession().getFormation().getId(),
                enrollment.getSession().getFormation().getTitle(),
                enrollment.getSession().getFormation().getSessionCount(),
                trainerFullName,
                enrollment.getStatus(),
                enrollment.getEnrolledAt(),
                enrollment.getSession().getStartDate(),
                enrollment.getSession().getEndDate(),
                enrollment.getSession().getOnline(),
                enrollment.getSession().getLocation(),
                enrollment.getSession().getMeetingUrl(),
                enrollment.getSession().getStatus(),
                enrollment.getVirtualAttendancePercentage(),
                enrollment.getVirtualAttendanceQualified()
        );
    }
}
