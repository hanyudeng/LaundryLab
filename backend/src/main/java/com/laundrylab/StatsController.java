package com.laundrylab;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/supervisor/stats")
public class StatsController {
    private final JdbcTemplate db;
    public StatsController(JdbcTemplate db) { this.db = db; }

    @GetMapping
    public Map<String, List<Map<String, Object>>> stats(HttpServletRequest request) {
        var user = (AuthController.User) request.getAttribute("authUser");
        String scope = user.role().equals("ADMIN") ? "" : " AND EXISTS (SELECT 1 FROM dorm_supervisor_assignment a WHERE a.dormitory_id = r.dormitory_id AND a.supervisor_id = ? AND a.is_active = TRUE)";
        String from = " FROM wash_cycle c JOIN machine m ON m.machine_id = c.machine_id JOIN laundry_room r ON r.room_id = m.room_id WHERE c.started_at >= UTC_TIMESTAMP() - INTERVAL 7 DAY" + scope;
        List<Map<String, Object>> hours = user.role().equals("ADMIN")
            ? db.queryForList("SELECT HOUR(c.started_at) AS hour, COUNT(*) AS cycles" + from + " GROUP BY HOUR(c.started_at) ORDER BY hour")
            : db.queryForList("SELECT HOUR(c.started_at) AS hour, COUNT(*) AS cycles" + from + " GROUP BY HOUR(c.started_at) ORDER BY hour", user.id());
        List<Map<String, Object>> machines = user.role().equals("ADMIN")
            ? db.queryForList("SELECT m.machine_id AS id, m.machine_code AS code, COUNT(*) AS cycles" + from + " GROUP BY m.machine_id, m.machine_code ORDER BY cycles DESC")
            : db.queryForList("SELECT m.machine_id AS id, m.machine_code AS code, COUNT(*) AS cycles" + from + " GROUP BY m.machine_id, m.machine_code ORDER BY cycles DESC", user.id());
        return Map.of("hours", hours, "machines", machines);
    }
}
