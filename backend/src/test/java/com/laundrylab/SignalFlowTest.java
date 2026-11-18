package com.laundrylab;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:laundrytest;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.sql.init.mode=always", "app.signal-key=test-secret"
})
class SignalFlowTest {
    @Autowired SignalController signals;
    @Autowired MachineController machines;
    @Autowired JdbcTemplate db;

    @Test
    void startRetryPendingAndComplete() {
        var start = new SignalController.SignalRequest("event-1", 1, "START", "TEST_PAGE");
        var first = signals.receive("test-secret", start);
        assertThat(first.processStatus()).isEqualTo("ACCEPTED");
        assertThat(signals.receive("test-secret", start).cycleId()).isEqualTo(first.cycleId());
        assertThat(db.queryForObject("SELECT COUNT(*) FROM wash_cycle", Integer.class)).isEqualTo(1);
        assertThat(signals.receive("test-secret", new SignalController.SignalRequest("event-2", 1, "START", "TEST_PAGE")).reason())
            .isEqualTo("CYCLE_ALREADY_ACTIVE");
        assertThat(machines.machine(1).status()).isEqualTo("RUNNING");
        db.update("UPDATE wash_cycle SET estimated_end_at = TIMESTAMP '2020-01-01 00:00:00' WHERE cycle_id = ?", first.cycleId());
        assertThat(machines.machine(1).status()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(signals.receive("test-secret", new SignalController.SignalRequest("event-3", 1, "COMPLETE", "TEST_PAGE")).processStatus())
            .isEqualTo("ACCEPTED");
        assertThat(machines.machine(1).status()).isEqualTo("AVAILABLE");
        assertThat(signals.receive("test-secret", new SignalController.SignalRequest("event-4", 1, "COMPLETE", "TEST_PAGE")).reason())
            .isEqualTo("NO_ACTIVE_CYCLE");
    }
}
