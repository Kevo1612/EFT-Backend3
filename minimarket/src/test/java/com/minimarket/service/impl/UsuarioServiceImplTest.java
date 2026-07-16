package com.minimarket.service.impl;

import com.minimarket.entity.Usuario;
import com.minimarket.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("admin");
        usuario.setPassword("hash");
    }

    @Test
    void findAllRetornaTodosLosUsuarios() {
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));

        assertThat(usuarioService.findAll()).containsExactly(usuario);
    }

    @Test
    void findByIdRetornaOptionalConUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertThat(usuarioService.findById(1L)).isPresent().contains(usuario);
    }

    @Test
    void findByIdRetornaOptionalVacioCuandoNoExiste() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.empty());

        assertThat(usuarioService.findById(2L)).isEmpty();
    }

    @Test
    void findByUsernameRetornaUsuarioCuandoExiste() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));

        assertThat(usuarioService.findByUsername("admin")).isPresent().contains(usuario);
    }

    @Test
    void findByUsernameRetornaVacioCuandoNoExiste() {
        when(usuarioRepository.findByUsername("inexistente")).thenReturn(Optional.empty());

        assertThat(usuarioService.findByUsername("inexistente")).isEmpty();
    }

    @Test
    void saveDelegaEnElRepositorio() {
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        assertThat(usuarioService.save(usuario)).isEqualTo(usuario);
    }

    @Test
    void deleteByIdDelegaEnElRepositorio() {
        usuarioService.deleteById(1L);

        verify(usuarioRepository).deleteById(1L);
    }
}
