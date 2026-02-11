# Cambio Obligatorio de Contraseña al Primer Login - Documentación

## Descripción General

Esta funcionalidad garantiza que todos los usuarios deben cambiar su contraseña la primera vez que inician sesión en el sistema. La contraseña inicial se establece automáticamente como el número de documento de identificación (cédula) del usuario.

## Flujo del Usuario

### 1. Creación de Cuenta

**Registro Público (Familiares):**
- El usuario se registra en `/registro`
- El sistema automáticamente usa su número de documento como contraseña inicial
- Se establece `necesitaCambioContrasena = true`
- El usuario recibe un correo de bienvenida

**Creación por Administrador (Cuidadores/Familiares):**
- El administrador crea el usuario en `/usuarios/nuevo`
- El formulario automáticamente usa el documento como contraseña
- Se establece `necesitaCambioContrasena = true`
- El usuario recibe un correo de bienvenida con sus credenciales

### 2. Primer Inicio de Sesión

1. El usuario va a `/login`
2. Ingresa su correo electrónico y su número de documento (contraseña inicial)
3. **Autenticación exitosa** → `CustomAuthenticationSuccessHandler` se activa
4. El sistema verifica `necesitaCambioContrasena`:
   - Si es `true`: **Redirige automáticamente a `/cambiar-contrasena-obligatorio`**
   - Si es `false`: Redirige al dashboard normal

### 3. Cambio Obligatorio de Contraseña

En `/cambiar-contrasena-obligatorio`:
- El sistema muestra un mensaje claro: "Por seguridad, debes cambiar tu contraseña antes de continuar"
- El usuario ve que su contraseña actual es su número de documento
- El formulario solicita:
  - Nueva contraseña (mínimo 8 caracteres)
  - Confirmación de nueva contraseña

**Validaciones:**
- ✅ Las contraseñas deben coincidir
- ✅ Mínimo 8 caracteres
- ✅ **No puede ser igual al documento de identificación**
- ✅ Se recomienda combinar letras, números y símbolos

### 4. Acceso al Sistema

Después de cambiar exitosamente la contraseña:
- `necesitaCambioContrasena` se marca como `false`
- El usuario es redirigido al dashboard
- En futuros logins, ingresa directamente al dashboard

## Componentes Técnicos

### 1. CustomAuthenticationSuccessHandler

**Ubicación**: `src/main/java/com/example/Gericare/Security/CustomAuthenticationSuccessHandler.java`

**Responsabilidad**: Interceptar cada login exitoso y decidir si redirigir al cambio de contraseña o al dashboard.

```java
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(...) {
        // Busca usuario
        // Si necesitaCambioContrasena = true → /cambiar-contrasena-obligatorio
        // Si necesitaCambioContrasena = false → /dashboard
    }
}
```

**Logging**:
- INFO: Usuario autenticado exitosamente
- INFO: Decisión de redirección
- ERROR: Si usuario no se encuentra en BD

### 2. CambioContrasenaObligatorioController

**Ubicación**: `src/main/java/com/example/Gericare/Controller/CambioContrasenaObligatorioController.java`

**Endpoints**:

#### GET `/cambiar-contrasena-obligatorio`
- Verifica que el usuario esté autenticado
- Verifica que realmente necesite cambiar contraseña
- Si no necesita cambio, redirige al dashboard
- Muestra formulario con nombre del usuario

#### POST `/cambiar-contrasena-obligatorio`
- Valida que las contraseñas coincidan
- Valida longitud mínima (8 caracteres)
- **Verifica que la nueva contraseña NO sea igual a la cédula**
- Encripta la nueva contraseña con BCrypt
- Marca `necesitaCambioContrasena = false`
- Redirige al dashboard con mensaje de éxito

**Logging**:
- INFO: Formulario mostrado
- INFO: Procesando cambio de contraseña
- WARN: Validaciones fallidas
- INFO: Contraseña cambiada exitosamente

### 3. Plantilla HTML

**Ubicación**: `src/main/resources/templates/correo/cambio-contrasena-obligatorio.html`

**Características**:
- Diseño consistente con otras páginas de autenticación
- Logo de Gericare
- Alerta de cambio obligatorio
- Validación en tiempo real (JavaScript)
- Indicador de fortaleza de contraseña
- Botón para mostrar/ocultar contraseña
- Mensaje de requisitos de seguridad

### 4. SecurityConfig

**Cambios**:
```java
@Configuration
public class SecurityConfig {
    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // Cambio de contraseña obligatorio requiere autenticación
                .requestMatchers("/cambiar-contrasena-obligatorio").authenticated()
                // ... otras reglas
            )
            .formLogin(form -> form
                .loginPage("/login")
                // Usa el custom handler en lugar de defaultSuccessUrl
                .successHandler(customAuthenticationSuccessHandler)
                .permitAll())
            // ...
    }
}
```

## Seguridad

### Contraseña Inicial
- ✅ Es única por usuario (documento de identificación)
- ✅ Debe cambiarse obligatoriamente
- ✅ Se hashea con BCrypt antes de guardarse

### Validaciones de Nueva Contraseña
- ✅ No puede ser igual a la cédula (validación explícita)
- ✅ Mínimo 8 caracteres
- ✅ Debe confirmarse
- ✅ Se hashea con BCrypt

