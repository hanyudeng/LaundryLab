package com.laundrylab;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class MachineController {
    private final JdbcTemplate db;
    public MachineController(JdbcTemplate db) { this.db = db; }

    @GetMapping("/dormitories")
    public List<Map<String, Object>> dormitories(HttpServletRequest request) {
        AuthController.User user = (AuthController.User) request.getAttribute("authUser");
        if (user != null && user.role().equals("SUPERVISOR"))
            return db.queryForList("""
                SELECT d.dormitory_id AS id, d.name FROM dormitory d
                JOIN dorm_supervisor_assignment a ON a.dormitory_id = d.dormitory_id
                WHERE a.supervisor_id = ? AND a.is_active = TRUE AND d.is_active = TRUE
                ORDER BY d.dormitory_id
                """, user.id());
        return db.queryForList("SELECT dormitory_id AS id, name FROM dormitory WHERE is_active = TRUE ORDER BY dormitory_id");
    }

    @GetMapping("/dormitories/{dormitoryId}/rooms")
    public List<Map<String, Object>> rooms(@PathVariable long dormitoryId) {
        return db.queryForList("SELECT room_id AS id, name, location, entrance_side AS entranceSide FROM laundry_room WHERE dormitory_id = ? AND is_active = TRUE ORDER BY room_id", dormitoryId);
    }

    @GetMapping("/rooms/{roomId}/machines")
    public List<MachineView> machines(@PathVariable long roomId) {
        return db.query("""
            SELECT m.machine_id, m.machine_code, m.layout_x, m.layout_y, m.management_status, c.cycle_id,
                   c.cycle_status, c.estimated_end_at,
                   EXISTS (SELECT 1 FROM issue_report i WHERE i.machine_id = m.machine_id
                           AND i.status IN ('OPEN','IN_PROGRESS')) AS has_open_report,
                   (SELECT MAX(s.received_at) FROM machine_signal_log s WHERE s.machine_id = m.machine_id) AS last_signal_at
            FROM machine m LEFT JOIN wash_cycle c ON c.machine_id = m.machine_id
              AND c.cycle_status IN ('RUNNING','PENDING_CONFIRMATION')
            WHERE m.room_id = ? AND m.is_active = TRUE ORDER BY m.machine_id
            """, (rs, row) -> {
                LocalDateTime end = rs.getObject("estimated_end_at", LocalDateTime.class);
                LocalDateTime last = rs.getObject("last_signal_at", LocalDateTime.class);
                String management = rs.getString("management_status");
                String cycle = rs.getString("cycle_status");
                boolean hasOpenReport = rs.getBoolean("has_open_report");
                String status = "MAINTENANCE".equals(management) ? "MAINTENANCE"
                    : cycle == null ? (hasOpenReport ? "REPORTED" : "AVAILABLE")
                    : "PENDING_CONFIRMATION".equals(cycle) || !end.isAfter(LocalDateTime.now(ZoneOffset.UTC))
                        ? "PENDING_CONFIRMATION" : "RUNNING";
                long cycleId = rs.getLong("cycle_id");
                return new MachineView(rs.getLong("machine_id"), rs.getString("machine_code"), status,
                    cycleId == 0 ? null : cycleId,
                    end == null ? null : end.toInstant(ZoneOffset.UTC),
                    last == null ? null : last.toInstant(ZoneOffset.UTC),
                    rs.getObject("layout_x", Integer.class), rs.getObject("layout_y", Integer.class), hasOpenReport);
            }, roomId);
    }

    @GetMapping("/machines/{machineId}")
    public MachineView machine(@PathVariable long machineId) {
        Long roomId = db.query("SELECT room_id FROM machine WHERE machine_id = ? AND is_active = TRUE",
            rs -> rs.next() ? rs.getLong(1) : null, machineId);
        if (roomId == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Machine not found");
        return machines(roomId).stream().filter(m -> m.id() == machineId).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public record MachineView(long id, String machineCode, String status, Long cycleId,
                              Instant estimatedEndAt, Instant lastSignalAt, Integer layoutX, Integer layoutY,
                              boolean hasOpenReport) {}
}
