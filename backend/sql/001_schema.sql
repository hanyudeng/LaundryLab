CREATE DATABASE IF NOT EXISTS laundrylab CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE laundrylab;

CREATE TABLE user_account (
  user_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(254) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('STUDENT','SUPERVISOR','ADMIN') NOT NULL,
  email_verified_at DATETIME(3) NULL,
  account_status ENUM('ACTIVE','DISABLED') NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);
CREATE TABLE email_verification (
  verification_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  token_hash CHAR(64) NOT NULL UNIQUE,
  expires_at DATETIME(3) NOT NULL,
  used_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  FOREIGN KEY (user_id) REFERENCES user_account(user_id)
);
CREATE TABLE login_session (
  session_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  token_hash CHAR(64) NOT NULL UNIQUE,
  expires_at DATETIME(3) NOT NULL,
  revoked_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  FOREIGN KEY (user_id) REFERENCES user_account(user_id)
);
CREATE TABLE dormitory (
  dormitory_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE,
  is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE laundry_room (
  room_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  dormitory_id BIGINT UNSIGNED NOT NULL,
  name VARCHAR(100) NOT NULL,
  location VARCHAR(255) NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE (dormitory_id, name),
  FOREIGN KEY (dormitory_id) REFERENCES dormitory(dormitory_id)
);
CREATE TABLE machine (
  machine_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  room_id BIGINT UNSIGNED NOT NULL,
  machine_code VARCHAR(40) NOT NULL,
  management_status ENUM('NORMAL','MAINTENANCE') NOT NULL DEFAULT 'NORMAL',
  default_cycle_seconds INT UNSIGNED NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE (room_id, machine_code),
  CONSTRAINT chk_cycle_seconds CHECK (default_cycle_seconds > 0),
  FOREIGN KEY (room_id) REFERENCES laundry_room(room_id)
);
CREATE TABLE dorm_supervisor_assignment (
  supervisor_id BIGINT UNSIGNED NOT NULL,
  dormitory_id BIGINT UNSIGNED NOT NULL,
  assigned_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (supervisor_id, dormitory_id),
  FOREIGN KEY (supervisor_id) REFERENCES user_account(user_id),
  FOREIGN KEY (dormitory_id) REFERENCES dormitory(dormitory_id)
);
CREATE TABLE wash_cycle (
  cycle_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  machine_id BIGINT UNSIGNED NOT NULL,
  started_at DATETIME(3) NOT NULL,
  estimated_end_at DATETIME(3) NOT NULL,
  completed_at DATETIME(3) NULL,
  cycle_status ENUM('RUNNING','PENDING_CONFIRMATION','COMPLETED','CANCELLED') NOT NULL,
  active_machine_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN cycle_status IN ('RUNNING','PENDING_CONFIRMATION') THEN machine_id ELSE NULL END) STORED UNIQUE,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  INDEX idx_cycle_machine (machine_id, started_at),
  FOREIGN KEY (machine_id) REFERENCES machine(machine_id)
);
CREATE TABLE machine_signal_log (
  signal_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  source_event_id VARCHAR(100) NOT NULL UNIQUE,
  source_kind ENUM('TEST_PAGE','IOT_SWITCH') NOT NULL,
  machine_id BIGINT UNSIGNED NOT NULL,
  cycle_id BIGINT UNSIGNED NULL,
  signal_type ENUM('START','COMPLETE','FAULT') NOT NULL,
  event_occurred_at DATETIME(3) NULL,
  received_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  process_status ENUM('ACCEPTED','REJECTED') NOT NULL,
  error_reason VARCHAR(255) NULL,
  INDEX idx_signal_machine_received (machine_id, received_at),
  FOREIGN KEY (machine_id) REFERENCES machine(machine_id),
  FOREIGN KEY (cycle_id) REFERENCES wash_cycle(cycle_id)
);
CREATE TABLE reminder_subscription (
  subscription_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  cycle_id BIGINT UNSIGNED NOT NULL,
  status ENUM('ACTIVE','CANCELLED','EXPIRED') NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  cancelled_at DATETIME(3) NULL,
  UNIQUE (user_id, cycle_id),
  FOREIGN KEY (user_id) REFERENCES user_account(user_id),
  FOREIGN KEY (cycle_id) REFERENCES wash_cycle(cycle_id)
);
CREATE TABLE notification_log (
  notification_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  subscription_id BIGINT UNSIGNED NOT NULL,
  notification_type ENUM('FIVE_MINUTES_BEFORE','ESTIMATED_COMPLETE') NOT NULL,
  recipient_line_user_id VARCHAR(50) NOT NULL,
  line_retry_key CHAR(36) NOT NULL UNIQUE,
  send_status ENUM('PENDING','SENDING','ACCEPTED','FAILED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  attempt_count INT UNSIGNED NOT NULL DEFAULT 0,
  next_attempt_at DATETIME(3) NULL,
  last_attempt_at DATETIME(3) NULL,
  accepted_at DATETIME(3) NULL,
  line_message_id VARCHAR(255) NULL,
  last_error VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE (subscription_id, notification_type),
  FOREIGN KEY (subscription_id) REFERENCES reminder_subscription(subscription_id)
);
CREATE TABLE line_account_link (
  link_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL UNIQUE,
  line_user_id VARCHAR(50) NULL UNIQUE,
  link_status ENUM('LINKED','UNLINKED') NOT NULL DEFAULT 'UNLINKED',
  follow_status ENUM('FOLLOWING','BLOCKED','UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
  linked_at DATETIME(3) NULL,
  unlinked_at DATETIME(3) NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  FOREIGN KEY (user_id) REFERENCES user_account(user_id)
);
CREATE TABLE line_link_request (
  request_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  nonce_hash CHAR(64) NOT NULL UNIQUE,
  expires_at DATETIME(3) NOT NULL,
  used_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  FOREIGN KEY (user_id) REFERENCES user_account(user_id)
);
CREATE TABLE line_webhook_event_log (
  webhook_event_id VARCHAR(64) PRIMARY KEY,
  event_type VARCHAR(50) NOT NULL,
  line_user_id VARCHAR(50) NULL,
  event_occurred_at DATETIME(3) NOT NULL,
  received_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  process_status ENUM('RECEIVED','PROCESSED','IGNORED','FAILED') NOT NULL DEFAULT 'RECEIVED',
  processed_at DATETIME(3) NULL,
  error_reason VARCHAR(255) NULL
);
CREATE TABLE issue_report (
  report_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  reporter_id BIGINT UNSIGNED NOT NULL,
  machine_id BIGINT UNSIGNED NOT NULL,
  issue_type ENUM('FAULT','STATUS_MISMATCH','OTHER') NOT NULL,
  description TEXT NOT NULL,
  photo_path VARCHAR(500) NULL,
  status ENUM('OPEN','IN_PROGRESS','RESOLVED') NOT NULL DEFAULT 'OPEN',
  handler_id BIGINT UNSIGNED NULL,
  resolution TEXT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  FOREIGN KEY (reporter_id) REFERENCES user_account(user_id),
  FOREIGN KEY (machine_id) REFERENCES machine(machine_id),
  FOREIGN KEY (handler_id) REFERENCES user_account(user_id)
);
CREATE TABLE audit_log (
  audit_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  actor_id BIGINT UNSIGNED NOT NULL,
  target_type VARCHAR(50) NOT NULL,
  target_id BIGINT UNSIGNED NOT NULL,
  action VARCHAR(80) NOT NULL,
  before_value JSON NULL,
  after_value JSON NULL,
  reason VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  INDEX idx_audit_created (created_at),
  FOREIGN KEY (actor_id) REFERENCES user_account(user_id)
);
