package com.dpdms.dashboard_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

// Calls the approved feed (GET /api/<plural>) of each hazard service.
// Used by the dashboard summary and by the health check.
@Component
public class HazardDataFetcher {

    private final RestClient restClient;
    private final String floodUrl;
    private final String droughtUrl;
    private final String fireUrl;
    private final String zoonoticUrl;
    private final String miningUrl;

    public HazardDataFetcher(
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
        factory.setReadTimeout(java.time.Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    // Never throws for a dead service - returns empty list so one offline
    // hazard never blanks the whole dashboard.
    public List<Map<String, Object>> fetchApproved(String hazard) {
        try {
            return restClient.get()
                    .uri(urlFor(hazard) + "/api/" + plural(hazard))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    // For the health panel: true when the hazard service answers
    public boolean isUp(String hazard) {
        try {
            restClient.get().uri(urlFor(hazard) + "/api/" + plural(hazard)).retrieve().toBodilessEntity();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private String urlFor(String hazard) {
        return switch (hazard.toLowerCase()) {
            case "flood" -> floodUrl;
            case "drought" -> droughtUrl;
            case "fire" -> fireUrl;
            case "zoonotic" -> zoonoticUrl;
            case "mining" -> miningUrl;
            default -> throw new IllegalArgumentException("Unknown hazard: " + hazard);
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
