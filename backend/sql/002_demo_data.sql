USE laundrylab;
INSERT INTO dormitory (name) VALUES ('示範宿舍');
INSERT INTO laundry_room (dormitory_id, name, location) VALUES (1, '一樓洗衣間', '一樓東側');
INSERT INTO machine (room_id, machine_code, default_cycle_seconds) VALUES
  (1, 'A01', 1800), (1, 'A02', 2400), (1, 'A03', 1800), (1, 'A04', 2400), (1, 'A05', 1800);
