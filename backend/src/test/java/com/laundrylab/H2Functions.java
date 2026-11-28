package com.laundrylab;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class H2Functions {
    private H2Functions() {}

    public static Timestamp utcTimestamp(int precision) {
        return Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC));
    }
}
