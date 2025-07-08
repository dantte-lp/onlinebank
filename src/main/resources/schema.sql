-- ===================================================================
-- ONLINE BANK DATABASE SCHEMA
-- PostgreSQL 17
-- ===================================================================

-- -------------------------------------------------------------------
-- Create schema if not exists
-- -------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS onlinebank;
SET search_path TO onlinebank, public;

-- -------------------------------------------------------------------
-- Drop existing tables if needed (for clean restart)
-- -------------------------------------------------------------------
-- DROP TABLE IF EXISTS clients CASCADE;
-- DROP TABLE IF EXISTS spring_session CASCADE;
-- DROP TABLE IF EXISTS spring_session_attributes CASCADE;

-- -------------------------------------------------------------------
-- Create CLIENTS table
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS clients (
    id BIGSERIAL PRIMARY KEY,
    unique_id VARCHAR(36) NOT NULL UNIQUE,
    last_name VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    birth_date DATE NOT NULL,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    currency VARCHAR(10) NOT NULL,
    nationality VARCHAR(50) NOT NULL,
    phone_number VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,

    CONSTRAINT chk_account_number CHECK (account_number ~ '^[0-9]{20}$'),
    CONSTRAINT chk_phone_number CHECK (phone_number ~ '^\+[1-9]\d{1,14}$'),
    CONSTRAINT chk_currency CHECK (currency IN ('RUB', 'USD', 'EUR')),
    CONSTRAINT chk_birth_date CHECK (birth_date < CURRENT_DATE)
);

-- -------------------------------------------------------------------
-- Create indexes for better performance
-- -------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_clients_unique_id ON clients(unique_id);
CREATE INDEX IF NOT EXISTS idx_clients_last_name ON clients(last_name);
CREATE INDEX IF NOT EXISTS idx_clients_account_number ON clients(account_number);
CREATE INDEX IF NOT EXISTS idx_clients_phone_number ON clients(phone_number);
CREATE INDEX IF NOT EXISTS idx_clients_currency ON clients(currency);
CREATE INDEX IF NOT EXISTS idx_clients_nationality ON clients(nationality);
CREATE INDEX IF NOT EXISTS idx_clients_birth_date ON clients(birth_date);
CREATE INDEX IF NOT EXISTS idx_clients_created_at ON clients(created_at DESC);

-- Full text search index for name search
CREATE INDEX IF NOT EXISTS idx_clients_full_name ON clients
    USING gin(to_tsvector('russian', last_name || ' ' || first_name || ' ' || COALESCE(middle_name, '')));

