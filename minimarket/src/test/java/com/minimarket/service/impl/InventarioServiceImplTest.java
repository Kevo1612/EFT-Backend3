package com.minimarket.service.impl;

import com.minimarket.entity.Inventario;
import com.minimarket.repository.InventarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventarioServiceImplTest {

    @Mock
    private InventarioRepository inventarioRepository;

    @InjectMocks
    private InventarioServiceImpl inventarioService;

    private Inventario inventario;

    @BeforeEach
    void setUp() {
        inventario = new Inventario();
        inventario.setId(1L);
        inventario.setCantidad(10);
        inventario.setTipoMovimiento("Entrada");
        inventario.setFechaMovimiento(new Date());
    }

    @Test
    void findAllRetornaTodosLosMovimientos() {
        when(inventarioRepository.findAll()).thenReturn(List.of(inventario));

        assertThat(inventarioService.findAll()).containsExactly(inventario);
    }

    @Test
    void findByIdRetornaMovimientoCuandoExiste() {
        when(inventarioRepository.findById(1L)).thenReturn(Optional.of(inventario));

        assertThat(inventarioService.findById(1L)).isEqualTo(inventario);
    }

    @Test
    void findByIdRetornaNullCuandoNoExiste() {
        when(inventarioRepository.findById(2L)).thenReturn(Optional.empty());

        assertThat(inventarioService.findById(2L)).isNull();
    }

    @Test
    void saveDelegaEnElRepositorio() {
        when(inventarioRepository.save(inventario)).thenReturn(inventario);

        assertThat(inventarioService.save(inventario)).isEqualTo(inventario);
    }

    @Test
    void deleteByIdDelegaEnElRepositorio() {
        inventarioService.deleteById(1L);

        verify(inventarioRepository).deleteById(1L);
    }

    @Test
    void findByProductoIdRetornaListaFiltrada() {
        when(inventarioRepository.findByProductoId(3L)).thenReturn(List.of(inventario));

        assertThat(inventarioService.findByProductoId(3L)).containsExactly(inventario);
    }
}
