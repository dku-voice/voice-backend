-- V1__init_schema.sql
-- 초기 테이블 생성 마이그레이션
-- Entity 기준: Category, Allergen, Menu, MenuAllergen, MenuNutrient, Order, OrderItem, Payment

-- ── Category ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS category (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    name_ko     VARCHAR(50)     NOT NULL,
    name_en     VARCHAR(50)     NOT NULL,
    sort_order  INT             NOT NULL DEFAULT 0,
    is_active   TINYINT(1)      NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Allergen ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS allergen (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    name_ko     VARCHAR(50)     NOT NULL,
    name_en     VARCHAR(50)     NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Menu ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS menu (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    category_id BIGINT          NOT NULL,
    name_ko     VARCHAR(100)    NOT NULL,
    name_en     VARCHAR(100)    NOT NULL,
    price       INT             NOT NULL,
    image_url   VARCHAR(500),
    status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    CONSTRAINT fk_menu_category FOREIGN KEY (category_id)
        REFERENCES category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── MenuAllergen ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS menu_allergen (
    menu_id     BIGINT          NOT NULL,
    allergen_id BIGINT          NOT NULL,
    PRIMARY KEY (menu_id, allergen_id),
    CONSTRAINT fk_menu_allergen_menu     FOREIGN KEY (menu_id)
        REFERENCES menu (id),
    CONSTRAINT fk_menu_allergen_allergen FOREIGN KEY (allergen_id)
        REFERENCES allergen (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── MenuNutrient ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS menu_nutrient (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    menu_id     BIGINT          NOT NULL UNIQUE,
    calories    INT             NOT NULL DEFAULT 0,
    sodium      INT             NOT NULL DEFAULT 0,
    carbs       INT             NOT NULL DEFAULT 0,
    protein     INT             NOT NULL DEFAULT 0,
    fat         INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_menu_nutrient_menu FOREIGN KEY (menu_id)
        REFERENCES menu (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Orders ───────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    order_number    VARCHAR(50)     NOT NULL UNIQUE,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    total_price     INT             NOT NULL,
    ordered_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── OrderItem ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS order_item (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    order_id    BIGINT          NOT NULL,
    menu_id     BIGINT          NOT NULL,
    quantity    INT             NOT NULL,
    unit_price  INT             NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id)
        REFERENCES orders (id),
    CONSTRAINT fk_order_item_menu  FOREIGN KEY (menu_id)
        REFERENCES menu (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Payment ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payment (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    order_id            BIGINT          NOT NULL UNIQUE,
    pg_provider         VARCHAR(20)     NOT NULL,
    pg_transaction_id   VARCHAR(200)    NOT NULL UNIQUE,
    method              VARCHAR(20)     NOT NULL,
    amount              INT             NOT NULL,
    paid_at             DATETIME        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id)
        REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;