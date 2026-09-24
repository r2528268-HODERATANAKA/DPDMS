package com.dpdms.alert_service.controller;

import com.dpdms.alert_service.channel.AlertRequest;
import com.dpdms.alert_service.model.AlertLog;
import com.dpdms.alert_service.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Alert endpoints:
//   POST /api/alerts/send        - send one alert through all channels (hazard
//                                  services call this automatically on approval;
//                                  also handy for manual demos)
//   POST /api/alerts/scan        - pull approved incidents from all five hazard
//                                  services and alert any not alerted yet
//   GET  /api/alerts             - full alert log (newest first)
//   GET  /api/alerts/hazard/{h}  - log filtered by hazard
//   GET  /api/alerts/config      - which channels have real recipients configured
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService service;

    @PostMapping("/send")
    public ResponseEntity<List<AlertLog>> send(@Valid @RequestBody AlertRequest request) {
        return ResponseEntity.ok(service.sendAlert(request));
    }

    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scan() {
        return ResponseEntity.ok(service.scanApproved());
    }

    @GetMapping
    public ResponseEntity<List<AlertLog>> all() {
        return ResponseEntity.ok(service.allLogs());
    }

    @GetMapping("/hazard/{hazard}")
    public ResponseEntity<List<AlertLog>> byHazard(@PathVariable String hazard) {
        return ResponseEntity.ok(service.logsForHazard(hazard));
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> config() {
        // No secrets here - just enough to see what is live during a demo
        return ResponseEntity.ok(Map.of(
                "guide", "Configure recipients in application.properties " +
                        "(alert.recipients.email / whatsapp / telegram, comma separated)",
                "channels", "EMAIL, WHATSAPP, TELEGRAM (mock mode until configured)"));
    }
}
