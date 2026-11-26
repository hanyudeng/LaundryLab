package com.laundrylab;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LineReminderJob {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    @Value("${app.line.channel-access-token:}") private String token;
    public LineReminderJob(JdbcTemplate db, ObjectMapper json) { this.db = db; this.json = json; }

    @Scheduled(initialDelay = 60000, fixedDelay = 60000)
    public void run() {
        if (token.isBlank()) return;
        db.update("UPDATE notification_log SET send_status = 'FAILED', next_attempt_at = UTC_TIMESTAMP(3) WHERE send_status = 'SENDING' AND last_attempt_at < UTC_TIMESTAMP(3) - INTERVAL 5 MINUTE");
        queue("FIVE_MINUTES_BEFORE", "c.estimated_end_at <= UTC_TIMESTAMP(3) + INTERVAL 5 MINUTE");
        queue("ESTIMATED_COMPLETE", "c.estimated_end_at <= UTC_TIMESTAMP(3)");
        var pending = db.queryForList("""
            SELECT n.notification_id AS id, n.line_retry_key AS retryKey, n.recipient_line_user_id AS lineUser,
                   n.notification_type AS type, m.machine_code AS machineCode, n.attempt_count AS attempts
            FROM notification_log n JOIN reminder_subscription s ON s.subscription_id = n.subscription_id
            JOIN wash_cycle c ON c.cycle_id = s.cycle_id JOIN machine m ON m.machine_id = c.machine_id
            JOIN line_account_link l ON l.user_id = s.user_id
            WHERE n.send_status IN ('PENDING','FAILED') AND n.attempt_count < 3
              AND (n.next_attempt_at IS NULL OR n.next_attempt_at <= UTC_TIMESTAMP(3))
              AND s.status = 'ACTIVE' AND c.cycle_status IN ('RUNNING','PENDING_CONFIRMATION')
              AND l.link_status = 'LINKED' AND l.follow_status = 'FOLLOWING'
              AND l.line_user_id = n.recipient_line_user_id
            ORDER BY n.notification_id LIMIT 50
            """);
        for (var row : pending) send(row);
    }

    private void queue(String type, String due) {
        db.update("""
            INSERT IGNORE INTO notification_log (subscription_id, notification_type, recipient_line_user_id, line_retry_key)
            SELECT s.subscription_id, ?, l.line_user_id, UUID()
            FROM reminder_subscription s JOIN wash_cycle c ON c.cycle_id = s.cycle_id
            JOIN line_account_link l ON l.user_id = s.user_id
            WHERE s.status = 'ACTIVE' AND c.cycle_status IN ('RUNNING','PENDING_CONFIRMATION')
              AND l.link_status = 'LINKED' AND l.follow_status = 'FOLLOWING'
              AND c.estimated_end_at >= UTC_TIMESTAMP(3) - INTERVAL 1 DAY AND
            """ + due, type);
    }

    private void send(Map<String, Object> row) {
        long id = ((Number) row.get("id")).longValue();
        if (db.update("UPDATE notification_log SET send_status = 'SENDING', attempt_count = attempt_count + 1, last_attempt_at = UTC_TIMESTAMP(3) WHERE notification_id = ? AND send_status IN ('PENDING','FAILED')", id) == 0) return;
        String text = row.get("machineCode") + ("FIVE_MINUTES_BEFORE".equals(row.get("type"))
            ? " 預計約五分鐘後完成，請以現場狀態為準。" : " 已到預計完成時間；實際完成仍以現場訊號為準。");
        try {
            String body = json.writeValueAsString(Map.of("to", row.get("lineUser"),
                "messages", List.of(Map.of("type", "text", "text", text))));
            var request = HttpRequest.newBuilder(URI.create("https://api.line.me/v2/bot/message/push"))
                .timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("X-Line-Retry-Key", row.get("retryKey").toString())
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            int status = http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status == 200 || status == 409) {
                db.update("UPDATE notification_log SET send_status = 'ACCEPTED', accepted_at = UTC_TIMESTAMP(3), last_error = NULL WHERE notification_id = ?", id);
            } else {
                fail(id, "LINE HTTP " + status, status >= 500 || status == 429);
            }
        } catch (Exception error) {
            fail(id, error.getClass().getSimpleName(), true);
        }
    }

    private void fail(long id, String message, boolean retryable) {
        db.update("UPDATE notification_log SET send_status = 'FAILED', last_error = ?, next_attempt_at = CASE WHEN ? THEN UTC_TIMESTAMP(3) + INTERVAL 2 MINUTE ELSE NULL END, attempt_count = CASE WHEN ? THEN attempt_count ELSE 3 END WHERE notification_id = ?",
            message, retryable, retryable, id);
    }
}
