-- V4__add_store_id.sql
-- orders 테이블에 store_id 컬럼 추가 (다중 매장 구분용)

ALTER TABLE orders
    ADD COLUMN store_id VARCHAR(50) NOT NULL DEFAULT 'store-default' AFTER order_number;