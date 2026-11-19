-- V9__add_multilang_names.sql
-- 다국어(ja / es / th) 이름 컬럼 추가 및 기존 데이터 번역
-- 언어 코드: ko / en(기존) / ja / es / th
-- 컬럼은 NULL 허용: 번역이 없는 항목(관리자가 새로 추가한 메뉴 등)은 API에서 name_en으로 대체한다.

-- ── 1. 컬럼 추가 ────────────────────────────────────────────────────────────
ALTER TABLE menu
    ADD COLUMN name_ja VARCHAR(100) NULL AFTER name_en,
    ADD COLUMN name_es VARCHAR(100) NULL AFTER name_ja,
    ADD COLUMN name_th VARCHAR(100) NULL AFTER name_es;

ALTER TABLE menu_option
    ADD COLUMN name_ja VARCHAR(100) NULL AFTER name_en,
    ADD COLUMN name_es VARCHAR(100) NULL AFTER name_ja,
    ADD COLUMN name_th VARCHAR(100) NULL AFTER name_es;

ALTER TABLE category
    ADD COLUMN name_ja VARCHAR(100) NULL AFTER name_en,
    ADD COLUMN name_es VARCHAR(100) NULL AFTER name_ja,
    ADD COLUMN name_th VARCHAR(100) NULL AFTER name_es;

ALTER TABLE allergen
    ADD COLUMN name_ja VARCHAR(100) NULL AFTER name_en,
    ADD COLUMN name_es VARCHAR(100) NULL AFTER name_ja,
    ADD COLUMN name_th VARCHAR(100) NULL AFTER name_es;

-- ── 2. 메뉴 번역 (id 1~17) ──────────────────────────────────────────────────
UPDATE menu SET name_ja = 'テリバーガー',                   name_es = 'Hamburguesa Teri',                       name_th = 'เทริเบอร์เกอร์'                 WHERE id = 1;
UPDATE menu SET name_ja = 'リア エビバーガー',               name_es = 'Hamburguesa de camarón Ria',             name_th = 'ริอา เบอร์เกอร์กุ้ง'            WHERE id = 2;
UPDATE menu SET name_ja = 'リア プルコギバーガー',           name_es = 'Hamburguesa Bulgogi Ria',                name_th = 'ริอา บูลโกกิเบอร์เกอร์'         WHERE id = 3;
UPDATE menu SET name_ja = 'クラシックチーズバーガー',        name_es = 'Hamburguesa clásica con queso',          name_th = 'คลาสสิกชีสเบอร์เกอร์'          WHERE id = 4;
UPDATE menu SET name_ja = 'ホットクリスピーチキンバーガー',  name_es = 'Hamburguesa de pollo crujiente picante', name_th = 'ฮอตคริสปี้ชิคเก้นเบอร์เกอร์'    WHERE id = 5;
UPDATE menu SET name_ja = 'ダブルテリバーガー',              name_es = 'Hamburguesa Teri doble',                 name_th = 'ดับเบิลเทริเบอร์เกอร์'          WHERE id = 6;
UPDATE menu SET name_ja = 'ダブルクラシックチーズバーガー',  name_es = 'Hamburguesa clásica con queso doble',    name_th = 'ดับเบิลคลาสสิกชีสเบอร์เกอร์'    WHERE id = 7;
UPDATE menu SET name_ja = 'モッツァレラベーコンバーガー',    name_es = 'Hamburguesa de mozzarella y beicon',     name_th = 'มอสซาเรลลาเบคอนเบอร์เกอร์'     WHERE id = 8;
UPDATE menu SET name_ja = 'シーズニングポテト',              name_es = 'Patatas sazonadas',                      name_th = 'มันฝรั่งทอดปรุงรส'              WHERE id = 9;
UPDATE menu SET name_ja = 'フライドポテト',                  name_es = 'Patatas fritas',                         name_th = 'เฟรนช์ฟรายส์'                   WHERE id = 10;
UPDATE menu SET name_ja = 'チーズスティック',                name_es = 'Palitos de queso',                       name_th = 'ชีสสติ๊ก'                       WHERE id = 11;
UPDATE menu SET name_ja = 'イカリング',                      name_es = 'Aros de calamar',                        name_th = 'หมึกวงทอด'                      WHERE id = 12;
UPDATE menu SET name_ja = 'チキンナゲット',                  name_es = 'Nuggets de pollo',                       name_th = 'ไก่นักเก็ต'                     WHERE id = 13;
UPDATE menu SET name_ja = 'コーラ',                          name_es = 'Cola',                                   name_th = 'โคล่า'                          WHERE id = 14;
UPDATE menu SET name_ja = 'ゼロシュガーコーラ',              name_es = 'Cola sin azúcar',                        name_th = 'โคล่าซีโร่ชูการ์'               WHERE id = 15;
UPDATE menu SET name_ja = 'サイダー',                        name_es = 'Refresco de lima-limón',                 name_th = 'น้ำอัดลมรสเลมอนไลม์'            WHERE id = 16;
UPDATE menu SET name_ja = 'アイスアメリカーノ',              name_es = 'Americano helado',                       name_th = 'อเมริกาโน่เย็น'                 WHERE id = 17;

