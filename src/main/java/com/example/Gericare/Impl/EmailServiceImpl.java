package com.example.Gericare.Impl;

import com.example.Gericare.Service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;
    // componente que ejecuta el envío a través de smtp

    @Autowired
    private TemplateEngine templateEngine;
    // motor que procesa plantillas html

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    // url base usada dentro de los correos

    @Value("${spring.mail.from:connectgericare@gmail.com}")
    private String fromEmail;
    // correo remitente configurado en application.properties

    // Reseteo de Contraseña
    @Async
    @Override
    public void sendPasswordResetEmail(String to, String token) {
        try {
            logger.info("Iniciando envío de correo de recuperación de contraseña para: {}", to);
            logger.debug("Configuración - Base URL: {}, From Email: {}", baseUrl, fromEmail);
            
            String resetUrl = baseUrl + "/reset-password?token=" + token;
            Context context = new Context();
            context.setVariable("resetUrl", resetUrl);
            context.setVariable("email", to);
            context.setVariable("appBaseUrl", baseUrl);

            String htmlContent = templateEngine.process("emails/password-reset-email", context);
            enviarCorreoBase(to, "Solicitud de Cambio de Contraseña - Gericare Connect", htmlContent);
            
            logger.info("Correo de recuperación de contraseña enviado exitosamente a: {}", to);

        } catch (MessagingException e) {
            logger.error("Error al enviar correo de recuperación de contraseña a: {}. Error: {}", to, e.getMessage(), e);
            throw new IllegalStateException("Fallo al enviar el correo de reseteo.", e);
        } catch (Exception e) {
            logger.error("Error inesperado al enviar correo de recuperación de contraseña a: {}. Error: {}", to, e.getMessage(), e);
            throw new IllegalStateException("Error inesperado al enviar el correo de reseteo.", e);
        }
    }

    // Bienvenida
    //@Async
    @Override
    public void sendWelcomeEmail(String to, String nombre, String documentoIdentificacion) {
        try {
            logger.info("Enviando correo de bienvenida a: {}", to);
            
            Context context = new Context();
            context.setVariable("nombreUsuario", nombre);
            context.setVariable("documentoIdentificacion", documentoIdentificacion);
            context.setVariable("email", to);
            context.setVariable("loginUrl", baseUrl + "/login");
            context.setVariable("appBaseUrl", baseUrl);

            String htmlContent = templateEngine.process("emails/welcome-email", context);
            enviarCorreoBase(to, "¡Bienvenido a Gericare Connect!", htmlContent);
            
            logger.info("Correo de bienvenida enviado exitosamente a: {}", to);

        } catch (MessagingException e) {
            logger.error("Error al enviar correo de bienvenida a: {}. Error: {}", to, e.getMessage(), e);
            throw new IllegalStateException("Fallo al enviar el correo de bienvenida.", e);
        } catch (Exception e) {
            logger.error("Error inesperado al enviar correo de bienvenida a: {}. Error: {}", to, e.getMessage(), e);
            throw new IllegalStateException("Error inesperado al enviar el correo de bienvenida.", e);
        }
    }

    // Correo Masivo
    @Async
    @Override
    public void sendBulkEmail(List<String> recipients, String subject, String body) {
        // evita continuar si no hay destinatarios
        if (recipients == null || recipients.isEmpty()) {
            logger.warn("Intento de enviar correo masivo sin destinatarios");
            return;
        }

        try {
            logger.info("Enviando correo masivo a {} destinatarios. Asunto: {}", recipients.size(), subject);
            
            // convierte el texto plano a html con estilos básicos
            String formattedBody = formatTextToHtml(body);

            // crea un contexto con los datos que se insertan en la plantilla
            Context context = new Context();
            context.setVariable("subject", subject);
            context.setVariable("body", formattedBody);

            String htmlContent = templateEngine.process("emails/bulk-email", context); // procesa la plantilla thymeleaf para generar el html final
            MimeMessage mimeMessage = mailSender.createMimeMessage(); // crea un objeto mime que soporta html, adjuntos y multiformato
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8"); // helper que facilita la configuración del mensaje
            helper.setSubject(subject); // asigna asunto
            helper.setText(htmlContent, true); // asigna el html generado
            helper.setFrom(fromEmail); // asigna remitente
            helper.setTo(fromEmail); // asigna el campo to al remitente para ocultar destinatarios (correo se envía a sí mismo)
            helper.setBcc(recipients.toArray(new String[0])); // asigna los destinatarios reales en copia oculta (verdaderos destinatarios están ocultos)

            // agrega los logos inline si existen
            agregarLogosInline(helper);

            // ejecuta el envío a través del servidor smtp
            mailSender.send(mimeMessage);
            
            logger.info("Correo masivo enviado exitosamente a {} destinatarios", recipients.size());

        } catch (MessagingException e) {
            // registra un error si falla el envío
            logger.error("Error al enviar correo masivo a {} destinatarios. Error: {}", recipients.size(), e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error inesperado al enviar correo masivo. Error: {}", e.getMessage(), e);
        }
    }

    // Notificación Cambio de Correo
    @Async
    @Override
    public void sendEmailChangeNotification(String newEmail, String userName) {
        try {
            logger.info("Enviando notificación de cambio de correo a: {}", newEmail);
            
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("newEmail", newEmail);
            context.setVariable("appBaseUrl", baseUrl);

            String htmlContent = templateEngine.process("emails/email-change-notification", context);
            enviarCorreoBase(newEmail, "Tu correo en Gericare ha sido actualizado", htmlContent);
            
            logger.info("Notificación de cambio de correo enviada exitosamente a: {}", newEmail);

        } catch (MessagingException e) {
            logger.error("Error al enviar notificación de cambio de correo a: {}. Error: {}", newEmail, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error inesperado al enviar notificación de cambio de correo. Error: {}", e.getMessage(), e);
        }
    }

    // Métodos Auxiliares Privados (pa evitar repetir cod)

    private void enviarCorreoBase(String to, String subject, String htmlContent) throws MessagingException {
        logger.debug("Preparando envío de correo - Para: {}, Asunto: {}, Remitente: {}", to, subject, fromEmail);
        
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        helper.setFrom(fromEmail);

        agregarLogosInline(helper);

        logger.debug("Enviando correo a través del servidor SMTP...");
        mailSender.send(mimeMessage);
        logger.debug("Correo enviado exitosamente a través del servidor SMTP");
    }

    private void agregarLogosInline(MimeMessageHelper helper) {
        try {
            ClassPathResource logo = new ClassPathResource("static/images/Geri_Logo-..png");
            if (logo.exists()) helper.addInline("geriLogo", logo);
        } catch (Exception e) {

        }
    }



    // convierte un texto plano en un bloque html con estilos simples
    private String formatTextToHtml(String text) {
        String pStyle = "style=\"font-family: 'Poppins', Arial, sans-serif; font-size: 16px; color: #444444; line-height: 1.7; margin: 0 0 15px 0; text-align: center;\"";

        if (text == null || text.isBlank())
            return "<p " + pStyle + ">Sin contenido.</p>";

        // escapa caracteres para evitar errores html
        String safeText = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

        // reemplaza saltos de línea por etiquetas html
        String html = safeText.trim()
                .replaceAll("\r\n", "\n")
                .replaceAll("\n{2,}", "</p><p " + pStyle + ">")
                .replaceAll("\n", "<br>");

        return "<p " + pStyle + ">" + html + "</p>";
    }
}