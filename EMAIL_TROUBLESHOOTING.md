# Guía de Solución de Problemas de Correo Electrónico

## Problema Reportado
Los usuarios no reciben correos de recuperación de contraseña cuando intentan restablecer su contraseña usando la función "Recuperar Acceso".

## ¿Cómo Funciona el Sistema de Recuperación de Contraseña?

1. El usuario ingresa su correo electrónico en `/forgot-password`
2. El sistema genera un token único de recuperación
3. El token se guarda en la base de datos con una fecha de expiración (1 hora)
4. Se envía un correo electrónico con un enlace que contiene el token
5. El usuario hace clic en el enlace del correo para restablecer su contraseña

## Diagnóstico del Problema

### Paso 1: Revisar los Logs de la Aplicación

Con los cambios implementados, ahora el sistema registra información detallada sobre el proceso de envío de correos. Busca estos mensajes en los logs:

#### Logs Normales (Cuando Todo Funciona):
```
INFO  - Solicitud de recuperación de contraseña recibida para el correo: usuario@example.com
INFO  - Usuario encontrado: Juan Pérez (ID: 123)
INFO  - Token de recuperación generado y guardado exitosamente para el usuario: usuario@example.com
INFO  - Proceso de envío de correo iniciado para: usuario@example.com
INFO  - Iniciando envío de correo de recuperación de contraseña para: usuario@example.com
INFO  - Correo de recuperación de contraseña enviado exitosamente a: usuario@example.com
```

#### Logs de Error (Cuando Hay Problemas):
```
ERROR - Error al iniciar el envío del correo de recuperación para: usuario@example.com. Error: [mensaje de error]
ERROR - Error al enviar correo de recuperación de contraseña a: usuario@example.com. Error: [mensaje de error]
```

### Paso 2: Verificar la Configuración de Email

#### Variables de Entorno Requeridas

El sistema necesita estas variables de entorno configuradas:

| Variable | Descripción | Valor en Desarrollo | Valor en Azure |
|----------|-------------|---------------------|----------------|
| `MAIL_USERNAME` | Cuenta de Gmail | connectgericare@gmail.com | connectgericare@gmail.com |
| `MAIL_PASSWORD` | Contraseña de aplicación de Gmail | [contraseña local] | [contraseña de aplicación] |
| `APP_BASE_URL` | URL base de la aplicación | http://localhost:8080 | https://gericare-web-2026.azurewebsites.net |

#### Verificar Variables de Entorno en Azure

