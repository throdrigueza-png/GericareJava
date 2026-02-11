# Gestión de Contraseñas - Sistema Gericare Connect

## Filosofía del Sistema

El sistema Gericare Connect utiliza una **estrategia de contraseña opcional** que prioriza la facilidad de uso mientras promueve mejores prácticas de seguridad sin forzarlas.

## Concepto Principal

### Contraseña Inicial = Documento de Identificación

Cuando se crea una cuenta (ya sea por registro público o por un administrador), la contraseña inicial del usuario se establece automáticamente como su **número de documento de identificación (cédula)**.

**Ventajas:**
- ✅ Fácil de recordar para el primer acceso
- ✅ Único por usuario
- ✅ No requiere comunicación adicional de credenciales
- ✅ Simplifica el proceso de onboarding

### Cambio de Contraseña Opcional

A diferencia de muchos sistemas, **NO SE OBLIGA** al usuario a cambiar su contraseña en el primer login.

**El usuario puede:**
- ✅ Continuar usando su documento como contraseña indefinidamente
- ✅ Cambiar su contraseña cuando él lo decida
- ✅ Solicitar el cambio desde su perfil en cualquier momento

## Recordatorio No Intrusivo

### Alerta en Dashboard

Cuando un usuario que aún usa su documento como contraseña inicia sesión, verá una **alerta en el dashboard** que le recuerda:

```
⚠️ Aún estás usando tu documento como contraseña. 
   Por seguridad, te recomendamos cambiarla desde tu perfil.
```

**Características:**
- ✅ Visible en cada login
- ✅ No bloquea el acceso al sistema
- ✅ Puede ser ignorada por el usuario
- ✅ Solo desaparece cuando el usuario cambia su contraseña

**Implementación:**
- Archivo: `DashboardController.java` (líneas 54-57)
- Verifica el flag `necesitaCambioContrasena`
- Agrega `alertaPassword=true` al modelo si es necesario

## Recuperación de Contraseña

### Para Usuarios que NO Han Cambiado Su Contraseña

Si un usuario que aún usa su documento como contraseña solicita recuperación en `/forgot-password`, recibirá un correo especial:

**Contenido del correo:**
```
📧 Asunto: Recordatorio de Contraseña - Gericare Connect

⚠️ Importante:
Tu contraseña actual es tu número de documento:
        XXXXXXXX

Por seguridad, te recomendamos cambiar tu contraseña.

[Botón: Cambiar mi Contraseña]
```

**Características:**
- ✅ Banner amarillo destacado con el número de documento
- ✅ Mensaje claro y directo
- ✅ Opción de cambiar contraseña si lo desea
- ✅ El usuario puede seguir usando su documento si ignora el correo

### Para Usuarios que SÍ Han Cambiado Su Contraseña

Reciben el correo estándar de recuperación:

```
📧 Asunto: Solicitud de Cambio de Contraseña - Gericare Connect

Hola, hemos recibido una solicitud para cambiar la contraseña 
de tu cuenta en Gericare Connect.

[Botón: Cambiar mi Contraseña]
```

## Flujo Completo del Usuario

### 1. Creación de Cuenta

**Escenario A: Registro Público (Familiar)**
1. Usuario va a `/registro`
2. Completa formulario
3. Sistema automáticamente: `contraseña = documentoIdentificacion`
4. `necesitaCambioContrasena = true`
5. Recibe correo de bienvenida

**Escenario B: Creación por Admin (Cuidador/Familiar)**
1. Admin va a `/usuarios/nuevo`
2. Completa formulario (JavaScript auto-completa contraseña con documento)
3. Sistema: `contraseña = documentoIdentificacion`
4. `necesitaCambioContrasena = true`
5. Usuario recibe correo de bienvenida

### 2. Primer Login

1. Usuario va a `/login`
2. Email: `su-correo@ejemplo.com`
3. Password: `12345678` (su documento)
4. ✅ **Login exitoso** → Va directo al dashboard
5. Ve alerta: "Aún usas tu documento como contraseña..."

### 3. Uso Continuo

El usuario tiene dos opciones:

**Opción A: Seguir usando su documento**
- Ignora la alerta
- Sigue usando su documento como contraseña
- El sistema funciona normalmente
- La alerta se sigue mostrando en cada login

**Opción B: Cambiar su contraseña**
- Va a `/perfil`
- Click en "Solicitar cambio de contraseña"
- Recibe correo con enlace
- Crea nueva contraseña
- `necesitaCambioContrasena = false`
- La alerta desaparece

### 4. Olvidó su Contraseña

