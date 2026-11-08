-- 서비스에 꼭 필요한 기본 데이터. 운영 DB에도 들어간다. 지역은 산책을 기록할 때 자동으로 추가된다.
INSERT INTO experience_tag (tag_id, tag_key, name, phrase, icon, sort_order) VALUES
    (1, 'slow-walk', '천천히 걷기', '천천히 걷고 싶어요', 'walk', 1),
    (2, 'rest', '혼자 쉬기', '혼자 쉬고 싶어요', 'rest', 2),
    (3, 'explore', '동네 알아가기', '동네를 알아가고 싶어요', 'explore', 3);
