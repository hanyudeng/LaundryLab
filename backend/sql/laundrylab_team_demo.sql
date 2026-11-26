-- LaundryLab team demo for a fresh MySQL 8 database only.
-- Synthetic machine data and two verified test accounts. No real user data or login sessions.
-- Student: stu@o365.fcu.edu.tw / pass12345
-- Supervisor: sup@o365.fcu.edu.tw / pass12345
-- Never use these credentials in production.
CREATE DATABASE IF NOT EXISTS laundrylab CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE laundrylab;


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_log` (
  `audit_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `actor_id` bigint unsigned NOT NULL,
  `target_type` varchar(50) NOT NULL,
  `target_id` bigint unsigned NOT NULL,
  `action` varchar(80) NOT NULL,
  `before_value` json DEFAULT NULL,
  `after_value` json DEFAULT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`audit_id`),
  KEY `idx_audit_created` (`created_at`),
  KEY `actor_id` (`actor_id`),
  CONSTRAINT `audit_log_ibfk_1` FOREIGN KEY (`actor_id`) REFERENCES `user_account` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dorm_supervisor_assignment` (
  `supervisor_id` bigint unsigned NOT NULL,
  `dormitory_id` bigint unsigned NOT NULL,
  `assigned_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`supervisor_id`,`dormitory_id`),
  KEY `dormitory_id` (`dormitory_id`),
  CONSTRAINT `dorm_supervisor_assignment_ibfk_1` FOREIGN KEY (`supervisor_id`) REFERENCES `user_account` (`user_id`),
  CONSTRAINT `dorm_supervisor_assignment_ibfk_2` FOREIGN KEY (`dormitory_id`) REFERENCES `dormitory` (`dormitory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `dorm_supervisor_assignment` VALUES (2,1,'2026-11-25 19:54:25.253',1);
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dormitory` (
  `dormitory_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`dormitory_id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `dormitory` VALUES (1,'示範宿舍',1);
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `email_verification` (
  `verification_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `token_hash` char(64) NOT NULL,
  `expires_at` datetime(3) NOT NULL,
  `used_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`verification_id`),
  UNIQUE KEY `token_hash` (`token_hash`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `email_verification_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `issue_report` (
  `report_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `reporter_id` bigint unsigned NOT NULL,
  `machine_id` bigint unsigned NOT NULL,
  `issue_type` enum('FAULT','STATUS_MISMATCH','OTHER') NOT NULL,
  `description` text NOT NULL,
  `photo_path` varchar(500) DEFAULT NULL,
  `status` enum('OPEN','IN_PROGRESS','RESOLVED') NOT NULL DEFAULT 'OPEN',
  `handler_id` bigint unsigned DEFAULT NULL,
  `resolution` text,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`report_id`),
  KEY `reporter_id` (`reporter_id`),
  KEY `machine_id` (`machine_id`),
  KEY `handler_id` (`handler_id`),
  CONSTRAINT `issue_report_ibfk_1` FOREIGN KEY (`reporter_id`) REFERENCES `user_account` (`user_id`),
  CONSTRAINT `issue_report_ibfk_2` FOREIGN KEY (`machine_id`) REFERENCES `machine` (`machine_id`),
  CONSTRAINT `issue_report_ibfk_3` FOREIGN KEY (`handler_id`) REFERENCES `user_account` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `laundry_room` (
  `room_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `dormitory_id` bigint unsigned NOT NULL,
  `name` varchar(100) NOT NULL,
  `location` varchar(255) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`room_id`),
  UNIQUE KEY `dormitory_id` (`dormitory_id`,`name`),
  CONSTRAINT `laundry_room_ibfk_1` FOREIGN KEY (`dormitory_id`) REFERENCES `dormitory` (`dormitory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `laundry_room` VALUES (1,1,'一樓洗衣間','一樓東側',1);
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `line_account_link` (
  `link_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `line_user_id` varchar(50) DEFAULT NULL,
  `link_status` enum('LINKED','UNLINKED') NOT NULL DEFAULT 'UNLINKED',
  `follow_status` enum('FOLLOWING','BLOCKED','UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
  `linked_at` datetime(3) DEFAULT NULL,
  `unlinked_at` datetime(3) DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`link_id`),
  UNIQUE KEY `user_id` (`user_id`),
  UNIQUE KEY `line_user_id` (`line_user_id`),
  CONSTRAINT `line_account_link_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `line_link_request` (
  `request_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `nonce_hash` char(64) NOT NULL,
  `expires_at` datetime(3) NOT NULL,
  `used_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`request_id`),
  UNIQUE KEY `nonce_hash` (`nonce_hash`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `line_link_request_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `line_webhook_event_log` (
  `webhook_event_id` varchar(64) NOT NULL,
  `event_type` varchar(50) NOT NULL,
  `line_user_id` varchar(50) DEFAULT NULL,
  `event_occurred_at` datetime(3) NOT NULL,
  `received_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `process_status` enum('RECEIVED','PROCESSED','IGNORED','FAILED') NOT NULL DEFAULT 'RECEIVED',
  `processed_at` datetime(3) DEFAULT NULL,
  `error_reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`webhook_event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `login_session` (
  `session_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `token_hash` char(64) NOT NULL,
  `expires_at` datetime(3) NOT NULL,
  `revoked_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`session_id`),
  UNIQUE KEY `token_hash` (`token_hash`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `login_session_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `machine` (
  `machine_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `room_id` bigint unsigned NOT NULL,
  `machine_code` varchar(40) NOT NULL,
  `management_status` enum('NORMAL','MAINTENANCE') NOT NULL DEFAULT 'NORMAL',
  `default_cycle_seconds` int unsigned NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`machine_id`),
  UNIQUE KEY `room_id` (`room_id`,`machine_code`),
  CONSTRAINT `machine_ibfk_1` FOREIGN KEY (`room_id`) REFERENCES `laundry_room` (`room_id`),
  CONSTRAINT `chk_cycle_seconds` CHECK ((`default_cycle_seconds` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `machine` VALUES (1,1,'A01','NORMAL',1800,1,'2026-11-25 19:54:25.125'),(2,1,'A02','NORMAL',2400,1,'2026-11-25 19:54:25.125'),(3,1,'A03','NORMAL',1800,1,'2026-11-25 19:54:25.125'),(4,1,'A04','NORMAL',2400,1,'2026-11-25 19:54:25.125'),(5,1,'A05','NORMAL',1800,1,'2026-11-25 19:54:25.125');
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `machine_signal_log` (
  `signal_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `source_event_id` varchar(100) NOT NULL,
  `source_kind` enum('TEST_PAGE','IOT_SWITCH') NOT NULL,
  `machine_id` bigint unsigned NOT NULL,
  `cycle_id` bigint unsigned DEFAULT NULL,
  `signal_type` enum('START','COMPLETE','FAULT') NOT NULL,
  `event_occurred_at` datetime(3) DEFAULT NULL,
  `received_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `process_status` enum('ACCEPTED','REJECTED') NOT NULL,
  `error_reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`signal_id`),
  UNIQUE KEY `source_event_id` (`source_event_id`),
  KEY `idx_signal_machine_received` (`machine_id`,`received_at`),
  KEY `cycle_id` (`cycle_id`),
  CONSTRAINT `machine_signal_log_ibfk_1` FOREIGN KEY (`machine_id`) REFERENCES `machine` (`machine_id`),
  CONSTRAINT `machine_signal_log_ibfk_2` FOREIGN KEY (`cycle_id`) REFERENCES `wash_cycle` (`cycle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_log` (
  `notification_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `subscription_id` bigint unsigned NOT NULL,
  `notification_type` enum('FIVE_MINUTES_BEFORE','ESTIMATED_COMPLETE') NOT NULL,
  `recipient_line_user_id` varchar(50) NOT NULL,
  `line_retry_key` char(36) NOT NULL,
  `send_status` enum('PENDING','SENDING','ACCEPTED','FAILED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  `attempt_count` int unsigned NOT NULL DEFAULT '0',
  `next_attempt_at` datetime(3) DEFAULT NULL,
  `last_attempt_at` datetime(3) DEFAULT NULL,
  `accepted_at` datetime(3) DEFAULT NULL,
  `line_message_id` varchar(255) DEFAULT NULL,
  `last_error` varchar(500) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`notification_id`),
  UNIQUE KEY `line_retry_key` (`line_retry_key`),
  UNIQUE KEY `subscription_id` (`subscription_id`,`notification_type`),
  CONSTRAINT `notification_log_ibfk_1` FOREIGN KEY (`subscription_id`) REFERENCES `reminder_subscription` (`subscription_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reminder_subscription` (
  `subscription_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `cycle_id` bigint unsigned NOT NULL,
  `status` enum('ACTIVE','CANCELLED','EXPIRED') NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `cancelled_at` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`subscription_id`),
  UNIQUE KEY `user_id` (`user_id`,`cycle_id`),
  KEY `cycle_id` (`cycle_id`),
  CONSTRAINT `reminder_subscription_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`user_id`),
  CONSTRAINT `reminder_subscription_ibfk_2` FOREIGN KEY (`cycle_id`) REFERENCES `wash_cycle` (`cycle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_account` (
  `user_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `email` varchar(254) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` enum('STUDENT','SUPERVISOR','ADMIN') NOT NULL,
  `email_verified_at` datetime(3) DEFAULT NULL,
  `account_status` enum('ACTIVE','DISABLED') NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `user_account` VALUES (1,'stu@o365.fcu.edu.tw','$2y$10$y0C51NnRb3/NdZ5QxPEE3u1iUJnjFsfs1ab08W3/T2tkduPWC1Cw2','STUDENT','2026-11-25 11:54:25.253','ACTIVE','2026-11-25 19:54:25.253'),(2,'sup@o365.fcu.edu.tw','$2y$10$j356GrJShR2uOv8hRYkUv.8unDEX3Pd.8lgJ9nSXSlDKwJFuF7iYq','SUPERVISOR','2026-11-25 11:54:25.253','ACTIVE','2026-11-25 19:54:25.253');
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wash_cycle` (
  `cycle_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `machine_id` bigint unsigned NOT NULL,
  `started_at` datetime(3) NOT NULL,
  `estimated_end_at` datetime(3) NOT NULL,
  `completed_at` datetime(3) DEFAULT NULL,
  `cycle_status` enum('RUNNING','PENDING_CONFIRMATION','COMPLETED','CANCELLED') NOT NULL,
  `active_machine_id` bigint unsigned GENERATED ALWAYS AS ((case when (`cycle_status` in (_utf8mb4'RUNNING',_utf8mb4'PENDING_CONFIRMATION')) then `machine_id` else NULL end)) STORED,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`cycle_id`),
  UNIQUE KEY `active_machine_id` (`active_machine_id`),
  KEY `idx_cycle_machine` (`machine_id`,`started_at`),
  CONSTRAINT `wash_cycle_ibfk_1` FOREIGN KEY (`machine_id`) REFERENCES `machine` (`machine_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
