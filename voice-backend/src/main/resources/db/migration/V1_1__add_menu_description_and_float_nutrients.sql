-- V1에 있던 menu/menu_nutrient 변경을 별도 마이그레이션으로 분리
ALTER TABLE menu
    ADD COLUMN description_ko VARCHAR(255) NULL AFTER name_en,
    ADD COLUMN description_en VARCHAR(255) NULL AFTER description_ko;

ALTER TABLE menu_nutrient
    MODIFY COLUMN carbs   FLOAT NOT NULL DEFAULT 0,
    MODIFY COLUMN protein FLOAT NOT NULL DEFAULT 0,
    MODIFY COLUMN fat     FLOAT NOT NULL DEFAULT 0;