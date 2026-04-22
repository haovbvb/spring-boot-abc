INSERT INTO sys_user (id, username, password, nickname, status, create_by, update_by, is_deleted)
VALUES (1, 'demo', 'demo', 'Demo User', 1, 'system', 'system', 0) AS new
ON DUPLICATE KEY UPDATE
  username = new.username,
  password = new.password,
  nickname = new.nickname,
  status = new.status,
  is_deleted = 0;
