package com.example.Gericare.Service;

import com.example.Gericare.Entity.Usuario;
import com.example.Gericare.Repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para migrar cuentas legacy al nuevo sistema de contraseñas.
 * 
 * Este servicio identifica usuarios con contraseñas problemáticas y las actualiza
 * para que usen el documento de identificación como contraseña inicial.
 * 
 * USO:
 * 1. Inyectar este servicio en un Controller administrativo
 * 2. Llamar a migrateLegacyAccounts() desde un endpoint protegido
 * 3. Retornar el reporte al administrador
 */
@Service
public class MigracionPasswordService {

    private static final Logger logger = LoggerFactory.getLogger(MigracionPasswordService.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * DTO para el reporte de migración
     */
    public static class MigrationReport {
        private int totalUsuarios;
        private int usuariosMigrados;
        private int usuariosConError;
        private List<String> errores;
        private List<UsuarioMigrado> usuariosMigradosDetalle;

        public MigrationReport() {
            this.errores = new ArrayList<>();
            this.usuariosMigradosDetalle = new ArrayList<>();
        }

        // Getters y Setters
        public int getTotalUsuarios() { return totalUsuarios; }
        public void setTotalUsuarios(int totalUsuarios) { this.totalUsuarios = totalUsuarios; }
        
        public int getUsuariosMigrados() { return usuariosMigrados; }
        public void setUsuariosMigrados(int usuariosMigrados) { this.usuariosMigrados = usuariosMigrados; }
        
        public int getUsuariosConError() { return usuariosConError; }
        public void setUsuariosConError(int usuariosConError) { this.usuariosConError = usuariosConError; }
        
        public List<String> getErrores() { return errores; }
        public void setErrores(List<String> errores) { this.errores = errores; }
        
        public List<UsuarioMigrado> getUsuariosMigradosDetalle() { return usuariosMigradosDetalle; }
        public void setUsuariosMigradosDetalle(List<UsuarioMigrado> usuariosMigradosDetalle) { 
            this.usuariosMigradosDetalle = usuariosMigradosDetalle; 
        }

        public void addError(String error) {
            this.errores.add(error);
        }

        public void addUsuarioMigrado(UsuarioMigrado usuario) {
            this.usuariosMigradosDetalle.add(usuario);
        }
    }

    /**
     * DTO para detalles de usuario migrado
     */
    public static class UsuarioMigrado {
        private Long idUsuario;
        private String nombre;
        private String apellido;
        private String correoElectronico;
        private String documentoIdentificacion;
        private String rol;
        private String estadoAnterior;

        public UsuarioMigrado(Long idUsuario, String nombre, String apellido, 
                            String correoElectronico, String documentoIdentificacion,
                            String rol, String estadoAnterior) {
            this.idUsuario = idUsuario;
            this.nombre = nombre;
            this.apellido = apellido;
            this.correoElectronico = correoElectronico;
            this.documentoIdentificacion = documentoIdentificacion;
            this.rol = rol;
            this.estadoAnterior = estadoAnterior;
        }

        // Getters
        public Long getIdUsuario() { return idUsuario; }
        public String getNombre() { return nombre; }
        public String getApellido() { return apellido; }
        public String getCorreoElectronico() { return correoElectronico; }
        public String getDocumentoIdentificacion() { return documentoIdentificacion; }
        public String getRol() { return rol; }
        public String getEstadoAnterior() { return estadoAnterior; }
    }

    /**
     * Migra todas las cuentas legacy al nuevo sistema de contraseñas.
     * 
     * Identifica usuarios con:
     * - Contraseña NULL
     * - Contraseña sin hashear (no empieza con $2a$ o $2b$)
     * - Flag necesita_cambio_contrasena no configurado
     * 
     * Y los actualiza para usar su documento como contraseña inicial.
     * 
     * @return MigrationReport con el resumen de la migración
     */
    @Transactional
    public MigrationReport migrateLegacyAccounts() {
        logger.info("Iniciando migración de cuentas legacy...");
        
        MigrationReport report = new MigrationReport();
        List<Usuario> todosLosUsuarios = usuarioRepository.findAll();
        report.setTotalUsuarios(todosLosUsuarios.size());

        int migrados = 0;
        int errores = 0;

        for (Usuario usuario : todosLosUsuarios) {
            try {
                boolean necesitaMigracion = requiresMigration(usuario);
                
                if (necesitaMigracion) {
                    String estadoAnterior = getPasswordStatus(usuario);
                    
                    // Establecer contraseña como documento de identificación
                    String hashedPassword = passwordEncoder.encode(usuario.getDocumentoIdentificacion());
                    usuario.setContrasena(hashedPassword);
                    usuario.setNecesitaCambioContrasena(true);
                    
                    usuarioRepository.save(usuario);
                    
                    logger.info("Usuario migrado: {} {} ({})", 
                               usuario.getNombre(), 
                               usuario.getApellido(), 
                               usuario.getCorreoElectronico());
                    
                    report.addUsuarioMigrado(new UsuarioMigrado(
                        usuario.getIdUsuario(),
                        usuario.getNombre(),
                        usuario.getApellido(),
                        usuario.getCorreoElectronico(),
                        usuario.getDocumentoIdentificacion(),
                        usuario.getRol() != null ? usuario.getRol().getRolNombre().toString() : "SIN_ROL",
                        estadoAnterior
                    ));
                    
                    migrados++;
                }
            } catch (Exception e) {
                errores++;
                String errorMsg = String.format("Error al migrar usuario %s %s (%s): %s", 
                                              usuario.getNombre(),
                                              usuario.getApellido(),
                                              usuario.getCorreoElectronico(),
                                              e.getMessage());
                logger.error(errorMsg, e);
                report.addError(errorMsg);
            }
        }

        report.setUsuariosMigrados(migrados);
        report.setUsuariosConError(errores);

        logger.info("Migración completada. Usuarios migrados: {}, Errores: {}", migrados, errores);
        
        return report;
    }

    /**
     * Verifica si un usuario requiere migración
     */
    private boolean requiresMigration(Usuario usuario) {
        String password = usuario.getContrasena();
        
        // Contraseña NULL o vacía
        if (password == null || password.trim().isEmpty()) {
            return true;
        }
        
        // Contraseña sin hashear (no es BCrypt)
        if (!password.startsWith("$2a$") && !password.startsWith("$2b$")) {
            return true;
        }
        
        // Flag de cambio no configurado
        // Nota: Si el flag es null, también necesita migración
        // pero algunos usuarios válidos pueden tener el flag en true
        // así que solo migramos si la contraseña no está hasheada
        
        return false;
    }

    /**
     * Obtiene una descripción del estado de la contraseña del usuario
     */
    private String getPasswordStatus(Usuario usuario) {
        String password = usuario.getContrasena();
        
        if (password == null || password.trim().isEmpty()) {
            return "SIN_CONTRASEÑA";
        }
        
        if (!password.startsWith("$2a$") && !password.startsWith("$2b$")) {
            return "SIN_HASHEAR";
        }
        
        if (usuario.isNecesitaCambioContrasena()) {
            return "USA_DOCUMENTO";
        }
        
        return "OK";
    }

    /**
     * Obtiene un reporte de usuarios que necesitan migración (sin hacer cambios)
     */
    public MigrationReport getPreMigrationReport() {
        logger.info("Generando reporte pre-migración...");
        
        MigrationReport report = new MigrationReport();
        List<Usuario> todosLosUsuarios = usuarioRepository.findAll();
        report.setTotalUsuarios(todosLosUsuarios.size());

        int requierenMigracion = 0;

        for (Usuario usuario : todosLosUsuarios) {
            boolean necesitaMigracion = requiresMigration(usuario);
            
            if (necesitaMigracion) {
                String estadoAnterior = getPasswordStatus(usuario);
                
                report.addUsuarioMigrado(new UsuarioMigrado(
                    usuario.getIdUsuario(),
                    usuario.getNombre(),
                    usuario.getApellido(),
                    usuario.getCorreoElectronico(),
                    usuario.getDocumentoIdentificacion(),
                    usuario.getRol() != null ? usuario.getRol().getRolNombre().toString() : "SIN_ROL",
                    estadoAnterior
                ));
                
                requierenMigracion++;
            }
        }

        report.setUsuariosMigrados(0); // Aún no se ha migrado nadie
        report.setUsuariosConError(0);
        
        logger.info("Reporte pre-migración generado. Usuarios que requieren migración: {}", requierenMigracion);
        
        return report;
    }
}
