package com.example.Gericare.Service;

import com.example.Gericare.Entity.*;
import com.example.Gericare.Enums.EstadoUsuario;
import com.example.Gericare.Enums.RolNombre;
import com.example.Gericare.Enums.TipoDocumento;
import com.example.Gericare.Repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MigracionPasswordServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MigracionPasswordService migracionPasswordService;

    private Rol rolFamiliar;
    private Rol rolCuidador;

    @BeforeEach
    void setUp() {
        rolFamiliar = new Rol();
        rolFamiliar.setRolNombre(RolNombre.Familiar);
        rolFamiliar.setDescripcion("Rol de Familiar");

        rolCuidador = new Rol();
        rolCuidador.setRolNombre(RolNombre.Cuidador);
        rolCuidador.setDescripcion("Rol de Cuidador");
    }

    @Test
    void testMigrateLegacyAccounts_WithPasswordNull() {
        // Given: Usuario con contraseña NULL
        Familiar familiar = new Familiar();
        familiar.setIdUsuario(1L);
        familiar.setNombre("Juan");
        familiar.setApellido("Pérez");
        familiar.setCorreoElectronico("juan@test.com");
        familiar.setDocumentoIdentificacion("12345678");
        familiar.setContrasena(null); // Sin contraseña
        familiar.setRol(rolFamiliar);
        familiar.setEstado(EstadoUsuario.Activo);

        List<Usuario> usuarios = new ArrayList<>();
        usuarios.add(familiar);

        when(usuarioRepository.findAll()).thenReturn(usuarios);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(familiar);

        // When
        MigracionPasswordService.MigrationReport report = migracionPasswordService.migrateLegacyAccounts();

        // Then
        assertEquals(1, report.getTotalUsuarios());
        assertEquals(1, report.getUsuariosMigrados());
        assertEquals(0, report.getUsuariosConError());
        verify(passwordEncoder, times(1)).encode("12345678");
        verify(usuarioRepository, times(1)).save(familiar);
    }

    @Test
    void testMigrateLegacyAccounts_WithUnhashedPassword() {
        // Given: Usuario con contraseña sin hashear (texto plano)
        Cuidador cuidador = new Cuidador();
        cuidador.setIdUsuario(2L);
        cuidador.setNombre("María");
        cuidador.setApellido("González");
        cuidador.setCorreoElectronico("maria@test.com");
        cuidador.setDocumentoIdentificacion("87654321");
        cuidador.setContrasena("plainTextPassword"); // Sin hashear
        cuidador.setRol(rolCuidador);
        cuidador.setEstado(EstadoUsuario.Activo);

        List<Usuario> usuarios = new ArrayList<>();
        usuarios.add(cuidador);

        when(usuarioRepository.findAll()).thenReturn(usuarios);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(cuidador);

        // When
        MigracionPasswordService.MigrationReport report = migracionPasswordService.migrateLegacyAccounts();

        // Then
        assertEquals(1, report.getTotalUsuarios());
        assertEquals(1, report.getUsuariosMigrados());
        assertEquals(0, report.getUsuariosConError());
        verify(passwordEncoder, times(1)).encode("87654321");
        verify(usuarioRepository, times(1)).save(cuidador);
    }

    @Test
    void testMigrateLegacyAccounts_WithValidHashedPassword() {
        // Given: Usuario con contraseña válida (BCrypt)
        Familiar familiar = new Familiar();
        familiar.setIdUsuario(3L);
        familiar.setNombre("Pedro");
        familiar.setApellido("López");
        familiar.setCorreoElectronico("pedro@test.com");
        familiar.setDocumentoIdentificacion("11223344");
        familiar.setContrasena("$2a$10$validHashedPassword"); // Ya hasheada
        familiar.setRol(rolFamiliar);
        familiar.setEstado(EstadoUsuario.Activo);
        familiar.setNecesitaCambioContrasena(false);

        List<Usuario> usuarios = new ArrayList<>();
        usuarios.add(familiar);

        when(usuarioRepository.findAll()).thenReturn(usuarios);

        // When
        MigracionPasswordService.MigrationReport report = migracionPasswordService.migrateLegacyAccounts();

        // Then
        assertEquals(1, report.getTotalUsuarios());
        assertEquals(0, report.getUsuariosMigrados()); // No debe migrar
        assertEquals(0, report.getUsuariosConError());
        verify(passwordEncoder, never()).encode(anyString());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void testMigrateLegacyAccounts_MixedUsers() {
        // Given: Mezcla de usuarios
        Familiar familiar1 = new Familiar();
        familiar1.setIdUsuario(1L);
        familiar1.setNombre("Ana");
        familiar1.setApellido("Torres");
        familiar1.setCorreoElectronico("ana@test.com");
        familiar1.setDocumentoIdentificacion("11111111");
        familiar1.setContrasena(null); // Requiere migración
        familiar1.setRol(rolFamiliar);

        Cuidador cuidador1 = new Cuidador();
        cuidador1.setIdUsuario(2L);
        cuidador1.setNombre("Carlos");
        cuidador1.setApellido("Ruiz");
        cuidador1.setCorreoElectronico("carlos@test.com");
        cuidador1.setDocumentoIdentificacion("22222222");
        cuidador1.setContrasena("$2a$10$validHash"); // No requiere migración
        cuidador1.setRol(rolCuidador);
        cuidador1.setNecesitaCambioContrasena(false);

        Familiar familiar2 = new Familiar();
        familiar2.setIdUsuario(3L);
        familiar2.setNombre("Luis");
        familiar2.setApellido("Mora");
        familiar2.setCorreoElectronico("luis@test.com");
        familiar2.setDocumentoIdentificacion("33333333");
        familiar2.setContrasena("plaintext"); // Requiere migración
        familiar2.setRol(rolFamiliar);

        List<Usuario> usuarios = new ArrayList<>();
        usuarios.add(familiar1);
        usuarios.add(cuidador1);
        usuarios.add(familiar2);

        when(usuarioRepository.findAll()).thenReturn(usuarios);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        MigracionPasswordService.MigrationReport report = migracionPasswordService.migrateLegacyAccounts();

        // Then
        assertEquals(3, report.getTotalUsuarios());
        assertEquals(2, report.getUsuariosMigrados()); // Solo 2 requieren migración
        assertEquals(0, report.getUsuariosConError());
        verify(passwordEncoder, times(2)).encode(anyString());
        verify(usuarioRepository, times(2)).save(any(Usuario.class));
    }

    @Test
    void testGetPreMigrationReport() {
        // Given: Usuarios que requieren migración
        Familiar familiar = new Familiar();
        familiar.setIdUsuario(1L);
        familiar.setNombre("Laura");
        familiar.setApellido("Castro");
        familiar.setCorreoElectronico("laura@test.com");
        familiar.setDocumentoIdentificacion("99999999");
        familiar.setContrasena(null);
        familiar.setRol(rolFamiliar);

        List<Usuario> usuarios = new ArrayList<>();
        usuarios.add(familiar);

        when(usuarioRepository.findAll()).thenReturn(usuarios);

        // When
        MigracionPasswordService.MigrationReport report = migracionPasswordService.getPreMigrationReport();

        // Then
        assertEquals(1, report.getTotalUsuarios());
        assertEquals(0, report.getUsuariosMigrados()); // Pre-reporte, aún no se ha migrado
        assertEquals(0, report.getUsuariosConError());
        assertEquals(1, report.getUsuariosMigradosDetalle().size());
        verify(passwordEncoder, never()).encode(anyString()); // No debe hashear en pre-reporte
        verify(usuarioRepository, never()).save(any(Usuario.class)); // No debe guardar en pre-reporte
    }

    @Test
    void testMigrateLegacyAccounts_WithError() {
        // Given: Usuario que causará error al guardar
        Familiar familiar = new Familiar();
        familiar.setIdUsuario(1L);
        familiar.setNombre("Error");
        familiar.setApellido("Usuario");
        familiar.setCorreoElectronico("error@test.com");
        familiar.setDocumentoIdentificacion("88888888");
        familiar.setContrasena(null);
        familiar.setRol(rolFamiliar);

        List<Usuario> usuarios = new ArrayList<>();
        usuarios.add(familiar);

        when(usuarioRepository.findAll()).thenReturn(usuarios);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenThrow(new RuntimeException("Database error"));

        // When
        MigracionPasswordService.MigrationReport report = migracionPasswordService.migrateLegacyAccounts();

        // Then
        assertEquals(1, report.getTotalUsuarios());
        assertEquals(0, report.getUsuariosMigrados());
        assertEquals(1, report.getUsuariosConError());
        assertTrue(report.getErrores().size() > 0);
        assertTrue(report.getErrores().get(0).contains("Database error"));
    }

    @Test
    void testMigrationReport_SettersAndGetters() {
        // Given
        MigracionPasswordService.MigrationReport report = new MigracionPasswordService.MigrationReport();

        // When
        report.setTotalUsuarios(10);
        report.setUsuariosMigrados(5);
        report.setUsuariosConError(2);
        report.addError("Error 1");
        
        MigracionPasswordService.UsuarioMigrado usuario = new MigracionPasswordService.UsuarioMigrado(
            1L, "Test", "Usuario", "test@test.com", "12345678", "Familiar", "SIN_CONTRASEÑA"
        );
        report.addUsuarioMigrado(usuario);

        // Then
        assertEquals(10, report.getTotalUsuarios());
        assertEquals(5, report.getUsuariosMigrados());
        assertEquals(2, report.getUsuariosConError());
        assertEquals(1, report.getErrores().size());
        assertEquals(1, report.getUsuariosMigradosDetalle().size());
        assertEquals("test@test.com", report.getUsuariosMigradosDetalle().get(0).getCorreoElectronico());
    }
}