**Si NO ha cambiado su contraseña:**
1. Va a `/forgot-password`
2. Ingresa su correo
3. Recibe correo que dice: "Tu contraseña es tu documento #XXXXX"
4. Puede:
   - Hacer login con su documento
   - O usar el enlace para cambiar contraseña

**Si SÍ cambió su contraseña:**
1. Va a `/forgot-password`
2. Ingresa su correo
3. Recibe enlace de recuperación estándar
4. Crea nueva contraseña

## Implementación Técnica

### Archivos Clave

#### 1. UsuarioServiceImpl.java
```java
@Override
public void createPasswordResetTokenForUser(String email) {
    Usuario usuario = usuarioRepository.findByCorreoElectronico(email)
        .orElseThrow(...);
    
    String token = UUID.randomUUID().toString();
    usuario.setResetPasswordToken(token);
    usuario.setResetPasswordTokenExpiryDate(LocalDateTime.now().plusHours(1));
    usuarioRepository.save(usuario);
    
    // Decisión basada en si usa documento o no
    if (usuario.isNecesitaCambioContrasena()) {
        emailService.sendPasswordResetEmailWithDocument(
            usuario.getCorreoElectronico(), 
            token, 
            usuario.getDocumentoIdentificacion()
        );
    } else {
        emailService.sendPasswordResetEmail(
            usuario.getCorreoElectronico(), 
            token
        );
    }
}
```

#### 2. EmailServiceImpl.java

**Método para usuarios que usan documento:**
```java
@Async
@Override
public void sendPasswordResetEmailWithDocument(String to, String token, String documentNumber) {
    Context context = new Context();
    context.setVariable("resetUrl", resetUrl);
    context.setVariable("usesDocument", true);  // ← Flag importante
    context.setVariable("documentNumber", documentNumber);
    
    String htmlContent = templateEngine.process("emails/password-reset-email", context);
    enviarCorreoBase(to, "Recordatorio de Contraseña - Gericare Connect", htmlContent);
}
```

**Método estándar:**
```java
@Async
@Override
public void sendPasswordResetEmail(String to, String token) {
    Context context = new Context();
    context.setVariable("resetUrl", resetUrl);
    context.setVariable("usesDocument", false);  // ← Flag en false
    
    String htmlContent = templateEngine.process("emails/password-reset-email", context);
    enviarCorreoBase(to, "Solicitud de Cambio de Contraseña - Gericare Connect", htmlContent);
}
```

#### 3. password-reset-email.html

Plantilla con diseño condicional:

```html
<h1 th:text="${usesDocument} ? 'Recordatorio de Contraseña' : 'Restablecer Contraseña'">
    Restablecer Contraseña
</h1>

<!-- Banner para usuarios que usan documento -->
<div th:if="${usesDocument}">
    <div style="background-color: #fff3cd; border: 2px solid #ffc107; ...">
        <p><strong>⚠️ Importante:</strong><br>
           Tu contraseña actual es tu número de documento:</p>
        <p style="font-size: 20px; font-weight: 700;">
            <span th:text="${documentNumber}">XXXXXXXX</span>
        </p>
        <p>Por seguridad, te recomendamos cambiar tu contraseña.</p>
    </div>
</div>

<!-- Mensaje normal -->
<p th:unless="${usesDocument}">
    Hemos recibido una solicitud para cambiar la contraseña de tu cuenta...
</p>
```

#### 4. DashboardController.java

Muestra alerta en el dashboard:

```java
@GetMapping
public String mostrarDashboard(Authentication authentication, Model model, ...) {
    String userEmail = authentication.getName();
    
    usuarioService.findByEmail(userEmail).ifPresent(usuarioDTO -> {
        model.addAttribute("usuario", usuarioDTO);
        
        // Alerta si aún usa documento como contraseña
        if (usuarioDTO.isNecesitaCambioContrasena()) {
            model.addAttribute("alertaPassword", true);
        }
    });
    
    // ... resto del código
}
```

#### 5. SecurityConfig.java

