UPDATE menu
SET name_ko = '콜라',
    name_en = 'Cola',
    description_ko = REPLACE(description_ko, '펩시콜라', '콜라'),
    description_en = REPLACE(description_en, 'Pepsi Cola', 'Cola')
WHERE name_ko = '펩시콜라';