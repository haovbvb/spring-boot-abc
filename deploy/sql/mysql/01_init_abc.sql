-- MySQL init script for local development.
-- This script runs automatically on first container startup when the mysql volume is empty.

USE abc;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password VARCHAR(255) NOT NULL,
  nickname VARCHAR(64) DEFAULT NULL,
  status INT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  create_by VARCHAR(64) NOT NULL DEFAULT 'system',
  update_by VARCHAR(64) NOT NULL DEFAULT 'system',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_user (id, username, password, nickname, status, create_by, update_by, is_deleted)
VALUES (1, 'demo', 'demo', 'Demo User', 1, 'system', 'system', 0)
ON DUPLICATE KEY UPDATE
  username = VALUES(username),
  password = VALUES(password),
  nickname = VALUES(nickname),
  status = VALUES(status),
  is_deleted = 0;
