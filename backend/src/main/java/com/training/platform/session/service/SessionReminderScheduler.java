package com.training.platform.session.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionReminderScheduler {

    private final TrainingSessionService trainingSessionService;

    public SessionReminderScheduler(TrainingSessionService trainingSessionService) {
        this.trainingSessionService = trainingSessionService;
    }

    @Scheduled(initialDelay = 5000, fixedDelay = 60000)
    public void remindUpcomingSessions() {
        trainingSessionService.sendDueReminders();
    }
}
