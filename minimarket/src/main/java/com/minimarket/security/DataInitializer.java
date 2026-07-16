package com.minimarket.security;

import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.entity.Categoria;
import com.minimarket.repository.RolRepository;
import com.minimarket.repository.UsuarioRepository;
import com.minimarket.repository.CategoriaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoriaRepository categoriaRepository;

    public DataInitializer(UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            CategoriaRepository categoriaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    public void run(String... args) {

        Rol admin = rolRepository.findByNombre("ROLE_ADMIN")
                .orElseGet(() -> {
                    Rol rol = new Rol();
                    rol.setNombre("ROLE_ADMIN");
                    return rolRepository.save(rol);
                });

        Rol cajero = rolRepository.findByNombre("ROLE_CAJERO")
                .orElseGet(() -> {
                    Rol rol = new Rol();
                    rol.setNombre("ROLE_CAJERO");
                    return rolRepository.save(rol);
                });

        Rol cliente = rolRepository.findByNombre("ROLE_CLIENTE")
                .orElseGet(() -> {
                    Rol rol = new Rol();
                    rol.setNombre("ROLE_CLIENTE");
                    return rolRepository.save(rol);
                });
        categoriaRepository.findByNombre("Bebidas")
                .orElseGet(() -> {
                    Categoria c = new Categoria();
                    c.setNombre("Bebidas");
                    return categoriaRepository.save(c);
                });

        categoriaRepository.findByNombre("Abarrotes")
                .orElseGet(() -> {
                    Categoria c = new Categoria();
                    c.setNombre("Abarrotes");
                    return categoriaRepository.save(c);
                });

        categoriaRepository.findByNombre("Lácteos")
                .orElseGet(() -> {
                    Categoria c = new Categoria();
                    c.setNombre("Lácteos");
                    return categoriaRepository.save(c);
                });

        categoriaRepository.findByNombre("Limpieza")
                .orElseGet(() -> {
                    Categoria c = new Categoria();
                    c.setNombre("Limpieza");
                    return categoriaRepository.save(c);
                });

        categoriaRepository.findByNombre("Cuidado Personal")
                .orElseGet(() -> {
                    Categoria c = new Categoria();
                    c.setNombre("Cuidado Personal");
                    return categoriaRepository.save(c);
                });

        // ADMIN
        if (usuarioRepository.findByUsername("admin").isEmpty()) {
            Usuario usuario = new Usuario();
            usuario.setUsername("admin");
            usuario.setPassword(passwordEncoder.encode("1234"));
            usuario.setRoles(Set.of(admin));

            usuarioRepository.save(usuario);
        }

        // CAJERO
        if (usuarioRepository.findByUsername("cajero").isEmpty()) {
            Usuario usuario = new Usuario();
            usuario.setUsername("cajero");
            usuario.setPassword(passwordEncoder.encode("1234"));
            usuario.setRoles(Set.of(cajero));

            usuarioRepository.save(usuario);
        }

        // CLIENTE
        if (usuarioRepository.findByUsername("cliente").isEmpty()) {
            Usuario usuario = new Usuario();
            usuario.setUsername("cliente");
            usuario.setPassword(passwordEncoder.encode("1234"));
            usuario.setRoles(Set.of(cliente));

            usuarioRepository.save(usuario);
        }

        System.out.println("========================================");
        System.out.println("USUARIOS DE PRUEBA CREADOS");
        System.out.println("ADMIN   -> admin / 1234");
        System.out.println("CAJERO  -> cajero / 1234");
        System.out.println("CLIENTE -> cliente / 1234");
        System.out.println("========================================");
    }
}