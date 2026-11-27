package com.laundrylab;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.sql.PreparedStatement;
import java.sql.Statement;

@RestController
@RequestMapping("/api/signals")
public class SignalController {
    private final JdbcTemplate db;
    private final String key;
    public SignalController(JdbcTemplate db, @Value("${app.signal-key:}") String key) { this.db = db; this.key = key; }

    public record SignalRequest(String eventId, long machineId, String type, String sourceKind) {}
    public record SignalResult(String eventId, String processStatus, String reason, Long cycleId) {}

    @PostMapping
    @Transactional
    public SignalResult receive(@RequestHeader(value = "X-Signal-Key", required = false) String supplied,
                                @RequestBody SignalRequest request) {
        if (key.isBlank() || supplied == null || !MessageDigest.isEqual(
                key.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid signal key");
        return receiveTrusted(request);
    }

    @Transactional
    public SignalResult receiveTrusted(SignalRequest request) {
        if (request.eventId() == null || request.eventId().isBlank() || request.eventId().length() > 100
                || request.machineId() <= 0 || request.type() == null
                || !java.util.Set.of("START", "COMPLETE", "FAULT").contains(request.type())
                || request.sourceKind() == null
                || !java.util.Set.of("TEST_PAGE", "IOT_SWITCH").contains(request.sourceKind()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid signal event");

        // The unique event ID makes retries idempotent. Locking the machine serializes concurrent events.
        Map<String, Object> machine = db.query("SELECT machine_id, default_cycle_seconds, is_active, signal_source FROM machine WHERE machine_id = ? FOR UPDATE",
            rs -> rs.next() ? Map.of("id", rs.getLong(1), "duration", rs.getLong(2), "active", rs.getBoolean(3), "source", rs.getString(4)) : null,
            request.machineId());
        if (machine == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Machine not found");
        SignalResult previous = db.query("SELECT process_status, error_reason, cycle_id FROM machine_signal_log WHERE source_event_id = ?",
            rs -> rs.next() ? new SignalResult(request.eventId(), rs.getString(1), rs.getString(2),
                rs.getObject(3) == null ? null : rs.getLong(3)) : null, request.eventId());
        if (previous != null) return previous;

        Map<String, Object> active = db.query("SELECT cycle_id, estimated_end_at FROM wash_cycle WHERE machine_id = ? AND cycle_status IN ('RUNNING','PENDING_CONFIRMATION') FOR UPDATE",
            rs -> rs.next() ? Map.of("id", rs.getLong(1), "end", rs.getObject(2, LocalDateTime.class)) : null,
            request.machineId());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String reason = null;
        Long cycleId = null;
        if (!(boolean) machine.get("active")) reason = "MACHINE_DISABLED";
        else if (!request.sourceKind().equals(machine.get("source"))) reason = "SOURCE_MISMATCH";
        else if ("START".equals(request.type())) {
            if (active != null) reason = "CYCLE_ALREADY_ACTIVE";
            else {
                KeyHolder holder = new GeneratedKeyHolder();
                db.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO wash_cycle (machine_id, started_at, estimated_end_at, cycle_status) VALUES (?, ?, ?, 'RUNNING')",
                        Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, request.machineId());
                    ps.setObject(2, now);
                    ps.setObject(3, now.plusSeconds((long) machine.get("duration")));
                    return ps;
                }, holder);
                cycleId = holder.getKey().longValue();
            }
        } else if ("COMPLETE".equals(request.type())) {
            if (active == null) reason = "NO_ACTIVE_CYCLE";
            else {
                cycleId = (long) active.get("id");
                db.update("UPDATE wash_cycle SET cycle_status = 'COMPLETED', completed_at = ? WHERE cycle_id = ?", now, cycleId);
                db.update("UPDATE reminder_subscription SET status = 'EXPIRED' WHERE cycle_id = ? AND status = 'ACTIVE'", cycleId);
            }
        } else {
            // A fault is recorded for review; a signal never clears maintenance state.
            cycleId = active == null ? null : (long) active.get("id");
        }
        String status = reason == null ? "ACCEPTED" : "REJECTED";
        db.update("INSERT INTO machine_signal_log (source_event_id, source_kind, machine_id, cycle_id, signal_type, process_status, error_reason) VALUES (?, ?, ?, ?, ?, ?, ?)",
            request.eventId(), request.sourceKind(), request.machineId(), cycleId, request.type(), status, reason);
        return new SignalResult(request.eventId(), status, reason, cycleId);
    }
}
