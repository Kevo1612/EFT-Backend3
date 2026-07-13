package com.minimarket.security.config;

import com.minimarket.entity.Categoria;
import com.minimarket.entity.Producto;
import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.CategoriaRepository;
import com.minimarket.repository.ProductoRepository;
import com.minimarket.repository.RolRepository;
import com.minimarket.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            CategoriaRepository categoriaRepository,
            ProductoRepository productoRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            // Crear rol ADMIN
            Rol adminRol = rolRepository.findByNombre("ADMIN")
                    .orElseGet(() -> rolRepository.save(new Rol("ADMIN")));

            // Crear rol CLIENTE
            rolRepository.findByNombre("CLIENTE")
                    .orElseGet(() -> rolRepository.save(new Rol("CLIENTE")));

            // Crear usuario administrador
            if (usuarioRepository.findByUsername("admin").isEmpty()) {

                Usuario admin = new Usuario();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));

                Set<Rol> roles = new HashSet<>();
                roles.add(adminRol);

                admin.setRoles(roles);

                usuarioRepository.save(admin);
            }

            // Crear categoría
            Categoria categoria = categoriaRepository.findByNombre("Lácteos")
                    .orElseGet(() -> {
                        Categoria c = new Categoria();
                        c.setNombre("Lácteos");
                        return categoriaRepository.save(c);
                    });

           

            System.out.println("=========================================");
            System.out.println("Datos de prueba cargados correctamente");
            System.out.println("Usuario : admin");
            System.out.println("Password: admin123");
            System.out.println("=========================================");
        };
    }
}