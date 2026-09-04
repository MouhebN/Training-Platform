package com.training.platform.common.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendPasswordResetToken(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Training Platform - Password Reset");
        message.setText("""
                Hello,

                A password reset was requested for your Training Platform account.

                Use this reset token to choose a new password:
                %s

                This token expires in 15 minutes and can be used only once.

                If you did not request this reset, contact the platform administrator.
                """.formatted(token));
        mailSender.send(message);
    }

    public void sendWaitlistPromotion(String to, String learnerName, String sessionTitle) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("You have been promoted from the waitlist");
        message.setText("""
                Hello %s,

                A place is now available for the session:
                %s

                Your enrollment has been automatically promoted from WAITLISTED to CONFIRMED.

                Please check your Training Platform account for the latest session details.
                """.formatted(learnerName, sessionTitle));
        mailSender.send(message);
    }
}
