package com.cardapio.notification.domain.port;

public interface WhatsAppSender {

    /**
     * Sends a plain-text WhatsApp message.
     *
     * @param phoneE164 phone number in E.164 format (e.g. {@code +5511999998888})
     * @param body      text body
     */
    void sendText(String phoneE164, String body);
}
