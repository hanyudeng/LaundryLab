package com.laundrylab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class LineController {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    @Value("${app.line.channel-secret:}") private String secret;
    @Value("${app.line.bot-url:}") private String botUrl;
    private static final SecureRandom RANDOM = new SecureRandom();
    public LineController(JdbcTemplate db, ObjectMapper json) { this.db = db; this.json = json; }

    @GetMapping("/api/student/line")
    public Map<String, Object> status(HttpServletRequest request) {
        long id = ((AuthController.User) request.getAttribute("authUser")).id();
        var rows = db.queryForList("SELECT link_status, follow_status FROM line_account_link WHERE user_id = ?", id);
        return Map.of("linked", !rows.isEmpty() && "LINKED".equals(rows.get(0).get("link_status")),
            "following", !rows.isEmpty() && "FOLLOWING".equals(rows.get(0).get("follow_status")),
            "botUrl", botUrl);
    }

    @PostMapping("/api/student/line/link-code")
    public Map<String, Object> code(HttpServletRequest request) {
        long id = ((AuthController.User) request.getAttribute("authUser")).id();
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var expires = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10);
        db.update("INSERT INTO line_link_request (user_id, nonce_hash, expires_at) VALUES (?, ?, ?)", id, AuthController.hash(code), expires);
        return Map.of("code", code, "expiresAt", expires.toString() + "Z", "botUrl", botUrl);
    }

    @DeleteMapping("/api/student/line")
    @Transactional
    public Map<String, String> unlink(HttpServletRequest request) {
        long id = ((AuthController.User) request.getAttribute("authUser")).id();
        db.update("UPDATE line_account_link SET line_user_id = NULL, link_status = 'UNLINKED', follow_status = 'UNKNOWN', unlinked_at = UTC_TIMESTAMP(3) WHERE user_id = ?", id);
        db.update("UPDATE reminder_subscription SET status = 'CANCELLED', cancelled_at = UTC_TIMESTAMP(3) WHERE user_id = ? AND status = 'ACTIVE'", id);
        db.update("UPDATE notification_log n JOIN reminder_subscription s ON s.subscription_id = n.subscription_id SET n.send_status = 'CANCELLED' WHERE s.user_id = ? AND n.send_status IN ('PENDING','FAILED')", id);
        return Map.of("status", "UNLINKED");
    }

    @PostMapping("/api/line/webhook")
    @Transactional
    public void webhook(@RequestBody byte[] body, @RequestHeader(value = "x-line-signature", required = false) String signature) throws Exception {
        if (secret.isBlank() || signature == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expected = Base64.getEncoder().encode(mac.doFinal(body));
        if (!MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid signature");
        JsonNode events = json.readTree(body).path("events");
        for (JsonNode event : events) {
            String eventId = event.path("webhookEventId").asText();
            String type = event.path("type").asText();
            String lineUser = event.path("source").path("userId").asText();
            if (eventId.isBlank() || lineUser.isBlank()) continue;
            int inserted = db.update("INSERT IGNORE INTO line_webhook_event_log (webhook_event_id, event_type, line_user_id, event_occurred_at) VALUES (?, ?, ?, ?)",
                eventId, type, lineUser, LocalDateTime.ofEpochSecond(event.path("timestamp").asLong() / 1000, 0, ZoneOffset.UTC));
            if (inserted == 0) continue;
            if (type.equals("message") && event.path("message").path("type").asText().equals("text")) {
                String text = event.path("message").path("text").asText().trim();
                if (text.startsWith("link ")) {
                    String code = text.substring(5).trim();
                    var rows = db.queryForList("SELECT request_id, user_id FROM line_link_request WHERE nonce_hash = ? AND used_at IS NULL AND expires_at > UTC_TIMESTAMP(3) FOR UPDATE", AuthController.hash(code));
                    if (!rows.isEmpty()) {
                        long id = ((Number) rows.get(0).get("user_id")).longValue();
                        Integer boundElsewhere = db.queryForObject("SELECT COUNT(*) FROM line_account_link WHERE line_user_id = ? AND user_id <> ?", Integer.class, lineUser, id);
                        if (boundElsewhere != 0) continue;
                        db.update("UPDATE line_link_request SET used_at = UTC_TIMESTAMP(3) WHERE request_id = ?", rows.get(0).get("request_id"));
                        db.update("INSERT INTO line_account_link (user_id, line_user_id, link_status, follow_status, linked_at) VALUES (?, ?, 'LINKED', 'FOLLOWING', UTC_TIMESTAMP(3)) ON DUPLICATE KEY UPDATE line_user_id = VALUES(line_user_id), link_status = 'LINKED', follow_status = 'FOLLOWING', linked_at = UTC_TIMESTAMP(3)", id, lineUser);
                    }
                }
            } else if (type.equals("follow") || type.equals("unfollow")) {
                db.update("UPDATE line_account_link SET follow_status = ? WHERE line_user_id = ?", type.equals("follow") ? "FOLLOWING" : "BLOCKED", lineUser);
            }
            db.update("UPDATE line_webhook_event_log SET process_status = 'PROCESSED', processed_at = UTC_TIMESTAMP(3) WHERE webhook_event_id = ?", eventId);
        }
    }
}
