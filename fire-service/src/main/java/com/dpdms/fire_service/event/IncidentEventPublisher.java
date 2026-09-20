package com.dpdms.fire_service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.UUID;

/**
 * Publishes an {@link IncidentEvent} when an incident is approved. Fire-and-forget on
 * purpose: incident approval must never block on alert delivery (FR-ALR).
 */
public class IncidentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(IncidentEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public IncidentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishApproved(IncidentEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID().toString());
        }
        String routingKey = RabbitCommonConfig.ROUTING_APPROVED_PREFIX + event.getHazardType();
        rabbitTemplate.convertAndSend(RabbitCommonConfig.EXCHANGE, routingKey, event);
        log.info("Published incident.approved event {} routingKey={} incidentId={}",
                event.getEventId(), routingKey, event.getIncidentId());
    }

    public static IncidentEvent approvedNow(IncidentEvent event, String approvedBy) {
        event.setEventId(UUID.randomUUID().toString());
        event.setApprovedBy(approvedBy);
        event.setApprovedAt(Instant.now().toString());
        return event;
    }
}
