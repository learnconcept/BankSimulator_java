package com.banking.engine;

import com.banking.model.Account;
import com.banking.database.DatabaseManager;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AccountManagementEngine {
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
        // Try to load accounts from database first
        List<Account> dbAccounts = databaseManager.loadAllAccountsFromDB();
        if (!dbAccounts.isEmpty()) {
            for (Account account : dbAccounts) {
                accounts.put(account.getAccountNumber(), account);
            }
            System.out.println("✅ Loaded " + dbAccounts.size() + " accounts from database.");
        } else {
            // Create sample accounts if database is empty
            createSampleAccounts();
        }
    }

    private void createSampleAccounts() {
        createAccount("ACC001", "John Doe", 1000.0, "john.doe@email.com");
        createAccount("ACC002", "Jane Smith", 2500.0, "jane.smith@email.com");
        createAccount("ACC003", "Bob Johnson", 500.0, "bob.johnson@email.com");
        createAccount("ACC004", "Alice Brown", 7500.0, "alice.brown@email.com");
        createAccount("ACC005", "Charlie Wilson", 300.0, "charlie.wilson@email.com");
        System.out.println("✅ Created " + accounts.size() + " sample accounts.");
    }

    public Account createAccount(String accountNumber, String accountHolderName,
                                 double initialBalance, String email) {
        if (accounts.containsKey(accountNumber)) {
            throw new IllegalArgumentException("❌ Account number already exists: " + accountNumber);
        }

        if (initialBalance < 0) {
            throw new IllegalArgumentException("❌ Initial balance cannot be negative");
        }

        if (accountHolderName == null || accountHolderName.trim().isEmpty()) {
            throw new IllegalArgumentException("❌ Account holder name cannot be empty");
        }

        Account account = new Account(accountNumber, accountHolderName, initialBalance, email);
        accounts.put(accountNumber, account);

        // Sync to database
        databaseManager.syncAccountToDatabase(account);

        System.out.println("✅ Account created: " + account);
        return account;
    }

    public Account getAccount(String accountNumber) {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("❌ Account not found: " + accountNumber);
        }
        return account;
    }

    public List<Account> getAllAccounts() {
        return new ArrayList<>(accounts.values());
    }

    public boolean accountExists(String accountNumber) {
        return accounts.containsKey(accountNumber);
    }

    public double getAccountBalance(String accountNumber) {
        Account account = getAccount(accountNumber);
        return account.getBalance();
    }

    public void updateAccountBalance(String accountNumber, double newBalance) {
        Account account = getAccount(accountNumber);
        account.setBalance(newBalance);

        // Sync to database
        databaseManager.syncAccountToDatabase(account);
    }

    public void updateAccountEmail(String accountNumber, String newEmail) {
        Account account = getAccount(accountNumber);
        account.setEmail(newEmail);

        // Sync to database
        databaseManager.syncAccountToDatabase(account);
    }

    public void updateAccountHolderName(String accountNumber, String newName) {
        Account account = getAccount(accountNumber);
        account.setAccountHolderName(newName);

        // Sync to database
        databaseManager.syncAccountToDatabase(account);
    }

    public boolean deleteAccount(String accountNumber) {
        if (!accounts.containsKey(accountNumber)) {
            return false;
        }

        // In a real system, we might want to archive instead of delete
        accounts.remove(accountNumber);
        System.out.println("✅ Account deleted: " + accountNumber);
        return true;
    }

    public int getTotalAccounts() {
        return accounts.size();
    }

    public double getTotalBalance() {
        return accounts.values().stream()
                .mapToDouble(Account::getBalance)
                .sum();
    }

    public double getAverageBalance() {
        if (accounts.isEmpty()) return 0.0;
        return getTotalBalance() / accounts.size();
    }

    public List<Account> getAccountsWithLowBalance(double threshold) {
        List<Account> lowBalanceAccounts = new ArrayList<>();
        for (Account account : accounts.values()) {
            if (account.getBalance() < threshold) {
                lowBalanceAccounts.add(account);
            }
        }
        return lowBalanceAccounts;
    }

    public List<Account> searchAccountsByName(String namePattern) {
        List<Account> matchingAccounts = new ArrayList<>();
        String pattern = namePattern.toLowerCase();

        for (Account account : accounts.values()) {
            if (account.getAccountHolderName().toLowerCase().contains(pattern)) {
                matchingAccounts.add(account);
            }
        }
        return matchingAccounts;
    }

    public void syncAllAccountsToDatabase() {
        for (Account account : accounts.values()) {
            databaseManager.syncAccountToDatabase(account);
        }
        System.out.println("✅ Synced all accounts to database.");
    }
}