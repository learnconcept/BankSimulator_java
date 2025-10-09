package com.banking;

import com.banking.engine.*;
import com.banking.reporting.ReportGenerator;
import com.banking.notification.EmailService;
import com.banking.database.DatabaseManager;
import com.banking.exceptions.*;
import com.banking.config.BankingConfig;

import java.util.Scanner;
import java.util.List;
import java.util.Map;

public class BankingSimulator {
    private AccountManagementEngine accountEngine;
    private TransactionProcessingSystem transactionSystem;
    private BalanceAlertTracker alertTracker;
    private ReportGenerator reportGenerator;
    private EmailService emailService;
    private DatabaseManager databaseManager;
    private BankingConfig config;
    private Scanner scanner;
    private boolean running;

    public BankingSimulator() {
        this.config = new BankingConfig();
        this.accountEngine = AccountManagementEngine.getInstance();
        this.transactionSystem = TransactionProcessingSystem.getInstance();
        this.alertTracker = BalanceAlertTracker.getInstance();
        this.reportGenerator = new ReportGenerator();
        this.emailService = EmailService.getInstance();
        this.databaseManager = DatabaseManager.getInstance();
        this.scanner = new Scanner(System.in);
        this.running = true;
    }

    public void start() {
        displayWelcomeBanner();
        System.out.println("🚀 Initializing Banking Transaction Simulator...");

        // Display system status
        displaySystemStatus();

        while (running) {
            displayMainMenu();
            int choice = getIntInput("Enter your choice: ");

            try {
                switch (choice) {
                    case 1 -> createNewAccount();
                    case 2 -> performDeposit();
                    case 3 -> performWithdrawal();
                    case 4 -> performTransfer();
                    case 5 -> checkAccountBalance();
                    case 6 -> viewTransactionHistory();
                    case 7 -> generateReports();
                    case 8 -> setBalanceThreshold();
                    case 9 -> displayAllAccounts();
                    case 10 -> manageAccount();
                    case 11 -> systemAdministration();
                    case 0 -> shutdown();
                    default -> System.out.println("❌ Invalid choice. Please try again.");
                }
            } catch (Exception e) {
                System.out.println("❌ Error: " + e.getMessage());
            }

            System.out.println();
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
        }
    }

    private void displayWelcomeBanner() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                              ║");
        System.out.println("║           🏦 BANKING TRANSACTION SIMULATOR 🏦               ║");
        System.out.println("║                                                              ║");
        System.out.println("║          Java + JDBC + MySQL Implementation                 ║");
        System.out.println("║                                                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private void displaySystemStatus() {
        System.out.println("📊 SYSTEM STATUS:");
        System.out.println("───────────────────────────────────────────────────");
        System.out.println("✅ Accounts: " + accountEngine.getTotalAccounts());
        System.out.println("✅ Transactions: " + transactionSystem.getTotalTransactions());
        System.out.println("✅ Database: " + (databaseManager.isConnected() ? "Connected" : "Disconnected"));
        System.out.println("✅ Email Service: " + (emailService.isEnabled() ? "Enabled" : "Disabled (Simulation)"));
        System.out.println("✅ Balance Monitoring: Active");
        System.out.println("───────────────────────────────────────────────────");
        System.out.println();
    }

    private void displayMainMenu() {
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║                   MAIN MENU                    ║");
        System.out.println("╠════════════════════════════════════════════════╣");
        System.out.println("║  1  ➤ Create New Account                       ║");
        System.out.println("║  2  ➤ Deposit Money                            ║");
        System.out.println("║  3  ➤ Withdraw Money                           ║");
        System.out.println("║  4  ➤ Transfer Money                           ║");
        System.out.println("║  5  ➤ Check Account Balance                    ║");
        System.out.println("║  6  ➤ View Transaction History                 ║");
        System.out.println("║  7  ➤ Generate Reports                         ║");
        System.out.println("║  8  ➤ Set Balance Alert Threshold              ║");
        System.out.println("║  9  ➤ Display All Accounts                     ║");
        System.out.println("║  10 ➤ Manage Account                           ║");
        System.out.println("║  11 ➤ System Administration                    ║");
        System.out.println("║  0  ➤ Exit                                     ║");
        System.out.println("╚════════════════════════════════════════════════╝");
    }

