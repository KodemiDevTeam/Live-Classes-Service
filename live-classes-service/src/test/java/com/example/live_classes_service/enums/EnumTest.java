package com.example.live_classes_service.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnumTest {

    @Test
    void sessionStatus_values() {
        assertEquals(3, SessionStatus.values().length);
        assertEquals(SessionStatus.SCHEDULED, SessionStatus.valueOf("SCHEDULED"));
        assertEquals(SessionStatus.LIVE, SessionStatus.valueOf("LIVE"));
        assertEquals(SessionStatus.ENDED, SessionStatus.valueOf("ENDED"));
    }

    @Test
    void sessionType_values() {
        assertEquals(2, SessionType.values().length);
        assertEquals(SessionType.LIVE_CLASS, SessionType.valueOf("LIVE_CLASS"));
        assertEquals(SessionType.CONFERENCE, SessionType.valueOf("CONFERENCE"));
    }
}
