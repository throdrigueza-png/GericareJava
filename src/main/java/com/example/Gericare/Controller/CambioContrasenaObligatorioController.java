package com.example.Gericare.Controller;

import com.example.Gericare.Entity.Usuario;
import com.example.Gericare.Repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CambioContrasenaObligatorioController {

    private static final Logger logger = LoggerFactory.getLogger(CambioContrasenaObligatorioController.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/cambiar-contrasena-obligatorio")
    public String mostrarFormularioCambioObligatorio(Authentication authentication, Model model) {
        String userEmail = authentication.getName();
        logger.info("Mostrando formulario de cambio de contraseña obligatorio para: {}", userEmail);
        
        // Verificar que el usuario realmente necesita cambiar la contraseña
        Usuario usuario = usuarioRepository.findByCorreoElectronico(userEmail).orElse(null);
        if (usuario == null) {
            logger.warn("Usuario no encontrado: {}", userEmail);
            return "redirect:/login";
        }
        
        if (!usuario.isNecesitaCambioContrasena()) {
            logger.info("Usuario {} no necesita cambiar contraseña, redirigiendo a dashboard", userEmail);
            return "redirect:/dashboard";
        }
        
        model.addAttribute("nombreUsuario", usuario.getNombre() + " " + usuario.getApellido());
        model.addAttribute("email", userEmail);
        return "correo/cambio-contrasena-obligatorio";
    }

    @PostMapping("/cambiar-contrasena-obligatorio")
    public String procesarCambioObligatorio(
            Authentication authentication,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {
        
        String userEmail = authentication.getName();
        logger.info("Procesando cambio de contraseña obligatorio para: {}", userEmail);
        
        // Validar que las contraseñas coincidan
        if (!password.equals(confirmPassword)) {
            logger.warn("Las contraseñas no coinciden para el usuario: {}", userEmail);
            redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden.");
            return "redirect:/cambiar-contrasena-obligatorio";
        }
        
        // Validar longitud mínima
        if (password.length() < 8) {
            logger.warn("Contraseña muy corta para el usuario: {}", userEmail);
            redirectAttributes.addFlashAttribute("error", "La contraseña debe tener al menos 8 caracteres.");
            return "redirect:/cambiar-contrasena-obligatorio";
        }
        
        // Buscar el usuario
        Usuario usuario = usuarioRepository.findByCorreoElectronico(userEmail).orElse(null);
        if (usuario == null) {
            logger.error("Usuario no encontrado al procesar cambio de contraseña: {}", userEmail);
            return "redirect:/login";
        }
        
        // Verificar que no esté usando la misma contraseña (la cédula)
        if (passwordEncoder.matches(password, usuario.getContrasena())) {
            logger.warn("Usuario {} intentó usar la misma contraseña (cédula)", userEmail);
            redirectAttributes.addFlashAttribute("error", "Debes elegir una contraseña diferente a tu documento de identificación.");
            return "redirect:/cambiar-contrasena-obligatorio";
        }
        
        // Actualizar la contraseña
        usuario.setContrasena(passwordEncoder.encode(password));
        usuario.setNecesitaCambioContrasena(false);
        usuarioRepository.save(usuario);
        
        logger.info("Contraseña cambiada exitosamente para el usuario: {}", userEmail);
        
        redirectAttributes.addFlashAttribute("successMessage", "¡Tu contraseña ha sido actualizada exitosamente! Ya puedes usar el sistema.");
        return "redirect:/dashboard";
    }
}
