-- V3__init_users.sql
-- 관리자/주방직원 계정 초기화
-- BCrypt 암호화: admin1234, kds1234

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)     NOT NULL UNIQUE,
    password    VARCHAR(255)    NOT NULL,
    role        VARCHAR(20)     NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 관리자 계정 (admin / admin1234)
-- 주방 직원 계정 (kds / kds1234)
INSERT INTO users (username, password, role) VALUES
('admin', '$2a$10$7Q8nF5Kz2RvMxP3LdY6VneI1qKjHmWbTgcOsZpEuDaNXlJyRfA8Ci', 'ADMIN'),
('kds',   '$2a$10$3mNpQ9Rv4WxKzL7gY2UdoeBtJhMcFsViTXqEnlCwPaZDkGyjR5H6u', 'STAFF');