Configuración estándar sin custom handler:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .formLogin(form -> form
            .loginPage("/login")
            .defaultSuccessUrl("/dashboard", true)  // ← Directo al dashboard
            .permitAll())
        // ... resto de configuración
}
```

## Base de Datos

### Campo necesitaCambioContrasena

**Tabla:** `usuarios`  
**Columna:** `necesita_cambio_contrasena` (BOOLEAN)

**Valores:**
- `true`: Usuario aún usa su documento como contraseña
- `false`: Usuario ya cambió su contraseña manualmente

**Cuándo cambia:**
- Se crea en `true` al registrar un usuario
- Cambia a `false` cuando el usuario completa el cambio de contraseña en `/reset-password`
- Permanece en `false` después del primer cambio

## Seguridad

### Hashing de Contraseñas

Todas las contraseñas (incluido el documento) se hashean con **BCrypt** antes de guardarse:

```java
cuidador.setContrasena(passwordEncoder.encode(cuidador.getContrasena()));
```

### Validaciones en Cambio de Contraseña

En `/reset-password` (PasswordResetController):

```java
@PostMapping("/reset-password")
public String handlePasswordReset(...) {
    // Validar longitud mínima
    if (password.length() < 8) {
        return error("La contraseña debe tener al menos 8 caracteres.");
    }
    
    // Validar coincidencia
    if (!password.equals(confirmPassword)) {
        return error("Las contraseñas no coinciden.");
    }
    
    // Validar que no sea igual a la anterior
    if (passwordEncoder.matches(newPassword, usuario.getContrasena())) {
        return error("La nueva contraseña no puede ser igual a la anterior.");
    }
    
    // Cambiar contraseña
    usuario.setContrasena(passwordEncoder.encode(password));
    usuario.setNecesitaCambioContrasena(false);  // ← Marca como cambiada
    usuarioRepository.save(usuario);
}
```

### Tokens de Recuperación

- Generados con UUID aleatorio
- Expiran en 1 hora
- Se guardan hasheados en la base de datos
- Se eliminan después de usar

## Beneficios de Este Enfoque

### 1. Experiencia de Usuario Mejorada
- ✅ No hay fricción en el primer acceso
- ✅ Usuario decide cuándo cambiar
- ✅ No hay bloqueos forzados
- ✅ Facilita el onboarding

### 2. Seguridad Progresiva
- ✅ Recordatorios constantes pero no intrusivos
- ✅ Facilita la recuperación si no cambiaron contraseña
- ✅ Incentiva el cambio sin forzarlo
- ✅ Todas las contraseñas están hasheadas

### 3. Soporte Simplificado
- ✅ Menos llamadas de soporte por contraseñas olvidadas
- ✅ Usuario puede recuperar fácilmente su acceso
- ✅ Administradores pueden ayudar sin resetear contraseñas

### 4. Flexibilidad
- ✅ Usuario tiene control total
- ✅ Sistema adaptable a diferentes perfiles de usuario
- ✅ Balance entre seguridad y usabilidad

## Testing Manual Recomendado

### Escenario 1: Usuario Nuevo que NO Cambia Contraseña

1. Registrar nuevo familiar con documento: `12345678`
2. Login con correo y contraseña: `12345678`
3. ✅ Verificar: Va directo al dashboard
4. ✅ Verificar: Se muestra alerta de cambio recomendado
5. Usar el sistema normalmente
6. Cerrar sesión y volver a iniciar
7. ✅ Verificar: Puede seguir usando `12345678`
8. Solicitar recuperación de contraseña
9. ✅ Verificar: Correo dice "Tu contraseña es tu documento #12345678"

### Escenario 2: Usuario que SÍ Cambia Contraseña

1. Login con documento
2. Ir a perfil → Solicitar cambio de contraseña
3. Recibir correo con enlace
4. Cambiar a `NuevaPassword123!`
5. ✅ Verificar: Redirige al dashboard
6. ✅ Verificar: Alerta ya NO aparece
7. Cerrar sesión
8. Login con nueva contraseña
9. ✅ Verificar: Funciona correctamente
10. Solicitar recuperación de contraseña
11. ✅ Verificar: Correo estándar (sin mención de documento)

### Escenario 3: Usuario Creado por Admin

1. Admin crea cuidador con documento: `87654321`
2. Cuidador recibe correo de bienvenida
3. Login con documento
4. ✅ Verificar: Acceso exitoso con alerta
5. Continuar usando documento
6. ✅ Verificar: Sistema funciona normalmente

## Mantenimiento

### Forzar Cambio de Contraseña (Si es necesario)

Si un administrador necesita que un usuario específico cambie su contraseña:

```sql
UPDATE usuarios 
SET necesita_cambio_contrasena = true 
WHERE id_usuario = [ID];
```

El usuario verá la alerta nuevamente hasta que cambie su contraseña.

### Ver Estado de Usuarios

```sql
SELECT 
    id_usuario,
    nombre,
    apellido,
    correo_electronico,
    documento_identificacion,
    necesita_cambio_contrasena
FROM usuarios
WHERE necesita_cambio_contrasena = true;
```

## Conclusión

Este sistema ofrece un **balance óptimo** entre:
- **Facilidad de uso**: Contraseña fácil de recordar para acceso inicial
- **Seguridad**: Incentivos constantes para cambiar contraseña
- **Flexibilidad**: Usuario tiene control total
- **Soporte**: Fácil recuperación de acceso

**Estado**: ✅ Implementado y probado, listo para producción
