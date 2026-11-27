package com.laundrylab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:linelab;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.sql.init.mode=always", "app.auth.secure-cookie=false",
    "app.line.channel-secret=test-channel-secret"
})
@AutoConfigureMockMvc
class LineWebhookTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired ObjectMapper json;

    @Test
    void signedMessageLinksStudentOnce() throws Exception {
        String email = "line-student@o365.fcu.edu.tw";
        String password = "line-test-password";
        db.update("INSERT INTO user_account (email, password_hash, role, email_verified_at, account_status) VALUES (?, ?, 'STUDENT', CURRENT_TIMESTAMP, 'ACTIVE')",
            email, new BCryptPasswordEncoder().encode(password));
        Cookie session = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(new AuthController.Credentials(email, password))))
            .andExpect(status().isOk()).andReturn().getResponse().getCookie("laundry_session");
        String response = mvc.perform(post("/api/student/line/link-code").cookie(session))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String code = json.readTree(response).path("code").asText();
        String event = json.writeValueAsString(java.util.Map.of("events", java.util.List.of(java.util.Map.of(
            "webhookEventId", "line-test-1", "type", "message", "timestamp", System.currentTimeMillis(),
            "source", java.util.Map.of("userId", "Uteststudent"),
            "message", java.util.Map.of("type", "text", "text", "link " + code)))));

        mvc.perform(post("/api/line/webhook").contentType(MediaType.APPLICATION_JSON).content(event))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/line/webhook").contentType(MediaType.APPLICATION_JSON).content(event)
            .header("X-Line-Signature", "invalid"))
            .andExpect(status().isForbidden());
        assertThat(db.queryForObject("SELECT COUNT(*) FROM line_account_link", Integer.class)).isZero();

        String signature = signature(event);
        mvc.perform(post("/api/line/webhook").contentType(MediaType.APPLICATION_JSON).content(event)
            .header("X-Line-Signature", signature)).andExpect(status().isOk());
        mvc.perform(post("/api/line/webhook").contentType(MediaType.APPLICATION_JSON).content(event)
            .header("X-Line-Signature", signature)).andExpect(status().isOk());
        assertThat(db.queryForObject("SELECT link_status FROM line_account_link WHERE line_user_id = ?", String.class, "Uteststudent"))
            .isEqualTo("LINKED");
        assertThat(db.queryForObject("SELECT COUNT(*) FROM line_webhook_event_log", Integer.class)).isEqualTo(1);
    }

    private static String signature(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-channel-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
