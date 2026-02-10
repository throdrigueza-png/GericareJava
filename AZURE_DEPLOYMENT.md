# Configuración de Variables de Entorno para Azure

Para que el sistema de correo funcione correctamente en Azure, es necesario configurar las siguientes variables de entorno en la configuración de la aplicación de Azure.

## Variables de Entorno Requeridas

### 1. APP_BASE_URL
- **Nombre:** `APP_BASE_URL`
- **Valor:** `https://gericare-web-2026.azurewebsites.net`
- **Descripción:** URL base de la aplicación desplegada en Azure. Se usa para generar enlaces en los correos electrónicos (recuperación de contraseña, enlaces de login, etc.)

### 2. MAIL_USERNAME
- **Nombre:** `MAIL_USERNAME`
- **Valor:** `connectgericare@gmail.com`
- **Descripción:** Dirección de correo electrónico desde la cual se enviarán los correos del sistema

### 3. MAIL_PASSWORD
- **Nombre:** `MAIL_PASSWORD`
- **Valor:** `pqvg qjoq yeio wlau` (Contraseña de aplicación de Gmail)
- **Descripción:** Contraseña de aplicación de Gmail. IMPORTANTE: Esta debe ser una "Contraseña de aplicación" generada en la configuración de seguridad de Gmail, NO la contraseña regular de la cuenta.

## Cómo Configurar las Variables de Entorno en Azure

1. Ve al portal de Azure (https://portal.azure.com)
2. Navega a tu App Service: **gericare-web-2026**
3. En el menú lateral, selecciona **Configuration** (Configuración)
4. En la pestaña **Application settings**, haz clic en **+ New application setting**
5. Agrega cada una de las variables mencionadas arriba con sus valores correspondientes
6. Haz clic en **Save** para guardar los cambios
7. Reinicia la aplicación para que los cambios surtan efecto

## Verificación

Después de configurar las variables de entorno:

1. Verifica que la aplicación se haya reiniciado correctamente
2. Intenta registrar un nuevo usuario para verificar que se envíe el correo de bienvenida
3. Prueba la funcionalidad de recuperación de contraseña en `/forgot-password`
4. Los administradores pueden probar el envío de correos masivos desde `/admin/correos/nuevo`

## Funcionalidades de Correo

El sistema incluye las siguientes funcionalidades de correo:

1. **Correo de Bienvenida:** Se envía automáticamente cuando un nuevo usuario (Familiar) se registra en el sistema
2. **Recuperación de Contraseña:** Permite a los usuarios recuperar su contraseña mediante un enlace enviado por correo
3. **Correos Masivos (Admin):** Los administradores pueden enviar correos masivos a grupos de usuarios (Cuidadores, Familiares, o ambos)
4. **Notificación de Cambio de Correo:** Cuando un usuario actualiza su correo electrónico, se envía una notificación al nuevo correo

## Notas de Seguridad

- La contraseña de aplicación de Gmail debe mantenerse confidencial
- No compartas las credenciales del correo en el código fuente
- Considera rotar las credenciales periódicamente
- Verifica que la cuenta de Gmail tenga habilitado el acceso a aplicaciones menos seguras o use contraseñas de aplicación

## Solución de Problemas

Si los correos no se están enviando:

1. Verifica que todas las variables de entorno estén configuradas correctamente en Azure
2. Verifica que la contraseña de aplicación de Gmail sea correcta
3. Revisa los logs de la aplicación en Azure para ver errores específicos
4. Verifica que la cuenta de Gmail no esté bloqueada o suspendida
5. Asegúrate de que el servidor SMTP de Gmail (smtp.gmail.com:587) sea accesible desde Azure
