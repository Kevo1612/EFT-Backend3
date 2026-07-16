package com.minimarket.service.impl;

import com.minimarket.entity.Rol;
import com.minimarket.repository.RolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolServiceImplTest {

    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private RolServiceImpl rolService;

    @Test
    void findByNombreRetornaRolCuandoExiste() {
        Rol rol = new Rol();
        rol.setId(1L);
        rol.setNombre("ROLE_ADMIN");

        when(rolRepository.findByNombre("ROLE_ADMIN")).thenReturn(Optional.of(rol));

        Optional<Rol> resultado = rolService.findByNombre("ROLE_ADMIN");

        assertThat(resultado).isPresent().contains(rol);
    }

    @Test
    void findByNombreRetornaVacioCuandoNoExiste() {
        when(rolRepository.findByNombre("ROLE_INEXISTENTE")).thenReturn(Optional.empty());

        Optional<Rol> resultado = rolService.findByNombre("ROLE_INEXISTENTE");

        assertThat(resultado).isEmpty();
    }
}
