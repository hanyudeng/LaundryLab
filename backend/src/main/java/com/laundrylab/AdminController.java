package com.laundrylab;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final JdbcTemplate db;
    public AdminController(JdbcTemplate db) { this.db = db; }

    public record RoleChange(String role) {}

    @PutMapping("/users/{userId}/role")
    @Transactional
    public Map<String, String> changeRole(@PathVariable long userId, @RequestBody RoleChange input,
            HttpServletRequest request) {
        long actorId = ((AuthController.User) request.getAttribute("authUser")).id();
        if (actorId == userId || input.role() == null
                || !java.util.Set.of("STUDENT", "SUPERVISOR").contains(input.role()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role change");
        var roles = db.queryForList("SELECT role FROM user_account WHERE user_id = ? AND account_status = 'ACTIVE' FOR UPDATE", userId);
        if (roles.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        String before = (String) roles.get(0).get("role");
        if (before.equals("ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot change admin role");
        db.update("UPDATE user_account SET role = ? WHERE user_id = ?", input.role(), userId);
        if (input.role().equals("STUDENT"))
            db.update("UPDATE dorm_supervisor_assignment SET is_active = FALSE WHERE supervisor_id = ?", userId);
        db.update("UPDATE login_session SET revoked_at = ? WHERE user_id = ? AND revoked_at IS NULL",
            LocalDateTime.now(ZoneOffset.UTC), userId);
        db.update("INSERT INTO audit_log (actor_id, target_type, target_id, action, before_value, after_value) VALUES (?, 'USER', ?, 'CHANGE_ROLE', ?, ?)",
            actorId, userId, "{\"role\":\"" + before + "\"}", "{\"role\":\"" + input.role() + "\"}");
        return Map.of("role", input.role());
    }

    @PutMapping("/supervisors/{userId}/dormitories/{dormitoryId}")
    @Transactional
    public Map<String, String> assignDormitory(@PathVariable long userId, @PathVariable long dormitoryId,
            HttpServletRequest request) {
        long actorId = ((AuthController.User) request.getAttribute("authUser")).id();
        requireSupervisorAndDormitory(userId, dormitoryId);
        db.update("""
            INSERT INTO dorm_supervisor_assignment (supervisor_id, dormitory_id, is_active)
            VALUES (?, ?, TRUE) ON DUPLICATE KEY UPDATE is_active = TRUE
            """, userId, dormitoryId);
        db.update("INSERT INTO audit_log (actor_id, target_type, target_id, action) VALUES (?, 'DORM_SUPERVISOR_ASSIGNMENT', ?, 'ASSIGN')",
            actorId, dormitoryId);
        return Map.of("status", "ASSIGNED");
    }

    @DeleteMapping("/supervisors/{userId}/dormitories/{dormitoryId}")
    @Transactional
    public Map<String, String> unassignDormitory(@PathVariable long userId, @PathVariable long dormitoryId,
            HttpServletRequest request) {
        long actorId = ((AuthController.User) request.getAttribute("authUser")).id();
        int changed = db.update("UPDATE dorm_supervisor_assignment SET is_active = FALSE WHERE supervisor_id = ? AND dormitory_id = ? AND is_active = TRUE",
            userId, dormitoryId);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found");
        db.update("INSERT INTO audit_log (actor_id, target_type, target_id, action) VALUES (?, 'DORM_SUPERVISOR_ASSIGNMENT', ?, 'UNASSIGN')",
            actorId, dormitoryId);
        return Map.of("status", "UNASSIGNED");
    }

    private void requireSupervisorAndDormitory(long userId, long dormitoryId) {
        Integer supervisors = db.queryForObject("SELECT COUNT(*) FROM user_account WHERE user_id = ? AND role = 'SUPERVISOR' AND account_status = 'ACTIVE'", Integer.class, userId);
        Integer dorms = db.queryForObject("SELECT COUNT(*) FROM dormitory WHERE dormitory_id = ? AND is_active = TRUE", Integer.class, dormitoryId);
        if (supervisors == 0 || dorms == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Supervisor or dormitory not found");
    }
}
