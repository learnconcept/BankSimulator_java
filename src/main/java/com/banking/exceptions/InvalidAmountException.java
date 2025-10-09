package com.banking.exceptions;

public class InvalidAmountException extends BankingException {
    public InvalidAmountException(String message) {
        super(message);
    }

    public InvalidAmountException(double amount) {
        super(String.format("Invalid amount: $%.2f. Amount must be positive.", amount));
    }
}