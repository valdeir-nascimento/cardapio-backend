package com.cardapio.notification.infrastructure.template;

import com.cardapio.notification.domain.model.NotificationTemplate;
import com.cardapio.notification.domain.port.TemplateRenderer;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Map;

@Component
class ThymeleafTemplateRenderer implements TemplateRenderer {

    private final TemplateEngine emailHtml;
    private final TemplateEngine emailSubject;
    private final TemplateEngine whatsApp;

    ThymeleafTemplateRenderer() {
        this.emailHtml = engine("notification-templates/email/", ".html", TemplateMode.HTML);
        this.emailSubject = engine("notification-templates/email/", ".subject.txt", TemplateMode.TEXT);
        this.whatsApp = engine("notification-templates/whatsapp/", ".txt", TemplateMode.TEXT);
    }

    private static TemplateEngine engine(String prefix, String suffix, TemplateMode mode) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix(prefix);
        resolver.setSuffix(suffix);
        resolver.setTemplateMode(mode);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @Override
    public EmailContent renderEmail(NotificationTemplate template, Map<String, Object> model) {
        Context ctx = new Context();
        ctx.setVariables(model);
        String key = template.key();
        String subject = emailSubject.process(key, ctx).strip();
        String html = emailHtml.process(key, ctx);
        return new EmailContent(subject, html, null);
    }

    @Override
    public String renderWhatsApp(NotificationTemplate template, Map<String, Object> model) {
        Context ctx = new Context();
        ctx.setVariables(model);
        return whatsApp.process(template.key(), ctx).strip();
    }
}
