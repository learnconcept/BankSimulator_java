package com.banking.exceptions;

public class InsufficientFundsException extends BankingException {
    private double currentBalance;
    private double requestedAmount;

    public InsufficientFundsException(double currentBalance, double requestedAmount) {
        super(String.format("Insufficient funds. Current balance: $%.2f, Requested: $%.2f, Shortfall: $%.2f",
                currentBalance, requestedAmount, requestedAmount - currentBalance));
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
    }

    public double getCurrentBalance() { return currentBalance; }
    public double getRequestedAmount() { return requestedAmount; }
    public double getShortfall() { return requestedAmount - currentBalance; }
}