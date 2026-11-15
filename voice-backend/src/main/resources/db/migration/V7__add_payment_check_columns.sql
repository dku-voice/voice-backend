-- 결제 시도 후 결과 미확인 주문을 스케줄러가 재확인하기 위한 컬럼
ALTER TABLE orders
    ADD COLUMN payment_requested_at DATETIME NULL,
    ADD COLUMN payment_checked_at   DATETIME NULL,
    ADD COLUMN payment_check_count  INT NOT NULL DEFAULT 0;

CREATE INDEX idx_orders_payment_check ON orders (status, payment_requested_at);