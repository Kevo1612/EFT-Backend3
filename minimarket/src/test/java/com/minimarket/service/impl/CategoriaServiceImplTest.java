package com.minimarket.service.impl;

import com.minimarket.entity.Categoria;
import com.minimarket.repository.CategoriaRepository;
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
class CategoriaServiceImplTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaServiceImpl categoriaService;

    private Categoria categoria;

    @BeforeEach
    void setUp() {
        categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Bebidas");
    }

    @Test
    void findAllRetornaTodasLasCategorias() {
        when(categoriaRepository.findAll()).thenReturn(List.of(categoria));

        List<Categoria> resultado = categoriaService.findAll();

        assertThat(resultado).containsExactly(categoria);
    }

    @Test
    void findByIdRetornaCategoriaCuandoExiste() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        assertThat(categoriaService.findById(1L)).isEqualTo(categoria);
    }

    @Test
    void findByIdRetornaNullCuandoNoExiste() {
        when(categoriaRepository.findById(2L)).thenReturn(Optional.empty());

        assertThat(categoriaService.findById(2L)).isNull();
    }

    @Test
    void saveDelegaEnElRepositorio() {
        when(categoriaRepository.save(categoria)).thenReturn(categoria);

        assertThat(categoriaService.save(categoria)).isEqualTo(categoria);
        verify(categoriaRepository).save(categoria);
    }

    @Test
    void deleteByIdDelegaEnElRepositorio() {
        categoriaService.deleteById(1L);

        verify(categoriaRepository).deleteById(1L);
    }
}
