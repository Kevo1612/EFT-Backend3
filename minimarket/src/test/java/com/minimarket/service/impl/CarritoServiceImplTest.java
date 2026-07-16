package com.minimarket.service.impl;

import com.minimarket.entity.Carrito;
import com.minimarket.repository.CarritoRepository;
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
class CarritoServiceImplTest {

    @Mock
    private CarritoRepository carritoRepository;

    @InjectMocks
    private CarritoServiceImpl carritoService;

    private Carrito carrito;

    @BeforeEach
    void setUp() {
        carrito = new Carrito();
        carrito.setId(1L);
        carrito.setCantidad(3);
    }

    @Test
    void findAllRetornaTodosLosElementos() {
        when(carritoRepository.findAll()).thenReturn(List.of(carrito));

        List<Carrito> resultado = carritoService.findAll();

        assertThat(resultado).hasSize(1).contains(carrito);
        verify(carritoRepository).findAll();
    }

    @Test
    void findByIdRetornaElementoCuandoExiste() {
        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));

        Carrito resultado = carritoService.findById(1L);

        assertThat(resultado).isEqualTo(carrito);
    }

    @Test
    void findByIdRetornaNullCuandoNoExiste() {
        when(carritoRepository.findById(99L)).thenReturn(Optional.empty());

        Carrito resultado = carritoService.findById(99L);

        assertThat(resultado).isNull();
    }

    @Test
    void saveDelegaEnElRepositorio() {
        when(carritoRepository.save(carrito)).thenReturn(carrito);

        Carrito resultado = carritoService.save(carrito);

        assertThat(resultado).isEqualTo(carrito);
        verify(carritoRepository).save(carrito);
    }

    @Test
    void deleteByIdDelegaEnElRepositorio() {
        carritoService.deleteById(1L);

        verify(carritoRepository).deleteById(1L);
    }

    @Test
    void findByUsuarioIdRetornaListaFiltrada() {
        when(carritoRepository.findByUsuarioId(5L)).thenReturn(List.of(carrito));

        List<Carrito> resultado = carritoService.findByUsuarioId(5L);

        assertThat(resultado).hasSize(1);
        verify(carritoRepository).findByUsuarioId(5L);
    }
}
