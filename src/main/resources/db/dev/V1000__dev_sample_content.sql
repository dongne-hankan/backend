-- 개발용 샘플 데이터. local·test 프로필에서만 적용한다.
-- 주민 이야기는 화면 시안의 가상 예시이고, 실제 인터뷰가 아니다. 좌표와 주소는 확인되지 않은 값이다.

INSERT INTO region (region_id, region_key, name, province, center_lat, center_lng) VALUES
    (1, 'suseong', '수성구', 'DAEGU', 35.8581000, 128.6306000),
    (2, 'andong', '안동시', 'GYEONGBUK', 36.5684000, 128.7294000),
    (3, 'gyeongju', '경주시', 'GYEONGBUK', 35.8562000, 129.2247000);

INSERT INTO experience_tag (tag_id, tag_key, name, phrase, icon, sort_order) VALUES
    (1, 'slow-walk', '천천히 걷기', '천천히 걷고 싶어요', 'walk', 1),
    (2, 'rest', '혼자 쉬기', '혼자 쉬고 싶어요', 'rest', 2),
    (3, 'explore', '동네 알아가기', '동네를 알아가고 싶어요', 'explore', 3);

INSERT INTO place (place_id, place_key, name, address, lat, lng, checked_on, time_sensitive, source_url, visible) VALUES
    (101, 'suseong-lake', '수성못', '대구 수성구 두산동', 35.8272000, 128.6175000, '2026-11-01', 0, 'https://www.mmj.suseong.kr/pages/sub.htm?nav_code=new1708668244', 1),
    (102, 'sanghwa-park', '상화동산', '대구 수성구 두산동', 35.8291000, 128.6158000, '2026-11-01', 0, NULL, 1),
    (201, 'woryeonggyo', '월영교', '경북 안동시 상아동', 36.5757000, 128.7661000, '2026-11-01', 0, 'https://www.tourandong.com/public/sub1/sub3_2.cshtml', 1),
    (202, 'andong-folk-museum', '안동민속박물관', '경북 안동시 성곡동', 36.5726000, 128.7644000, '2026-11-01', 1, NULL, 1),
    (301, 'hwangseong-park', '황성공원', '경북 경주시 황성동', 35.8566000, 129.2175000, '2026-11-01', 0, 'https://gyeongju.go.kr/tour/page.do?area_uid=105&cmd=2&code_uid=1010&mnu_uid=2296', 1);

INSERT INTO resident_story (story_id, story_key, place_id, resident_intro, reason, how_to_enjoy, consent_scope, visible) VALUES
    (501, 'story-suseong-lake', 101, '두산동 주민 (가상 예시)', '멀리 나갈 힘은 없어도, 물가를 조금 걷고 나면 하루가 정리되는 느낌이에요.', '물가를 따라 짧게 걷고, 마음에 드는 풍경 앞에서 잠깐 멈춰 보세요.', 'INTRO_ONLY', 1),
    (502, 'story-sanghwa-park', 102, '두산동 주민 (가상 예시)', '못을 한 바퀴 돌고 나서 여기 앉아 있으면 마음이 차분해져요.', '벤치에 앉아 못 쪽을 바라보며 잠깐 쉬어 가세요.', 'INTRO_ONLY', 1),
    (503, 'story-woryeonggyo', 201, '상아동 주민 (가상 예시)', '같은 물가라도 매번 보이는 모습이 달라요. 잠깐 보고 오는 것만으로도 기분이 바뀌어요.', '다리와 주변 풍경을 보며 천천히 걷고, 눈에 들어온 장면 하나를 기억해 보세요.', 'INTRO_ONLY', 1),
    (504, 'story-folk-museum', 202, '성곡동 주민 (가상 예시)', '이 동네가 어떻게 살아왔는지 조금 알고 나면 길이 다르게 보여요.', '전시를 천천히 본 뒤 박물관 앞길을 걸어 보세요.', 'INTRO_ONLY', 1),
    (505, 'story-hwangseong-park', 301, '황성동 주민 (가상 예시)', '특별히 할 일이 없어도 나무 사이를 걸어요. 아무것도 안 해도 되는 시간이 좋거든요.', '나무가 있는 산책로를 따라 걷고, 벤치에서 잠깐 쉬어 가세요.', 'INTRO_ONLY', 1);

INSERT INTO course (course_id, course_key, region_id, name, intro, transport, duration_minutes, visible) VALUES
    (1, 'suseong-walk-30', 1, '수성못 물가 따라 천천히', '퇴근길에 잠깐 걷기 좋은 물가 산책', '도보', 30, 1),
    (2, 'suseong-rest-30', 1, '수성못에서 잠깐 쉬어가기', '휴대폰을 내려놓고 물가에 머무는 시간', '도보', 30, 1),
    (3, 'suseong-walk-60', 1, '수성못 한 바퀴와 상화동산', '못을 돌고 동산에서 숨을 고르는 저녁', '도보', 60, 1),
    (4, 'andong-walk-30', 2, '월영교 물가 산책', '다리 위에서 바라보는 안동의 풍경', '도보', 30, 1),
    (5, 'andong-explore-60', 2, '월영교에서 민속박물관까지', '물가를 지나 동네의 시간을 알아가는 길', '도보', 60, 1),
    (6, 'gyeongju-rest-30', 3, '황성공원 벤치에서 쉬기', '나무 곁에서 아무것도 안 해도 되는 시간', '도보', 30, 1),
    (7, 'gyeongju-walk-60', 3, '황성공원 숲길 걷기', '솔숲을 따라 내 속도로 걷는 한 시간', '도보', 60, 1),
    (8, 'gyeongju-hidden-30', 3, '숨김 처리된 샘플 코스', '숨김 동작을 확인하기 위한 샘플', '도보', 30, 0);

INSERT INTO course_tag (course_id, tag_id) VALUES
    (1, 1), (1, 3), (2, 2), (3, 1), (4, 1), (4, 2), (5, 3), (6, 2), (7, 1), (8, 1);

INSERT INTO course_stop (stop_id, course_id, place_id, stop_order, stay_minutes, move_minutes_to_next, highlight_story_id) VALUES
    (1, 1, 101, 1, 20, 5, 501),
    (2, 1, 102, 2, 5, NULL, 502),
    (3, 2, 101, 1, 30, NULL, 501),
    (4, 3, 101, 1, 40, 10, 501),
    (5, 3, 102, 2, 10, NULL, 502),
    (6, 4, 201, 1, 30, NULL, 503),
    (7, 5, 201, 1, 30, 10, 503),
    (8, 5, 202, 2, 20, NULL, 504),
    (9, 6, 301, 1, 30, NULL, 505),
    (10, 7, 301, 1, 60, NULL, 505),
    (11, 8, 301, 1, 30, NULL, 505);
