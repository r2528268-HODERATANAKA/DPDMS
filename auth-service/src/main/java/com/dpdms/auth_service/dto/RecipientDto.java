package com.dpdms.auth_service.dto;

/** Alert recipient projection returned by the internal lookup endpoint. */
public record RecipientDto(String fullName, String email, String phone) {
}
