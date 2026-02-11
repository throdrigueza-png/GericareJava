-- =====================================================
-- SCRIPT DE MIGRACIÓN DE CUENTAS LEGACY
-- Gericare Connect - Sistema de Gestión de Contraseñas
-- =====================================================
-- 
-- PROPÓSITO:
-- Este script migra usuarios existentes que fueron creados ANTES de 
-- implementar el sistema de gestión de contraseñas.
--
-- QUÉ HACE:
-- 1. Identifica usuarios con contraseñas problemáticas (NULL, vacías, o sin hashear)
-- 2. Establece la contraseña como el documento de identificación
-- 3. Marca necesita_cambio_contrasena = true para que vean la alerta
-- 4. Genera reportes de usuarios migrados
--
-- ⚠️ IMPORTANTE: 
-- - Hacer BACKUP de la base de datos ANTES de ejecutar
-- - Revisar el reporte de usuarios afectados
-- - Notificar a los usuarios sobre el cambio
--
-- =====================================================

-- =====================================================
-- PASO 1: VERIFICACIÓN PREVIA
-- =====================================================

-- Ver usuarios actuales y su estado
SELECT 
    '===== ESTADO ACTUAL DE USUARIOS =====' as mensaje;

SELECT 
    id_usuario,
    nombre,
    apellido,
    correo_electronico,
    documento_identificacion,
    rol_tipo,
    CASE 
        WHEN contrasena IS NULL THEN '❌ SIN CONTRASEÑA'
        WHEN contrasena NOT LIKE '$2a$%' AND contrasena NOT LIKE '$2b$%' THEN '⚠️ NO HASHEADA'
        WHEN necesita_cambio_contrasena = true THEN '⚠️ USA DOCUMENTO'
        ELSE '✅ OK'
    END as estado_contrasena,
    necesita_cambio_contrasena
FROM usuarios
WHERE estado = 'Activo'
ORDER BY 
    CASE 
        WHEN contrasena IS NULL THEN 1
        WHEN contrasena NOT LIKE '$2a$%' AND contrasena NOT LIKE '$2b$%' THEN 2
        ELSE 3
    END,
    rol_tipo;

-- Contar usuarios problemáticos
SELECT 
    '===== USUARIOS QUE REQUIEREN MIGRACIÓN =====' as mensaje;

SELECT 
    COUNT(*) as total_usuarios_problemáticos,
    SUM(CASE WHEN contrasena IS NULL THEN 1 ELSE 0 END) as sin_contrasena,
    SUM(CASE WHEN contrasena IS NOT NULL AND contrasena NOT LIKE '$2a$%' AND contrasena NOT LIKE '$2b$%' THEN 1 ELSE 0 END) as sin_hashear,
    SUM(CASE WHEN necesita_cambio_contrasena IS NULL THEN 1 ELSE 0 END) as sin_flag_cambio
FROM usuarios
WHERE estado = 'Activo'
AND (
    contrasena IS NULL 
    OR (contrasena NOT LIKE '$2a$%' AND contrasena NOT LIKE '$2b$%')
    OR necesita_cambio_contrasena IS NULL
);

-- =====================================================
-- PASO 2: BACKUP DE SEGURIDAD (Crear tabla temporal)
-- =====================================================

DROP TABLE IF EXISTS usuarios_backup_migracion;

CREATE TABLE usuarios_backup_migracion AS
SELECT * FROM usuarios;

SELECT 
    '===== BACKUP CREADO =====' as mensaje,
    COUNT(*) as usuarios_respaldados
FROM usuarios_backup_migracion;

-- =====================================================
-- PASO 3: MIGRACIÓN DE CONTRASEÑAS
-- =====================================================

-- NOTA IMPORTANTE:
-- Este script NO puede hashear las contraseñas directamente en SQL
-- porque BCrypt requiere la implementación de Java.
-- 
-- SOLUCIÓN: Marcar los usuarios para que la aplicación los procese
-- o usar un endpoint administrativo.
--
-- Por ahora, actualizamos el flag necesita_cambio_contrasena
-- para que el sistema sepa que estos usuarios necesitan configuración.

-- Actualizar flag para usuarios sin contraseña hasheada
UPDATE usuarios 
SET necesita_cambio_contrasena = true
WHERE estado = 'Activo'
AND (
    contrasena IS NULL 
    OR (contrasena NOT LIKE '$2a$%' AND contrasena NOT LIKE '$2b$%')
    OR necesita_cambio_contrasena IS NULL
);

SELECT 
    '===== USUARIOS MARCADOS PARA MIGRACIÓN =====' as mensaje,
    ROW_COUNT() as usuarios_actualizados;

