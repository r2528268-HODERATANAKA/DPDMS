package com.dpdms.alert_service.model;

// The three ways DPDMS can notify people.
// EMAIL     - plain SMTP (Gmail app password works; MOCK mode by default)
// WHATSAPP  - Meta WhatsApp Cloud API (MOCK mode by default)
// TELEGRAM  - Telegram Bot API (active once a bot token is configured)
public enum AlertChannelType {
    EMAIL,
    WHATSAPP,
    TELEGRAM
}
