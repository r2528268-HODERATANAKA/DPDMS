package com.dpdms.report_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

// Fetches the APPROVED incidents of one hazard service.
// The caller's X-User-* headers are FORWARDED so the hazard service still
// applies its scoping rules - a recorder can never report on another ward.
@Component
public class HazardDataClient {

    private final RestClient restClient;
    private final String floodUrl;
    private final String droughtUrl;
    private final String fireUrl;
    private final String zoonoticUrl;
    private final String miningUrl;

    public HazardDataClient(
            @Value("${hazard.flood.url:http://localhost:8081}") String floodUrl,
            @Value("${hazard.drought.url:http://localhost:8082}") String droughtUrl,
            @Value("${hazard.fire.url:http://localhost:8083}") String fireUrl,
            @Value("${hazard.zoonotic.url:http://localhost:8084}") String zoonoticUrl,
            @Value("${hazard.mining.url:http://localhost:8085}") String miningUrl) {
        this.floodUrl = floodUrl;
        this.droughtUrl = droughtUrl;
        this.fireUrl = fireUrl;
        this.zoonoticUrl = zoonoticUrl;
        this.miningUrl = miningUrl;
        java.net.http.HttpClient jdk = java.net.http.HttpClient.newBuilder().build();
        org.springframework.http.client.JdkClientHttpRequestFactory factory =
                new org.springframework.http.client.JdkClientHttpRequestFactory(jdk);
        factory.setReadTimeout(java.time.Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    // Returns the approved incidents as raw maps (JSON as it comes from the hazard service)
    public List<Map<String, Object>> fetchApproved(String hazard, HttpHeaders callerHeaders) {
        String url = urlFor(hazard) + "/api/" + plural(hazard);
        try {
            return restClient.get()
                    .uri(url)
                    .headers(headers -> copyIdentity(callerHeaders, headers))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Could not reach the " + hazard + " service at " + url + ": " + ex.getMessage(), ex);
        }
    }

    // Only the identity headers travel to the hazard service
    private void copyIdentity(HttpHeaders from, HttpHeaders to) {
        for (String name : new String[]{"X-User-Name", "X-User-Role", "X-User-Ward", "X-User-Hazard"}) {
            String value = from.getFirst(name);
            if (value != null) {
                to.set(name, value);
            }
        }
    }

    private String urlFor(String hazard) {
        return switch (hazard.toLowerCase()) {
            case "flood" -> floodUrl;
            case "drought" -> droughtUrl;
            case "fire" -> fireUrl;
            case "zoonotic" -> zoonoticUrl;
            case "mining" -> miningUrl;
            default -> throw new IllegalArgumentException("Unknown hazard: " + hazard
                    + " (use flood, drought, fire, zoonotic or mining)");
        };
    }

    private String plural(String hazard) {
        return switch (hazard.toLowerCase()) {
            case "flood" -> "floods";
            case "drought" -> "droughts";
            case "fire" -> "fires";
            case "zoonotic" -> "zoonotics";
            case "mining" -> "minings";
            default -> hazard + "s";
        };
    }
}
