package com.laundrylab;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/facilities")
public class FacilityAdminController {
    private final JdbcTemplate db;
    public FacilityAdminController(JdbcTemplate db) { this.db = db; }
    public record Facility(String name, String location, Long dormitoryId, Long roomId,
                           String code, Integer defaultCycleSeconds, Boolean isActive) {}

    @GetMapping
    public Map<String, Object> list() {
        return Map.of(
            "dormitories", db.queryForList("SELECT dormitory_id AS id, name, is_active AS isActive FROM dormitory ORDER BY dormitory_id"),
            "rooms", db.queryForList("SELECT room_id AS id, dormitory_id AS dormitoryId, name, location, is_active AS isActive FROM laundry_room ORDER BY room_id"),
            "machines", db.queryForList("SELECT machine_id AS id, room_id AS roomId, machine_code AS code, default_cycle_seconds AS defaultCycleSeconds, is_active AS isActive FROM machine ORDER BY machine_id")
        );
    }

    @PostMapping("/{type}")
    public Map<String, String> create(@PathVariable String type, @RequestBody Facility input) {
        switch (type) {
            case "dormitories" -> db.update("INSERT INTO dormitory (name) VALUES (?)", name(input.name()));
            case "rooms" -> {
                if (input.dormitoryId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
                db.update("INSERT INTO laundry_room (dormitory_id, name, location) VALUES (?, ?, ?)", input.dormitoryId(), name(input.name()), input.location());
            }
            case "machines" -> {
                if (input.roomId() == null || input.defaultCycleSeconds() == null || input.defaultCycleSeconds() <= 0)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
                db.update("INSERT INTO machine (room_id, machine_code, default_cycle_seconds) VALUES (?, ?, ?)", input.roomId(), name(input.code()), input.defaultCycleSeconds());
            }
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return Map.of("status", "CREATED");
    }

    @PutMapping("/{type}/{id}")
    public Map<String, String> update(@PathVariable String type, @PathVariable long id, @RequestBody Facility input) {
        int changed;
        switch (type) {
            case "dormitories" -> changed = db.update("UPDATE dormitory SET name = COALESCE(?, name), is_active = COALESCE(?, is_active) WHERE dormitory_id = ?",
                input.name() == null ? null : name(input.name()), input.isActive(), id);
            case "rooms" -> changed = db.update("UPDATE laundry_room SET name = COALESCE(?, name), location = COALESCE(?, location), is_active = COALESCE(?, is_active) WHERE room_id = ?",
                input.name() == null ? null : name(input.name()), input.location(), input.isActive(), id);
            case "machines" -> {
                if (input.defaultCycleSeconds() != null && input.defaultCycleSeconds() <= 0)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
                changed = db.update("UPDATE machine SET machine_code = COALESCE(?, machine_code), default_cycle_seconds = COALESCE(?, default_cycle_seconds), is_active = COALESCE(?, is_active) WHERE machine_id = ?",
                    input.code() == null ? null : name(input.code()), input.defaultCycleSeconds(), input.isActive(), id);
            }
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return Map.of("status", "UPDATED");
    }

    private String name(String value) {
        if (value == null || value.isBlank() || value.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid name");
        return value.trim();
    }
}