-- ── 3. 메뉴 옵션 번역 (메뉴마다 복제된 행이라 이름 기준으로 일괄 변경) ─────
UPDATE menu_option SET name_ja = 'チーズ追加',        name_es = 'Extra de queso',      name_th = 'เพิ่มชีส'                  WHERE name_ko = '치즈 추가';
UPDATE menu_option SET name_ja = 'トマト追加',        name_es = 'Extra de tomate',     name_th = 'เพิ่มมะเขือเทศ'            WHERE name_ko = '토마토 추가';
UPDATE menu_option SET name_ja = 'ベーコン追加',      name_es = 'Extra de beicon',     name_th = 'เพิ่มเบคอน'                WHERE name_ko = '베이컨 추가';
UPDATE menu_option SET name_ja = 'ビーフパティ追加',  name_es = 'Extra de carne de res', name_th = 'เพิ่มเนื้อบีฟแพตตี้'    WHERE name_ko = '비프패티 추가';
UPDATE menu_option SET name_ja = '半熟卵追加',        name_es = 'Extra de huevo poco hecho', name_th = 'เพิ่มไข่ยางมะตูม'   WHERE name_ko = '반숙계란 추가';
UPDATE menu_option SET name_ja = 'トマト抜き',        name_es = 'Sin tomate',          name_th = 'ไม่ใส่มะเขือเทศ'           WHERE name_ko = '토마토 제거';
UPDATE menu_option SET name_ja = '玉ねぎ抜き',        name_es = 'Sin cebolla',         name_th = 'ไม่ใส่หอมหัวใหญ่'          WHERE name_ko = '양파 제거';
UPDATE menu_option SET name_ja = 'ソース少なめ',      name_es = 'Menos salsa',         name_th = 'ซอสน้อย'                   WHERE name_ko = '소스 적게';
UPDATE menu_option SET name_ja = 'オニオンソース',    name_es = 'Salsa de cebolla',    name_th = 'ซอสหัวหอม'                 WHERE name_ko = '어니언 소스';
UPDATE menu_option SET name_ja = 'チーズソース',      name_es = 'Salsa de queso',      name_th = 'ซอสชีส'                    WHERE name_ko = '치즈 소스';
UPDATE menu_option SET name_ja = 'チリソース',        name_es = 'Salsa picante',       name_th = 'ซอสพริก'                   WHERE name_ko = '칠리 소스';

-- ── 4. 카테고리 번역 (id 1~3) ───────────────────────────────────────────────
UPDATE category SET name_ja = 'バーガー', name_es = 'Hamburguesas',     name_th = 'เบอร์เกอร์'  WHERE id = 1;
UPDATE category SET name_ja = 'サイド',   name_es = 'Acompañamientos',  name_th = 'เมนูเสริม'   WHERE id = 2;
UPDATE category SET name_ja = 'ドリンク', name_es = 'Bebidas',          name_th = 'เครื่องดื่ม' WHERE id = 3;

-- ── 5. 알레르기 번역 (id 1~13) ──────────────────────────────────────────────
UPDATE allergen SET name_ja = '卵',       name_es = 'Huevo',          name_th = 'ไข่'          WHERE id = 1;
UPDATE allergen SET name_ja = '小麦',     name_es = 'Trigo',          name_th = 'ข้าวสาลี'     WHERE id = 2;
UPDATE allergen SET name_ja = '大豆',     name_es = 'Soja',           name_th = 'ถั่วเหลือง'   WHERE id = 3;
UPDATE allergen SET name_ja = '乳',       name_es = 'Leche',          name_th = 'นม'           WHERE id = 4;
UPDATE allergen SET name_ja = '牛肉',     name_es = 'Carne de res',   name_th = 'เนื้อวัว'     WHERE id = 5;
UPDATE allergen SET name_ja = '鶏肉',     name_es = 'Pollo',          name_th = 'เนื้อไก่'     WHERE id = 6;
UPDATE allergen SET name_ja = 'トマト',   name_es = 'Tomate',         name_th = 'มะเขือเทศ'    WHERE id = 7;
UPDATE allergen SET name_ja = '豚肉',     name_es = 'Cerdo',          name_th = 'เนื้อหมู'     WHERE id = 8;
UPDATE allergen SET name_ja = 'えび',     name_es = 'Camarón',        name_th = 'กุ้ง'         WHERE id = 9;
UPDATE allergen SET name_ja = '落花生',   name_es = 'Cacahuete',      name_th = 'ถั่วลิสง'     WHERE id = 10;
UPDATE allergen SET name_ja = '貝類',     name_es = 'Moluscos',       name_th = 'หอย'          WHERE id = 11;
UPDATE allergen SET name_ja = 'いか',     name_es = 'Calamar',        name_th = 'หมึก'         WHERE id = 12;
UPDATE allergen SET name_ja = '亜硫酸塩', name_es = 'Sulfitos',       name_th = 'ซัลไฟต์'      WHERE id = 13;

-- 적용 후 확인용:
--   SELECT id, name_ko, name_ja, name_es, name_th FROM menu;
--   SELECT COUNT(*) FROM menu_option WHERE name_ja IS NULL;   -- 0이어야 함