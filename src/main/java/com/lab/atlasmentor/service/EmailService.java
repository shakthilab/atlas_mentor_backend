package com.lab.atlasmentor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

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

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Email Verification - Atlas Mentor");

            String htmlContent =
                    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" +
                            "<html xmlns='http://www.w3.org/1999/xhtml' lang='en'>" +
                            "<head>" +
                            "<meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />" +
                            "<meta name='viewport' content='width=device-width, initial-scale=1.0' />" +
                            "<meta name='x-apple-disable-message-reformatting' />" +
                            "<title>Account Activated - AtlasMentor</title>" +
                            "</head>" +

                            // Full-width outer wrapper
                            "<body style='margin:0;padding:0;width:100%;background-color:#f0f2f5;-webkit-text-size-adjust:100%;-ms-text-size-adjust:100%;'>" +
                            "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='border-collapse:collapse;background-color:#f0f2f5;min-width:100%;'>" +
                            "<tr><td align='center' style='padding:40px 16px;'>" +

                            // Brand name above card
                            "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='border-collapse:collapse;max-width:560px;'>" +
                            "<tr><td align='center' style='padding-bottom:16px;'>" +
                            "<span style='font-size:22px;font-weight:800;color:#1a73e8;font-family:Segoe UI,Helvetica Neue,Arial,sans-serif;letter-spacing:-0.5px;'>AtlasMentor</span>" +
                            "</td></tr>" +
                            "</table>" +

                            // Card wrapper — 560px max, full width on mobile
                            "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='border-collapse:collapse;max-width:560px;background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.10);'>" +

                            // ── BODY ──────────────────────────────────────────
                            "<tr><td align='center' style='padding:48px 40px 36px;font-family:Segoe UI,Helvetica Neue,Arial,sans-serif;'>" +

                            // Icon: envelope + green badge
                            "<table role='presentation' cellpadding='0' cellspacing='0' border='0' style='border-collapse:collapse;margin:0 auto 28px auto;'>" +
                            "<tr>" +
                            "<td align='center' valign='middle' style='width:80px;height:60px;background-color:#1a73e8;border-radius:10px;padding:0;'>" +
                            // envelope SVG
                            "<img src='https://i.imgur.com/placeholder.png' width='0' height='0' alt='' style='display:none;' />" +
                            // Draw envelope with nested table
                            "<table role='presentation' cellpadding='0' cellspacing='0' border='0' style='border-collapse:collapse;margin:0 auto;' width='56' height='60'>" +
                            "<tr><td height='18'></td></tr>" +
                            "<tr>" +
                            "<td style='width:56px;height:8px;border-left:2px solid #ffffff;border-top:2px solid #ffffff;border-right:2px solid #ffffff;border-radius:3px 3px 0 0;font-size:0;'></td>" +
                            "</tr>" +
                            "<tr>" +
                            "<td align='center' style='width:56px;height:20px;border-left:2px solid #ffffff;border-bottom:2px solid #ffffff;border-right:2px solid #ffffff;border-radius:0 0 3px 3px;font-size:7px;color:#ffffff;'>&#9660;</td>" +
                            "</tr>" +
                            "<tr><td height='10'></td></tr>" +
                            "</table>" +
                            "</td>" +
                            // green checkmark badge
                            "<td valign='bottom' style='width:18px;padding-bottom:0;'>" +
                            "<div style='width:26px;height:26px;background-color:#34a853;border-radius:50%;border:3px solid #ffffff;text-align:center;line-height:20px;font-size:14px;font-weight:bold;color:#ffffff;margin-left:-12px;margin-bottom:-8px;'>&#10003;</div>" +
                            "</td>" +
                            "</tr></table>" +

                            "<h1 style='margin:0 0 14px 0;font-size:26px;font-weight:800;color:#1a1a2e;font-family:Segoe UI,Helvetica Neue,Arial,sans-serif;'>Account Activated</h1>" +
                            "<p style='margin:0 0 10px 0;font-size:17px;font-weight:700;color:#333333;font-family:Segoe UI,Helvetica Neue,Arial,sans-serif;'>Hello,</p>" +
                            "<p style='margin:0 0 32px 0;font-size:15px;color:#666666;line-height:1.75;font-family:Segoe UI,Helvetica Neue,Arial,sans-serif;'>Thank you! Your email has been verified.<br />Your account is now active. Please use the link below to login.</p>" +

                            // CTA button
                            "<table role='presentation' cellpadding='0' cellspacing='0' border='0' style='border-collapse:collapse;margin:0 auto;'>" +
                            "<tr><td align='center' style='border-radius:8px;background-color:#1a73e8;'>" +
                            "<a href='" + verificationLink + "' target='_blank' style='display:inline-block;padding:15px 40px;font-size:14px;font-weight:800;color:#ffffff;text-decoration:none;text-transform:uppercase;letter-spacing:1px;font-family:Segoe UI,Helvetica Neue,Arial,sans-serif;border-radius:8px;background-color:#1a73e8;mso-padding-alt:0;'>Login to Your Account</a>" +
                            "</td></tr></table>" +

                            "</td></tr>" +

                            // ── DIVIDER ──────────────────────────────────────
                            "<tr><td style='height:1px;background-color:#eeeeee;font-size:0;line-height:0;'>&nbsp;</td></tr>" +

                            // ── FOOTER ───────────────────────────────────────
                            "<tr><td align='center' style='padding:22px 40px;background-color:#f8f9fa;font-family:Segoe UI,Helvetica Neue,Arial,sans-serif;'>" +
                            "<p style='margin:0;font-size:13px;color:#999999;line-height:1.6;'>Thank you for choosing <span style='color:#1a73e8;font-weight:700;'>AtlasMentor</span>.<br />If you didn't create this account, you can safely ignore this email.</p>" +
                            "</td></tr>" +

                            "</table>" + // end card

                            "</td></tr></table>" + // end outer wrapper
                            "</body></html>";

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Password Reset - Atlas Mentor");

            String htmlContent =
                    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" +
                            "<html xmlns='http://www.w3.org/1999/xhtml' lang='en'>" +
                            "<head>" +
                            "<meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />" +
                            "<meta name='viewport' content='width=device-width, initial-scale=1.0' />" +
                            "<meta name='x-apple-disable-message-reformatting' />" +
                            "<title>Password Reset - AtlasMentor</title>" +
                            "</head>" +

                            "<body style='margin:0;padding:0;width:100%;background-color:#f0f2f5;-webkit-text-size-adjust:100%;-ms-text-size-adjust:100%;'>" +
                            "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='border-collapse:collapse;background-color:#f0f2f5;min-width:100%;'>" +
                            "<tr><td align='center' style='padding:40px 16px;'>" +

                            // Brand name
                            "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='border-collapse:collapse;max-width:560px;'>" +
                            "<tr><td align='center' style='padding-bottom:16px;'>" +
                            "<span style='font-size:22px;font-weight:800;color:#1a73e8;font-family:Segoe UI,Helvetica Neue,Arial,sans-serif;letter-spacing:-0.5px;'>AtlasMentor</span>" +
                            "</td></tr>" +
                            "</table>" +

                            // Card
                            "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='border-collapse:collapse;max-width:560px;background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.10);'>" +

                            // ── BODY ──────────────────────────────────────────
                            "<tr><td align='center' style='padding:48px 40px 36px;font-family:Segoe UI,Helvetica Neue,Arial,sans-serif;'>" +

                            // Lock icon circle + amber badge
                            "<table role='presentation' cellpadding='0' cellspacing='0' border='0' style='border-collapse:collapse;margin:0 auto 28px auto;'>" +
                            "<tr>" +
                            "<td align='center' valign='middle' style='width:80px;height:80px;background-color:#1a73e8;border-radius:50%;'>" +
                            "<table role='presentation' cellpadding='0' cellspacing='0' border='0' style='border-collapse:collapse;margin:0 auto;'>" +
                            "<tr><td align='center'>" +
                            // shackle
                            "<div style='width:22px;height:12px;border-top:3px solid #ffffff;border-left:3px solid #ffffff;border-right:3px solid #ffffff;border-radius:11px 11px 0 0;margin:0 auto 0 auto;font-size:0;'></div>" +
                            // lock body
                            "<table role='presentation' cellpadding='0' cellspacing='0' border='0' style='border-collapse:collapse;border:3px solid #ffffff;border-radius:4px;width:34px;height:24px;background-color:#1a73e8;margin-top:0;'>" +
                            "<tr><td align='center' valign='middle'>" +
                            "<div style='width:8px;height:8px;background-color:#ffffff;border-radius:50%;margin:0 auto;'></div>" +
                            "</td></tr>" +
                            "</table>" +
                            "</td></tr>" +
                            "</table>" +
                            "</td>" +
                            // amber clock badge
                            "<td valign='bottom' style='width:18px;'>" +
                            "<div style='width:26px;height:26px;background-color:#fbbc04;border-radius:50%;border:3px solid #ffffff;text-align:center;line-height:20px;font-size:13px;color:#ffffff;margin-left:-12px;margin-bottom:-4px;'>&#9201;</div>" +
                            "</td>" +
                            "</tr></table>" +

                            "<h1 style='margin:0 0 14px 0;font-size:26px;font-weight:800;color:#1a1a2e;font-family:Segoe UI,Helvetica Neue,Arial,sans-serif;'>Password Reset</h1>" +
                            "<p style='margin:0 0 10px 0;font-size:17px;font-weight:700;color:#333333;font-family:Segoe UI,Helvetica Neue,Arial,sans-serif;'>Hello,</p>" +
                            "<p style='margin:0 0 18px 0;font-size:15px;color:#666666;line-height:1.75;font-family:Segoe UI,Helvetica Neue,Arial,sans-serif;'>We received a request to reset your password for your AtlasMentor account.<br />Click the button below to create a new password.</p>" +
                            "<p style='margin:0 0 32px 0;font-size:13px;color:#999999;font-style:italic;line-height:1.6;font-family:Segoe UI,Helvetica Neue,Arial,sans-serif;'>If you didn't request this, please ignore this email or contact our support team.</p>" +

                            // CTA button
                            "<table role='presentation' cellpadding='0' cellspacing='0' border='0' style='border-collapse:collapse;margin:0 auto;'>" +
                            "<tr><td align='center' style='border-radius:8px;background-color:#1a73e8;'>" +
                            "<a href='" + resetLink + "' target='_blank' style='display:inline-block;padding:15px 40px;font-size:14px;font-weight:800;color:#ffffff;text-decoration:none;text-transform:uppercase;letter-spacing:1px;font-family:Segoe UI,Helvetica Neue,Arial,sans-serif;border-radius:8px;background-color:#1a73e8;mso-padding-alt:0;'>Reset Password</a>" +
                            "</td></tr></table>" +

                            "<p style='margin:18px 0 0 0;font-size:13px;color:#bbbbbb;font-family:Segoe UI,Helvetica Neue,Arial,sans-serif;'>This link expires in 1 hour for security reasons.</p>" +

                            "</td></tr>" +

                            // ── DIVIDER ──────────────────────────────────────
                            "<tr><td style='height:1px;background-color:#eeeeee;font-size:0;line-height:0;'>&nbsp;</td></tr>" +

                            // ── FOOTER ───────────────────────────────────────
                            "<tr><td align='center' style='padding:22px 40px;background-color:#f8f9fa;font-family:Segoe UI,Helvetica Neue,Arial,sans-serif;'>" +
                            "<p style='margin:0;font-size:13px;color:#999999;line-height:1.6;'>Thank you for choosing <span style='color:#1a73e8;font-weight:700;'>AtlasMentor</span>.<br />This is an automated email, please do not reply directly.</p>" +
                            "</td></tr>" +

                            "</table>" + // end card

                            "</td></tr></table>" + // end outer wrapper
                            "</body></html>";

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send password reset email", e);
        }
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