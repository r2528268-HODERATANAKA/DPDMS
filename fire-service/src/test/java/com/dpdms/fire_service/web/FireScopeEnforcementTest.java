package com.dpdms.fire_service.web;

import com.dpdms.fire_service.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end RBAC enforcement test - the test the workflow guide explicitly requires:
 *
 *   "Tests: a mismatched supervisor's token must get 403"
 *
 * Real JWTs (minted with the shared secret) flow through the real JwtAuthFilter +
 * SecurityConfig into the service, with an in-memory H2 database standing in for MySQL.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fire;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.port=1",
        "spring.rabbitmq.connection-timeout=200",
        "dpdms.jwt.secret=test-secret-that-is-long-enough-for-hs256-0123456789"
})
@AutoConfigureMockMvc
class FireScopeEnforcementTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    private String token(String username, String fullName, String role, String ward, String hazard) {
        return jwtService.issue(username, fullName, role, ward, hazard);
    }

    private String ward4Recorder()  { return token("ward4.fire", "Ward 4 Recorder", "WARD_RECORDER", "Ward 4", "fire"); }
    private String fireSupervisor() { return token("fire.supervisor", "Fire Supervisor", "PROVINCIAL_SUPERVISOR", null, "fire"); }
    private String floodSupervisor(){ return token("flood.supervisor", "Flood Supervisor", "PROVINCIAL_SUPERVISOR", null, "flood"); }

    private String fireBody(String ward) {
        return """
                {
                  "ward": "%s",
                  "district": "Mudzi",
                  "province": "Mashonaland East",
                  "occurredAt": "2026-09-18T10:30:00",
                  "reporter": "ward4.fire",
                  "severity": "HIGH",
                  "latitude": -16.75,
                  "longitude": 32.28,
                  "areaBurnedHectares": 12.5,
                  "suspectedCause": "Escaped land-clearing burn",
                  "injuries": 0,
                  "fatalities": 0,
                  "structuresDestroyed": 0,
                  "fireStatus": "ACTIVE"
                }
                """.formatted(ward);
    }

    private String createIncident(String bearer) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/fire/incidents")
                        .header("Authorization", "Bearer " + bearer)
                        .contentType("application/json")
                        .content(fireBody("Ward 4")))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getContentAsString().replaceAll(".*\"id\":(\\d+).*", "$1");
    }

    @Test
    @DisplayName("No token -> 401")
    void missingTokenIsUnauthorized() throws Exception {
        mvc.perform(post("/api/v1/fire/incidents")
                        .contentType("application/json").content(fireBody("Ward 4")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Ward recorder of fire/Ward 4 captures (201) with all five indicators")
    void recorderCaptures() throws Exception {
        mvc.perform(post("/api/v1/fire/incidents")
                        .header("Authorization", "Bearer " + ward4Recorder())
                        .contentType("application/json").content(fireBody("Ward 4")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hazardType").value("fire"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.areaBurnedHectares").value(12.5))
                .andExpect(jsonPath("$.suspectedCause").value("Escaped land-clearing burn"))
                .andExpect(jsonPath("$.structuresDestroyed").value(0))
                .andExpect(jsonPath("$.fireStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("Recorder of another hazard (flood) cannot capture fire -> 403")
    void otherHazardRecorderForbidden() throws Exception {
        mvc.perform(post("/api/v1/fire/incidents")
                        .header("Authorization", "Bearer " + token("ward4.flood", "Ward 4 Flood", "WARD_RECORDER", "Ward 4", "flood"))
                        .contentType("application/json").content(fireBody("Ward 4")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Fire recorder cannot capture outside their own ward -> 403")
    void outsideWardForbidden() throws Exception {
        mvc.perform(post("/api/v1/fire/incidents")
                        .header("Authorization", "Bearer " + ward4Recorder())
                        .contentType("application/json").content(fireBody("Ward 9")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("MISMATCHED SUPERVISOR: flood supervisor's token approving a fire incident -> 403")
    void mismatchedSupervisorGets403() throws Exception {
        String id = createIncident(ward4Recorder());

        mvc.perform(post("/api/v1/fire/incidents/" + id + "/approve")
                        .header("Authorization", "Bearer " + floodSupervisor())
                        .contentType("application/json")
                        .content("{\"comment\":\"I supervise floods, not fires\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Correct fire supervisor approves; reviewer is the token's name claim")
    void correctSupervisorApproves() throws Exception {
        String id = createIncident(ward4Recorder());

        mvc.perform(post("/api/v1/fire/incidents/" + id + "/approve")
                        .header("Authorization", "Bearer " + fireSupervisor())
                        .contentType("application/json")
                        .content("{\"comment\":\"Verified with the ward councillor\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedBy").value("Fire Supervisor"))
                .andExpect(jsonPath("$.reviewNotes").value("Verified with the ward councillor"));
    }

    @Test
    @DisplayName("Recorders never review -> 403")
    void recorderCannotReview() throws Exception {
        String id = createIncident(ward4Recorder());

        mvc.perform(post("/api/v1/fire/incidents/" + id + "/approve")
                        .header("Authorization", "Bearer " + ward4Recorder())
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Fire recorder sees only their own ward's incidents")
    void recorderSeesOnlyOwnWard() throws Exception {
        createIncident(ward4Recorder());

        mvc.perform(get("/api/v1/fire/incidents")
                        .header("Authorization", "Bearer " + ward4Recorder()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ward").value("Ward 4"));
    }

    @Test
    @DisplayName("Supervisor sees every ward of their hazard")
    void supervisorSeesAllWards() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/fire/incidents")
                        .header("Authorization", "Bearer " + fireSupervisor()))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("Ward 4"));
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("Ward 11"));
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("Ward 12"));
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("Ward 13"));
    }

    @Test
    @DisplayName("Approved-only feed returns only APPROVED incidents")
    void approvedFeedOnlyApproved() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/fire/incidents/approved")
                        .header("Authorization", "Bearer " + fireSupervisor()))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("\"status\":\"PENDING\""));
    }
}
