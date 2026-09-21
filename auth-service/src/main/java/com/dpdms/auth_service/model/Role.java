package com.dpdms.auth_service.model;

// The three DPDMS roles (from the marking guide):
//
// WARD_RECORDER        - captures incidents in ONE ward for ONE hazard.
//                        Must have a concrete ward + hazard on the account (FR-SCOPE-01).
// PROVINCIAL_SUPERVISOR- approves/rejects/returns records for ONE hazard across all wards.
//                        Must have a concrete hazard; ward stays empty.
// PROVINCIAL_ADMIN     - creates accounts and manages users. No ward/hazard restriction.
public enum Role {
    WARD_RECORDER,
    PROVINCIAL_SUPERVISOR,
    PROVINCIAL_ADMIN
}