-- -------------------------------------------------------------------
-- Create trigger for updated_at timestamp
-- -------------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_clients_updated_at ON clients;
CREATE TRIGGER update_clients_updated_at
    BEFORE UPDATE ON clients
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- -------------------------------------------------------------------
-- Create Spring Session tables (if using JDBC session)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS spring_session (
    primary_id CHAR(36) NOT NULL,
    session_id CHAR(36) NOT NULL,
    creation_time BIGINT NOT NULL,
    last_access_time BIGINT NOT NULL,
    max_inactive_interval INT NOT NULL,
    expiry_time BIGINT NOT NULL,
    principal_name VARCHAR(100),
    CONSTRAINT spring_session_pk PRIMARY KEY (primary_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS spring_session_ix1 ON spring_session (session_id);
CREATE INDEX IF NOT EXISTS spring_session_ix2 ON spring_session (expiry_time);
CREATE INDEX IF NOT EXISTS spring_session_ix3 ON spring_session (principal_name);

CREATE TABLE IF NOT EXISTS spring_session_attributes (
    session_primary_id CHAR(36) NOT NULL,
    attribute_name VARCHAR(200) NOT NULL,
    attribute_bytes BYTEA NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id)
        REFERENCES spring_session(primary_id) ON DELETE CASCADE
);

-- -------------------------------------------------------------------
-- Create statistics view
-- -------------------------------------------------------------------
CREATE OR REPLACE VIEW client_statistics AS
SELECT
    COUNT(*) as total_clients,
    COUNT(CASE WHEN currency = 'RUB' THEN 1 END) as rub_accounts,
    COUNT(CASE WHEN currency = 'USD' THEN 1 END) as usd_accounts,
    COUNT(CASE WHEN currency = 'EUR' THEN 1 END) as eur_accounts,
    AVG(EXTRACT(YEAR FROM AGE(birth_date))) as average_age,
    MIN(created_at) as first_client_date,
    MAX(created_at) as last_client_date
FROM clients;

-- -------------------------------------------------------------------
-- Create function to generate next account number
-- -------------------------------------------------------------------
CREATE OR REPLACE FUNCTION generate_next_account_number()
RETURNS VARCHAR AS $$
DECLARE
    max_account_number BIGINT;
    new_account_number VARCHAR(20);
BEGIN
    -- Get the maximum account number
    SELECT COALESCE(MAX(CAST(account_number AS BIGINT)), 10000000000000000000)
    INTO max_account_number
    FROM clients
    WHERE account_number ~ '^[0-9]{20}$';

    -- Generate new account number
    new_account_number := LPAD((max_account_number + 1)::TEXT, 20, '0');

    -- Ensure it's exactly 20 digits
    IF LENGTH(new_account_number) > 20 THEN
        new_account_number := SUBSTRING(new_account_number FROM 1 FOR 20);
    END IF;

    RETURN new_account_number;
END;
$$ LANGUAGE plpgsql;

-- -------------------------------------------------------------------
-- Create function to check database health
-- -------------------------------------------------------------------
CREATE OR REPLACE FUNCTION check_database_health()
RETURNS TABLE(
    status VARCHAR,
    client_count BIGINT,
    db_size TEXT,
    version TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        'UP'::VARCHAR as status,
        COUNT(*)::BIGINT as client_count,
        pg_size_pretty(pg_database_size(current_database()))::TEXT as db_size,
        version()::TEXT as version
    FROM clients;
END;
$$ LANGUAGE plpgsql;

-- -------------------------------------------------------------------
-- Create audit log table (optional, for future use)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(50) NOT NULL,
    operation VARCHAR(10) NOT NULL,
    user_name VARCHAR(100),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    row_id BIGINT,
    old_data JSONB,
    new_data JSONB
);

CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp ON audit_log(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_table_operation ON audit_log(table_name, operation);

-- -------------------------------------------------------------------
-- Grant permissions (adjust as needed)
-- -------------------------------------------------------------------
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA onlinebank TO your_app_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA onlinebank TO your_app_user;
-- GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA onlinebank TO your_app_user;

-- -------------------------------------------------------------------
-- Comments for documentation
-- -------------------------------------------------------------------
COMMENT ON TABLE clients IS 'Основная таблица клиентов банка';
COMMENT ON COLUMN clients.unique_id IS 'UUID уникальный идентификатор клиента';
COMMENT ON COLUMN clients.account_number IS '20-значный номер банковского счета';
COMMENT ON COLUMN clients.currency IS 'Валюта счета: RUB, USD, EUR';
COMMENT ON COLUMN clients.nationality IS 'Гражданство клиента (ISO код страны)';
COMMENT ON COLUMN clients.phone_number IS 'Номер телефона в международном формате E.164';
COMMENT ON COLUMN clients.version IS 'Версия записи для оптимистичной блокировки';

-- -------------------------------------------------------------------
-- Sample data for testing (commented out by default)
-- -------------------------------------------------------------------
/*
INSERT INTO clients (unique_id, last_name, first_name, middle_name, birth_date,
                    account_number, currency, nationality, phone_number)
VALUES
    ('550e8400-e29b-41d4-a716-446655440001', 'Иванов', 'Иван', 'Иванович',
     '1990-01-15', '10000000000000000001', 'RUB', 'RUSSIA', '+79001234567'),
    ('550e8400-e29b-41d4-a716-446655440002', 'Петров', 'Петр', 'Петрович',
     '1985-05-20', '10000000000000000002', 'USD', 'RUSSIA', '+79001234568'),
    ('550e8400-e29b-41d4-a716-446655440003', 'Сидорова', 'Елена', 'Александровна',
     '1992-12-10', '10000000000000000003', 'EUR', 'RUSSIA', '+79001234569');
*/

-- -------------------------------------------------------------------
-- Useful queries for maintenance
-- -------------------------------------------------------------------
/*
-- Check table sizes
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'onlinebank'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Check index usage
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE schemaname = 'onlinebank'
ORDER BY idx_scan DESC;

-- Check for duplicate phone numbers or account numbers
SELECT phone_number, COUNT(*)
FROM clients
GROUP BY phone_number
HAVING COUNT(*) > 1;

-- Vacuum and analyze tables
VACUUM ANALYZE clients;
*/