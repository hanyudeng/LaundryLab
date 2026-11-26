package com.laundrylab;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/student/reminders")
public class ReminderController {
    private final JdbcTemplate db;
    public ReminderController(JdbcTemplate db) { this.db = db; }

    @GetMapping
    public List<Map<String, Object>> list(HttpServletRequest request) {
        long user = ((AuthController.User) request.getAttribute("authUser")).id();
        return db.queryForList("""
            SELECT s.subscription_id AS id, s.cycle_id AS cycleId, s.status,
                   m.machine_code AS machineCode, d.name AS dorm, r.name AS room,
                   c.estimated_end_at AS estimatedEndAt
            FROM reminder_subscription s JOIN wash_cycle c ON c.cycle_id = s.cycle_id
            JOIN machine m ON m.machine_id = c.machine_id JOIN laundry_room r ON r.room_id = m.room_id
            JOIN dormitory d ON d.dormitory_id = r.dormitory_id
            WHERE s.user_id = ? AND s.status = 'ACTIVE' AND c.cycle_status IN ('RUNNING','PENDING_CONFIRMATION')
            ORDER BY s.created_at DESC
            """, user);
    }

    @PostMapping("/{cycleId}")
    @Transactional
    public Map<String, String> subscribe(@PathVariable long cycleId, HttpServletRequest request) {
        long user = ((AuthController.User) request.getAttribute("authUser")).id();
        Integer linked = db.queryForObject("SELECT COUNT(*) FROM line_account_link WHERE user_id = ? AND link_status = 'LINKED' AND follow_status = 'FOLLOWING'", Integer.class, user);
        if (linked == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "請先綁定並追蹤 LINE 官方帳號");
        Integer active = db.queryForObject("SELECT COUNT(*) FROM wash_cycle WHERE cycle_id = ? AND cycle_status IN ('RUNNING','PENDING_CONFIRMATION')", Integer.class, cycleId);
        if (active == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cycle no longer active");
        db.update("INSERT INTO reminder_subscription (user_id, cycle_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE status = 'ACTIVE', cancelled_at = NULL", user, cycleId);
        db.update("UPDATE notification_log n JOIN reminder_subscription s ON s.subscription_id = n.subscription_id SET n.send_status = 'PENDING' WHERE s.user_id = ? AND s.cycle_id = ? AND n.send_status = 'CANCELLED' AND n.attempt_count = 0", user, cycleId);
        return Map.of("status", "ACTIVE");
    }

    @DeleteMapping("/{cycleId}")
    @Transactional
    public Map<String, String> cancel(@PathVariable long cycleId, HttpServletRequest request) {
        long user = ((AuthController.User) request.getAttribute("authUser")).id();
        db.update("UPDATE reminder_subscription SET status = 'CANCELLED', cancelled_at = UTC_TIMESTAMP(3) WHERE user_id = ? AND cycle_id = ? AND status = 'ACTIVE'", user, cycleId);
        db.update("UPDATE notification_log n JOIN reminder_subscription s ON s.subscription_id = n.subscription_id SET n.send_status = 'CANCELLED' WHERE s.user_id = ? AND s.cycle_id = ? AND n.send_status IN ('PENDING','FAILED')", user, cycleId);
        return Map.of("status", "CANCELLED");
    }
}
