UPDATE menu SET name_ko = '새우버거', name_en = 'Shrimp Burger',
    name_ja = 'エビバーガー', name_es = 'Hamburguesa de camarón', name_th = 'เบอร์เกอร์กุ้ง',
    description_ko = REPLACE(description_ko, '상큼한 토마토 소스', '고소한 타르타르 소스'),
    description_en = REPLACE(description_en, 'tangy tomato sauce', 'creamy tartar sauce')
WHERE id = 2;

UPDATE menu SET name_ko = '불고기버거', name_en = 'Bulgogi Burger',
    name_ja = 'プルコギバーガー', name_es = 'Hamburguesa Bulgogi', name_th = 'บูลโกกิเบอร์เกอร์'
WHERE id = 3;

UPDATE menu SET name_en = 'Teriyaki Burger' WHERE id = 1;
UPDATE menu SET name_en = 'Classic Cheeseburger' WHERE id = 4;
UPDATE menu SET name_en = 'Double Teriyaki Burger' WHERE id = 6;
UPDATE menu SET name_en = 'Double Classic Cheeseburger' WHERE id = 7;
UPDATE menu SET name_ko = '모짜렐라 베이컨 버거' WHERE id = 8;
UPDATE menu SET name_ko = '오징어링', name_en = 'Squid Rings' WHERE id = 12;
