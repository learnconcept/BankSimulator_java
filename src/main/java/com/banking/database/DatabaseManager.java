package com.banking.database;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.config.BankingConfig;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatabaseManager {
    private static final Logger logger = LogManager.getLogger(DatabaseManager.class);

    private static DatabaseManager instance;
    private Connection connection;
    private BankingConfig config;

    private DatabaseManager() {
        this.config = new BankingConfig();
        initializeDatabase();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeDatabase() {
        try {
            String url = config.getProperty("database.url");
            String username = config.getProperty("database.username");
            String password = config.getProperty("database.password");

            Properties properties = new Properties();
            properties.setProperty("user", username);
            properties.setProperty("password", password);
            properties.setProperty("useSSL", "false");
            properties.setProperty("serverTimezone", "UTC");
            properties.setProperty("allowPublicKeyRetrieval", "true");

            connection = DriverManager.getConnection(url, properties);
            logger.info("Database connection established successfully to: {}", url);

            // Test connection by creating tables if they don't exist
            createTablesIfNotExist();

        } catch (SQLException e) {
            System.err.println("❌ Failed to initialize database: " + e.getMessage());
            System.err.println("⚠️  Continuing with in-memory operations only.");
        }
    }

    private void createTablesIfNotExist() {
        if (connection == null) return;

        String[] createTablesSQL = {
                "CREATE TABLE IF NOT EXISTS accounts (" +
                        "    account_number VARCHAR(20) PRIMARY KEY," +
                        "    account_holder_name VARCHAR(100) NOT NULL," +
                        "    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00," +
                        "    email VARCHAR(100)," +
                        "    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                        ")",

                "CREATE TABLE IF NOT EXISTS transactions (" +
                        "    transaction_id INT AUTO_INCREMENT PRIMARY KEY," +
                        "    account_number VARCHAR(20) NOT NULL," +
                        "    transaction_type ENUM('DEPOSIT', 'WITHDRAWAL', 'TRANSFER') NOT NULL," +
                        "    amount DECIMAL(15,2) NOT NULL," +
                        "    target_account VARCHAR(20)," +
                        "    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "    status ENUM('SUCCESS', 'FAILED') NOT NULL," +
                        "    description TEXT," +
                        "    FOREIGN KEY (account_number) REFERENCES accounts(account_number)" +
                        ")",

                "CREATE TABLE IF NOT EXISTS balance_alerts (" +
                        "    alert_id INT AUTO_INCREMENT PRIMARY KEY," +
                        "    account_number VARCHAR(20) NOT NULL," +
                        "    alert_type ENUM('LOW_BALANCE', 'OVERDRAFT_ATTEMPT', 'THRESHOLD_BREACH') NOT NULL," +
                        "    message TEXT NOT NULL," +
                        "    alert_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "    FOREIGN KEY (account_number) REFERENCES accounts(account_number)" +
                        ")"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : createTablesSQL) {
                stmt.execute(sql);
            }
            logger.info("Database tables verified/created successfully.");
        } catch (SQLException e) {
            logger.error("Failed to create tables: {}", e.getMessage(), e);
        }
    }

    public void logTransaction(Transaction transaction) {
        if (connection == null) {
            logger.debug("Cannot log transaction: Database connection is null");
            return;
        }

        String sql = "INSERT INTO transactions (account_number, transaction_type, amount, " +
                "target_account, status, description) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, transaction.getAccountNumber());
            stmt.setString(2, transaction.getTransactionType().toString());
            stmt.setDouble(3, transaction.getAmount());
            stmt.setString(4, transaction.getTargetAccount());
            stmt.setString(5, transaction.getStatus().toString());
            stmt.setString(6, transaction.getDescription());

            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to log transaction: {}", e.getMessage(), e);
        }
    }

    public void syncAccountToDatabase(Account account) {
        if (connection == null) return;

        String checkSql = "SELECT COUNT(*) FROM accounts WHERE account_number = ?";
        String insertSql = "INSERT INTO accounts (account_number, account_holder_name, balance, email) VALUES (?, ?, ?, ?)";
        String updateSql = "UPDATE accounts SET balance = ?, email = ?, account_holder_name = ? WHERE account_number = ?";

        try {
            // Check if account exists
            PreparedStatement checkStmt = connection.prepareStatement(checkSql);
            checkStmt.setString(1, account.getAccountNumber());
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                // Update existing account
                PreparedStatement updateStmt = connection.prepareStatement(updateSql);
                updateStmt.setDouble(1, account.getBalance());
                updateStmt.setString(2, account.getEmail());
                updateStmt.setString(3, account.getAccountHolderName());
                updateStmt.setString(4, account.getAccountNumber());
                updateStmt.executeUpdate();
            } else {
                // Insert new account
                PreparedStatement insertStmt = connection.prepareStatement(insertSql);
                insertStmt.setString(1, account.getAccountNumber());
                insertStmt.setString(2, account.getAccountHolderName());
                insertStmt.setDouble(3, account.getBalance());
                insertStmt.setString(4, account.getEmail());
                insertStmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to sync account to database: " + e.getMessage());
        }
    }

    public List<Transaction> getTransactionHistoryFromDB(String accountNumber) {
        List<Transaction> transactions = new ArrayList<>();
        if (connection == null) return transactions;

        String sql = "SELECT * FROM transactions WHERE account_number = ? OR target_account = ? " +
                "ORDER BY transaction_date DESC LIMIT 100";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, accountNumber);
            stmt.setString(2, accountNumber);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Transaction transaction = new Transaction(
                        rs.getString("transaction_id"),
                        rs.getString("account_number"),
                        Transaction.TransactionType.valueOf(rs.getString("transaction_type")),
                        rs.getDouble("amount"),
                        rs.getString("target_account"),
                        rs.getTimestamp("transaction_date").toLocalDateTime(),
                        Transaction.TransactionStatus.valueOf(rs.getString("status")),
                        rs.getString("description")
                );
                transactions.add(transaction);
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch transaction history: " + e.getMessage());
        }

        return transactions;
    }

    public void logBalanceAlert(String accountNumber, String alertType, String message) {
        if (connection == null) return;

        String sql = "INSERT INTO balance_alerts (account_number, alert_type, message) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, accountNumber);
            stmt.setString(2, alertType);
            stmt.setString(3, message);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Failed to log balance alert: " + e.getMessage());
        }
    }

    public List<Account> loadAllAccountsFromDB() {
        List<Account> accounts = new ArrayList<>();
        if (connection == null) return accounts;

        String sql = "SELECT * FROM accounts";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Account account = new Account(
                        rs.getString("account_number"),
                        rs.getString("account_holder_name"),
                        rs.getDouble("balance"),
                        rs.getString("email"),
                        rs.getTimestamp("created_date").toLocalDateTime(),
                        rs.getTimestamp("last_updated").toLocalDateTime()
                );
                accounts.add(account);
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to load accounts from database: " + e.getMessage());
        }

        return accounts;
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("✅ Database connection closed.");
            } catch (SQLException e) {
                System.err.println("❌ Error closing database connection: " + e.getMessage());
            }
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}