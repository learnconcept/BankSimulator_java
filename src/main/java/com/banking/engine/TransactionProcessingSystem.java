package com.banking.engine;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.exceptions.*;
import com.banking.database.DatabaseManager;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class TransactionProcessingSystem {
    private AccountManagementEngine accountEngine;
    private List<Transaction> transactions;
    private DatabaseManager databaseManager;
    private static TransactionProcessingSystem instance;

    private TransactionProcessingSystem() {
        this.accountEngine = AccountManagementEngine.getInstance();
        this.transactions = new CopyOnWriteArrayList<>();
        this.databaseManager = DatabaseManager.getInstance();
        loadTransactionHistory();
    }

    public static TransactionProcessingSystem getInstance() {
        if (instance == null) {
            instance = new TransactionProcessingSystem();
        }
        return instance;
    }

    private void loadTransactionHistory() {
        // For simplicity, we'll load recent transactions for all accounts
        // In a real system, you might want more sophisticated loading
        System.out.println("✅ Transaction processing system initialized.");
    }

    public Transaction deposit(String accountNumber, double amount) throws BankingException {
        validateAmount(amount);

        if (!accountEngine.accountExists(accountNumber)) {
            throw new AccountNotFoundException(accountNumber);
        }

        Account account = accountEngine.getAccount(accountNumber);
        double newBalance = account.getBalance() + amount;
        accountEngine.updateAccountBalance(accountNumber, newBalance);

        String transactionId = generateTransactionId();
        Transaction transaction = new Transaction(
                transactionId, accountNumber, Transaction.TransactionType.DEPOSIT,
                amount, null, Transaction.TransactionStatus.SUCCESS,
                String.format("Deposit of $%.2f to account %s", amount, accountNumber)
        );

        transactions.add(transaction);
        databaseManager.logTransaction(transaction);

        System.out.printf("✅ Deposit successful: $%.2f to account %s. New balance: $%.2f%n",
                amount, accountNumber, newBalance);

        return transaction;
    }

    public Transaction withdraw(String accountNumber, double amount) throws BankingException {
        validateAmount(amount);

        if (!accountEngine.accountExists(accountNumber)) {
            throw new AccountNotFoundException(accountNumber);
        }

        Account account = accountEngine.getAccount(accountNumber);
        double currentBalance = account.getBalance();

        if (currentBalance < amount) {
            Transaction failedTransaction = new Transaction(
                    generateTransactionId(), accountNumber, Transaction.TransactionType.WITHDRAWAL,
                    amount, null, Transaction.TransactionStatus.FAILED,
                    String.format("Withdrawal failed: Insufficient funds. Current: $%.2f, Requested: $%.2f",
                            currentBalance, amount)
            );

            transactions.add(failedTransaction);
            databaseManager.logTransaction(failedTransaction);
            throw new InsufficientFundsException(currentBalance, amount);
        }

        double newBalance = currentBalance - amount;
        accountEngine.updateAccountBalance(accountNumber, newBalance);

        Transaction transaction = new Transaction(
                generateTransactionId(), accountNumber, Transaction.TransactionType.WITHDRAWAL,
                amount, null, Transaction.TransactionStatus.SUCCESS,
                String.format("Withdrawal of $%.2f from account %s", amount, accountNumber)
        );

        transactions.add(transaction);
        databaseManager.logTransaction(transaction);

        System.out.printf("✅ Withdrawal successful: $%.2f from account %s. New balance: $%.2f%n",
                amount, accountNumber, newBalance);

        return transaction;
    }

    public Transaction transfer(String fromAccount, String toAccount, double amount) throws BankingException {
        validateAmount(amount);

        if (!accountEngine.accountExists(fromAccount)) {
            throw new AccountNotFoundException(fromAccount);
        }

        if (!accountEngine.accountExists(toAccount)) {
            throw new AccountNotFoundException(toAccount);
        }

        if (fromAccount.equals(toAccount)) {
            throw new InvalidAmountException("Cannot transfer to the same account");
        }

        Account sourceAccount = accountEngine.getAccount(fromAccount);
        double sourceBalance = sourceAccount.getBalance();

        if (sourceBalance < amount) {
            Transaction failedTransaction = new Transaction(
                    generateTransactionId(), fromAccount, Transaction.TransactionType.TRANSFER,
                    amount, toAccount, Transaction.TransactionStatus.FAILED,
                    String.format("Transfer failed: Insufficient funds. Current: $%.2f, Requested: $%.2f",
                            sourceBalance, amount)
            );

            transactions.add(failedTransaction);
            databaseManager.logTransaction(failedTransaction);
            throw new InsufficientFundsException(sourceBalance, amount);
        }

        // Perform transfer atomically
        double newSourceBalance = sourceBalance - amount;
        accountEngine.updateAccountBalance(fromAccount, newSourceBalance);

        Account targetAccount = accountEngine.getAccount(toAccount);
        double newTargetBalance = targetAccount.getBalance() + amount;
        accountEngine.updateAccountBalance(toAccount, newTargetBalance);

        Transaction transaction = new Transaction(
                generateTransactionId(), fromAccount, Transaction.TransactionType.TRANSFER,
                amount, toAccount, Transaction.TransactionStatus.SUCCESS,
                String.format("Transfer of $%.2f from %s to %s", amount, fromAccount, toAccount)
        );

        transactions.add(transaction);
        databaseManager.logTransaction(transaction);

        System.out.printf("✅ Transfer successful: $%.2f from %s to %s. " +
                        "Source new balance: $%.2f, Target new balance: $%.2f%n",
                amount, fromAccount, toAccount, newSourceBalance, newTargetBalance);

        return transaction;
    }

    private void validateAmount(double amount) throws InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }

        // Additional validation: maximum transaction limit
        if (amount > 1000000) {
            throw new InvalidAmountException("Transaction amount exceeds maximum limit of $1,000,000");
        }
    }

    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public List<Transaction> getTransactionHistory(String accountNumber) {
        // First check in-memory transactions
        List<Transaction> accountTransactions = new ArrayList<>();
        for (Transaction transaction : transactions) {
            if (transaction.getAccountNumber().equals(accountNumber) ||
                    (transaction.getTargetAccount() != null && transaction.getTargetAccount().equals(accountNumber))) {
                accountTransactions.add(transaction);
            }
        }

        // If not enough transactions in memory, load from database
        if (accountTransactions.size() < 10) {
            List<Transaction> dbTransactions = databaseManager.getTransactionHistoryFromDB(accountNumber);
            for (Transaction dbTxn : dbTransactions) {
                boolean exists = accountTransactions.stream()
                        .anyMatch(t -> t.getTransactionId().equals(dbTxn.getTransactionId()));
                if (!exists) {
                    accountTransactions.add(dbTxn);
                }
            }
        }

        // Sort by date descending
        accountTransactions.sort((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()));

        return accountTransactions;
    }

    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }

    public int getTotalTransactions() {
        return transactions.size();
    }

    public double getTotalTransactionVolume() {
        return transactions.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.SUCCESS)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public Map<String, Integer> getTransactionStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("TOTAL", transactions.size());

        long deposits = transactions.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.DEPOSIT)
                .count();
        stats.put("DEPOSITS", (int) deposits);

        long withdrawals = transactions.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.WITHDRAWAL)
                .count();
        stats.put("WITHDRAWALS", (int) withdrawals);

        long transfers = transactions.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.TRANSFER)
                .count();
        stats.put("TRANSFERS", (int) transfers);

        long successful = transactions.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.SUCCESS)
                .count();
        stats.put("SUCCESSFUL", (int) successful);

        long failed = transactions.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.FAILED)
                .count();
        stats.put("FAILED", (int) failed);

        return stats;
    }

    public void clearTransactionHistory() {
        transactions.clear();
        System.out.println("✅ Transaction history cleared from memory.");
    }
}