-- =====================================================
-- PASO 4: REPORTE DE USUARIOS MIGRADOS
-- =====================================================

SELECT 
    '===== USUARIOS QUE NECESITAN NUEVA CONTRASEÑA =====' as mensaje;

SELECT 
    id_usuario,
    nombre,
    apellido,
    correo_electronico,
    documento_identificacion,
    rol_tipo,
    '⚠️ DEBE USAR DOCUMENTO COMO CONTRASEÑA: ' as instruccion,
    documento_identificacion as contraseña_temporal
FROM usuarios
WHERE estado = 'Activo'
AND necesita_cambio_contrasena = true
ORDER BY rol_tipo, apellido;

-- =====================================================
-- PASO 5: VERIFICACIÓN POST-MIGRACIÓN
-- =====================================================

SELECT 
    '===== VERIFICACIÓN FINAL =====' as mensaje;

SELECT 
    rol_tipo,
    COUNT(*) as total_usuarios,
    SUM(CASE WHEN necesita_cambio_contrasena = true THEN 1 ELSE 0 END) as usan_documento,
    SUM(CASE WHEN necesita_cambio_contrasena = false THEN 1 ELSE 0 END) as contraseña_personalizada
FROM usuarios
WHERE estado = 'Activo'
GROUP BY rol_tipo
ORDER BY rol_tipo;

-- =====================================================
-- PASO 6: INSTRUCCIONES PARA COMPLETAR LA MIGRACIÓN
-- =====================================================

/*
NOTA IMPORTANTE: PASOS ADICIONALES REQUERIDOS

Este script SQL solo marca los usuarios que necesitan migración.
Para COMPLETAR la migración, debes hacer UNA de estas opciones:

OPCIÓN 1: Usar el endpoint administrativo de la aplicación
-----------------------------------------------------------
1. Crear un endpoint en el backend (ver código Java más abajo)
2. Ejecutar el endpoint como administrador
3. El endpoint hasheará las contraseñas usando BCrypt

OPCIÓN 2: Migración manual por usuario
---------------------------------------
1. Cada usuario usa "Recuperar Contraseña" desde la aplicación
2. Reciben un correo con instrucciones
3. Pueden crear su nueva contraseña

OPCIÓN 3: Resetear contraseñas desde el admin panel
----------------------------------------------------
1. Admin accede al panel de usuarios
2. Resetea la contraseña de cada usuario
3. Notifica al usuario su nueva contraseña temporal (documento)

OPCIÓN 4: Script de aplicación (Java)
--------------------------------------
Ver archivo: scripts/MigracionPasswordService.java
Ejecutar como parte de la aplicación Spring Boot

RECOMENDACIÓN: Usar OPCIÓN 4 (Script Java)
*/

-- =====================================================
-- PASO 7: ROLLBACK (Solo si hay problemas)
-- =====================================================

/*
-- EN CASO DE EMERGENCIA: RESTAURAR DESDE BACKUP

-- Desactivar verificaciones de foreign keys temporalmente
SET FOREIGN_KEY_CHECKS = 0;

-- Restaurar usuarios desde backup
TRUNCATE TABLE usuarios;
INSERT INTO usuarios SELECT * FROM usuarios_backup_migracion;

-- Reactivar verificaciones
SET FOREIGN_KEY_CHECKS = 1;

-- Verificar restauración
SELECT COUNT(*) as usuarios_restaurados FROM usuarios;
SELECT '===== ROLLBACK COMPLETADO =====' as mensaje;
*/

-- =====================================================
-- PASO 8: LIMPIEZA (Opcional - después de verificar)
-- =====================================================

/*
-- Eliminar tabla de backup después de confirmar que todo funciona
-- SOLO ejecutar después de varios días de pruebas exitosas

DROP TABLE IF EXISTS usuarios_backup_migracion;
SELECT '===== BACKUP ELIMINADO =====' as mensaje;
*/

-- =====================================================
-- FIN DEL SCRIPT
-- =====================================================

SELECT 
    '=====================================' as '',
    'MIGRACIÓN SQL COMPLETADA' as mensaje,
    '=====================================' as ' ';

SELECT 
    'PRÓXIMOS PASOS:' as instrucciones,
    '1. Revisar el reporte de usuarios migrados' as paso_1,
    '2. Ejecutar el servicio Java para hashear contraseñas' as paso_2,
    '3. Notificar a usuarios sobre el cambio' as paso_3,
    '4. Probar login con algunos usuarios' as paso_4,
    '5. Monitorear logs durante 24-48 horas' as paso_5;
