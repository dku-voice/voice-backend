-- V2__sample_data_and_menu_option.sql
-- 롯데리아 공식 영양성분표(2026.05.21) 기준
-- 출처: https://www.lotteeatz.com/upload/stg/etc/ria/items.html

-- ============================================================
-- 1. 카테고리
-- ============================================================
INSERT INTO category (id, name_ko, name_en, display_order) VALUES
(1, '버거',   'Burger', 1),
(2, '사이드', 'Side',   2),
(3, '음료',   'Drink',  3);

-- ============================================================
-- 2. 알레르기 (롯데리아 공식 항목 기준)
-- ============================================================
INSERT INTO allergen (id, name_ko, name_en) VALUES
(1,  '달걀',     'Egg'),
(2,  '밀',       'Wheat'),
(3,  '대두',     'Soy'),
(4,  '우유',     'Milk'),
(5,  '쇠고기',   'Beef'),
(6,  '닭고기',   'Chicken'),
(7,  '토마토',   'Tomato'),
(8,  '돼지고기', 'Pork'),
(9,  '새우',     'Shrimp'),
(10, '땅콩',     'Peanut'),
(11, '조개류',   'Shellfish'),
(12, '오징어',   'Squid'),
(13, '아황산류', 'Sulfites');

-- ============================================================
-- 3. 메뉴
-- ============================================================
INSERT INTO menu (id, category_id, name_ko, name_en, description_ko, description_en, price, status, image_url) VALUES

-- 버거 (공식 단품 기준)
(1,  1, '데리버거',             'Teri Burger',
     '달콤한 데리야끼 소스와 부드러운 패티의 만남', 'Soft patty with sweet teriyaki sauce',
     2800, 'ACTIVE', 'https://placehold.co/400x300?text=Teri+Burger'),

(2,  1, '리아 새우버거',         'Ria Shrimp Burger',
     '통통한 새우 패티와 상큼한 토마토 소스', 'Plump shrimp patty with tangy tomato sauce',
     4200, 'ACTIVE', 'https://placehold.co/400x300?text=Shrimp+Burger'),

(3,  1, '리아 불고기버거',        'Ria Bulgogi Burger',
     '불고기 양념 패티와 신선한 채소의 조화', 'Bulgogi-seasoned patty with fresh vegetables',
     3800, 'ACTIVE', 'https://placehold.co/400x300?text=Bulgogi+Burger'),

(4,  1, '클래식 치즈버거',        'Classic Cheese Burger',
     '두툼한 비프 패티에 진한 체다 치즈', 'Thick beef patty with rich cheddar cheese',
     4000, 'ACTIVE', 'https://placehold.co/400x300?text=Classic+Cheese'),

(5,  1, '핫크리스피 치킨버거',    'Hot Crispy Chicken Burger',
     '바삭한 치킨 필레와 매콤한 소스', 'Crispy chicken fillet with spicy sauce',
     4500, 'ACTIVE', 'https://placehold.co/400x300?text=Crispy+Chicken'),

(6,  1, '더블 데리버거',          'Double Teri Burger',
     '데리야끼 패티 두 장으로 더욱 든든하게', 'Double teriyaki patties for a more satisfying meal',
     4200, 'ACTIVE', 'https://placehold.co/400x300?text=Double+Teri'),

(7,  1, '더블 클래식 치즈버거',   'Double Classic Cheese Burger',
     '패티 두 장과 풍성한 치즈의 프리미엄 버거', 'Premium burger with double patties and generous cheese',
     5500, 'ACTIVE', 'https://placehold.co/400x300?text=Double+Classic'),

(8,  1, '모짜렐라 베이컨버거',    'Mozzarella Bacon Burger',
     '쭉 늘어나는 모짜렐라와 바삭한 베이컨', 'Stretchy mozzarella with crispy bacon',
     5800, 'ACTIVE', 'https://placehold.co/400x300?text=Mozzarella+Bacon'),

-- 사이드
(9,  2, '양념감자',   'Seasoned Fries',
     '어니언·치즈·칠리 소스 선택 가능한 양념 감자', 'Seasoned fries: choice of onion, cheese, or chili sauce',
     2700, 'ACTIVE', 'https://placehold.co/400x300?text=Seasoned+Fries'),

