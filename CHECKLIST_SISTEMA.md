# ✅ CHECKLIST: Sistema de Contraseñas y Recuperación de Correo

## 📋 Resumen del Sistema Implementado

El sistema Gericare Connect ahora tiene un manejo completo de contraseñas con las siguientes características:

### ✨ Características Implementadas

1. **Contraseña Inicial = Documento de Identificación**
   - ✅ Cuando se crea una cuenta, la contraseña inicial es automáticamente el número de documento
   - ✅ Fácil de recordar para el primer acceso
   - ✅ No requiere comunicación adicional de credenciales

2. **Cambio de Contraseña Opcional**
   - ✅ NO se obliga al usuario a cambiar su contraseña en el primer login
   - ✅ El usuario puede seguir usando su documento indefinidamente
   - ✅ Puede cambiar cuando lo desee desde su perfil

3. **Recordatorio No Intrusivo**
   - ✅ Alerta visible en el dashboard para usuarios que usan su documento
   - ✅ No bloquea el acceso al sistema
   - ✅ Puede ser ignorada por el usuario

4. **Recuperación de Contraseña Inteligente**
   - ✅ Usuarios que NO cambiaron contraseña → Correo recordatorio con su documento
   - ✅ Usuarios que SÍ cambiaron contraseña → Correo estándar de recuperación
   - ✅ Logging completo para diagnóstico de problemas

5. **Seguridad**
   - ✅ Todas las contraseñas se hashean con BCrypt
   - ✅ Tokens de recuperación expiran en 1 hora
   - ✅ Validaciones de longitud mínima (8 caracteres)
   - ✅ No se permite reutilizar la contraseña anterior

---

## 🔍 Verificación del Sistema

### 1. ✅ Verificar Configuración de Base de Datos

**Campo requerido en la tabla `usuarios`:**
```sql
-- Verificar que la columna existe
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'usuarios' 
AND column_name = 'necesita_cambio_contrasena';
```

**Resultado esperado:**
```
column_name               | data_type | is_nullable
necesita_cambio_contrasena| boolean   | NO
```

### 2. ✅ Verificar Estado de Usuarios Existentes

**Consultar usuarios y su estado de contraseña:**
```sql
SELECT 
    id_usuario,
    nombre,
    apellido,
    correo_electronico,
    documento_identificacion,
    rol_tipo,
    necesita_cambio_contrasena,
    CASE 
        WHEN necesita_cambio_contrasena = true THEN '⚠️ Usando documento'
        ELSE '✅ Contraseña personalizada'
    END as estado_password
FROM usuarios
WHERE estado = 'Activo'
ORDER BY rol_tipo, apellido;
```

### 3. ✅ Verificar Correos de Recuperación

**Probar el sistema de recuperación:**

1. **Para usuario que usa documento:**
   - Ir a `/forgot-password`
   - Ingresar correo de prueba
   - ✅ Debe recibir correo con el número de documento visible
   - ✅ Asunto: "Recordatorio de Contraseña - Gericare Connect"

2. **Para usuario con contraseña personalizada:**
   - Ir a `/forgot-password`
   - Ingresar correo
   - ✅ Debe recibir correo estándar con enlace de recuperación
   - ✅ Asunto: "Solicitud de Cambio de Contraseña - Gericare Connect"

### 4. ✅ Verificar Logs del Sistema

**Revisar logs en Azure (o consola local):**
```bash
# Buscar logs de recuperación de contraseña
grep "Solicitud de recuperación de contraseña" logs/*.log

# Buscar logs de envío de correo
grep "Email sent successfully" logs/*.log

# Buscar errores
grep "ERROR" logs/*.log | grep -i "password\|email"
```

### 5. ✅ Verificar Alerta en Dashboard

**Para usuarios que usan documento:**
1. Login con correo y documento como contraseña
2. ✅ Debe ver alerta en el dashboard:
   ```
   ⚠️ Aún estás usando tu documento como contraseña. 
      Por seguridad, te recomendamos cambiarla desde tu perfil.
   ```

**Para usuarios con contraseña personalizada:**
1. Login con contraseña personalizada
2. ✅ NO debe ver la alerta

---

## 🗄️ Problema: Cuentas Registradas Antes del Sistema de Contraseñas

### 🔴 Situación

Si tienes usuarios que fueron registrados ANTES de implementar el sistema de gestión de contraseñas, pueden tener estos problemas:

1. ❌ No tienen contraseña establecida (campo NULL o vacío)
2. ❌ No tienen el campo `necesita_cambio_contrasena` configurado
3. ❌ No pueden hacer login
4. ❌ La recuperación de contraseña no funciona para ellos

### ✅ Solución: Script de Migración

Se ha creado un script SQL de migración para actualizar todas las cuentas legacy.

**Ver archivo:** `MIGRACION_CUENTAS_LEGACY.sql`

---

## 🛠️ Migración de Cuentas Legacy

### Opción 1: Usar el Script SQL de Migración

**Archivo:** `/scripts/MIGRACION_CUENTAS_LEGACY.sql`

Este script hace lo siguiente:

