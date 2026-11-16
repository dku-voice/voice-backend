-- V6__add_drink_menus.sql  (초안: 가격을 채운 뒤 확장자 '.draft'를 지우고 db/migration/에 넣을 것)
-- 음료 메뉴 4종 추가 (카테고리 3번 '음료'는 V2에서 이미 생성됨, 메뉴는 0개였음)
--
-- 데이터 출처: 롯데리아 영양성분표(배달용, 2026.10.29 기준)
--   https://www.lotteeatz.com/upload/stg/etc/ria/items.html
--   → 열량(kcal), 단백질(g), 나트륨(mg)은 공식 값 그대로. 4종 모두 공식 표의 알레르기 항목이 '-'(없음)
--   → 지방(g)·탄수화물(g)은 공식 표에 없음(표에는 당류, 포화지방만 있음).
--     콜라·사이다는 지방 0, 탄수화물 = 당류 값으로 '근사'했음 (기존 메뉴는 fatsecret 값을 썼으므로 맞출지 결정 필요)
--   → 가격은 공식 표에 없어서 비워 둠. 'PRICE'로 표시된 4곳을 채울 것
--
-- 적용 전 체크: 가격 자리에 NULL 표시(골뱅이 두 개로 감싼 PRICE 문구)가 남아 있지 않아야 함

-- ── 메뉴 (카테고리 3 = 음료, R 사이즈 기준) ───────────────────────────────
INSERT INTO menu (id, category_id, name_ko, name_en, description_ko, description_en, price, status, image_url) VALUES
(14, 3, '펩시콜라',         'Pepsi Cola',
     '시원하고 청량한 탄산음료', 'Cool and refreshing carbonated drink',
     2100, 'ACTIVE', 'https://dku-voice-assets.s3.ap-southeast-2.amazonaws.com/pepsi_cola.png'),

(15, 3, '제로슈거콜라',     'Zero Sugar Cola',
     '당 걱정 없는 제로슈거 콜라', 'Zero sugar cola with no sugar and no calories',
     2100, 'ACTIVE', 'https://dku-voice-assets.s3.ap-southeast-2.amazonaws.com/zero_sugar_cola.png'),

(16, 3, '사이다',           'Cider',
     '깔끔하고 시원한 탄산음료', 'Clean and refreshing lemon-lime soda',
     2100, 'ACTIVE', 'https://dku-voice-assets.s3.ap-southeast-2.amazonaws.com/cider.png'),

(17, 3, '아이스아메리카노', 'Iced Americano',
     '깔끔하게 즐기는 아이스 아메리카노 (고카페인 함유)', 'Clean iced americano (high caffeine)',
     2600, 'ACTIVE', 'https://dku-voice-assets.s3.ap-southeast-2.amazonaws.com/iced_americano.png');

-- ── 영양정보: (menu_id, calories, protein, fat, carbs, sodium) ───────────────
-- calories·protein·sodium = 공식 표 / fat·carbs = 근사값(위 설명 참고)
INSERT INTO menu_nutrient (menu_id, calories, protein, fat, carbs, sodium) VALUES
(14, 128, 0, 0, 31, 15),   -- 펩시콜라(R)       420g
(15,   0, 0, 0,  0, 55),   -- 제로슈거콜라(R)   420g
(16, 133, 0, 0, 31,  5),   -- 사이다(R)         420g
(17,   5, 0, 0,  0,  0);   -- 아이스아메리카노  340g (카페인 52mg, 컬럼이 없어 설명에만 표기)

-- 알레르기 매핑(menu_allergen)은 4종 모두 해당 없음이라 추가하지 않음.
-- 음료 옵션(menu_option)도 추가하지 않음 (사이드 10~13번과 동일).
