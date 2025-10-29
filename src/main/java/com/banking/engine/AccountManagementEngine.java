package com.banking.engine;

import com.banking.model.Account;
import com.banking.database.DatabaseManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AccountManagementEngine {
    private static final Logger logger = LogManager.getLogger(AccountManagementEngine.class);

    private Map<String, Account> accounts;
    private DatabaseManager databaseManager;
    private static AccountManagementEngine instance;

    private AccountManagementEngine() {
        this.accounts = new ConcurrentHashMap<>();
        this.databaseManager = DatabaseManager.getInstance();
        initializeAccounts();
    }

    public static AccountManagementEngine getInstance() {
        if (instance == null) {
            instance = new AccountManagementEngine();
        }
        return instance;
    }

    private void initializeAccounts() {
        logger.info("Initializing Account Management Engine...");

        // Try to load accounts from database first
        List<Account> dbAccounts = databaseManager.loadAllAccountsFromDB();
        if (!dbAccounts.isEmpty()) {
            for (Account account : dbAccounts) {
                accounts.put(account.getAccountNumber(), account);
            }
            logger.info("Loaded {} accounts from database.", dbAccounts.size());
            logger.debug("Loaded accounts: {}", dbAccounts);
        } else {
            // Create sample accounts if database is empty
            logger.warn("No accounts found in database. Creating sample accounts.");
            createSampleAccounts();
        }

        logger.info("Account Management Engine initialized successfully with {} accounts.", accounts.size());
    }

    private void createSampleAccounts() {
        logger.debug("Creating sample accounts...");

        createAccount("ACC001", "John Doe", 1000.0, "john.doe@email.com");
        createAccount("ACC002", "Jane Smith", 2500.0, "jane.smith@email.com");
        createAccount("ACC003", "Bob Johnson", 500.0, "bob.johnson@email.com");
        createAccount("ACC004", "Alice Brown", 7500.0, "alice.brown@email.com");
        createAccount("ACC005", "Charlie Wilson", 300.0, "charlie.wilson@email.com");

        logger.info("Created {} sample accounts.", accounts.size());
    }

    public Account createAccount(String accountNumber, String accountHolderName,
                                 double initialBalance, String email) {
        logger.debug("Attempting to create account: number={}, holder={}, balance={}, email={}",
                accountNumber, accountHolderName, initialBalance, email);

        if (accounts.containsKey(accountNumber)) {
            logger.error("Account creation failed: Account number already exists - {}", accountNumber);
            throw new IllegalArgumentException("Account number already exists: " + accountNumber);
        }

        if (initialBalance < 0) {
            logger.error("Account creation failed: Initial balance cannot be negative - {}", initialBalance);
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }

        if (accountHolderName == null || accountHolderName.trim().isEmpty()) {
            logger.error("Account creation failed: Account holder name cannot be empty");
            throw new IllegalArgumentException("Account holder name cannot be empty");
        }

        Account account = new Account(accountNumber, accountHolderName, initialBalance, email);
        accounts.put(accountNumber, account);

        // Sync to database
        databaseManager.syncAccountToDatabase(account);

        logger.info("Account created successfully: {}", account);
        logger.debug("Account details: number={}, holder={}, balance={}, email={}",
                accountNumber, accountHolderName, initialBalance, email);

        return account;
    }

    public Account getAccount(String accountNumber) {
        logger.debug("Retrieving account: {}", accountNumber);

        Account account = accounts.get(accountNumber);
        if (account == null) {
            logger.warn("Account not found: {}", accountNumber);
            throw new IllegalArgumentException("Account not found: " + accountNumber);
        }

        logger.debug("Account retrieved successfully: {}", accountNumber);
        return account;
    }

    public List<Account> getAllAccounts() {
        logger.debug("Retrieving all accounts. Total accounts: {}", accounts.size());
        return new ArrayList<>(accounts.values());
    }

    public boolean accountExists(String accountNumber) {
        boolean exists = accounts.containsKey(accountNumber);
        logger.debug("Account existence check: {} -> {}", accountNumber, exists);
        return exists;
    }

    public double getAccountBalance(String accountNumber) {
        logger.debug("Retrieving balance for account: {}", accountNumber);

        Account account = getAccount(accountNumber);
        double balance = account.getBalance();

        logger.debug("Balance retrieved for account {}: {}", accountNumber, balance);
        return balance;
    }

    public void updateAccountBalance(String accountNumber, double newBalance) {
        logger.debug("Updating balance for account {}: new balance = {}", accountNumber, newBalance);

        Account account = getAccount(accountNumber);
        double oldBalance = account.getBalance();
        account.setBalance(newBalance);

        // Sync to database
        databaseManager.syncAccountToDatabase(account);

        logger.info("Balance updated for account {}: {} -> {}", accountNumber, oldBalance, newBalance);
    }

    public void updateAccountEmail(String accountNumber, String newEmail) {
        logger.debug("Updating email for account {}: new email = {}", accountNumber, newEmail);

        Account account = getAccount(accountNumber);
        String oldEmail = account.getEmail();
        account.setEmail(newEmail);

        // Sync to database
        databaseManager.syncAccountToDatabase(account);

        logger.info("Email updated for account {}: {} -> {}", accountNumber, oldEmail, newEmail);
    }

    public void updateAccountHolderName(String accountNumber, String newName) {
        logger.debug("Updating account holder name for account {}: new name = {}", accountNumber, newName);

        Account account = getAccount(accountNumber);
        String oldName = account.getAccountHolderName();
        account.setAccountHolderName(newName);

        // Sync to database
        databaseManager.syncAccountToDatabase(account);

        logger.info("Account holder name updated for account {}: {} -> {}", accountNumber, oldName, newName);
    }

    public boolean deleteAccount(String accountNumber) {
        logger.debug("Attempting to delete account: {}", accountNumber);

        if (!accounts.containsKey(accountNumber)) {
            logger.warn("Account deletion failed: Account not found - {}", accountNumber);
            return false;
        }

        // In a real system, we might want to archive instead of delete
        Account removedAccount = accounts.remove(accountNumber);
        logger.info("Account deleted successfully: {}", accountNumber);
        logger.debug("Deleted account details: {}", removedAccount);

        return true;
    }

    public int getTotalAccounts() {
        int total = accounts.size();
        logger.debug("Total accounts count: {}", total);
        return total;
    }

    public double getTotalBalance() {
        double totalBalance = accounts.values().stream()
                .mapToDouble(Account::getBalance)
                .sum();

        logger.debug("Total balance across all accounts: {}", totalBalance);
        return totalBalance;
    }

    public double getAverageBalance() {
        if (accounts.isEmpty()) {
            logger.debug("Average balance calculation: No accounts available, returning 0.0");
            return 0.0;
        }

        double averageBalance = getTotalBalance() / accounts.size();
        logger.debug("Average balance per account: {}", averageBalance);
        return averageBalance;
    }

    public List<Account> getAccountsWithLowBalance(double threshold) {
        logger.debug("Searching for accounts with balance below threshold: {}", threshold);

        List<Account> lowBalanceAccounts = new ArrayList<>();
        for (Account account : accounts.values()) {
            if (account.getBalance() < threshold) {
                lowBalanceAccounts.add(account);
            }
        }

        logger.info("Found {} accounts with balance below {}", lowBalanceAccounts.size(), threshold);
        logger.debug("Low balance accounts: {}", lowBalanceAccounts);

        return lowBalanceAccounts;
    }

    public List<Account> searchAccountsByName(String namePattern) {
        logger.debug("Searching accounts by name pattern: '{}'", namePattern);

        List<Account> matchingAccounts = new ArrayList<>();
        String pattern = namePattern.toLowerCase();

        for (Account account : accounts.values()) {
            if (account.getAccountHolderName().toLowerCase().contains(pattern)) {
                matchingAccounts.add(account);
            }
        }

        logger.info("Found {} accounts matching name pattern '{}'", matchingAccounts.size(), namePattern);
        logger.debug("Matching accounts: {}", matchingAccounts);

        return matchingAccounts;
    }

    public void syncAllAccountsToDatabase() {
        logger.info("Starting synchronization of all accounts to database...");

        int count = 0;
        for (Account account : accounts.values()) {
            databaseManager.syncAccountToDatabase(account);
            count++;
        }

        logger.info("Successfully synced {} accounts to database.", count);
    }

    // Method to get account statistics for logging
    public void logAccountStatistics() {
        if (accounts.isEmpty()) {
            logger.warn("No accounts available for statistics.");
            return;
        }

        double totalBalance = getTotalBalance();
        double averageBalance = getAverageBalance();
        double maxBalance = accounts.values().stream()
                .mapToDouble(Account::getBalance)
                .max()
                .orElse(0.0);
        double minBalance = accounts.values().stream()
                .mapToDouble(Account::getBalance)
                .min()
                .orElse(0.0);

        logger.info("Account Statistics:");
        logger.info("  Total Accounts: {}", accounts.size());
        logger.info("  Total Balance: ${:.2f}", totalBalance);
        logger.info("  Average Balance: ${:.2f}", averageBalance);
        logger.info("  Maximum Balance: ${:.2f}", maxBalance);
        logger.info("  Minimum Balance: ${:.2f}", minBalance);
    }
}