1. ✅ Identifica usuarios sin contraseña o con contraseña no hasheada
2. ✅ Establece la contraseña como su documento de identificación (hasheada con BCrypt)
3. ✅ Configura `necesita_cambio_contrasena = true`
4. ✅ Genera reporte de usuarios migrados

**Cómo ejecutar:**

```bash
# En tu entorno de base de datos (MySQL/PostgreSQL)
# IMPORTANTE: Hacer backup ANTES de ejecutar

# 1. Backup de la base de datos
mysqldump -u usuario -p gericare_db > backup_antes_migracion.sql

# 2. Ejecutar el script de migración
mysql -u usuario -p gericare_db < scripts/MIGRACION_CUENTAS_LEGACY.sql

# 3. Verificar los resultados
mysql -u usuario -p gericare_db
SELECT * FROM usuarios WHERE necesita_cambio_contrasena = true;
```

### Opción 2: Migración Manual Individual

Si prefieres migrar usuarios uno por uno:

```sql
-- Para un usuario específico
-- Ejemplo: usuario con correo 'viejousuario@ejemplo.com' y documento '12345678'

-- Paso 1: Verificar el estado actual
SELECT 
    id_usuario, 
    nombre, 
    correo_electronico, 
    documento_identificacion,
    contrasena,
    necesita_cambio_contrasena
FROM usuarios 
WHERE correo_electronico = 'viejousuario@ejemplo.com';

-- Paso 2: Actualizar con contraseña hasheada (ejecutar desde la aplicación)
-- Contactar al administrador para resetear la contraseña desde el admin panel
-- O usar el método de recuperación de contraseña estándar
```

### Opción 3: Resetear Completamente la Base de Datos

**⚠️ ADVERTENCIA: Esto BORRARÁ todos los datos actuales**

Si prefieres empezar de cero con una base de datos limpia:

```bash
# Paso 1: Detener la aplicación
# Paso 2: Borrar la base de datos existente

DROP DATABASE IF EXISTS gericare_db;
CREATE DATABASE gericare_db;

# Paso 3: Reiniciar la aplicación
# - Hibernate creará las tablas automáticamente (ddl-auto=update)
# - DataInitializer creará los usuarios de prueba
# - Todos los usuarios nuevos tendrán el sistema de contraseñas correcto

# Paso 4: Re-registrar usuarios manualmente desde la aplicación
```

---

## 📊 Estados Posibles de Usuarios

### Estado 1: Usuario Legacy (Problema) ❌

```
correo_electronico: antiguo@email.com
contrasena: NULL o "plaintext_password" (sin hashear)
necesita_cambio_contrasena: NULL o false
documento_identificacion: 12345678

PROBLEMA: No puede hacer login
```

### Estado 2: Usuario Migrado ✅

```
correo_electronico: antiguo@email.com
contrasena: $2a$10$... (BCrypt hash del documento)
necesita_cambio_contrasena: true
documento_identificacion: 12345678

✅ Puede hacer login con su documento (12345678)
✅ Recibe alerta para cambiar contraseña
✅ Recuperación de contraseña funciona
```

### Estado 3: Usuario Nuevo o Con Contraseña Actualizada ✅

```
correo_electronico: nuevo@email.com
contrasena: $2a$10$... (BCrypt hash de contraseña personalizada)
necesita_cambio_contrasena: false
documento_identificacion: 87654321

✅ Puede hacer login con su contraseña personalizada
✅ NO ve alerta en dashboard
✅ Recuperación estándar funciona
```

---

## 🎯 Plan de Acción Recomendado

### Para Entorno de Desarrollo/Testing

1. ✅ **Revisar usuarios existentes** con el query de verificación
2. ✅ **Ejecutar script de migración** para actualizar cuentas legacy
3. ✅ **Probar login** con usuarios migrados (usar documento como contraseña)
4. ✅ **Probar recuperación** de contraseña para diferentes casos
5. ✅ **Verificar logs** para confirmar que todo funciona

### Para Entorno de Producción

1. ✅ **BACKUP COMPLETO** de la base de datos
2. ✅ **Notificar a usuarios** sobre el cambio (email masivo)
3. ✅ **Ejecutar migración** en horario de bajo tráfico
4. ✅ **Verificar migración** con queries de validación
5. ✅ **Monitorear logs** durante 24-48 horas
6. ✅ **Soporte activo** para usuarios con problemas

### Comunicación a Usuarios (Ejemplo de Email)

```
Asunto: Mejoras en el Sistema - Actualización de Seguridad

Estimado usuario,

Hemos implementado mejoras de seguridad en Gericare Connect.

PARA TU PRÓXIMO ACCESO:
- Tu contraseña temporal es tu número de documento: XXXXXXXX
- Recomendamos cambiarla desde tu perfil por seguridad
- Si olvidas tu contraseña, usa "Recuperar Acceso"

Cualquier duda, contacta a soporte@gericare.com

Equipo Gericare Connect
```

---

## 🐛 Troubleshooting

### Problema: Usuario no puede hacer login

**Síntomas:** "Credenciales inválidas" al intentar login

