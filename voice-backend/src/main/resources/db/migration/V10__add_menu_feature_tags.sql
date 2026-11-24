-- 추천용 특징 태그: description_ko / description_en 끝에 한 줄을 덧붙인다. (기존 문구는 유지)
-- 태그 없음: 고소함, 1인 추천(팀 결정으로 제외). 매콤함은 5번만.
-- 주의: 키오스크 화면에 설명이 그대로 노출되면 '특징:' 줄도 고객에게 보인다.

UPDATE menu SET description_ko = CONCAT(description_ko, ' 특징: 달콤함, 부드러움, 간편함, 아이 추천'),
    description_en = CONCAT(description_en, ' Features: Sweet, Soft, Easy to carry, Kid-friendly')
WHERE id = 1 AND name_ko = '데리버거';

UPDATE menu SET description_ko = CONCAT(description_ko, ' 특징: 산뜻함, 바삭함, 간편함, 아이 추천, 대표 메뉴'),
    description_en = CONCAT(description_en, ' Features: Refreshing, Crispy, Easy to carry, Kid-friendly, Signature')
WHERE id = 2 AND name_ko = '리아 새우버거';

UPDATE menu SET description_ko = CONCAT(description_ko, ' 특징: 달콤함, 부드러움, 채소 많음, 간편함, 아이 추천, 대표 메뉴'),
    description_en = CONCAT(description_en, ' Features: Sweet, Soft, Lots of vegetables, Easy to carry, Kid-friendly, Signature')
WHERE id = 3 AND name_ko = '리아 불고기버거';

UPDATE menu SET description_ko = CONCAT(description_ko, ' 특징: 부드러움, 간편함'),
    description_en = CONCAT(description_en, ' Features: Soft, Easy to carry')
WHERE id = 4 AND name_ko = '클래식 치즈버거';

UPDATE menu SET description_ko = CONCAT(description_ko, ' 특징: 매콤함, 바삭함'),
    description_en = CONCAT(description_en, ' Features: Spicy, Crispy')
WHERE id = 5 AND name_ko = '핫크리스피 치킨버거';

UPDATE menu SET description_ko = CONCAT(description_ko, ' 특징: 달콤함, 부드러움'),
    description_en = CONCAT(description_en, ' Features: Sweet, Soft')
WHERE id = 6 AND name_ko = '더블 데리버거';

UPDATE menu SET description_ko = CONCAT(description_ko, ' 특징: 부드러움'),
    description_en = CONCAT(description_en, ' Features: Soft')
WHERE id = 7 AND name_ko = '더블 클래식 치즈버거';

UPDATE menu SET description_ko = CONCAT(description_ko, ' 특징: 바삭함'),
    description_en = CONCAT(description_en, ' Features: Crispy')
WHERE id = 8 AND name_ko = '모짜렐라 베이컨버거';

UPDATE menu SET description_ko = CONCAT(description_ko, ' 특징: 바삭함, 아이 추천'),
    description_en = CONCAT(description_en, ' Features: Crispy, Kid-friendly')
WHERE id = 9 AND name_ko = '양념감자';

UPDATE menu SET description_ko = CONCAT(description_ko, ' 특징: 바삭함'),
    description_en = CONCAT(description_en, ' Features: Crispy')
WHERE id = 10 AND name_ko = '감자튀김';

UPDATE menu SET description_ko = CONCAT(description_ko, ' 특징: 바삭함, 간편함, 아이 추천'),
    description_en = CONCAT(description_en, ' Features: Crispy, Easy to carry, Kid-friendly')
WHERE id = 11 AND name_ko = '치즈스틱';

UPDATE menu SET description_ko = CONCAT(description_ko, ' 특징: 바삭함, 간편함'),
    description_en = CONCAT(description_en, ' Features: Crispy, Easy to carry')
WHERE id = 12 AND name_ko = '통오징어링';

UPDATE menu SET description_ko = CONCAT(description_ko, ' 특징: 바삭함'),
    description_en = CONCAT(description_en, ' Features: Crispy')
WHERE id = 13 AND name_ko = '치킨너겟';

UPDATE menu SET description_ko = CONCAT(description_ko, ' 특징: 달콤함, 간편함, 아이 추천'),
    description_en = CONCAT(description_en, ' Features: Sweet, Easy to carry, Kid-friendly')
WHERE id = 14 AND name_ko = '콜라';

UPDATE menu SET description_ko = CONCAT(description_ko, ' 특징: 달콤함, 간편함, 아이 추천'),
    description_en = CONCAT(description_en, ' Features: Sweet, Easy to carry, Kid-friendly')
WHERE id = 15 AND name_ko = '제로슈거콜라';

UPDATE menu SET description_ko = CONCAT(description_ko, ' 특징: 달콤함, 간편함, 아이 추천'),
    description_en = CONCAT(description_en, ' Features: Sweet, Easy to carry, Kid-friendly')
WHERE id = 16 AND name_ko = '사이다';

UPDATE menu SET description_ko = CONCAT(description_ko, ' 특징: 간편함'),
    description_en = CONCAT(description_en, ' Features: Easy to carry')
WHERE id = 17 AND name_ko = '아이스아메리카노';
