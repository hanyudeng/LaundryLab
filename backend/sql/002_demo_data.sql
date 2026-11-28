USE laundrylab;
INSERT INTO dormitory (name) VALUES ('A棟宿舍'), ('B棟宿舍');
INSERT INTO laundry_room (dormitory_id, name, location) VALUES (1, '一樓洗衣間', '一樓東側');
INSERT INTO machine (room_id, machine_code, default_cycle_seconds) VALUES
  (1, 'A01', 1800), (1, 'A02', 2400), (1, 'A03', 1800), (1, 'A04', 2400), (1, 'A05', 1800);
UPDATE machine SET
  layout_x = CASE machine_code WHEN 'A01' THEN 18 WHEN 'A02' THEN 50 WHEN 'A03' THEN 82 WHEN 'A04' THEN 18 WHEN 'A05' THEN 50 END,
  layout_y = CASE machine_code WHEN 'A01' THEN 31 WHEN 'A02' THEN 31 WHEN 'A03' THEN 31 WHEN 'A04' THEN 76 WHEN 'A05' THEN 76 END
WHERE room_id = 1;
