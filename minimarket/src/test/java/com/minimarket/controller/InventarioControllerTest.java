package com.minimarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.entity.Categoria;
import com.minimarket.entity.Inventario;
import com.minimarket.entity.Producto;
import com.minimarket.repository.CategoriaRepository;
import com.minimarket.repository.InventarioRepository;
import com.minimarket.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InventarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Producto producto;

    @BeforeEach
    void setUp() {
        inventarioRepository.deleteAll();
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();

        Categoria categoria = new Categoria();
        categoria.setNombre("Abarrotes_" + System.nanoTime());
        categoria = categoriaRepository.save(categoria);

        producto = new Producto();
        producto.setNombre("Arroz");
        producto.setPrecio(1500.0);
        producto.setStock(50);
        producto.setCategoria(categoria);
        producto = productoRepository.save(producto);
    }

    private Inventario crearMovimiento() {
        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setCantidad(10);
        inventario.setTipoMovimiento("Entrada");
        inventario.setFechaMovimiento(new Date());
        return inventario;
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroPuedeListarMovimientos() throws Exception {
        inventarioRepository.save(crearMovimiento());

        mockMvc.perform(get("/api/inventario"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNoPuedeListarMovimientos() throws Exception {
        mockMvc.perform(get("/api/inventario"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeObtenerMovimientoPorId() throws Exception {
        Inventario inventario = inventarioRepository.save(crearMovimiento());

        mockMvc.perform(get("/api/inventario/{id}", inventario.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void obtenerMovimientoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/inventario/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroPuedeRegistrarMovimiento() throws Exception {
        mockMvc.perform(post("/api/inventario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearMovimiento())))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroPuedeActualizarMovimientoExistente() throws Exception {
        Inventario inventario = inventarioRepository.save(crearMovimiento());
        inventario.setCantidad(99);

        mockMvc.perform(put("/api/inventario/{id}", inventario.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inventario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(99));
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void actualizarMovimientoInexistenteRetorna404() throws Exception {
        mockMvc.perform(put("/api/inventario/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearMovimiento())))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeEliminarMovimiento() throws Exception {
        Inventario inventario = inventarioRepository.save(crearMovimiento());

        mockMvc.perform(delete("/api/inventario/{id}", inventario.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroNoPuedeEliminarMovimiento() throws Exception {
        Inventario inventario = inventarioRepository.save(crearMovimiento());

        mockMvc.perform(delete("/api/inventario/{id}", inventario.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminarMovimientoInexistenteRetorna404() throws Exception {
        mockMvc.perform(delete("/api/inventario/{id}", 999999L))
                .andExpect(status().isNotFound());
    }
}
