package com.dpdms.zoonotic_disease_service.model;

// Lifecycle of every incident record in DPDMS (same in all hazard services).
// PENDING                -> just captured by a ward recorder, waiting for review
// APPROVED               -> confirmed by the provincial supervisor, visible on the dashboard
// REJECTED               -> not a real/valid incident, kept for audit purposes
// CORRECTIONS_REQUESTED  -> supervisor sent it back to the recorder for fixing
public enum IncidentStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CORRECTIONS_REQUESTED
}
