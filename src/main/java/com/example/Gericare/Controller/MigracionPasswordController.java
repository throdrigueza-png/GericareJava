package com.example.Gericare.Controller;

import com.example.Gericare.Service.MigracionPasswordService;
import com.example.Gericare.Service.MigracionPasswordService.MigrationReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller para administrar la migración de cuentas legacy.
 * 
 * Solo accesible por usuarios con rol Administrador.
 * 
 * Endpoints:
 * - GET  /admin/migracion-passwords: Ver reporte pre-migración
 * - POST /admin/migracion-passwords/ejecutar: Ejecutar migración
 */
@Controller
@RequestMapping("/admin/migracion-passwords")
@PreAuthorize("hasRole('Administrador')")
public class MigracionPasswordController {

    @Autowired
    private MigracionPasswordService migracionService;

    /**
     * Muestra la página de migración con un reporte preliminar
     */
    @GetMapping
    public String showMigrationPage(Model model) {
        // Obtener reporte sin hacer cambios
        MigrationReport preReport = migracionService.getPreMigrationReport();
        
        model.addAttribute("report", preReport);
        model.addAttribute("esPreReporte", true);
        
        return "admin/migracion-passwords";
    }

    /**
     * Ejecuta la migración de cuentas legacy
     */
    @PostMapping("/ejecutar")
    public String executeMigration(Model model, RedirectAttributes redirectAttributes) {
        try {
            // Ejecutar migración
            MigrationReport report = migracionService.migrateLegacyAccounts();
            
            // Agregar mensaje de éxito
            if (report.getUsuariosMigrados() > 0) {
                redirectAttributes.addFlashAttribute("successMessage", 
                    String.format("✅ Migración completada exitosamente. %d usuarios migrados.", 
                                report.getUsuariosMigrados()));
            } else {
                redirectAttributes.addFlashAttribute("infoMessage", 
                    "ℹ️ No se encontraron usuarios que requieran migración.");
            }
            
            // Si hubo errores, también notificar
            if (report.getUsuariosConError() > 0) {
                redirectAttributes.addFlashAttribute("warningMessage", 
                    String.format("⚠️ %d usuarios tuvieron errores durante la migración. Revisa los logs.", 
                                report.getUsuariosConError()));
            }
            
            redirectAttributes.addFlashAttribute("report", report);
            redirectAttributes.addFlashAttribute("esPreReporte", false);
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Error durante la migración: " + e.getMessage());
        }
        
        return "redirect:/admin/migracion-passwords";
    }
}
