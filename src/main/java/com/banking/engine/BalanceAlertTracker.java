package com.banking.engine;

import com.banking.model.Account;
import com.banking.notification.EmailService;
import com.banking.database.DatabaseManager;
import com.banking.config.BankingConfig;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BalanceAlertTracker {
    private AccountManagementEngine accountEngine;
    private EmailService emailService;
    private DatabaseManager databaseManager;
    private BankingConfig config;
    private ScheduledExecutorService scheduler;
    private Map<String, Double> accountThresholds;
    private Set<String> notifiedAccounts;

    private static BalanceAlertTracker instance;

    private BalanceAlertTracker() {
        this.accountEngine = AccountManagementEngine.getInstance();
        this.emailService = EmailService.getInstance();
        this.databaseManager = DatabaseManager.getInstance();
        this.config = new BankingConfig();
        this.accountThresholds = new HashMap<>();
        this.notifiedAccounts = new HashSet<>();
        initializeDefaultThresholds();
        startMonitoring();
    }

    public static BalanceAlertTracker getInstance() {
        if (instance == null) {
            instance = new BalanceAlertTracker();
        }
        return instance;
    }

    private void initializeDefaultThresholds() {
        double defaultThreshold = config.getDoubleProperty("alert.low_balance_threshold", 100.0);

        // Set default thresholds for existing accounts
        for (Account account : accountEngine.getAllAccounts()) {
            accountThresholds.put(account.getAccountNumber(), defaultThreshold);
        }
        System.out.println("✅ Balance alert tracker initialized with default threshold: $" + defaultThreshold);
    }

    private void startMonitoring() {
        int checkInterval = config.getIntProperty("alert.check_interval_seconds", 30);
        scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkAllAccountBalances();
            } catch (Exception e) {
                System.err.println("❌ Error in balance monitoring: " + e.getMessage());
            }
        }, 0, checkInterval, TimeUnit.SECONDS);

        System.out.println("✅ Balance monitoring started (interval: " + checkInterval + " seconds)");
    }

    public void checkAllAccountBalances() {
        for (Account account : accountEngine.getAllAccounts()) {
            checkAccountBalance(account);
        }
    }

    private void checkAccountBalance(Account account) {
        double balance = account.getBalance();
        double lowThreshold = accountThresholds.getOrDefault(account.getAccountNumber(),
                config.getDoubleProperty("alert.low_balance_threshold", 100.0));
        double highThreshold = config.getDoubleProperty("alert.high_balance_threshold", 10000.0);

        String accountKey = account.getAccountNumber();

        // Check for low balance
        if (balance < lowThreshold) {
            if (!notifiedAccounts.contains(accountKey + "_LOW")) {
                String subject = "🚨 Low Balance Alert - Account: " + account.getAccountNumber();
                String message = String.format(
                        "Dear %s,%n%nYour account %s has a low balance of $%.2f, " +
                                "which is below the threshold of $%.2f.%n" +
                                "Please consider depositing funds to avoid any inconvenience.%n%n" +
                                "Best regards,%nBanking System",
                        account.getAccountHolderName(), account.getAccountNumber(), balance, lowThreshold
                );

                sendAlert(account, "LOW_BALANCE", message);
                notifiedAccounts.add(accountKey + "_LOW");

                // Remove from high balance notifications if applicable
                notifiedAccounts.remove(accountKey + "_HIGH");
            }
        } else {
            notifiedAccounts.remove(accountKey + "_LOW");
        }

        // Check for high balance (potential fraud or large transaction)
        if (balance > highThreshold) {
            if (!notifiedAccounts.contains(accountKey + "_HIGH")) {
                String subject = "📈 High Balance Notice - Account: " + account.getAccountNumber();
                String message = String.format(
                        "Dear %s,%n%nYour account %s has a high balance of $%.2f.%n" +
                                "This is for your information. If this is unexpected, please contact customer support.%n%n" +
                                "Best regards,%nBanking System",
                        account.getAccountHolderName(), account.getAccountNumber(), balance
                );

                sendAlert(account, "THRESHOLD_BREACH", message);
                notifiedAccounts.add(accountKey + "_HIGH");
            }
        } else {
            notifiedAccounts.remove(accountKey + "_HIGH");
        }
    }

    public void checkOverdraftAttempt(String accountNumber, double attemptedAmount, double currentBalance) {
        Account account = accountEngine.getAccount(accountNumber);
        String subject = "💳 Overdraft Attempt Alert - Account: " + accountNumber;
        String message = String.format(
                "Dear %s,%n%nAn attempt was made to withdraw $%.2f from your account %s, " +
                        "but was declined due to insufficient funds.%n" +
                        "Current balance: $%.2f%nAttempted amount: $%.2f%nShortfall: $%.2f%n%n" +
                        "Please ensure sufficient funds are available for transactions.%n%n" +
                        "Best regards,%nBanking System",
                account.getAccountHolderName(), attemptedAmount, accountNumber,
                currentBalance, attemptedAmount, attemptedAmount - currentBalance
        );

        sendAlert(account, "OVERDRAFT_ATTEMPT", message);
    }

    private void sendAlert(Account account, String alertType, String message) {
        // Send email alert if enabled
        boolean emailEnabled = config.getBooleanProperty("email.enabled", false);
        if (emailEnabled) {
            String subject = "Banking System Alert - " + alertType.replace("_", " ");
            emailService.sendBalanceAlert(account.getEmail(), subject, message);
        } else {
            // Simulate email sending (print to console)
            System.out.println("✉️  EMAIL ALERT (Simulated)");
            System.out.println("   To: " + account.getEmail());
            System.out.println("   Type: " + alertType);
            System.out.println("   Account: " + account.getAccountNumber());
            System.out.println("   Balance: $" + account.getBalance());
        }

        // Log alert to database
        databaseManager.logBalanceAlert(account.getAccountNumber(), alertType, message);

        System.out.println("✅ Balance alert processed for account: " + account.getAccountNumber());
    }

    public void setAccountThreshold(String accountNumber, double threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("❌ Threshold cannot be negative");
        }

        if (!accountEngine.accountExists(accountNumber)) {
            throw new IllegalArgumentException("❌ Account not found: " + accountNumber);
        }

        accountThresholds.put(accountNumber, threshold);
        System.out.printf("✅ Threshold for account %s set to $%.2f%n", accountNumber, threshold);

        // Reset notification status for this account
        notifiedAccounts.remove(accountNumber + "_LOW");
        notifiedAccounts.remove(accountNumber + "_HIGH");

        // Immediate check after threshold change
        Account account = accountEngine.getAccount(accountNumber);
        checkAccountBalance(account);
    }

    public double getAccountThreshold(String accountNumber) {
        return accountThresholds.getOrDefault(accountNumber,
                config.getDoubleProperty("alert.low_balance_threshold", 100.0));
    }

    public Map<String, Double> getAllAccountThresholds() {
        return new HashMap<>(accountThresholds);
    }

    public void stopMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
                System.out.println("✅ Balance monitoring stopped.");
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void resetNotifications() {
        notifiedAccounts.clear();
        System.out.println("✅ Notification tracking reset.");
    }
}