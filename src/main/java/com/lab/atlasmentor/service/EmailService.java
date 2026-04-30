package com.lab.atlasmentor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    public void sendVerificationEmail(String toEmail, String verificationToken) {
        String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Email Verification - Atlas Mentor");
        message.setText("Please click the following link to verify your email address:\n\n" +
                       verificationLink + "\n\n" +
                       "This link will expire in 24 hours.\n\n" +
                       "If you did not create an account, please ignore this email.");
        
        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Password Reset - Atlas Mentor");
        message.setText("Please click the following link to reset your password:\n\n" +
                       resetLink + "\n\n" +
                       "This link will expire in 1 hour.\n\n" +
                       "If you did not request a password reset, please ignore this email.");
        
        mailSender.send(message);
    }

    public void sendEmployeeCredentialsEmail(String toEmail, String employeeName, String password) {
        String loginUrl = frontendUrl + "/login";
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Welcome to Atlas Mentor - Your Account Credentials");
        message.setText("Dear " + employeeName + ",\n\n" +
                       "Your employee account has been created successfully!\n\n" +
                       "Login URL: " + loginUrl + "\n" +
                       "Email: " + toEmail + "\n" +
                       "Temporary Password: " + password + "\n\n" +
                       "Please login and change your password for security reasons.\n\n" +
                       "If you have any questions, please contact your administrator.\n\n" +
                       "Best regards,\n" +
                       "Atlas Mentor Team");
        
        mailSender.send(message);
    }
}