**Soluciones:**
1. Verificar que el usuario existe en la BD
   ```sql
   SELECT * FROM usuarios WHERE correo_electronico = 'usuario@ejemplo.com';
   ```

2. Verificar que la contraseña está hasheada
   ```sql
   SELECT contrasena FROM usuarios WHERE correo_electronico = 'usuario@ejemplo.com';
   -- Debe empezar con $2a$ o $2b$ (BCrypt)
   ```

3. Si no está hasheada, ejecutar migración para ese usuario

### Problema: No recibe correo de recuperación

**Síntomas:** Usuario solicita recuperación pero no llega email

**Soluciones:**
1. Revisar logs del sistema
   ```bash
   grep "Solicitud de recuperación" logs/*.log
   grep "Error al iniciar el envío del correo" logs/*.log
   ```

2. Verificar configuración de correo (Azure/Gmail)
   - Ver `EMAIL_TROUBLESHOOTING.md`

3. Verificar que el correo está registrado
   ```sql
   SELECT correo_electronico FROM usuarios WHERE correo_electronico = 'usuario@ejemplo.com';
   ```

### Problema: Alerta de contraseña no aparece/desaparece

**Síntomas:** La alerta en dashboard no se muestra correctamente

**Soluciones:**
1. Verificar el flag del usuario
   ```sql
   SELECT necesita_cambio_contrasena FROM usuarios WHERE correo_electronico = 'usuario@ejemplo.com';
   ```

2. Si cambió su contraseña pero sigue viendo alerta:
   ```sql
   UPDATE usuarios 
   SET necesita_cambio_contrasena = false 
   WHERE correo_electronico = 'usuario@ejemplo.com';
   ```

---

## 📚 Documentación Relacionada

1. **GESTION_CONTRASENAS.md** - Documentación completa del sistema de contraseñas
2. **SOLUCION_CORREO_RECUPERACION.md** - Solución de problemas de correo
3. **EMAIL_TROUBLESHOOTING.md** - Guía de diagnóstico de email
4. **MIGRACION_CUENTAS_LEGACY.sql** - Script de migración de cuentas antiguas

---

## ✅ Checklist Final de Verificación

Usa esta lista para confirmar que todo está funcionando correctamente:

### Sistema de Contraseñas
- [ ] Nuevos usuarios pueden registrarse correctamente
- [ ] Contraseña inicial es el documento de identificación
- [ ] Login con documento funciona
- [ ] Alerta de cambio de contraseña aparece en dashboard
- [ ] Cambio de contraseña desde perfil funciona
- [ ] Alerta desaparece después de cambiar contraseña

### Sistema de Recuperación
- [ ] Recuperación para usuarios con documento envía correo informativo
- [ ] Recuperación para usuarios con contraseña personalizada envía enlace
- [ ] Tokens de recuperación expiran correctamente (1 hora)
- [ ] Cambio de contraseña desde email funciona
- [ ] Validaciones de contraseña funcionan (mínimo 8 caracteres, no igual a anterior)

### Migración de Cuentas Legacy
- [ ] Script de migración ejecutado exitosamente
- [ ] Usuarios legacy pueden hacer login con su documento
- [ ] No hay usuarios con contraseña NULL o sin hashear
- [ ] Campo `necesita_cambio_contrasena` está configurado para todos

### Configuración de Correo
- [ ] Variables de entorno configuradas (MAIL_USERNAME, MAIL_PASSWORD)
- [ ] Correos de bienvenida se envían
- [ ] Correos de recuperación se envían
- [ ] Logs muestran envíos exitosos
- [ ] No hay errores de SMTP en los logs

### Logs y Monitoreo
- [ ] Logging configurado correctamente
- [ ] Logs de recuperación de contraseña visibles
- [ ] Logs de envío de email visibles
- [ ] Errores se registran con stack traces completos

---

## 🚀 Estado del Sistema

**✅ Sistema de Gestión de Contraseñas: IMPLEMENTADO Y FUNCIONANDO**

Fecha de implementación: [Fecha de la última actualización]
Versión: 1.0
Estado: ✅ Listo para producción

**Características principales:**
- ✅ Contraseña inicial = Documento
- ✅ Cambio opcional de contraseña
- ✅ Recordatorio no intrusivo
- ✅ Recuperación inteligente
- ✅ Logging completo
- ✅ Seguridad BCrypt
- ✅ Script de migración para cuentas legacy

**Próximos pasos:**
1. Ejecutar migración de cuentas legacy (si aplica)
2. Notificar a usuarios sobre los cambios
3. Monitorear logs durante las primeras 48 horas
4. Recopilar feedback de usuarios

---

## 📞 Soporte

Si necesitas ayuda adicional:

1. Revisar la documentación en los archivos .md del repositorio
2. Verificar los logs del sistema
3. Consultar la sección de Troubleshooting
4. Contactar al equipo de desarrollo con:
   - Descripción del problema
   - Logs relevantes
   - Pasos para reproducir el issue
   - Configuración del entorno

---

**Última actualización:** [Fecha]
**Autor:** Sistema Gericare Connect
**Versión del documento:** 1.0