(10, 2, '감자튀김',   'French Fries',
     '바삭하게 튀긴 황금빛 감자튀김', 'Crispy golden french fries',
     2200, 'ACTIVE', 'https://placehold.co/400x300?text=French+Fries'),

(11, 2, '치즈스틱',   'Cheese Sticks',
     '쭉 늘어나는 모짜렐라 치즈스틱', 'Stretchy mozzarella cheese sticks',
     2900, 'ACTIVE', 'https://placehold.co/400x300?text=Cheese+Sticks'),

(12, 2, '통오징어링', 'Calamari Rings',
     '쫄깃한 오징어를 통째로 튀긴 오징어링', 'Whole squid rings fried to perfection',
     3200, 'ACTIVE', 'https://placehold.co/400x300?text=Calamari+Rings'),

(13, 2, '치킨너겟',   'Chicken Nuggets',
     '촉촉하고 바삭한 치킨너겟 6조각', 'Juicy and crispy chicken nuggets (6 pieces)',
     2900, 'ACTIVE', 'https://placehold.co/400x300?text=Chicken+Nuggets');

-- ============================================================
-- 4. 메뉴-알레르기 매핑 (롯데리아 공식 기준)
-- ============================================================
INSERT INTO menu_allergen (menu_id, allergen_id) VALUES
-- 데리버거: 달걀 밀 대두 우유 쇠고기 닭고기 조개류
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,11),
-- 리아 새우버거: 달걀 밀 대두 우유 토마토 새우
(2,1),(2,2),(2,3),(2,4),(2,7),(2,9),
-- 리아 불고기버거: 달걀 밀 대두 우유 쇠고기 토마토 돼지고기 닭고기
(3,1),(3,2),(3,3),(3,4),(3,5),(3,7),(3,8),(3,6),
-- 클래식 치즈버거: 달걀 밀 대두 우유 쇠고기
(4,1),(4,2),(4,3),(4,4),(4,5),
-- 핫크리스피 치킨버거: 달걀 밀 대두 닭고기 토마토
(5,1),(5,2),(5,3),(5,6),(5,7),
-- 더블 데리버거: 달걀 밀 대두 우유 쇠고기 닭고기 조개류
(6,1),(6,2),(6,3),(6,4),(6,5),(6,6),(6,11),
-- 더블 클래식 치즈버거: 달걀 밀 대두 우유 쇠고기
(7,1),(7,2),(7,3),(7,4),(7,5),
-- 모짜렐라 베이컨버거: 달걀 밀 대두 우유 쇠고기 돼지고기
(8,1),(8,2),(8,3),(8,4),(8,5),(8,8),
-- 양념감자: 대두 토마토
(9,3),(9,7),
-- 감자튀김: 대두
(10,3),
-- 치즈스틱: 달걀 밀 대두 우유
(11,1),(11,2),(11,3),(11,4),
-- 통오징어링: 달걀 밀 대두 오징어
(12,1),(12,2),(12,3),(12,12),
-- 치킨너겟: 달걀 밀 대두 닭고기 쇠고기
(13,1),(13,2),(13,3),(13,6),(13,5);

-- ============================================================
-- 5. 영양정보
-- 전체: calories·protein·fat·carbs → fatsecret.kr 기준
--       sodium → 롯데리아 공식(lotteeatz.com) 기준
-- calories: kcal / protein·fat·carbs: g / sodium: mg
-- ============================================================
INSERT INTO menu_nutrient (menu_id, calories, protein, fat, carbs, sodium) VALUES
(1,  356, 12.0, 16.0, 42.0,  590),   -- 데리버거
(2,  492, 15.0, 26.0, 46.0,  900),   -- 리아 새우버거
(3,  476, 20.0, 22.0, 50.0,  880),   -- 리아 불고기버거
(4,  472, 15.0, 13.5, 72.0,  710),   -- 클래식 치즈버거
(5,  503, 22.0,  7.3, 82.0,  900),   -- 핫크리스피 치킨버거
(6,  446, 19.0, 20.5, 46.2,  740),   -- 더블 데리버거
(7,  731, 25.0, 35.0, 78.8, 1030),   -- 더블 클래식 치즈버거
(8,  715, 30.0, 18.7, 50.0, 1000),   -- 모짜렐라 베이컨버거
(9,  359,  5.0,  4.3,  1.0,  520),   -- 양념감자
(10, 267,  4.0, 12.0, 36.0,  380),   -- 감자튀김
(11, 160,  8.7,  9.4, 12.2,  530),   -- 치즈스틱 (2개 기준)
(12, 166,  8.0,  0.5,  8.0,  490),   -- 통오징어링
(13, 198, 12.0,  3.3,  0.0,  510);   -- 치킨너겟

