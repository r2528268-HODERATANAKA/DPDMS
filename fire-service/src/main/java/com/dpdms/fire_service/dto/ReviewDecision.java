package com.dpdms.fire_service.dto;

import jakarta.validation.constraints.Size;

/** Review body for approve / reject / request-corrections. */
public record ReviewDecision(
        @Size(max = 1000, message = "Comment must be at most 1000 characters") String comment) {
}
