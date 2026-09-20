package com.dpdms.fire_service.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Workflow state machine of the SDD (state diagram + transition table). */
class IncidentStatusTest {

    @Test
    void pendingCanBeApprovedRejectedOrSentBack() {
        assertTrue(IncidentStatus.PENDING.canTransitionTo(IncidentStatus.APPROVED));
        assertTrue(IncidentStatus.PENDING.canTransitionTo(IncidentStatus.REJECTED));
        assertTrue(IncidentStatus.PENDING.canTransitionTo(IncidentStatus.CORRECTIONS_REQUESTED));
    }

    @Test
    void correctionsRequestedCanOnlyBeResubmitted() {
        assertTrue(IncidentStatus.CORRECTIONS_REQUESTED.canTransitionTo(IncidentStatus.PENDING));
        assertFalse(IncidentStatus.CORRECTIONS_REQUESTED.canTransitionTo(IncidentStatus.APPROVED));
    }

    @Test
    void terminalStatesHaveNoOutgoingEdges() {
        assertFalse(IncidentStatus.APPROVED.canTransitionTo(IncidentStatus.PENDING));
        assertFalse(IncidentStatus.REJECTED.canTransitionTo(IncidentStatus.APPROVED));
        assertFalse(IncidentStatus.PENDING.canTransitionTo(IncidentStatus.PENDING));
    }
}
