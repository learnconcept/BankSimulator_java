package com.banking.notification;

import com.banking.config.BankingConfig;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EmailService {
    private static EmailService instance;
    private BankingConfig config;
    private boolean enabled;

    private EmailService() {
        this.config = new BankingConfig();
        this.enabled = config.getBooleanProperty("email.enabled", false);
        initializeEmailService();
    }

    public static EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }

    private void initializeEmailService() {
        if (enabled) {
            System.out.println("✅ Email service initialized (SIMULATION MODE)");
            System.out.println("   SMTP Host: " + config.getProperty("email.smtp.host"));
            System.out.println("   SMTP Port: " + config.getProperty("email.smtp.port"));
        } else {
            System.out.println("✅ Email service initialized (CONSOLE MODE - emails will be logged to console)");
        }
    }

    public void sendBalanceAlert(String toEmail, String subject, String message) {
        if (enabled) {
            sendRealEmail(toEmail, subject, message);
        } else {
            simulateEmailSending(toEmail, subject, message);
        }
    }

    private void simulateEmailSending(String toEmail, String subject, String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        System.out.println("\n═══════════════════════════════════════════════════");
        System.out.println("📧 SIMULATED EMAIL ALERT");
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("Timestamp: " + timestamp);
        System.out.println("To: " + toEmail);
        System.out.println("Subject: " + subject);
        System.out.println("───────────────────────────────────────────────────");
        System.out.println(message);
        System.out.println("═══════════════════════════════════════════════════\n");

        // Log email to file
        logEmailToFile(toEmail, subject, message);
    }

    private void sendRealEmail(String toEmail, String subject, String message) {
        // In a real implementation, this would use JavaMail API
        // For now, we'll simulate it and log to file

        System.out.println("📧 SENDING REAL EMAIL (Simulated)");
        System.out.println("   To: " + toEmail);
        System.out.println("   Subject: " + subject);

        // Log to file as if it was sent
        logEmailToFile(toEmail, subject, message);

        System.out.println("✅ Email sent successfully to: " + toEmail);
    }

    private void logEmailToFile(String toEmail, String subject, String message) {
        String logDirectory = "email_logs/";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        try {
            // Ensure directory exists
            new File(logDirectory).mkdirs();

            String filename = logDirectory + "email_" + timestamp + ".txt";
            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                writer.println("Email Log - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                writer.println("==========================================");
                writer.println("To: " + toEmail);
                writer.println("Subject: " + subject);
                writer.println("------------------------------------------");
                writer.println(message);
                writer.println("==========================================");
            }

            // Also append to main email log file
            try (PrintWriter writer = new PrintWriter(new FileWriter(logDirectory + "all_emails.log", true))) {
                writer.println("[" + timestamp + "] TO: " + toEmail + " | SUBJECT: " + subject);
            }

        } catch (IOException e) {
            System.err.println("❌ Failed to log email: " + e.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        config.setProperty("email.enabled", String.valueOf(enabled));
        config.saveConfiguration();
        System.out.println("✅ Email service " + (enabled ? "enabled" : "disabled"));
    }

    // Real email sending implementation (commented out for safety)
    /*
    private void sendRealEmailWithJavaMail(String toEmail, String subject, String messageBody) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", config.getProperty("email.smtp.host"));
        props.put("mail.smtp.port", config.getProperty("email.smtp.port"));

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                    config.getProperty("email.username"),
                    config.getProperty("email.password")
                );
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.getProperty("email.username")));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(messageBody);

            Transport.send(message);
            System.out.println("✅ Real email sent successfully to: " + toEmail);
        } catch (MessagingException e) {
            System.err.println("❌ Failed to send real email: " + e.getMessage());
            // Fall back to simulation
            simulateEmailSending(toEmail, subject, messageBody);
        }
    }
    */
}