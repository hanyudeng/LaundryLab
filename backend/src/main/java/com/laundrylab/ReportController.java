package com.laundrylab;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final JdbcTemplate db;
    public ReportController(JdbcTemplate db) { this.db = db; }

    public record NewReport(long machineId, String issueType, String description) {}
    public record ReportUpdate(String status, String resolution) {}

    @GetMapping
    public List<Map<String, Object>> list(HttpServletRequest request) {
        var user = (AuthController.User) request.getAttribute("authUser");
        String base = """
            SELECT i.report_id AS id, i.reporter_id AS reporterId, u.email AS reporter,
                   d.name AS dorm, r.name AS room, m.machine_code AS machine,
                   i.issue_type AS issueType, i.description, i.status,
                   i.created_at AS createdAt, i.resolution AS note, h.email AS handler
            FROM issue_report i JOIN user_account u ON u.user_id = i.reporter_id
            JOIN machine m ON m.machine_id = i.machine_id
            JOIN laundry_room r ON r.room_id = m.room_id
            JOIN dormitory d ON d.dormitory_id = r.dormitory_id
            LEFT JOIN user_account h ON h.user_id = i.handler_id
            """;
        if (user.role().equals("STUDENT"))
            return db.queryForList(base + " WHERE i.reporter_id = ? ORDER BY i.created_at DESC", user.id());
        if (user.role().equals("SUPERVISOR"))
            return db.queryForList(base + " WHERE EXISTS (SELECT 1 FROM dorm_supervisor_assignment a WHERE a.dormitory_id = r.dormitory_id AND a.supervisor_id = ? AND a.is_active = TRUE) ORDER BY i.created_at DESC", user.id());
        return db.queryForList(base + " ORDER BY i.created_at DESC");
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody NewReport input, HttpServletRequest request) {
        var user = (AuthController.User) request.getAttribute("authUser");
        if (!user.role().equals("STUDENT")) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (input.description() == null || input.description().isBlank() || input.description().length() > 5000
                || input.issueType() == null || !java.util.Set.of("FAULT", "STATUS_MISMATCH", "OTHER").contains(input.issueType()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report");
        Integer exists = db.queryForObject("SELECT COUNT(*) FROM machine WHERE machine_id = ? AND is_active = TRUE", Integer.class, input.machineId());
        if (exists == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Machine not found");
        var key = new GeneratedKeyHolder();
        db.update(connection -> {
            var statement = connection.prepareStatement(
                "INSERT INTO issue_report (reporter_id, machine_id, issue_type, description) VALUES (?, ?, ?, ?)",
                new String[] { "report_id" });
            statement.setLong(1, user.id());
            statement.setLong(2, input.machineId());
            statement.setString(3, input.issueType());
            statement.setString(4, input.description().trim());
            return statement;
        }, key);
        return Map.of("id", key.getKey().longValue());
    }

    @PutMapping("/{id}")
    public Map<String, String> update(@PathVariable long id, @RequestBody ReportUpdate input, HttpServletRequest request) {
        var user = (AuthController.User) request.getAttribute("authUser");
        if (user.role().equals("STUDENT")) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (input.status() == null || !java.util.Set.of("OPEN", "IN_PROGRESS", "RESOLVED").contains(input.status()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        if (user.role().equals("SUPERVISOR")) {
            Integer scope = db.queryForObject("""
                SELECT COUNT(*) FROM issue_report i JOIN machine m ON m.machine_id = i.machine_id
                JOIN laundry_room r ON r.room_id = m.room_id
                JOIN dorm_supervisor_assignment a ON a.dormitory_id = r.dormitory_id
                WHERE i.report_id = ? AND a.supervisor_id = ? AND a.is_active = TRUE
                """, Integer.class, id, user.id());
            if (scope == 0) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        int changed = db.update("UPDATE issue_report SET status = ?, resolution = ?, handler_id = ? WHERE report_id = ?",
            input.status(), input.resolution(), user.id(), id);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return Map.of("status", input.status());
    }
}
