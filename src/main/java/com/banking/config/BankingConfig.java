package com.banking.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Properties;

public class BankingConfig {
    private static final Logger logger = LogManager.getLogger(BankingConfig.class);

    private static final String CONFIG_FILE = "src/main/resources/banking_config.properties";
    private Properties properties;

    public BankingConfig() {
        properties = new Properties();
        loadConfiguration();
    }

    private void loadConfiguration() {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
            logger.info("Configuration loaded successfully from: {}", CONFIG_FILE);
        } catch (IOException e) {
            logger.error("Failed to load configuration file: {}", e.getMessage());
            createDefaultConfiguration();
        }
    }

    private void createDefaultConfiguration() {
        properties.setProperty("database.url", "jdbc:mysql://localhost:3306/banking_simulator");
        properties.setProperty("database.username", "root");
        properties.setProperty("database.password", "password");
        properties.setProperty("database.driver", "com.mysql.cj.jdbc.Driver");

        properties.setProperty("email.smtp.host", "smtp.gmail.com");
        properties.setProperty("email.smtp.port", "587");
        properties.setProperty("email.username", "your-email@gmail.com");
        properties.setProperty("email.password", "your-app-password");
        properties.setProperty("email.enabled", "false");

        properties.setProperty("alert.low_balance_threshold", "100.0");
        properties.setProperty("alert.high_balance_threshold", "10000.0");
        properties.setProperty("alert.check_interval_seconds", "30");

        properties.setProperty("report.directory", "reports/");
        properties.setProperty("report.generate_daily", "true");

        properties.setProperty("application.name", "Banking Transaction Simulator");
        properties.setProperty("application.version", "1.0.0");

        try {
            // Ensure resources directory exists
            new File("src/main/resources").mkdirs();

            try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
                properties.store(output, "Banking System Configuration");
                logger.info("Default configuration file created: {}", CONFIG_FILE);
            }
        } catch (IOException e) {
            logger.error("Failed to create configuration file: {}", e.getMessage(), e);
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public double getDoubleProperty(String key, double defaultValue) {
        try {
            return Double.parseDouble(properties.getProperty(key));
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }

    public int getIntProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(properties.getProperty(key));
        } catch (NullPointerException e) {
            return defaultValue;
        }
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public void saveConfiguration() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "Banking System Configuration");
            logger.info("✅ Configuration saved successfully.");
        } catch (IOException e) {
            logger.error("❌ Failed to save configuration: " + e.getMessage());
        }
    }
}