    private void createNewAccount() {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║              CREATE NEW ACCOUNT                ║");
        System.out.println("╚════════════════════════════════════════════════╝");

        String accountNumber = getStringInput("Enter account number: ");
        String accountHolder = getStringInput("Enter account holder name: ");
        double initialBalance = getDoubleInput("Enter initial balance: $");
        String email = getStringInput("Enter email address: ");

        try {
            accountEngine.createAccount(accountNumber, accountHolder, initialBalance, email);
            alertTracker.setAccountThreshold(accountNumber, 100.0); // Default threshold
            System.out.println("✅ Account created successfully!");
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Failed to create account: " + e.getMessage());
        }
    }

    private void performDeposit() {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║                  DEPOSIT MONEY                 ║");
        System.out.println("╚════════════════════════════════════════════════╝");

        String accountNumber = getStringInput("Enter account number: ");
        double amount = getDoubleInput("Enter deposit amount: $");

        try {
            transactionSystem.deposit(accountNumber, amount);
            System.out.println("✅ Deposit completed successfully!");
        } catch (BankingException e) {
            System.out.println("❌ Deposit failed: " + e.getMessage());
        }
    }

    private void performWithdrawal() {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║                 WITHDRAW MONEY                 ║");
        System.out.println("╚════════════════════════════════════════════════╝");

        String accountNumber = getStringInput("Enter account number: ");
        double amount = getDoubleInput("Enter withdrawal amount: $");

        try {
            transactionSystem.withdraw(accountNumber, amount);
            System.out.println("✅ Withdrawal completed successfully!");
        } catch (InsufficientFundsException e) {
            System.out.println("❌ Withdrawal failed: " + e.getMessage());
            // Trigger overdraft alert
            alertTracker.checkOverdraftAttempt(accountNumber, amount, e.getCurrentBalance());
        } catch (BankingException e) {
            System.out.println("❌ Withdrawal failed: " + e.getMessage());
        }
    }

    private void performTransfer() {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║                 TRANSFER MONEY                 ║");
        System.out.println("╚════════════════════════════════════════════════╝");

        String fromAccount = getStringInput("Enter source account number: ");
        String toAccount = getStringInput("Enter target account number: ");
        double amount = getDoubleInput("Enter transfer amount: $");

        try {
            transactionSystem.transfer(fromAccount, toAccount, amount);
            System.out.println("✅ Transfer completed successfully!");
        } catch (InsufficientFundsException e) {
            System.out.println("❌ Transfer failed: " + e.getMessage());
            alertTracker.checkOverdraftAttempt(fromAccount, amount, e.getCurrentBalance());
        } catch (BankingException e) {
            System.out.println("❌ Transfer failed: " + e.getMessage());
        }
    }

