package com.banking.model;

import java.time.LocalDateTime;

public class Transaction {
    private String transactionId;
    private String accountNumber;
    private TransactionType transactionType;
    private double amount;
    private String targetAccount;
    private LocalDateTime transactionDate;
    private TransactionStatus status;
    private String description;

    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER
    }

    public enum TransactionStatus {
        SUCCESS, FAILED
    }

    public Transaction(String transactionId, String accountNumber, TransactionType transactionType,
                       double amount, String targetAccount, TransactionStatus status, String description) {
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.transactionType = transactionType;
        this.amount = amount;
        this.targetAccount = targetAccount;
        this.transactionDate = LocalDateTime.now();
        this.status = status;
        this.description = description;
    }

    // Constructor for database operations
    public Transaction(String transactionId, String accountNumber, TransactionType transactionType,
                       double amount, String targetAccount, LocalDateTime transactionDate,
                       TransactionStatus status, String description) {
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.transactionType = transactionType;
        this.amount = amount;
        this.targetAccount = targetAccount;
        this.transactionDate = transactionDate;
        this.status = status;
        this.description = description;
    }

    // Getters
    public String getTransactionId() { return transactionId; }
    public String getAccountNumber() { return accountNumber; }
    public TransactionType getTransactionType() { return transactionType; }
    public double getAmount() { return amount; }
    public String getTargetAccount() { return targetAccount; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public TransactionStatus getStatus() { return status; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return String.format("Transaction{id='%s', type=%s, amount=%.2f, account='%s', status=%s, date=%s}",
                transactionId, transactionType, amount, accountNumber, status, transactionDate);
    }

    public String toFormattedString() {
        return String.format(
                "%-12s %-15s %-10s $%-9.2f %-15s %-20s",
                transactionId,
                transactionType,
                status,
                amount,
                targetAccount != null ? targetAccount : "N/A",
                transactionDate != null ? transactionDate.toString() : "N/A"
        );
    }
}