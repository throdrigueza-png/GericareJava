package com.example.Gericare.Service;

import java.util.List;

public interface EmailService {

    // Envío de correo de recuperación de contraseña
    void sendPasswordResetEmail(String to, String token);
    
    // Envío de correo de recuperación de contraseña cuando aún usa documento
    void sendPasswordResetEmailWithDocument(String to, String token, String documentNumber);

    // Envío de correo de bienvenida al registro
    void sendWelcomeEmail(String to, String nombre, String documentoIdentificacion);

    // Envío de correos masivos (Admin)
    void sendBulkEmail(List<String> recipients, String subject, String body);

    // Envío de notificación cuando un usuario cambia su correo
    void sendEmailChangeNotification(String newEmail, String userName);
}