-- ============================================================
-- 6. menu_option 테이블 생성
-- ============================================================
CREATE TABLE menu_option (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    menu_id       BIGINT      NOT NULL,
    name_ko       VARCHAR(50) NOT NULL,
    name_en       VARCHAR(50) NOT NULL,
    option_type   VARCHAR(10) NOT NULL,  -- 'ADD' | 'REMOVE'
    extra_price   INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_menu_option_menu
        FOREIGN KEY (menu_id) REFERENCES menu(id)
        ON DELETE CASCADE
);

-- ============================================================
-- 7. order_item_option 테이블 생성
-- ============================================================
CREATE TABLE order_item_option (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    order_item_id  BIGINT NOT NULL,
    menu_option_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_oio_order_item
        FOREIGN KEY (order_item_id) REFERENCES order_item(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_oio_menu_option
        FOREIGN KEY (menu_option_id) REFERENCES menu_option(id)
        ON DELETE CASCADE
);

-- ============================================================
-- 8. 메뉴 옵션 데이터
-- ============================================================

-- ── 버거(1~8) ADD 토핑 (롯데리아 공식 5종) ──────────────────
INSERT INTO menu_option (menu_id, name_ko, name_en, option_type, extra_price)
SELECT id, '치즈 추가',    'Add Cheese',     'ADD', 500  FROM menu WHERE id BETWEEN 1 AND 8;

INSERT INTO menu_option (menu_id, name_ko, name_en, option_type, extra_price)
SELECT id, '토마토 추가',  'Add Tomato',     'ADD', 300  FROM menu WHERE id BETWEEN 1 AND 8;

INSERT INTO menu_option (menu_id, name_ko, name_en, option_type, extra_price)
SELECT id, '베이컨 추가',  'Add Bacon',      'ADD', 700  FROM menu WHERE id BETWEEN 1 AND 8;

INSERT INTO menu_option (menu_id, name_ko, name_en, option_type, extra_price)
SELECT id, '비프패티 추가','Add Beef Patty', 'ADD', 1500 FROM menu WHERE id BETWEEN 1 AND 8;

INSERT INTO menu_option (menu_id, name_ko, name_en, option_type, extra_price)
SELECT id, '반숙계란 추가','Add Soft Egg',   'ADD', 500  FROM menu WHERE id BETWEEN 1 AND 8;

-- ── 버거(1~8) REMOVE 옵션 ────────────────────────────────────
INSERT INTO menu_option (menu_id, name_ko, name_en, option_type, extra_price)
SELECT id, '토마토 제거', 'No Tomato', 'REMOVE', 0 FROM menu WHERE id BETWEEN 1 AND 8;

INSERT INTO menu_option (menu_id, name_ko, name_en, option_type, extra_price)
SELECT id, '양파 제거',   'No Onion',  'REMOVE', 0 FROM menu WHERE id BETWEEN 1 AND 8;

INSERT INTO menu_option (menu_id, name_ko, name_en, option_type, extra_price)
SELECT id, '소스 적게',   'Less Sauce','REMOVE', 0 FROM menu WHERE id BETWEEN 1 AND 8;

-- ── 양념감자(9) 소스 선택 (3종 중 1택) ──────────────────────
INSERT INTO menu_option (menu_id, name_ko, name_en, option_type, extra_price) VALUES
(9, '어니언 소스', 'Onion Sauce',  'ADD', 0),
(9, '치즈 소스',   'Cheese Sauce', 'ADD', 0),
(9, '칠리 소스',   'Chili Sauce',  'ADD', 0);