    private void checkAccountBalance() {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║              CHECK ACCOUNT BALANCE             ║");
        System.out.println("╚════════════════════════════════════════════════╝");

        String accountNumber = getStringInput("Enter account number: ");

        try {
            double balance = accountEngine.getAccountBalance(accountNumber);
            double threshold = alertTracker.getAccountThreshold(accountNumber);
            System.out.println("┌────────────────────────────────────────────┐");
            System.out.println("│              ACCOUNT BALANCE               │");
            System.out.println("├────────────────────────────────────────────┤");
            System.out.printf("│ Account: %-32s │%n", accountNumber);
            System.out.printf("│ Current Balance: $%-24.2f │%n", balance);
            System.out.printf("│ Low Balance Threshold: $%-19.2f │%n", threshold);

            if (balance < threshold) {
                System.out.println("│                                            │");
                System.out.println("│         🚨 BALANCE BELOW THRESHOLD!       │");
            }
            System.out.println("└────────────────────────────────────────────┘");

        } catch (IllegalArgumentException e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }

    private void viewTransactionHistory() {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║            TRANSACTION HISTORY                 ║");
        System.out.println("╚════════════════════════════════════════════════╝");

        String accountNumber = getStringInput("Enter account number: ");

        try {
            var transactions = transactionSystem.getTransactionHistory(accountNumber);
            if (transactions.isEmpty()) {
                System.out.println("📭 No transactions found for account: " + accountNumber);
            } else {
                System.out.println("┌─────────────────────────────────────────────────────────────────────────────────────────────────────┐");
                System.out.println("│                              TRANSACTION HISTORY FOR ACCOUNT: " + accountNumber + "                       │");
                System.out.println("├──────────────┬─────────────┬──────────┬────────────┬──────────────┬────────────────┬─────────────────┤");
                System.out.printf ("│ %-12s │ %-11s │ %-8s │ %-10s │ %-12s │ %-14s │ %-15s │%n",
                        "Date", "Time", "Type", "Status", "Amount", "Target Account", "Transaction ID");
                System.out.println("├──────────────┼─────────────┼──────────┼────────────┼──────────────┼────────────────┼─────────────────┤");

                int count = 0;
                for (var transaction : transactions) {
                    if (count++ >= 20) { // Limit to 20 transactions for display
                        System.out.println("│ ...                                                                                           │");
                        break;
                    }

                    String date = transaction.getTransactionDate().format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"));
                    String time = transaction.getTransactionDate().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                    String status = transaction.getStatus() == com.banking.model.Transaction.TransactionStatus.SUCCESS ? "✅" : "❌";

                    System.out.printf("│ %-12s │ %-11s │ %-8s │ %-10s │ $%-11.2f │ %-14s │ %-15s │%n",
                            date, time, transaction.getTransactionType(), status,
                            transaction.getAmount(),
                            transaction.getTargetAccount() != null ? transaction.getTargetAccount() : "N/A",
                            transaction.getTransactionId().substring(0, 8) + "...");
                }

                System.out.println("└──────────────┴─────────────┴──────────┴────────────┴──────────────┴────────────────┴─────────────────┘");
                System.out.println("📊 Total transactions displayed: " + Math.min(transactions.size(), 20) + " of " + transactions.size());
            }
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }

    private void generateReports() {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║                GENERATE REPORTS                ║");
        System.out.println("╚════════════════════════════════════════════════╝");
        System.out.println("1. Account Summary Report");
        System.out.println("2. Transaction Report for Account");
        System.out.println("3. Daily Transaction Report");
        System.out.println("4. System Health Report");
        System.out.println("5. Export Accounts to CSV");
        System.out.println("6. Export Transactions to CSV");

        int choice = getIntInput("Choose report type: ");

        switch (choice) {
            case 1 -> {
                reportGenerator.generateAccountSummaryReport();
                System.out.println("✅ Account summary report generated in reports/ directory");
            }
            case 2 -> {
                String accountNumber = getStringInput("Enter account number: ");
                reportGenerator.generateTransactionReport(accountNumber);
                System.out.println("✅ Transaction report generated in reports/ directory");
            }
            case 3 -> {
                reportGenerator.generateDailyTransactionReport();
                System.out.println("✅ Daily transaction report generated in reports/ directory");
            }
            case 4 -> {
                reportGenerator.generateSystemHealthReport();
                System.out.println("✅ System health report generated in reports/ directory");
            }
            case 5 -> {
                String filename = getStringInput("Enter CSV filename (without extension): ");
                reportGenerator.exportToCSV(filename);
                System.out.println("✅ Accounts exported to CSV in reports/ directory");
            }
            case 6 -> {
                String accountNumber = getStringInput("Enter account number: ");
                String filename = getStringInput("Enter CSV filename (without extension): ");
                reportGenerator.exportTransactionsToCSV(accountNumber, filename);
                System.out.println("✅ Transactions exported to CSV in reports/ directory");
            }
            default -> System.out.println("❌ Invalid choice.");
        }
    }

    private void setBalanceThreshold() {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║          SET BALANCE ALERT THRESHOLD           ║");
        System.out.println("╚════════════════════════════════════════════════╝");

        String accountNumber = getStringInput("Enter account number: ");
        double threshold = getDoubleInput("Enter new threshold amount: $");

        try {
            alertTracker.setAccountThreshold(accountNumber, threshold);
            System.out.printf("✅ Balance threshold for account %s set to $%.2f%n", accountNumber, threshold);
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }

    private void displayAllAccounts() {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║                 ALL ACCOUNTS                   ║");
        System.out.println("╚════════════════════════════════════════════════╝");

        var accounts = accountEngine.getAllAccounts();

        if (accounts.isEmpty()) {
            System.out.println("📭 No accounts found.");
        } else {
            System.out.println("┌─────────────────────────────────────────────────────────────────────────────┐");
            System.out.println("│                                ACCOUNTS                                     │");
            System.out.println("├─────────────┬───────────────────┬──────────────┬────────────────────────────┤");
            System.out.printf ("│ %-11s │ %-17s │ %-12s │ %-26s │%n",
                    "Account", "Holder", "Balance", "Email");
            System.out.println("├─────────────┼───────────────────┼──────────────┼────────────────────────────┤");

            for (var account : accounts) {
                System.out.printf("│ %-11s │ %-17s │ $%-11.2f │ %-26s │%n",
                        account.getAccountNumber(),
                        account.getAccountHolderName(),
                        account.getBalance(),
                        account.getEmail());
            }

            System.out.println("└─────────────┴───────────────────┴──────────────┴────────────────────────────┘");
            System.out.println("📊 Total accounts: " + accounts.size());
            System.out.printf("💰 Total balance: $%.2f%n", accountEngine.getTotalBalance());
            System.out.printf("📈 Average balance: $%.2f%n", accountEngine.getAverageBalance());
        }
    }

    private void manageAccount() {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║                MANAGE ACCOUNT                  ║");
        System.out.println("╚════════════════════════════════════════════════╝");
        System.out.println("1. Update Account Holder Name");
        System.out.println("2. Update Email Address");
        System.out.println("3. View Account Details");
        System.out.println("4. Search Accounts by Name");

        int choice = getIntInput("Choose option: ");
        String accountNumber = getStringInput("Enter account number: ");

        try {
            switch (choice) {
                case 1 -> {
                    String newName = getStringInput("Enter new account holder name: ");
                    accountEngine.updateAccountHolderName(accountNumber, newName);
                    System.out.println("✅ Account holder name updated successfully!");
                }
                case 2 -> {
                    String newEmail = getStringInput("Enter new email address: ");
                    accountEngine.updateAccountEmail(accountNumber, newEmail);
                    System.out.println("✅ Email address updated successfully!");
                }
                case 3 -> {
                    var account = accountEngine.getAccount(accountNumber);
                    System.out.println(account.toDetailedString());
                }
                case 4 -> {
                    String searchTerm = getStringInput("Enter name to search: ");
                    var results = accountEngine.searchAccountsByName(searchTerm);
                    if (results.isEmpty()) {
                        System.out.println("❌ No accounts found matching: " + searchTerm);
                    } else {
                        System.out.println("🔍 Search Results:");
                        for (var account : results) {
                            System.out.printf(" - %s: %s ($%.2f)%n",
                                    account.getAccountNumber(),
                                    account.getAccountHolderName(),
                                    account.getBalance());
                        }
                    }
                }
                default -> System.out.println("❌ Invalid choice.");
            }
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }

    private void systemAdministration() {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║             SYSTEM ADMINISTRATION              ║");
        System.out.println("╚════════════════════════════════════════════════╝");
        System.out.println("1. Toggle Email Service");
        System.out.println("2. View System Statistics");
        System.out.println("3. Sync All Accounts to Database");
        System.out.println("4. Check All Account Balances");
        System.out.println("5. Reset Alert Notifications");

        int choice = getIntInput("Choose option: ");

        switch (choice) {
            case 1 -> {
                boolean currentStatus = emailService.isEnabled();
                emailService.setEnabled(!currentStatus);
                System.out.println("✅ Email service is now " + (emailService.isEnabled() ? "enabled" : "disabled"));
            }
            case 2 -> {
                displaySystemStatistics();
            }
            case 3 -> {
                accountEngine.syncAllAccountsToDatabase();
                System.out.println("✅ All accounts synced to database.");
            }
            case 4 -> {
                alertTracker.checkAllAccountBalances();
                System.out.println("✅ Balance check completed for all accounts.");
            }
            case 5 -> {
                alertTracker.resetNotifications();
                System.out.println("✅ Alert notifications reset.");
            }
            default -> System.out.println("❌ Invalid choice.");
        }
    }

    private void displaySystemStatistics() {
        System.out.println("\n📊 SYSTEM STATISTICS:");
        System.out.println("───────────────────────────────────────────────────");

        // Account Statistics
        System.out.println("ACCOUNTS:");
        System.out.printf("  Total: %d%n", accountEngine.getTotalAccounts());
        System.out.printf("  Total Balance: $%.2f%n", accountEngine.getTotalBalance());
        System.out.printf("  Average Balance: $%.2f%n", accountEngine.getAverageBalance());

        // Transaction Statistics
        var stats = transactionSystem.getTransactionStatistics();
        System.out.println("TRANSACTIONS:");
        System.out.printf("  Total: %d%n", stats.getOrDefault("TOTAL", 0));
        System.out.printf("  Deposits: %d%n", stats.getOrDefault("DEPOSITS", 0));
        System.out.printf("  Withdrawals: %d%n", stats.getOrDefault("WITHDRAWALS", 0));
        System.out.printf("  Transfers: %d%n", stats.getOrDefault("TRANSFERS", 0));
        System.out.printf("  Successful: %d%n", stats.getOrDefault("SUCCESSFUL", 0));
        System.out.printf("  Failed: %d%n", stats.getOrDefault("FAILED", 0));
        System.out.printf("  Total Volume: $%.2f%n", transactionSystem.getTotalTransactionVolume());

        // System Status
        System.out.println("SYSTEM STATUS:");
        System.out.println("  Database: " + (databaseManager.isConnected() ? "✅ Connected" : "❌ Disconnected"));
        System.out.println("  Email Service: " + (emailService.isEnabled() ? "✅ Enabled" : "⚠️ Disabled"));
        System.out.println("  Balance Monitoring: ✅ Active");
    }

    private void shutdown() {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║                  SHUTTING DOWN                  ║");
        System.out.println("╚════════════════════════════════════════════════╝");

        System.out.println("🔄 Saving data and shutting down services...");

        // Stop monitoring
        alertTracker.stopMonitoring();

        // Sync all accounts to database
        accountEngine.syncAllAccountsToDatabase();

        // Close database connection
        databaseManager.closeConnection();

        running = false;
        System.out.println("✅ Banking Simulator shut down successfully!");
        System.out.println("👋 Thank you for using our Banking System!");
    }

    // Utility methods for input handling
    private String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                int value = Integer.parseInt(scanner.nextLine().trim());
                return value;
            } catch (NumberFormatException e) {
                System.out.println("❌ Invalid input. Please enter a valid number.");
            }
        }
    }

    private double getDoubleInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                double value = Double.parseDouble(scanner.nextLine().trim());
                if (value < 0) {
                    System.out.println("❌ Amount cannot be negative. Please try again.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("❌ Invalid input. Please enter a valid amount.");
            }
        }
    }

    public static void main(String[] args) {
        BankingSimulator simulator = new BankingSimulator();

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n⚠️  Shutdown signal received. Cleaning up...");
            simulator.alertTracker.stopMonitoring();
            simulator.databaseManager.closeConnection();
        }));

        try {
            simulator.start();
        } catch (Exception e) {
            System.err.println("💥 Critical error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}