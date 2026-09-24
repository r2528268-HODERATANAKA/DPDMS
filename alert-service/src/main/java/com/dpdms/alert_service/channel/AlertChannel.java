package com.dpdms.alert_service.channel;

import com.dpdms.alert_service.model.AlertChannelType;
import com.dpdms.alert_service.model.AlertLog;

// One delivery channel (Email / WhatsApp / Telegram).
//
// This is the STRATEGY pattern: AlertService only knows this interface and
// loops over all implementations it finds in the Spring context. Adding a new
// channel later (e.g. SMS) = add ONE new class, no changes to AlertService.
//
// Implementations NEVER throw for business reasons - a failed provider call is
// returned as an AlertLog with status FAILED so one dead channel can never
// stop the others from sending.
public interface AlertChannel {

    AlertChannelType type();

    // Deliver "message" to "recipient"; always returns a log row (SENT/FAILED/SKIPPED)
    AlertLog send(AlertRequest request, String recipient);
}