1. Ve al Portal de Azure (https://portal.azure.com)
2. Navega a tu App Service: **gericare-web-2026**
3. En el menú lateral, selecciona **Configuration** > **Application settings**
4. Verifica que estas variables existan y tengan valores correctos
5. Si haces cambios, guarda y reinicia la aplicación

### Paso 3: Verificar la Cuenta de Gmail

#### Configuración de Gmail Requerida

1. **Verificación en Dos Pasos**: Debe estar ACTIVADA
   - Ve a https://myaccount.google.com/security
   - Busca "Verificación en dos pasos" y actívala si no lo está

2. **Contraseña de Aplicación**: Debe ser generada y configurada
   - Ve a https://myaccount.google.com/apppasswords
   - Genera una nueva contraseña de aplicación
   - Usa esta contraseña (16 caracteres sin espacios) en la variable `MAIL_PASSWORD`

3. **Cuenta no Bloqueada**: 
   - Verifica que la cuenta no esté bloqueada por actividad sospechosa
   - Revisa https://myaccount.google.com/security

### Paso 4: Verificar Conectividad de Red

#### En Azure App Service:
```bash
# Verificar que Azure puede alcanzar los servidores de Gmail
tcpping smtp.gmail.com 587
```

Si el puerto 587 está bloqueado, el servicio no podrá enviar correos.

### Paso 5: Revisar la Base de Datos

Verifica que los correos electrónicos de los usuarios estén correctamente almacenados:

```sql
-- Buscar usuario por correo
SELECT * FROM usuarios WHERE correo_electronico = 'usuario@example.com';

-- Ver tokens de recuperación activos
SELECT id_usuario, correo_electronico, reset_password_token, reset_password_token_expiry_date 
FROM usuarios 
WHERE reset_password_token IS NOT NULL;
```

## Errores Comunes y Soluciones

### Error: "Authentication failed"
**Causa**: Contraseña incorrecta o cuenta sin contraseña de aplicación
**Solución**: 
- Verifica que estés usando una contraseña de aplicación, no la contraseña normal de Gmail
- Genera una nueva contraseña de aplicación
- Actualiza la variable de entorno `MAIL_PASSWORD`

### Error: "Connection timeout"
**Causa**: No se puede conectar al servidor SMTP de Gmail
**Solución**:
- Verifica que el puerto 587 esté abierto en el firewall
- Verifica la conectividad de red
- Intenta con otro servidor SMTP si es necesario

### Error: "Invalid Addresses"
**Causa**: Dirección de correo electrónico inválida
**Solución**:
- Verifica que el correo en la base de datos esté bien formado
- Verifica que `spring.mail.from` esté configurado correctamente

### Error: "User not found"
**Causa**: El correo electrónico no existe en la base de datos
**Solución**:
- El usuario debe estar registrado en el sistema
- Verifica que el correo esté escrito correctamente (sin espacios extra)

### No se muestra ningún error pero el correo no llega
**Causa Posible 1**: El correo está en la carpeta de spam
**Solución**: Verifica la carpeta de spam/correo no deseado

**Causa Posible 2**: El correo está siendo enviado pero con retraso
**Solución**: 
- Espera unos minutos (puede haber retraso en la entrega)
- Revisa los logs para confirmar que el envío fue exitoso

**Causa Posible 3**: El método @Async no está funcionando correctamente
**Solución**:
- Verifica que la aplicación tenga `@EnableAsync` en la clase principal
- Revisa los logs de Spring para ver si hay errores de configuración

## Cómo Habilitar Logs de Depuración

Para obtener más información sobre lo que está pasando, puedes habilitar logs de depuración de Spring Mail:

### En `application.properties`:
```properties
# Logs de depuración de Spring Mail
spring.mail.properties.mail.debug=true
logging.level.org.springframework.mail=DEBUG
logging.level.com.example.Gericare.Impl.EmailServiceImpl=DEBUG
logging.level.com.example.Gericare.Impl.UsuarioServiceImpl=DEBUG
```

Esto mostrará toda la comunicación SMTP en los logs, incluyendo:
- Conexión al servidor
- Autenticación
- Envío del mensaje
- Respuestas del servidor

## Prueba Manual del Sistema de Correo

### Usando la Aplicación Web:
1. Ve a `/forgot-password`
2. Ingresa un correo registrado
3. Revisa los logs inmediatamente después
4. Espera 1-2 minutos y revisa tu bandeja de entrada
5. Revisa la carpeta de spam si no llega

### Usando una Herramienta de Prueba SMTP:
Puedes probar la configuración SMTP directamente sin la aplicación:

```bash
# Instalar swaks (SMTP test tool)
# En Linux:
sudo apt-get install swaks

# Probar conexión SMTP
swaks --to destino@example.com \
      --from connectgericare@gmail.com \
      --server smtp.gmail.com:587 \
      --auth LOGIN \
      --auth-user connectgericare@gmail.com \
      --auth-password [TU_CONTRASEÑA_DE_APLICACION] \
      --tls
```

## Contacto para Soporte

Si después de seguir esta guía el problema persiste:
1. Recopila los logs completos del sistema
2. Documenta los pasos que seguiste
3. Incluye capturas de pantalla de la configuración
4. Contacta al equipo de desarrollo con toda esta información

## Referencias

- [Configuración de Contraseñas de Aplicación de Google](https://support.google.com/accounts/answer/185833)
- [Documentación de Spring Mail](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#mail)
- [Documentación de Azure App Service](https://docs.microsoft.com/en-us/azure/app-service/)
