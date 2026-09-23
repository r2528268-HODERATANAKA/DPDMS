package com.dpdms.zoonotic_disease_service.model;

// How far the outbreak investigation has gone:
// SUSPECTED = reported symptoms only, CONFIRMED = lab-verified, UNDER_CONTROL = spread stopped
public enum OutbreakStatus {
    SUSPECTED,
    CONFIRMED,
    UNDER_CONTROL
}
