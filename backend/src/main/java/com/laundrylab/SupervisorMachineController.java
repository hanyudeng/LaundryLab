package com.laundrylab;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/supervisor/machines")
public class SupervisorMachineController {
    private final JdbcTemplate db;
    public SupervisorMachineController(JdbcTemplate db) { this.db = db; }
    public record Maintenance(String status, String reason) {}

    @PutMapping("/{id}/maintenance")
    public Map<String, String> update(@PathVariable long id, @RequestBody Maintenance input, HttpServletRequest request) {
        var user = (AuthController.User) request.getAttribute("authUser");
        if (input.status() == null || !java.util.Set.of("NORMAL", "MAINTENANCE").contains(input.status())
                || input.reason() == null || input.reason().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reason and status required");
        if (user.role().equals("SUPERVISOR")) {
            Integer assigned = db.queryForObject("""
                SELECT COUNT(*) FROM machine m JOIN laundry_room r ON r.room_id = m.room_id
                JOIN dorm_supervisor_assignment a ON a.dormitory_id = r.dormitory_id
                WHERE m.machine_id = ? AND a.supervisor_id = ? AND a.is_active = TRUE
                """, Integer.class, id, user.id());
            if (assigned == 0) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        int changed = db.update("UPDATE machine SET management_status = ? WHERE machine_id = ?", input.status(), id);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        db.update("INSERT INTO audit_log (actor_id, target_type, target_id, action, reason) VALUES (?, 'MACHINE', ?, 'MAINTENANCE_STATUS', ?)",
            user.id(), id, input.reason().trim());
        return Map.of("status", input.status());
    }
}
