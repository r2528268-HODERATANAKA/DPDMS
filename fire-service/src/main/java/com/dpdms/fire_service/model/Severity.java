package com.dpdms.fire_service.model;

/**
 * Severity scale shared with the other hazard services (matches flood-service's
 * LOW/MEDIUM/HIGH/CRITICAL). Implements SeverityScale so the alert engine can rank it
 * polymorphically.
 */
public enum Severity implements SeverityScale {

    LOW(1, false),
    MEDIUM(2, false),
    HIGH(3, true),
    CRITICAL(4, true);

    private final int rank;
    private final boolean immediate;

    Severity(int rank, boolean immediate) {
        this.rank = rank;
        this.immediate = immediate;
    }

    @Override
    public int rank() { return rank; }

    @Override
    public boolean triggersImmediateAlert() { return immediate; }
}
