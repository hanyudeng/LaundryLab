package com.laundrylab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:authlab;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.sql.init.mode=always", "app.auth.secure-cookie=false"
})
@AutoConfigureMockMvc
class AuthFlowTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @MockitoBean JavaMailSender mail;

    @Test
    void verifiedSessionAndSupervisorDormScope() throws Exception {
        String credentials = "{\"email\":\"student01@o365.fcu.edu.tw\",\"password\":\"example-pass-123\"}";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials))
            .andExpect(status().isAccepted());
        assertThat(db.queryForObject("SELECT role FROM user_account WHERE email = ?", String.class, "student01@o365.fcu.edu.tw"))
            .isEqualTo("STUDENT");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials))
            .andExpect(status().isForbidden());

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        org.mockito.Mockito.verify(mail).send(message.capture());
        String token = message.getValue().getText().split("token=")[1].trim();
        mvc.perform(post("/api/auth/verify").contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + token + "\"}"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/auth/verify").contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + token + "\"}"))
            .andExpect(status().isBadRequest());
        Cookie cookie = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials))
            .andExpect(status().isOk()).andReturn().getResponse().getCookie("laundry_session");
        assertThat(cookie).isNotNull();
        mvc.perform(get("/api/auth/me").cookie(cookie)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());

        long userId = db.queryForObject("SELECT user_id FROM user_account WHERE email = ?", Long.class, "student01@o365.fcu.edu.tw");
        db.update("UPDATE user_account SET role = 'SUPERVISOR' WHERE user_id = ?", userId);
        db.update("INSERT INTO dorm_supervisor_assignment VALUES (?, 1, TRUE)", userId);
        db.update("INSERT INTO dormitory VALUES (2, 'Other', TRUE)");
        db.update("INSERT INTO laundry_room (room_id,dormitory_id,name,location,is_active) VALUES (2, 2, 'Other room', 'Other floor', TRUE)");
        mvc.perform(get("/api/dormitories/1/rooms").cookie(cookie)).andExpect(status().isOk());
        mvc.perform(get("/api/dormitories/2/rooms").cookie(cookie)).andExpect(status().isForbidden());
        mvc.perform(get("/api/rooms/2/machines").cookie(cookie)).andExpect(status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/supervisor/machines/1/maintenance")
            .cookie(cookie).contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"MAINTENANCE\",\"reason\":\"需要檢查\"}"))
            .andExpect(status().isOk());
        assertThat(db.queryForObject("SELECT management_status FROM machine WHERE machine_id = 1", String.class)).isEqualTo("MAINTENANCE");
        mvc.perform(post("/api/auth/logout").cookie(cookie)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").cookie(cookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void reportPersistsAndStudentCannotHandleIt() throws Exception {
        var encoder = new BCryptPasswordEncoder();
        db.update("INSERT INTO user_account (email, password_hash, role, email_verified_at, account_status) VALUES (?, ?, 'STUDENT', CURRENT_TIMESTAMP, 'ACTIVE')",
            "reporter@o365.fcu.edu.tw", encoder.encode("reporter-pass-123"));
        Cookie student = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"reporter@o365.fcu.edu.tw\",\"password\":\"reporter-pass-123\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getCookie("laundry_session");
        mvc.perform(post("/api/reports").cookie(student).contentType(MediaType.APPLICATION_JSON)
            .content("{\"machineId\":1,\"issueType\":\"FAULT\",\"description\":\"無法啟動\"}"))
            .andExpect(status().isOk());
        assertThat(db.queryForObject("SELECT COUNT(*) FROM issue_report WHERE description = '無法啟動'", Integer.class)).isEqualTo(1);
        mvc.perform(get("/api/reports").cookie(student)).andExpect(status().isOk());
        long id = db.queryForObject("SELECT report_id FROM issue_report WHERE description = '無法啟動'", Long.class);
        mvc.perform(get("/api/machines/1").cookie(student))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REPORTED"))
            .andExpect(jsonPath("$.hasOpenReport").value(true));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/reports/" + id)
            .cookie(student).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\"}"))
            .andExpect(status().isForbidden());
        db.update("UPDATE issue_report SET status='RESOLVED' WHERE report_id=?", id);
        mvc.perform(get("/api/machines/1").cookie(student))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AVAILABLE"))
            .andExpect(jsonPath("$.hasOpenReport").value(false));
    }

    @Test
    void onlyAdminCanAssignSupervisorAndRevokesOldSession() throws Exception {
        var encoder = new BCryptPasswordEncoder();
        db.update("INSERT INTO user_account (email, password_hash, role, email_verified_at, account_status) VALUES (?, ?, 'ADMIN', CURRENT_TIMESTAMP, 'ACTIVE')",
            "admin@laundrylab.local", encoder.encode("admin-pass-123"));
        db.update("INSERT INTO user_account (email, password_hash, role, email_verified_at, account_status) VALUES (?, ?, 'STUDENT', CURRENT_TIMESTAMP, 'ACTIVE')",
            "candidate@o365.fcu.edu.tw", encoder.encode("candidate-pass-123"));
        long candidate = db.queryForObject("SELECT user_id FROM user_account WHERE email = ?", Long.class, "candidate@o365.fcu.edu.tw");
        Cookie student = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"candidate@o365.fcu.edu.tw\",\"password\":\"candidate-pass-123\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getCookie("laundry_session");
        Cookie admin = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"admin@laundrylab.local\",\"password\":\"admin-pass-123\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getCookie("laundry_session");
        mvc.perform(get("/api/admin/facilities").cookie(student)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/facilities").cookie(admin)).andExpect(status().isOk());
        mvc.perform(post("/api/admin/facilities/dormitories").cookie(admin)
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Test dormitory\"}"))
            .andExpect(status().isOk());
        assertThat(db.queryForObject("SELECT COUNT(*) FROM dormitory WHERE name = 'Test dormitory'", Integer.class)).isEqualTo(1);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/users/" + candidate + "/role")
            .cookie(student).contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"SUPERVISOR\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/users/" + candidate + "/role")
            .cookie(admin).contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"SUPERVISOR\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").cookie(student)).andExpect(status().isUnauthorized());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/supervisors/" + candidate + "/dormitories/1")
            .cookie(admin)).andExpect(status().isOk());
        assertThat(db.queryForObject("SELECT COUNT(*) FROM dorm_supervisor_assignment WHERE supervisor_id = ? AND dormitory_id = 1 AND is_active = TRUE", Integer.class, candidate))
            .isEqualTo(1);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM audit_log WHERE actor_id = (SELECT user_id FROM user_account WHERE email = 'admin@laundrylab.local')", Integer.class))
            .isEqualTo(2);
    }
}
