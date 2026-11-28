-- Apply once to an existing LaundryLab MySQL database before running the floor-plan UI.
ALTER TABLE laundry_room ADD COLUMN entrance_side ENUM('LEFT','RIGHT','TOP','BOTTOM') NOT NULL DEFAULT 'RIGHT';
ALTER TABLE machine ADD COLUMN layout_x TINYINT UNSIGNED NULL, ADD COLUMN layout_y TINYINT UNSIGNED NULL;

-- The supplied sketch shows A01–A03 above A04–A05 in the first-floor room.
UPDATE machine SET
  layout_x = CASE machine_code WHEN 'A01' THEN 18 WHEN 'A02' THEN 50 WHEN 'A03' THEN 82 WHEN 'A04' THEN 18 WHEN 'A05' THEN 50 END,
  layout_y = CASE machine_code WHEN 'A01' THEN 31 WHEN 'A02' THEN 31 WHEN 'A03' THEN 31 WHEN 'A04' THEN 76 WHEN 'A05' THEN 76 END
WHERE room_id = 1 AND machine_code IN ('A01','A02','A03','A04','A05');