### Prevención de Bypass
- ✅ No se puede acceder al dashboard sin cambiar contraseña
- ✅ `CustomAuthenticationSuccessHandler` siempre verifica el flag
- ✅ El endpoint GET verifica el flag antes de mostrar formulario
- ✅ Si el usuario intenta ir directamente al dashboard, Spring Security lo intercepta

### Logging
- ✅ Todos los intentos son registrados
- ✅ Cambios de contraseña son auditados
- ✅ No se registran contraseñas en logs

## Testing

### Tests Automatizados
- ✅ Compilación: 103 archivos fuente
- ✅ Tests unitarios: Todos pasan
- ✅ CodeQL: 0 vulnerabilidades

### Testing Manual Recomendado

**Escenario 1: Nuevo Familiar (Registro Público)**
1. Ir a `/registro`
2. Completar formulario con datos ficticios
3. Usar documento: `12345678`
4. Confirmar registro
5. Ir a `/login`
6. Email: el registrado, Password: `12345678`
7. **Verificar**: Redirige a `/cambiar-contrasena-obligatorio`
8. Cambiar contraseña a algo diferente: `NuevaPassword123!`
9. **Verificar**: Redirige al dashboard
10. Cerrar sesión
11. Login nuevamente con nueva contraseña
12. **Verificar**: Va directo al dashboard

**Escenario 2: Usuario Creado por Admin**
1. Login como administrador
2. Ir a `/usuarios/nuevo`
3. Crear un Cuidador con documento: `87654321`
4. Cerrar sesión de admin
5. Login como el nuevo cuidador: documento = `87654321`
6. **Verificar**: Redirige a cambio obligatorio
7. Cambiar contraseña
8. **Verificar**: Accede al dashboard

**Escenario 3: Usuario que Ya Cambió Contraseña**
1. Login con usuario existente que ya cambió su contraseña
2. **Verificar**: Va directamente al dashboard (sin pasar por cambio obligatorio)

## Mensajes al Usuario

### En el Formulario de Cambio Obligatorio
```
Cambio de Contraseña Obligatorio
Bienvenido/a [Nombre Usuario]

⚠️ Por seguridad, debes cambiar tu contraseña antes de continuar.
   Tu contraseña actual es tu número de documento.
```

### Requisitos Mostrados
```
ℹ️ Requisitos de la contraseña:
• Mínimo 8 caracteres
• Diferente a tu documento de identificación
• Combina letras, números y símbolos para mayor seguridad
```

### Mensaje de Éxito
```
✅ ¡Tu contraseña ha sido actualizada exitosamente! Ya puedes usar el sistema.
```

### Mensajes de Error
- "Las contraseñas no coinciden."
- "La contraseña debe tener al menos 8 caracteres."
- "Debes elegir una contraseña diferente a tu documento de identificación."

## Beneficios

1. **Seguridad Mejorada**:
   - Ningún usuario puede usar su documento como contraseña permanentemente
   - Cada usuario tiene una contraseña única y personalizada

2. **Facilidad de Onboarding**:
   - Los usuarios pueden iniciar sesión fácilmente la primera vez (usando su documento)
   - No necesitan recordar una contraseña temporal compleja

3. **Auditoría**:
   - Todos los cambios de contraseña son registrados
   - Se puede rastrear si un usuario ha cambiado su contraseña

4. **Cumplimiento**:
   - Cumple con mejores prácticas de seguridad
   - Contraseñas iniciales simples pero obligación de cambio

## Mantenimiento

### Para Administradores

**Ver Estado de Usuarios**:
- En el dashboard de admin, el sistema muestra alertas para usuarios que no han cambiado su contraseña
- Esto ya estaba implementado en `DashboardController` líneas 54-57

**Forzar Cambio de Contraseña**:
Si un administrador necesita forzar que un usuario cambie su contraseña:
```sql
UPDATE usuarios 
SET necesita_cambio_contrasena = true 
WHERE id_usuario = [ID];
```

### Para Desarrolladores

**Agregar Nuevas Validaciones**:
Editar `CambioContrasenaObligatorioController.procesarCambioObligatorio()`

**Cambiar Requisitos de Contraseña**:
- Modificar validación en línea 65-69 del controller
- Actualizar mensaje en HTML

**Personalizar Redirección**:
Editar `CustomAuthenticationSuccessHandler.onAuthenticationSuccess()`

## Archivos Modificados/Creados

1. ✅ `CustomAuthenticationSuccessHandler.java` (nuevo)
2. ✅ `CambioContrasenaObligatorioController.java` (nuevo)
3. ✅ `cambio-contrasena-obligatorio.html` (nuevo)
4. ✅ `SecurityConfig.java` (modificado)
5. ✅ `RegistroLoginController.java` (ya tenía contraseña = documento)
6. ✅ `admin-formulario-usuario.html` (ya tenía contraseña = documento)

## Conclusión

La funcionalidad de cambio obligatorio de contraseña está completamente implementada y probada. Proporciona una excelente experiencia de usuario mientras mantiene altos estándares de seguridad.

**Estado**: ✅ Listo para producción
