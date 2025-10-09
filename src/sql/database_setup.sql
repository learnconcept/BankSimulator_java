-- Create database
CREATE DATABASE IF NOT EXISTS banking_simulator;
USE banking_simulator;

-- Drop tables if they exist (for clean setup)
DROP TABLE IF EXISTS balance_alerts;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS accounts;

-- Create accounts table
CREATE TABLE accounts (
    account_number VARCHAR(20) PRIMARY KEY,
    account_holder_name VARCHAR(100) NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    email VARCHAR(100),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create transactions table
CREATE TABLE transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL,
    transaction_type ENUM('DEPOSIT', 'WITHDRAWAL', 'TRANSFER') NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    target_account VARCHAR(20),
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('SUCCESS', 'FAILED') NOT NULL,
    description TEXT,
    FOREIGN KEY (account_number) REFERENCES accounts(account_number),
    INDEX idx_account_date (account_number, transaction_date),
    INDEX idx_date (transaction_date)
);

-- Create balance_alerts table
CREATE TABLE balance_alerts (
    alert_id INT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL,
    alert_type ENUM('LOW_BALANCE', 'OVERDRAFT_ATTEMPT', 'THRESHOLD_BREACH') NOT NULL,
    message TEXT NOT NULL,
    alert_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_number) REFERENCES accounts(account_number),
    INDEX idx_account_alert (account_number, alert_date)
);

-- Insert sample data
INSERT INTO accounts (account_number, account_holder_name, balance, email) VALUES
('ACC001', 'John Doe', 1000.00, 'john.doe@email.com'),
('ACC002', 'Jane Smith', 2500.50, 'jane.smith@email.com'),
('ACC003', 'Bob Johnson', 500.00, 'bob.johnson@email.com'),
('ACC004', 'Alice Brown', 7500.75, 'alice.brown@email.com'),
('ACC005', 'Charlie Wilson', 300.00, 'charlie.wilson@email.com');

-- Insert sample transactions
INSERT INTO transactions (account_number, transaction_type, amount, target_account, status, description) VALUES
('ACC001', 'DEPOSIT', 500.00, NULL, 'SUCCESS', 'Initial deposit'),
('ACC002', 'DEPOSIT', 1000.00, NULL, 'SUCCESS', 'Account opening'),
('ACC001', 'WITHDRAWAL', 200.00, NULL, 'SUCCESS', 'ATM withdrawal'),
('ACC002', 'TRANSFER', 300.00, 'ACC001', 'SUCCESS', 'Fund transfer to ACC001');

-- Display table structure
DESCRIBE accounts;
DESCRIBE transactions;
DESCRIBE balance_alerts;

-- Display sample data
SELECT 'Accounts:' AS '';
SELECT * FROM accounts;

SELECT 'Transactions:' AS '';
SELECT * FROM transactions;