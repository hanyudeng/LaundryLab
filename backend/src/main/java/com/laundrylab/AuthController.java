package com.laundrylab;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final BCryptPasswordEncoder PASSWORDS = new BCryptPasswordEncoder();
    private final JdbcTemplate db;
    private final JavaMailSender mail;
    private final String domain;
    private final String publicBaseUrl;
    private final String mailFrom;
    private final boolean secureCookie;

    public AuthController(JdbcTemplate db, JavaMailSender mail,
            @Value("${app.auth.allowed-email-domain}") String domain,
            @Value("${app.auth.public-base-url}") String publicBaseUrl,
            @Value("${app.auth.mail-from}") String mailFrom,
            @Value("${app.auth.secure-cookie}") boolean secureCookie) {
        this.db = db;
        this.mail = mail;
        this.domain = domain.toLowerCase();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/$", "");
        this.mailFrom = mailFrom;
        this.secureCookie = secureCookie;
    }

    public record Credentials(String email, String password) {}
    public record Verification(String token) {}
    public record User(long id, String email, String role) {}

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<Map<String, String>> register(@RequestBody Credentials input) {
        String email = input.email() == null ? "" : input.email().trim().toLowerCase();
        if (!email.matches("^[^@\\s]+@" + java.util.regex.Pattern.quote(domain) + "$")
                || input.password() == null || input.password().length() < 8 || input.password().length() > 128)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a school email and an 8–128 character password");
        try {
            db.update("INSERT INTO user_account (email, password_hash, role) VALUES (?, ?, 'STUDENT')",
                email, PASSWORDS.encode(input.password()));
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account already exists");
        }
        long userId = db.queryForObject("SELECT user_id FROM user_account WHERE email = ?", Long.class, email);
        String token = randomToken();
        db.update("INSERT INTO email_verification (user_id, token_hash, expires_at) VALUES (?, ?, ?)",
            userId, hash(token), LocalDateTime.now(ZoneOffset.UTC).plusMinutes(30));
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("LaundryLab 信箱驗證");
        message.setText("請在 30 分鐘內開啟以下連結驗證信箱：\n" + publicBaseUrl + "/verify?token=" + token);
        mail.send(message);
        return ResponseEntity.accepted().body(Map.of("message", "請至學校信箱收取驗證連結"));
    }

    @PostMapping("/verify")
    @Transactional
    public Map<String, String> verify(@RequestBody Verification input) {
        if (input.token() == null || input.token().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing token");
        var now = LocalDateTime.now(ZoneOffset.UTC);
        var rows = db.queryForList("SELECT user_id FROM email_verification WHERE token_hash = ? AND used_at IS NULL AND expires_at > ? FOR UPDATE",
            hash(input.token()), now);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired link");
        long userId = ((Number) rows.get(0).get("user_id")).longValue();
        db.update("UPDATE email_verification SET used_at = ? WHERE token_hash = ?", now, hash(input.token()));
        db.update("UPDATE user_account SET email_verified_at = ? WHERE user_id = ?", now, userId);
        return Map.of("message", "信箱已驗證，請登入");
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody Credentials input) {
        String email = input.email() == null ? "" : input.email().trim().toLowerCase();
        var rows = db.queryForList("SELECT user_id, email, role, password_hash, email_verified_at, account_status FROM user_account WHERE email = ?", email);
        if (rows.isEmpty() || input.password() == null || !PASSWORDS.matches(input.password(), (String) rows.get(0).get("password_hash")))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        var row = rows.get(0);
        if (!"ACTIVE".equals(row.get("account_status"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account disabled");
        if (row.get("email_verified_at") == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Verify your email first");
        long id = ((Number) row.get("user_id")).longValue();
        String token = randomToken();
        db.update("INSERT INTO login_session (user_id, token_hash, expires_at) VALUES (?, ?, ?)",
            id, hash(token), LocalDateTime.now(ZoneOffset.UTC).plusDays(7));
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(token, 7 * 24 * 60 * 60).toString())
            .body(new User(id, (String) row.get("email"), (String) row.get("role")));
    }

    @GetMapping("/me")
    public User me(HttpServletRequest request) { return (User) request.getAttribute("authUser"); }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = ApiAuthInterceptor.sessionToken(request);
        db.update("UPDATE login_session SET revoked_at = ? WHERE token_hash = ? AND revoked_at IS NULL",
            LocalDateTime.now(ZoneOffset.UTC), hash(token));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", 0).toString()).build();
    }

    User sessionUser(String token) {
        if (token == null || token.isBlank()) return null;
        var rows = db.query("""
            SELECT u.user_id, u.email, u.role FROM login_session s
            JOIN user_account u ON u.user_id = s.user_id
            WHERE s.token_hash = ? AND s.revoked_at IS NULL AND s.expires_at > ?
              AND u.account_status = 'ACTIVE' AND u.email_verified_at IS NOT NULL
            """, (rs, n) -> new User(rs.getLong(1), rs.getString(2), rs.getString(3)),
            hash(token), LocalDateTime.now(ZoneOffset.UTC));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private ResponseCookie cookie(String value, long maxAge) {
        return ResponseCookie.from("laundry_session", value).httpOnly(true).secure(secureCookie)
            .sameSite("Lax").path("/").maxAge(maxAge).build();
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
