CREATE TABLE dormitory (dormitory_id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), is_active BOOLEAN);
CREATE TABLE laundry_room (room_id BIGINT AUTO_INCREMENT PRIMARY KEY, dormitory_id BIGINT, name VARCHAR(100), location VARCHAR(255), is_active BOOLEAN);
CREATE TABLE machine (machine_id BIGINT AUTO_INCREMENT PRIMARY KEY, room_id BIGINT, machine_code VARCHAR(40), management_status VARCHAR(20), default_cycle_seconds BIGINT, is_active BOOLEAN);
CREATE TABLE wash_cycle (cycle_id BIGINT AUTO_INCREMENT PRIMARY KEY, machine_id BIGINT, started_at TIMESTAMP, estimated_end_at TIMESTAMP, completed_at TIMESTAMP, cycle_status VARCHAR(30));
CREATE TABLE machine_signal_log (signal_id BIGINT AUTO_INCREMENT PRIMARY KEY, source_event_id VARCHAR(100) UNIQUE, source_kind VARCHAR(20), machine_id BIGINT, cycle_id BIGINT, signal_type VARCHAR(20), received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, process_status VARCHAR(20), error_reason VARCHAR(255));
CREATE TABLE reminder_subscription (subscription_id BIGINT AUTO_INCREMENT PRIMARY KEY, cycle_id BIGINT, status VARCHAR(20));
INSERT INTO dormitory VALUES (1, 'Demo', TRUE);
INSERT INTO laundry_room VALUES (1, 1, 'Room', 'First floor', TRUE);
INSERT INTO machine VALUES (1, 1, 'A01', 'NORMAL', 1800, TRUE);
