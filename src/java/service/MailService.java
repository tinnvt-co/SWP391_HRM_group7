package service;

import config.MailConfig;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class MailService {

    private final Session session;

    public MailService() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            MailConfig.SMTP_HOST);
        props.put("mail.smtp.port",            MailConfig.SMTP_PORT);
        props.put("mail.smtp.ssl.trust",       MailConfig.SMTP_HOST);

        this.session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(MailConfig.USERNAME, MailConfig.PASSWORD);
            }
        });
    }

    public void sendHtml(String toEmail, String subject, String htmlBody)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(MailConfig.USERNAME, MailConfig.FROM_NAME, "UTF-8"));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        message.setSubject(subject, "UTF-8");
        message.setContent(htmlBody, "text/html; charset=UTF-8");
        Transport.send(message);
    }

    public boolean isConfigured() {
        return MailConfig.USERNAME != null && !MailConfig.USERNAME.isBlank()
                && MailConfig.PASSWORD != null && !MailConfig.PASSWORD.isBlank();
    }

    public void sendPasswordResetEmail(String toEmail, String fullName, String resetLink)
            throws MessagingException, UnsupportedEncodingException {
        String subject = "HRM System - Password Reset Request";
        String html = "<!DOCTYPE html><html><body style=\"font-family:Arial,sans-serif;background:#f4f6f8;padding:24px;\">"
                + "<div style=\"max-width:560px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,0.08);\">"
                + "<div style=\"background:linear-gradient(135deg,#1a3c5e,#2d6a9f);padding:24px;text-align:center;color:#fff;\">"
                + "<h2 style=\"margin:0;\">HRM System</h2>"
                + "<p style=\"margin:4px 0 0;opacity:0.85;\">Password Reset Request</p>"
                + "</div>"
                + "<div style=\"padding:32px 28px;color:#333;line-height:1.6;\">"
                + "<p>Hello <strong>" + escape(fullName) + "</strong>,</p>"
                + "<p>We received a request to reset the password for your HRM System account. "
                + "Click the button below to set a new password. This link will expire in <strong>15 minutes</strong>.</p>"
                + "<div style=\"text-align:center;margin:28px 0;\">"
                + "<a href=\"" + resetLink + "\" "
                + "style=\"display:inline-block;background:linear-gradient(135deg,#1a3c5e,#2d6a9f);color:#fff;text-decoration:none;padding:12px 28px;border-radius:8px;font-weight:600;\">"
                + "Reset Password</a>"
                + "</div>"
                + "<p style=\"font-size:13px;color:#666;\">If the button does not work, copy and paste this URL into your browser:</p>"
                + "<p style=\"font-size:13px;word-break:break-all;color:#2d6a9f;\">" + resetLink + "</p>"
                + "<hr style=\"border:none;border-top:1px solid #eee;margin:24px 0;\">"
                + "<p style=\"font-size:12px;color:#888;\">If you did not request a password reset, please ignore this email. Your password will remain unchanged.</p>"
                + "</div></div></body></html>";
        sendHtml(toEmail, subject, html);
    }

    public void sendAccountCreatedEmail(String toEmail, String fullName, String username,
                                        String temporaryPassword, String loginLink)
            throws MessagingException, UnsupportedEncodingException {
        String subject = "HRM System - Your Account Has Been Created";
        String html = "<!DOCTYPE html><html><body style=\"font-family:Arial,sans-serif;background:#f4f6f8;padding:24px;\">"
                + "<div style=\"max-width:560px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,0.08);\">"
                + "<div style=\"background:linear-gradient(135deg,#1a3c5e,#2d6a9f);padding:24px;text-align:center;color:#fff;\">"
                + "<h2 style=\"margin:0;\">HRM System</h2>"
                + "<p style=\"margin:4px 0 0;opacity:0.85;\">New Employee Account</p>"
                + "</div>"
                + "<div style=\"padding:32px 28px;color:#333;line-height:1.6;\">"
                + "<p>Hello <strong>" + escape(fullName) + "</strong>,</p>"
                + "<p>Your HRM System employee account has been created.</p>"
                + "<div style=\"background:#f8fafc;border:1px solid #e5e7eb;border-radius:10px;padding:16px;margin:20px 0;\">"
                + "<p style=\"margin:0 0 8px;\"><strong>Username:</strong> " + escape(username) + "</p>"
                + "<p style=\"margin:0;\"><strong>Temporary password:</strong> " + escape(temporaryPassword) + "</p>"
                + "</div>"
                + "<div style=\"text-align:center;margin:28px 0;\">"
                + "<a href=\"" + loginLink + "\" "
                + "style=\"display:inline-block;background:linear-gradient(135deg,#1a3c5e,#2d6a9f);color:#fff;text-decoration:none;padding:12px 28px;border-radius:8px;font-weight:600;\">"
                + "Sign In</a>"
                + "</div>"
                + "<p style=\"font-size:13px;color:#666;\">Please sign in and change your password after first login.</p>"
                + "<p style=\"font-size:13px;word-break:break-all;color:#2d6a9f;\">" + loginLink + "</p>"
                + "</div></div></body></html>";
        sendHtml(toEmail, subject, html);
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
