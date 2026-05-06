package com.cardapio.notification.domain.port;

public interface EmailSender {

    /**
     * Sends an e-mail. Throws on transport failure or non-2xx provider response.
     *
     * @param to       recipient address
     * @param subject  short e-mail subject
     * @param html     rendered HTML body (preferred display)
     * @param text     plain-text fallback (optional, may be null)
     */
    void send(String to, String subject, String html, String text);
}
