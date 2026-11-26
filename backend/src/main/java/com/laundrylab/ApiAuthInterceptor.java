package com.laundrylab;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiAuthInterceptor implements HandlerInterceptor {
    private final AuthController auth;
    private final JdbcTemplate db;

    public ApiAuthInterceptor(AuthController auth, JdbcTemplate db) { this.auth = auth; this.db = db; }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (path.equals("/api/auth/register") || path.equals("/api/auth/verify")
                || path.equals("/api/auth/login") || path.equals("/api/signals")) return true;
        AuthController.User user = auth.sessionUser(sessionToken(request));
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        if (path.startsWith("/api/admin/") && !user.role().equals("ADMIN"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin required");
        if (path.startsWith("/api/supervisor/") && !user.role().equals("SUPERVISOR") && !user.role().equals("ADMIN"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Supervisor required");
        if (path.startsWith("/api/student/") && !user.role().equals("STUDENT"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student required");
        if (user.role().equals("SUPERVISOR")) checkDormScope(path, user.id());
        request.setAttribute("authUser", user);
        return true;
    }

    private void checkDormScope(String path, long supervisorId) {
        String sql = null;
        Long id = null;
        try {
            if (path.matches("/api/dormitories/\\d+/rooms")) {
                id = Long.parseLong(path.split("/")[3]);
                sql = "SELECT COUNT(*) FROM dorm_supervisor_assignment WHERE supervisor_id = ? AND dormitory_id = ? AND is_active = TRUE";
            } else if (path.matches("/api/rooms/\\d+/machines")) {
                id = Long.parseLong(path.split("/")[3]);
                sql = "SELECT COUNT(*) FROM dorm_supervisor_assignment a JOIN laundry_room r ON r.dormitory_id = a.dormitory_id WHERE a.supervisor_id = ? AND r.room_id = ? AND a.is_active = TRUE";
            } else if (path.matches("/api/machines/\\d+")) {
                id = Long.parseLong(path.split("/")[3]);
                sql = "SELECT COUNT(*) FROM dorm_supervisor_assignment a JOIN laundry_room r ON r.dormitory_id = a.dormitory_id JOIN machine m ON m.room_id = r.room_id WHERE a.supervisor_id = ? AND m.machine_id = ? AND a.is_active = TRUE";
            }
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid resource ID");
        }
        if (sql != null && db.queryForObject(sql, Integer.class, supervisorId, id) == 0)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Dormitory not assigned");
    }

    static String sessionToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies())
            if (cookie.getName().equals("laundry_session")) return cookie.getValue();
        return null;
    }
}
