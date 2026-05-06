package com.cardapio.notification.domain.port;

import com.cardapio.notification.domain.model.NotificationTemplate;

import java.util.Map;

public interface TemplateRenderer {

    record EmailContent(String subject, String html, String text) {}

    EmailContent renderEmail(NotificationTemplate template, Map<String, Object> model);

    String renderWhatsApp(NotificationTemplate template, Map<String, Object> model);
}
