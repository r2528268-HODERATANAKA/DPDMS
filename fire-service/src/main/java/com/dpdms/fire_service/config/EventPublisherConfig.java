package com.dpdms.fire_service.config;

import com.dpdms.fire_service.event.IncidentEventPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Provides the incident-event publisher wired to RabbitMQ. */
@Configuration
public class EventPublisherConfig {

    @Bean
    public IncidentEventPublisher incidentEventPublisher(RabbitTemplate rabbitTemplate) {
        return new IncidentEventPublisher(rabbitTemplate);
    }
}
