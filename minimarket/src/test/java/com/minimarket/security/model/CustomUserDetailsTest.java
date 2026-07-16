package com.minimarket.security.model;

import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserDetailsTest {

    @Test
    void exponeLosDatosBasicosDelUsuario() {
        Usuario usuario = new Usuario();
        usuario.setUsername("admin");
        usuario.setPassword("hash-seguro");
        usuario.setRoles(Set.of());

        CustomUserDetails userDetails = new CustomUserDetails(usuario);

        assertThat(userDetails.getUsername()).isEqualTo("admin");
        assertThat(userDetails.getPassword()).isEqualTo("hash-seguro");
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    void mapeaLosRolesAAuthoritiesDeSpringSecurity() {
        Rol admin = new Rol();
        admin.setNombre("ROLE_ADMIN");
        Rol cajero = new Rol();
        cajero.setNombre("ROLE_CAJERO");

        Usuario usuario = new Usuario();
        usuario.setUsername("multi");
        usuario.setPassword("hash");
        usuario.setRoles(Set.of(admin, cajero));

        CustomUserDetails userDetails = new CustomUserDetails(usuario);

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_CAJERO");
    }
}
