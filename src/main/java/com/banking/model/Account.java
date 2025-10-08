package com.banking.model;

import java.time.LocalDateTime;

public class Account {
    private String accountNumber;
    private String accountHolderName;
    private double balance;
    private String email;
    private LocalDateTime createdDate;
    private LocalDateTime lastUpdated;

    public Account(String accountNumber, String accountHolderName, double initialBalance, String email) {
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.balance = initialBalance;
        this.email = email;
        this.createdDate = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    // Full constructor for database operations
    public Account(String accountNumber, String accountHolderName, double balance,
                   String email, LocalDateTime createdDate, LocalDateTime lastUpdated) {
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.balance = balance;
        this.email = email;
        this.createdDate = createdDate;
        this.lastUpdated = lastUpdated;
    }

    // Getters and setters
    public String getAccountNumber() { return accountNumber; }
    public String getAccountHolderName() { return accountHolderName; }
    public double getBalance() { return balance; }
    public String getEmail() { return email; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }

    public void setBalance(double balance) {
        this.balance = balance;
        this.lastUpdated = LocalDateTime.now();
    }
    public void setEmail(String email) {
        this.email = email;
        this.lastUpdated = LocalDateTime.now();
    }
    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
        this.lastUpdated = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("Account{accountNumber='%s', holder='%s', balance=%.2f, email='%s'}",
                accountNumber, accountHolderName, balance, email);
    }

    public String toDetailedString() {
        return String.format(
                "Account Details:\n" +
                        "  Number: %s\n" +
                        "  Holder: %s\n" +
                        "  Balance: $%.2f\n" +
                        "  Email: %s\n" +
                        "  Created: %s\n" +
                        "  Last Updated: %s",
                accountNumber, accountHolderName, balance, email,
                createdDate, lastUpdated
        );
    }
}