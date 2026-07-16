package com.minimarket.service.impl;

import com.minimarket.entity.DetalleVenta;
import com.minimarket.repository.DetalleVentaRepository;
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
class DetalleVentaServiceImplTest {

    @Mock
    private DetalleVentaRepository detalleVentaRepository;

    @InjectMocks
    private DetalleVentaServiceImpl detalleVentaService;

    private DetalleVenta detalle;

    @BeforeEach
    void setUp() {
        detalle = new DetalleVenta();
        detalle.setId(1L);
        detalle.setCantidad(2);
        detalle.setPrecio(1990.0);
    }

    @Test
    void findAllRetornaTodosLosDetalles() {
        when(detalleVentaRepository.findAll()).thenReturn(List.of(detalle));

        assertThat(detalleVentaService.findAll()).containsExactly(detalle);
    }

    @Test
    void findByIdRetornaDetalleCuandoExiste() {
        when(detalleVentaRepository.findById(1L)).thenReturn(Optional.of(detalle));

        assertThat(detalleVentaService.findById(1L)).isEqualTo(detalle);
    }

    @Test
    void findByIdRetornaNullCuandoNoExiste() {
        when(detalleVentaRepository.findById(2L)).thenReturn(Optional.empty());

        assertThat(detalleVentaService.findById(2L)).isNull();
    }

    @Test
    void saveDelegaEnElRepositorio() {
        when(detalleVentaRepository.save(detalle)).thenReturn(detalle);

        assertThat(detalleVentaService.save(detalle)).isEqualTo(detalle);
    }

    @Test
    void deleteByIdDelegaEnElRepositorio() {
        detalleVentaService.deleteById(1L);

        verify(detalleVentaRepository).deleteById(1L);
    }

    @Test
    void findByVentaIdRetornaListaFiltrada() {
        when(detalleVentaRepository.findByVentaId(7L)).thenReturn(List.of(detalle));

        assertThat(detalleVentaService.findByVentaId(7L)).containsExactly(detalle);
    }
}
