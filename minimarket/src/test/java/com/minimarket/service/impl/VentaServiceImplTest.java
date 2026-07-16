package com.minimarket.service.impl;

import com.minimarket.entity.Venta;
import com.minimarket.repository.VentaRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VentaServiceImplTest {

    @Mock
    private VentaRepository ventaRepository;

    @InjectMocks
    private VentaServiceImpl ventaService;

    private Venta venta;

    @BeforeEach
    void setUp() {
        venta = new Venta();
        venta.setId(1L);
        venta.setFecha(new Date());
    }

    @Test
    void findAllRetornaTodasLasVentas() {
        when(ventaRepository.findAll()).thenReturn(List.of(venta));

        assertThat(ventaService.findAll()).containsExactly(venta);
    }

    @Test
    void findByIdRetornaVentaCuandoExiste() {
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));

        assertThat(ventaService.findById(1L)).isEqualTo(venta);
    }

    @Test
    void findByIdRetornaNullCuandoNoExiste() {
        when(ventaRepository.findById(2L)).thenReturn(Optional.empty());

        assertThat(ventaService.findById(2L)).isNull();
    }

    @Test
    void saveDelegaEnElRepositorio() {
        when(ventaRepository.save(venta)).thenReturn(venta);

        assertThat(ventaService.save(venta)).isEqualTo(venta);
    }

    @Test
    void findByUsuarioIdRetornaListaFiltrada() {
        when(ventaRepository.findByUsuarioId(6L)).thenReturn(List.of(venta));

        assertThat(ventaService.findByUsuarioId(6L)).containsExactly(venta);
    }
}
