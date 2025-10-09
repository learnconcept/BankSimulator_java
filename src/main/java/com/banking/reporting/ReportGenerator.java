package com.banking.reporting;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.engine.AccountManagementEngine;
import com.banking.engine.TransactionProcessingSystem;
import com.banking.config.BankingConfig;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ReportGenerator {
    private AccountManagementEngine accountEngine;
    private TransactionProcessingSystem transactionSystem;
    private BankingConfig config;
    private String reportDirectory;

    public ReportGenerator() {
        this.accountEngine = AccountManagementEngine.getInstance();
        this.transactionSystem = TransactionProcessingSystem.getInstance();
        this.config = new BankingConfig();
        this.reportDirectory = config.getProperty("report.directory", "reports/");
        createReportDirectory();
    }

    private void createReportDirectory() {
        File directory = new File(reportDirectory);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                System.out.println("✅ Report directory created: " + reportDirectory);
            } else {
                System.err.println("❌ Failed to create report directory: " + reportDirectory);
            }
        }
    }

    public void generateAccountSummaryReport() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = reportDirectory + "account_summary_" + timestamp + ".txt";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("═══════════════════════════════════════════════════");
            writer.println("           BANK ACCOUNT SUMMARY REPORT            ");
            writer.println("═══════════════════════════════════════════════════");
            writer.println("Generated at: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println("═══════════════════════════════════════════════════");
            writer.println();

            List<Account> accounts = accountEngine.getAllAccounts();
            writer.println("Total Accounts: " + accounts.size());
            writer.println();

            double totalBalance = 0;
            int lowBalanceCount = 0;
            double lowBalanceThreshold = config.getDoubleProperty("alert.low_balance_threshold", 100.0);

            writer.println("┌─────────────────────────────────────────────────────────────────────────────┐");
            writer.println("│                                ACCOUNTS                                     │");
            writer.println("├─────────────┬───────────────────┬──────────────┬────────────────────────────┤");
            writer.printf ("│ %-11s │ %-17s │ %-12s │ %-26s │%n",
                    "Account", "Holder", "Balance", "Email");
            writer.println("├─────────────┼───────────────────┼──────────────┼────────────────────────────┤");

            for (Account account : accounts) {
                String balanceStr = String.format("$%.2f", account.getBalance());
                if (account.getBalance() < lowBalanceThreshold) {
                    balanceStr = "🚨 " + balanceStr;
                    lowBalanceCount++;
                }

                writer.printf("│ %-11s │ %-17s │ %-12s │ %-26s │%n",
                        account.getAccountNumber(),
                        account.getAccountHolderName(),
                        balanceStr,
                        account.getEmail());

                totalBalance += account.getBalance();
            }

            writer.println("└─────────────┴───────────────────┴──────────────┴────────────────────────────┘");
            writer.println();

            // Summary Statistics
            writer.println("SUMMARY STATISTICS:");
            writer.println("───────────────────────────────────────────────────");
            writer.printf("Total Accounts: %d%n", accounts.size());
            writer.printf("Total Balance: $%.2f%n", totalBalance);
            writer.printf("Average Balance: $%.2f%n", totalBalance / accounts.size());
            writer.printf("Accounts with Low Balance (<$%.2f): %d%n", lowBalanceThreshold, lowBalanceCount);
            writer.printf("Highest Balance: $%.2f%n",
                    accounts.stream().mapToDouble(Account::getBalance).max().orElse(0));
            writer.printf("Lowest Balance: $%.2f%n",
                    accounts.stream().mapToDouble(Account::getBalance).min().orElse(0));

            System.out.println("✅ Account summary report generated: " + filename);
        } catch (IOException e) {
            System.err.println("❌ Failed to generate account summary report: " + e.getMessage());
        }
    }

    public void generateTransactionReport(String accountNumber) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = reportDirectory + "transaction_report_" + accountNumber + "_" + timestamp + ".txt";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            Account account = accountEngine.getAccount(accountNumber);
            List<Transaction> transactions = transactionSystem.getTransactionHistory(accountNumber);

            writer.println("═══════════════════════════════════════════════════");
            writer.println("             TRANSACTION REPORT                   ");
            writer.println("═══════════════════════════════════════════════════");
            writer.println("Account: " + accountNumber);
            writer.println("Holder: " + account.getAccountHolderName());
            writer.println("Current Balance: $" + String.format("%.2f", account.getBalance()));
            writer.println("Generated at: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println("═══════════════════════════════════════════════════");
            writer.println();

            if (transactions.isEmpty()) {
                writer.println("No transactions found for this account.");
            } else {
                writer.println("Recent Transactions (Last " + transactions.size() + "):");
                writer.println();
                writer.println("┌─────────────────────────────────────────────────────────────────────────────────────────────────────┐");
                writer.println("│                                    TRANSACTION HISTORY                                              │");
                writer.println("├──────────────┬─────────────┬──────────┬────────────┬──────────────┬────────────────┬─────────────────┤");
                writer.printf ("│ %-12s │ %-11s │ %-8s │ %-10s │ %-12s │ %-14s │ %-15s │%n",
                        "Date", "Time", "Type", "Status", "Amount", "Target Account", "Transaction ID");
                writer.println("├──────────────┼─────────────┼──────────┼────────────┼──────────────┼────────────────┼─────────────────┤");

                for (Transaction transaction : transactions) {
                    String date = transaction.getTransactionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    String time = transaction.getTransactionDate().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    String amount = String.format("$%.2f", transaction.getAmount());
                    String status = transaction.getStatus() == Transaction.TransactionStatus.SUCCESS ? "✅ SUCCESS" : "❌ FAILED";

                    writer.printf("│ %-12s │ %-11s │ %-8s │ %-10s │ %-12s │ %-14s │ %-15s │%n",
                            date, time, transaction.getTransactionType(), status, amount,
                            transaction.getTargetAccount() != null ? transaction.getTargetAccount() : "N/A",
                            transaction.getTransactionId());
                }

                writer.println("└──────────────┴─────────────┴──────────┴────────────┴──────────────┴────────────────┴─────────────────┘");
                writer.println();

                // Transaction Summary
                Map<String, Integer> stats = transactionSystem.getTransactionStatistics();
                long successfulTransactions = transactions.stream()
                        .filter(t -> t.getStatus() == Transaction.TransactionStatus.SUCCESS)
                        .count();
                long failedTransactions = transactions.stream()
                        .filter(t -> t.getStatus() == Transaction.TransactionStatus.FAILED)
                        .count();

                double totalDeposits = transactions.stream()
                        .filter(t -> t.getTransactionType() == Transaction.TransactionType.DEPOSIT &&
                                t.getStatus() == Transaction.TransactionStatus.SUCCESS)
                        .mapToDouble(Transaction::getAmount)
                        .sum();

                double totalWithdrawals = transactions.stream()
                        .filter(t -> t.getTransactionType() == Transaction.TransactionType.WITHDRAWAL &&
                                t.getStatus() == Transaction.TransactionStatus.SUCCESS)
                        .mapToDouble(Transaction::getAmount)
                        .sum();

                double totalTransfersOut = transactions.stream()
                        .filter(t -> t.getTransactionType() == Transaction.TransactionType.TRANSFER &&
                                t.getAccountNumber().equals(accountNumber) &&
                                t.getStatus() == Transaction.TransactionStatus.SUCCESS)
                        .mapToDouble(Transaction::getAmount)
                        .sum();

                double totalTransfersIn = transactions.stream()
                        .filter(t -> t.getTransactionType() == Transaction.TransactionType.TRANSFER &&
                                t.getTargetAccount() != null &&
                                t.getTargetAccount().equals(accountNumber) &&
                                t.getStatus() == Transaction.TransactionStatus.SUCCESS)
                        .mapToDouble(Transaction::getAmount)
                        .sum();

                writer.println("TRANSACTION SUMMARY:");
                writer.println("───────────────────────────────────────────────────");
                writer.println("Total Transactions: " + transactions.size());
                writer.println("Successful: " + successfulTransactions);
                writer.println("Failed: " + failedTransactions);
                writer.printf("Success Rate: %.1f%%%n", (successfulTransactions * 100.0 / transactions.size()));
                writer.println();
                writer.printf("Total Deposits: $%.2f%n", totalDeposits);
                writer.printf("Total Withdrawals: $%.2f%n", totalWithdrawals);
                writer.printf("Total Transfers Out: $%.2f%n", totalTransfersOut);
                writer.printf("Total Transfers In: $%.2f%n", totalTransfersIn);
                writer.printf("Net Flow: $%.2f%n", (totalDeposits + totalTransfersIn - totalWithdrawals - totalTransfersOut));
            }

            System.out.println("✅ Transaction report generated: " + filename);
        } catch (IOException e) {
            System.err.println("❌ Failed to generate transaction report: " + e.getMessage());
        }
    }

    public void generateDailyTransactionReport() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filename = reportDirectory + "daily_transaction_report_" + timestamp + ".txt";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            List<Transaction> allTransactions = transactionSystem.getAllTransactions();
            LocalDateTime today = LocalDateTime.now();

            List<Transaction> todayTransactions = allTransactions.stream()
                    .filter(t -> t.getTransactionDate().toLocalDate().equals(today.toLocalDate()))
                    .toList();

            writer.println("═══════════════════════════════════════════════════");
            writer.println("           DAILY TRANSACTION REPORT               ");
            writer.println("═══════════════════════════════════════════════════");
            writer.println("Date: " + today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            writer.println("Generated at: " + today.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            writer.println("═══════════════════════════════════════════════════");
            writer.println();

            if (todayTransactions.isEmpty()) {
                writer.println("No transactions today.");
            } else {
                double totalDeposits = todayTransactions.stream()
                        .filter(t -> t.getTransactionType() == Transaction.TransactionType.DEPOSIT &&
                                t.getStatus() == Transaction.TransactionStatus.SUCCESS)
                        .mapToDouble(Transaction::getAmount)
                        .sum();

                double totalWithdrawals = todayTransactions.stream()
                        .filter(t -> t.getTransactionType() == Transaction.TransactionType.WITHDRAWAL &&
                                t.getStatus() == Transaction.TransactionStatus.SUCCESS)
                        .mapToDouble(Transaction::getAmount)
                        .sum();

                double totalTransfers = todayTransactions.stream()
                        .filter(t -> t.getTransactionType() == Transaction.TransactionType.TRANSFER &&
                                t.getStatus() == Transaction.TransactionStatus.SUCCESS)
                        .mapToDouble(Transaction::getAmount)
                        .sum();

                writer.println("DAILY SUMMARY:");
                writer.println("───────────────────────────────────────────────────");
                writer.printf("Total Transactions: %d%n", todayTransactions.size());
                writer.printf("Total Deposits: $%.2f%n", totalDeposits);
                writer.printf("Total Withdrawals: $%.2f%n", totalWithdrawals);
                writer.printf("Total Transfers: $%.2f%n", totalTransfers);
                writer.printf("Net Movement: $%.2f%n", (totalDeposits - totalWithdrawals - totalTransfers));
                writer.println();

                writer.println("DETAILED TRANSACTIONS:");
                writer.println();
                writer.println("┌─────────────────────────────────────────────────────────────────────────────────────────────────────┐");
                writer.println("│                                  TODAY'S TRANSACTIONS                                               │");
                writer.println("├──────────────┬─────────────┬──────────────┬──────────┬────────────┬────────────────┬─────────────────┤");
                writer.printf ("│ %-12s │ %-11s │ %-12s │ %-8s │ %-10s │ %-14s │ %-15s │%n",
                        "Time", "Account", "Type", "Amount", "Status", "Target Account", "Transaction ID");
                writer.println("├──────────────┼─────────────┼──────────────┼──────────┼────────────┼────────────────┼─────────────────┤");

                for (Transaction transaction : todayTransactions) {
                    String time = transaction.getTransactionDate().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    String amount = String.format("$%.2f", transaction.getAmount());
                    String status = transaction.getStatus() == Transaction.TransactionStatus.SUCCESS ? "✅" : "❌";

                    writer.printf("│ %-12s │ %-11s │ %-12s │ %-8s │ %-10s │ %-14s │ %-15s │%n",
                            time,
                            transaction.getAccountNumber(),
                            transaction.getTransactionType(),
                            amount,
                            status,
                            transaction.getTargetAccount() != null ? transaction.getTargetAccount() : "N/A",
                            transaction.getTransactionId());
                }

                writer.println("└──────────────┴─────────────┴──────────────┴──────────┴────────────┴────────────────┴─────────────────┘");
            }

            System.out.println("✅ Daily transaction report generated: " + filename);
        } catch (IOException e) {
            System.err.println("❌ Failed to generate daily transaction report: " + e.getMessage());
        }
    }

    public void generateSystemHealthReport() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = reportDirectory + "system_health_report_" + timestamp + ".txt";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("═══════════════════════════════════════════════════");
            writer.println("             SYSTEM HEALTH REPORT                 ");
            writer.println("═══════════════════════════════════════════════════");
            writer.println("Generated at: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println("═══════════════════════════════════════════════════");
            writer.println();

            // Database Status
            writer.println("DATABASE STATUS:");
            writer.println("───────────────────────────────────────────────────");
            writer.println("Connection: " + (com.banking.database.DatabaseManager.getInstance().isConnected() ? "✅ Connected" : "❌ Disconnected"));
            writer.println();

            // Account Statistics
            List<Account> accounts = accountEngine.getAllAccounts();
            Map<String, Integer> transactionStats = transactionSystem.getTransactionStatistics();

            writer.println("ACCOUNT STATISTICS:");
            writer.println("───────────────────────────────────────────────────");
            writer.println("Total Accounts: " + accounts.size());
            writer.printf("Total Balance: $%.2f%n", accountEngine.getTotalBalance());
            writer.printf("Average Balance: $%.2f%n", accountEngine.getAverageBalance());
            writer.println("Accounts with Low Balance: " + accountEngine.getAccountsWithLowBalance(100.0).size());
            writer.println();

            // Transaction Statistics
            writer.println("TRANSACTION STATISTICS:");
            writer.println("───────────────────────────────────────────────────");
            writer.println("Total Transactions: " + transactionStats.getOrDefault("TOTAL", 0));
            writer.println("Successful: " + transactionStats.getOrDefault("SUCCESSFUL", 0));
            writer.println("Failed: " + transactionStats.getOrDefault("FAILED", 0));
            writer.printf("Total Volume: $%.2f%n", transactionSystem.getTotalTransactionVolume());
            writer.println();

            // System Information
            writer.println("SYSTEM INFORMATION:");
            writer.println("───────────────────────────────────────────────────");
            writer.println("Java Version: " + System.getProperty("java.version"));
            writer.println("OS: " + System.getProperty("os.name"));
            writer.println("Available Memory: " + Runtime.getRuntime().freeMemory() / (1024 * 1024) + " MB");
            writer.println("Total Memory: " + Runtime.getRuntime().totalMemory() / (1024 * 1024) + " MB");

            System.out.println("✅ System health report generated: " + filename);
        } catch (IOException e) {
            System.err.println("❌ Failed to generate system health report: " + e.getMessage());
        }
    }

    public void exportToCSV(String filename) {
        if (!filename.toLowerCase().endsWith(".csv")) {
            filename += ".csv";
        }

        String fullPath = reportDirectory + filename;

        try (PrintWriter writer = new PrintWriter(new FileWriter(fullPath))) {
            List<Account> accounts = accountEngine.getAllAccounts();

            // Write header
            writer.println("AccountNumber,AccountHolder,Balance,Email,CreatedDate,LastUpdated");

            // Write data
            for (Account account : accounts) {
                writer.printf("\"%s\",\"%s\",%.2f,\"%s\",\"%s\",\"%s\"%n",
                        account.getAccountNumber(),
                        account.getAccountHolderName(),
                        account.getBalance(),
                        account.getEmail(),
                        account.getCreatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        account.getLastUpdated().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }

            System.out.println("✅ Accounts exported to CSV: " + fullPath);
        } catch (IOException e) {
            System.err.println("❌ Failed to export to CSV: " + e.getMessage());
        }
    }

    public void exportTransactionsToCSV(String accountNumber, String filename) {
        if (!filename.toLowerCase().endsWith(".csv")) {
            filename += ".csv";
        }

        String fullPath = reportDirectory + filename;

        try (PrintWriter writer = new PrintWriter(new FileWriter(fullPath))) {
            List<Transaction> transactions = transactionSystem.getTransactionHistory(accountNumber);

            // Write header
            writer.println("TransactionID,AccountNumber,TransactionType,Amount,TargetAccount,TransactionDate,Status,Description");

            // Write data
            for (Transaction transaction : transactions) {
                writer.printf("\"%s\",\"%s\",\"%s\",%.2f,\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        transaction.getTransactionId(),
                        transaction.getAccountNumber(),
                        transaction.getTransactionType(),
                        transaction.getAmount(),
                        transaction.getTargetAccount() != null ? transaction.getTargetAccount() : "",
                        transaction.getTransactionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        transaction.getStatus(),
                        transaction.getDescription().replace("\"", "\"\""));
            }

            System.out.println("✅ Transactions exported to CSV: " + fullPath);
        } catch (IOException e) {
            System.err.println("❌ Failed to export transactions to CSV: " + e.getMessage());
        }
    }
}