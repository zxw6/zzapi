CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) NULL,
    email VARCHAR(128) NULL,
    phone VARCHAR(32) NULL,
    role_code VARCHAR(32) NOT NULL DEFAULT 'USER',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    package_restriction_enabled TINYINT(1) NOT NULL DEFAULT 1,
    last_login_at DATETIME NULL,
    last_active_at DATETIME NULL,
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_role_status (role_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS api_keys (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    access_key VARCHAR(96) NOT NULL,
    secret_hash VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    expires_at DATETIME NULL,
    last_used_at DATETIME NULL,
    user_package_id BIGINT NULL,
    model_group_id BIGINT NULL,
    total_quota DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    used_quota DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_api_keys_access_key (access_key),
    KEY idx_api_keys_user_status (user_id, status),
    KEY idx_api_keys_package (user_package_id),
    KEY idx_api_keys_group (model_group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS providers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider_code VARCHAR(32) NOT NULL,
    provider_name VARCHAR(64) NOT NULL,
    base_url VARCHAR(255) NOT NULL,
    provider_type VARCHAR(32) NOT NULL DEFAULT 'OPENAI_COMPATIBLE',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    priority_no INT NOT NULL DEFAULT 100,
    timeout_ms INT NOT NULL DEFAULT 60000,
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_providers_code (provider_code),
    KEY idx_providers_status_priority (status, priority_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS provider_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider_id BIGINT NOT NULL,
    token_name VARCHAR(64) NOT NULL,
    token_value_encrypted VARCHAR(1024) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    weight_no INT NOT NULL DEFAULT 100,
    rpm_limit INT NOT NULL DEFAULT 0,
    tpm_limit INT NOT NULL DEFAULT 0,
    current_balance DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    last_checked_at DATETIME NULL,
    last_used_at DATETIME NULL,
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    KEY idx_provider_tokens_provider_status (provider_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS models (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    model_code VARCHAR(64) NOT NULL,
    model_name VARCHAR(64) NOT NULL,
    model_type VARCHAR(32) NOT NULL DEFAULT 'CHAT',
    billing_type VARCHAR(32) NOT NULL DEFAULT 'TOKEN',
    prompt_price DECIMAL(18, 6) NOT NULL DEFAULT 0.000000,
    cached_prompt_price DECIMAL(18, 6) NOT NULL DEFAULT 0.000000,
    completion_price DECIMAL(18, 6) NOT NULL DEFAULT 0.000000,
    request_price DECIMAL(18, 6) NOT NULL DEFAULT 0.000000,
    image_price DECIMAL(18, 6) NOT NULL DEFAULT 0.000000,
    multiplier DECIMAL(10, 4) NOT NULL DEFAULT 1.0000,
    is_public TINYINT(1) NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_models_code (model_code),
    KEY idx_models_status_public (status, is_public)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS model_routes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    model_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    provider_token_id BIGINT NULL,
    upstream_model VARCHAR(128) NOT NULL,
    route_type VARCHAR(32) NOT NULL DEFAULT 'PRIMARY',
    priority_no INT NOT NULL DEFAULT 100,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_model_routes_unique (model_id, provider_id, upstream_model),
    KEY idx_model_routes_status_priority (status, priority_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wallets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    balance DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    frozen_balance DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    total_recharge DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    total_consume DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_wallets_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    wallet_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    direction VARCHAR(8) NOT NULL,
    amount DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    balance_before DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    balance_after DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
    description_text VARCHAR(255) NULL,
    transaction_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_transactions_order_no (order_no),
    KEY idx_transactions_user_date (user_id, transaction_date),
    KEY idx_transactions_type_direction (transaction_type, direction)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS request_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id VARCHAR(64) NOT NULL,
    user_id BIGINT NULL,
    api_key_id BIGINT NULL,
    user_package_id BIGINT NULL,
    model_code VARCHAR(64) NOT NULL,
    provider_id BIGINT NULL,
    provider_token_id BIGINT NULL,
    upstream_model VARCHAR(128) NULL,
    request_path VARCHAR(128) NOT NULL,
    request_method VARCHAR(16) NOT NULL,
    request_ip VARCHAR(64) NULL,
    request_body_json JSON NULL,
    response_body_json JSON NULL,
    prompt_tokens INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    total_tokens INT NOT NULL DEFAULT 0,
    cached_prompt_tokens INT NOT NULL DEFAULT 0,
    user_amount DECIMAL(18, 6) NOT NULL DEFAULT 0.000000,
    cost_amount DECIMAL(18, 6) NOT NULL DEFAULT 0.000000,
    latency_ms INT NOT NULL DEFAULT 0,
    success TINYINT(1) NOT NULL DEFAULT 1,
    status_code INT NOT NULL DEFAULT 200,
    error_message VARCHAR(500) NULL,
    request_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_request_logs_request_id (request_id),
    KEY idx_request_logs_package_date (user_package_id, request_date),
    KEY idx_request_logs_user_date (user_id, request_date),
    KEY idx_request_logs_model_date (model_code, request_date),
    KEY idx_request_logs_provider_date (provider_id, request_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS usage_daily (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stat_date DATE NOT NULL,
    user_id BIGINT NULL,
    model_code VARCHAR(64) NULL,
    provider_id BIGINT NULL,
    request_count BIGINT NOT NULL DEFAULT 0,
    success_count BIGINT NOT NULL DEFAULT 0,
    total_tokens BIGINT NOT NULL DEFAULT 0,
    user_amount DECIMAL(18, 6) NOT NULL DEFAULT 0.000000,
    cost_amount DECIMAL(18, 6) NOT NULL DEFAULT 0.000000,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_usage_daily_stat (stat_date, user_id, model_code, provider_id),
    KEY idx_usage_daily_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS model_groups (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_code VARCHAR(64) NOT NULL,
    group_name VARCHAR(64) NOT NULL,
    package_type VARCHAR(32) NOT NULL DEFAULT 'QUOTA',
    sale_price DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    package_days INT NOT NULL DEFAULT 30,
    daily_quota DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    weekly_quota DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    monthly_quota DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_model_groups_code (group_code),
    KEY idx_model_groups_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS model_group_models (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    model_id BIGINT NOT NULL,
    billing_type VARCHAR(32) NULL,
    prompt_price DECIMAL(18, 6) NULL,
    cached_prompt_price DECIMAL(18, 6) NULL,
    completion_price DECIMAL(18, 6) NULL,
    request_price DECIMAL(18, 6) NULL,
    multiplier DECIMAL(18, 4) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_model_group_models_unique (group_id, model_id),
    KEY idx_model_group_models_group (group_id, model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_model_packages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    package_name VARCHAR(64) NOT NULL,
    purchase_price DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    start_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_model_packages_user (user_id, status, expires_at),
    KEY idx_user_model_packages_group (group_id, status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS system_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(64) NOT NULL,
    config_value TEXT NULL,
    config_group VARCHAR(32) NOT NULL DEFAULT 'SYSTEM',
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_system_configs_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS site_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    settings_key VARCHAR(32) NOT NULL DEFAULT 'DEFAULT',
    site_name VARCHAR(128) NOT NULL,
    admin_email VARCHAR(128) NOT NULL,
    site_description VARCHAR(255) NULL,
    base_url VARCHAR(255) NOT NULL,
    footer_text VARCHAR(255) NULL,
    theme_mode VARCHAR(16) NOT NULL DEFAULT 'LIGHT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_site_settings_key (settings_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_user_id BIGINT NULL,
    operator_name VARCHAR(64) NULL,
    action_code VARCHAR(64) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id VARCHAR(64) NULL,
    detail_json JSON NULL,
    request_ip VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_audit_logs_action_time (action_code, created_at),
    KEY idx_audit_logs_operator_time (operator_user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS agent_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_key VARCHAR(96) NOT NULL,
    user_id BIGINT NOT NULL,
    api_key_id BIGINT NOT NULL,
    model_code VARCHAR(64) NOT NULL,
    workspace_root VARCHAR(512) NULL,
    summary_text LONGTEXT NULL,
    summary_message_id BIGINT NOT NULL DEFAULT 0,
    last_response_id VARCHAR(96) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_agent_sessions_key (session_key),
    KEY idx_agent_sessions_user (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS agent_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    response_id VARCHAR(96) NOT NULL,
    role_code VARCHAR(16) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    content_text LONGTEXT NULL,
    tool_name VARCHAR(64) NULL,
    tool_payload_json JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_agent_messages_session (session_id, id),
    KEY idx_agent_messages_response (response_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS agent_tool_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    response_id VARCHAR(96) NOT NULL,
    step_no INT NOT NULL,
    tool_name VARCHAR(64) NOT NULL,
    title VARCHAR(128) NULL,
    success TINYINT(1) NOT NULL DEFAULT 1,
    arguments_json JSON NULL,
    result_json JSON NULL,
    file_previews_json JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_agent_tool_logs_session (session_id, id),
    KEY idx_agent_tool_logs_response (response_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE users ADD COLUMN package_restriction_enabled TINYINT(1) NOT NULL DEFAULT 1',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'package_restriction_enabled'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE users ADD COLUMN last_active_at DATETIME NULL AFTER last_login_at',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'last_active_at'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE api_keys ADD COLUMN model_group_id BIGINT NULL',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'api_keys' AND column_name = 'model_group_id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE api_keys ADD COLUMN user_package_id BIGINT NULL',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'api_keys' AND column_name = 'user_package_id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE api_keys ADD COLUMN total_quota DECIMAL(18, 4) NOT NULL DEFAULT 0.0000',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'api_keys' AND column_name = 'total_quota'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE api_keys ADD COLUMN used_quota DECIMAL(18, 4) NOT NULL DEFAULT 0.0000',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'api_keys' AND column_name = 'used_quota'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'CREATE INDEX idx_api_keys_package ON api_keys (user_package_id)',
              'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'api_keys' AND index_name = 'idx_api_keys_package'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'CREATE INDEX idx_api_keys_group ON api_keys (model_group_id)',
              'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'api_keys' AND index_name = 'idx_api_keys_group'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE request_logs ADD COLUMN user_package_id BIGINT NULL AFTER api_key_id',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'request_logs' AND column_name = 'user_package_id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE request_logs ADD COLUMN cached_prompt_tokens INT NOT NULL DEFAULT 0 AFTER total_tokens',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'request_logs' AND column_name = 'cached_prompt_tokens'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'CREATE INDEX idx_request_logs_package_date ON request_logs (user_package_id, request_date)',
              'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'request_logs' AND index_name = 'idx_request_logs_package_date'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE models ADD COLUMN cached_prompt_price DECIMAL(18, 6) NOT NULL DEFAULT 0.000000 AFTER prompt_price',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'models' AND column_name = 'cached_prompt_price'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE model_group_models ADD COLUMN billing_type VARCHAR(32) NULL AFTER model_id',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'model_group_models' AND column_name = 'billing_type'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE model_group_models ADD COLUMN prompt_price DECIMAL(18, 6) NULL AFTER billing_type',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'model_group_models' AND column_name = 'prompt_price'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE model_group_models ADD COLUMN cached_prompt_price DECIMAL(18, 6) NULL AFTER prompt_price',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'model_group_models' AND column_name = 'cached_prompt_price'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE model_group_models ADD COLUMN completion_price DECIMAL(18, 6) NULL AFTER cached_prompt_price',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'model_group_models' AND column_name = 'completion_price'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE model_group_models ADD COLUMN request_price DECIMAL(18, 6) NULL AFTER completion_price',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'model_group_models' AND column_name = 'request_price'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE model_group_models ADD COLUMN multiplier DECIMAL(18, 4) NULL AFTER request_price',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'model_group_models' AND column_name = 'multiplier'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE agent_sessions ADD COLUMN summary_text LONGTEXT NULL',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'agent_sessions' AND column_name = 'summary_text'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE agent_sessions ADD COLUMN summary_message_id BIGINT NOT NULL DEFAULT 0',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'agent_sessions' AND column_name = 'summary_message_id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
