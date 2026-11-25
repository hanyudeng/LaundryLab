package com.laundrylab;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final JdbcTemplate db;
    private final String email;
    private final String password;

    public AdminBootstrap(JdbcTemplate db,
            @Value("${app.auth.bootstrap-admin-email:}") String email,
            @Value("${app.auth.bootstrap-admin-password:}") String password) {
        this.db = db;
        this.email = email.trim().toLowerCase();
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) return;
        if (email.isBlank() || password.length() < 12)
            throw new IllegalStateException("Bootstrap admin requires an email and a password of at least 12 characters");
        Integer admins = db.queryForObject("SELECT COUNT(*) FROM user_account WHERE role = 'ADMIN'", Integer.class);
        if (admins != null && admins > 0) return;
        db.update("""
            INSERT INTO user_account (email, password_hash, role, email_verified_at)
            VALUES (?, ?, 'ADMIN', ?)
            """, email, new BCryptPasswordEncoder().encode(password), LocalDateTime.now(ZoneOffset.UTC));
    }
}
