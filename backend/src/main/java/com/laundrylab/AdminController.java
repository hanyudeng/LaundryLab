package com.laundrylab;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final SignalController signals;
    public AdminController(JdbcTemplate db, SignalController signals) { this.db = db; this.signals = signals; }

    public record RoleChange(String role) {}
    public record SignalSourceChange(String source, String reason) {}

    @GetMapping("/machines/signal-sources")
    public java.util.List<Map<String, Object>> signalSources() {
        return db.queryForList("""
            SELECT m.machine_id AS id, m.machine_code AS machineCode,
                   m.signal_source AS source, m.is_active AS active,
                   (SELECT c.cycle_status FROM wash_cycle c WHERE c.machine_id = m.machine_id
                    AND c.cycle_status IN ('RUNNING','PENDING_CONFIRMATION') LIMIT 1) AS cycleStatus,
                   r.name AS roomName, d.name AS dormitoryName,
                   (SELECT MAX(s.received_at) FROM machine_signal_log s
                    WHERE s.machine_id = m.machine_id) AS lastSignalAt
            FROM machine m JOIN laundry_room r ON r.room_id = m.room_id
            JOIN dormitory d ON d.dormitory_id = r.dormitory_id
            ORDER BY d.dormitory_id, r.room_id, m.machine_id
            """);
    }

    @org.springframework.web.bind.annotation.PostMapping("/machines/{machineId}/test-signal/{type}")
    public SignalController.SignalResult testSignal(@PathVariable long machineId, @PathVariable String type) {
        if (!java.util.Set.of("START", "COMPLETE").contains(type))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid test signal");
        String source = db.query("SELECT signal_source FROM machine WHERE machine_id = ?",
            rs -> rs.next() ? rs.getString(1) : null, machineId);
        if (source == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Machine not found");
        if (!"TEST_PAGE".equals(source))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Machine is not configured for test signals");
        return signals.receiveTrusted(new SignalController.SignalRequest(
            "admin-test-" + java.util.UUID.randomUUID(), machineId, type, "TEST_PAGE"));
    }

    @PutMapping("/machines/{machineId}/signal-source")
    @Transactional
    public Map<String, String> setSignalSource(@PathVariable long machineId,
            @RequestBody SignalSourceChange input, HttpServletRequest request) {
        if (input.source() == null || !java.util.Set.of("TEST_PAGE", "IOT_SWITCH").contains(input.source())
                || input.reason() == null || input.reason().isBlank() || input.reason().length() > 500)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and reason are required");
        String before = db.query("SELECT signal_source FROM machine WHERE machine_id = ? FOR UPDATE",
            rs -> rs.next() ? rs.getString(1) : null, machineId);
        if (before == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Machine not found");
        if (!before.equals(input.source())) {
            db.update("UPDATE machine SET signal_source = ? WHERE machine_id = ?", input.source(), machineId);
            long actorId = ((AuthController.User) request.getAttribute("authUser")).id();
            db.update("""
                INSERT INTO audit_log (actor_id, target_type, target_id, action, before_value, after_value, reason)
                VALUES (?, 'MACHINE', ?, 'CHANGE_SIGNAL_SOURCE', ?, ?, ?)
                """, actorId, machineId, "{\"signalSource\":\"" + before + "\"}",
                "{\"signalSource\":\"" + input.source() + "\"}", input.reason().trim());
        }
        return Map.of("source", input.source());
    }

    @GetMapping("/users")
    public java.util.List<Map<String, Object>> users() {
        return db.queryForList("""
            SELECT u.user_id AS id, u.email, u.role, u.account_status AS accountStatus,
                   a.dormitory_id AS dormitoryId
            FROM user_account u LEFT JOIN dorm_supervisor_assignment a
              ON a.supervisor_id = u.user_id AND a.is_active = TRUE
            WHERE u.account_status = 'ACTIVE' ORDER BY u.user_id, a.dormitory_id
            """);
    }

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
