package com.minimarket.service.impl;

import com.minimarket.entity.Producto;
import com.minimarket.repository.ProductoRepository;
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
class ProductoServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoServiceImpl productoService;

    private Producto producto;

    @BeforeEach
    void setUp() {
        producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Coca Cola");
        producto.setPrecio(1990.0);
        producto.setStock(20);
    }

    @Test
    void findAllRetornaTodosLosProductos() {
        when(productoRepository.findAll()).thenReturn(List.of(producto));

        assertThat(productoService.findAll()).containsExactly(producto);
    }

    @Test
    void findByIdRetornaProductoCuandoExiste() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        assertThat(productoService.findById(1L)).isEqualTo(producto);
    }

    @Test
    void findByIdRetornaNullCuandoNoExiste() {
        when(productoRepository.findById(2L)).thenReturn(Optional.empty());

        assertThat(productoService.findById(2L)).isNull();
    }

    @Test
    void saveDelegaEnElRepositorio() {
        when(productoRepository.save(producto)).thenReturn(producto);

        assertThat(productoService.save(producto)).isEqualTo(producto);
    }

    @Test
    void deleteByIdDelegaEnElRepositorio() {
        productoService.deleteById(1L);

        verify(productoRepository).deleteById(1L);
    }

    @Test
    void findByCategoriaIdRetornaListaFiltrada() {
        when(productoRepository.findByCategoriaId(4L)).thenReturn(List.of(producto));

        assertThat(productoService.findByCategoriaId(4L)).containsExactly(producto);
    }
}
