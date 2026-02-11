package com.example.Gericare.Security;

import com.example.Gericare.Entity.Usuario;
import com.example.Gericare.Repository.UsuarioRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationSuccessHandler.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        String username = authentication.getName();
        logger.info("Usuario autenticado exitosamente: {}", username);
        
        // Buscar el usuario en la base de datos para verificar si necesita cambiar contraseña
        Usuario usuario = usuarioRepository.findByCorreoElectronico(username).orElse(null);
        
        if (usuario == null) {
            logger.error("Usuario autenticado pero no encontrado en BD: {}", username);
            response.sendRedirect("/login?error");
            return;
        }
        
        if (usuario.isNecesitaCambioContrasena()) {
            logger.info("Usuario {} necesita cambiar su contraseña, redirigiendo a /cambiar-contrasena-obligatorio", username);
            response.sendRedirect("/cambiar-contrasena-obligatorio");
        } else {
            logger.info("Usuario {} no necesita cambiar contraseña, redirigiendo a dashboard", username);
            response.sendRedirect("/dashboard");
        }
    }
}
