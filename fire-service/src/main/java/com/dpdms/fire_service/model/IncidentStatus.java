package com.dpdms.fire_service.model;

/**
 * Lifecycle states of an incident and the legal transitions between them.
 *
 * PENDING -> APPROVED | REJECTED | CORRECTIONS_REQUESTED
 * CORRECTIONS_REQUESTED -> PENDING (ward recorder re-submits)
 * APPROVED / REJECTED are terminal.
 *
 * OOP concept: ENCAPSULATION - the transition rules live in the domain model, so no
 * controller/service can put an incident into an illegal state.
 */
public enum IncidentStatus {

    PENDING,
    APPROVED,
    REJECTED,
    CORRECTIONS_REQUESTED;

    public boolean canTransitionTo(IncidentStatus target) {
        switch (this) {
            case PENDING:
                return target == APPROVED || target == REJECTED || target == CORRECTIONS_REQUESTED;
            case CORRECTIONS_REQUESTED:
                return target == PENDING;
            default:
                return false; // APPROVED and REJECTED are terminal
        }
    }
}
