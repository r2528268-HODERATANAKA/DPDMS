package com.dpdms.fire_service.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Fire domain tests: severity scale, workflow encapsulation, polymorphic contract. */
class FireDomainTest {

    private FireIncident incident() {
        return FireIncident.builder()
                .ward("Ward 4").district("Mudzi").province("Mashonaland East")
                .occurredAt(LocalDateTime.now()).reporter("ward4.fire")
                .severity(Severity.HIGH).latitude(-16.75).longitude(32.28)
                .areaBurnedHectares(10.0).suspectedCause("Escaped burn")
                .injuries(0).fatalities(0).structuresDestroyed(0)
                .fireStatus(FireStatus.ACTIVE)
                .build();
    }

    @Test
    void severityImplementsSharedScale() {
        assertTrue(SeverityScale.class.isAssignableFrom(Severity.class));
        assertTrue(Severity.LOW.rank() < Severity.HIGH.rank());
        assertFalse(Severity.MEDIUM.triggersImmediateAlert());
        assertTrue(Severity.CRITICAL.triggersImmediateAlert());
    }

    @Test
    void newIncidentStartsPendingAndEditable() {
        FireIncident e = incident();
        assertEquals(IncidentStatus.PENDING, e.getStatus());
        assertTrue(e.isEditable());
        assertEquals("fire", e.getHazardType());
    }

    @Test
    @DisplayName("Approval stamps reviewer, comment and time; terminal records freeze")
    void approvalStampsAndFreezes() {
        FireIncident e = incident();
        e.applyReview(IncidentStatus.APPROVED, "Fire Supervisor", "Verified");

        assertEquals(IncidentStatus.APPROVED, e.getStatus());
        assertEquals("Fire Supervisor", e.getReviewedBy());
        assertEquals("Verified", e.getReviewNotes());
        assertFalse(e.isEditable());
        assertThrows(IllegalStateException.class,
                () -> e.applyReview(IncidentStatus.REJECTED, "Fire Supervisor", "change of mind"));
    }
}
