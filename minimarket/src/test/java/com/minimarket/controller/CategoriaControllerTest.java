package com.minimarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.entity.Categoria;
import com.minimarket.repository.CategoriaRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CategoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Categoria categoria;

    @BeforeEach
    void setUp() {
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();

        categoria = new Categoria();
        categoria.setNombre("Bebidas");
        categoria = categoriaRepository.save(categoria);
    }

    private Categoria nuevaCategoria(String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        return c;
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clientePuedeListarCategorias() throws Exception {
        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bebidas")));
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroPuedeObtenerCategoriaPorId() throws Exception {
        mockMvc.perform(get("/api/categorias/{id}", categoria.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Bebidas"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void obtenerCategoriaInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/categorias/{id}", 9999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeCrearCategoria() throws Exception {
        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevaCategoria("Lácteos"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Lácteos"));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNoPuedeCrearCategoria() throws Exception {
        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevaCategoria("Otra"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeActualizarCategoriaExistente() throws Exception {
        Categoria actualizada = nuevaCategoria("Bebidas Actualizadas");

        mockMvc.perform(put("/api/categorias/{id}", categoria.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actualizada)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Bebidas Actualizadas"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void actualizarCategoriaInexistenteRetorna404() throws Exception {
        mockMvc.perform(put("/api/categorias/{id}", 9999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevaCategoria("Nueva"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeEliminarCategoria() throws Exception {
        mockMvc.perform(delete("/api/categorias/{id}", categoria.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminarCategoriaInexistenteRetorna404() throws Exception {
        mockMvc.perform(delete("/api/categorias/{id}", 9999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroNoPuedeEliminarCategoria() throws Exception {
        mockMvc.perform(delete("/api/categorias/{id}", categoria.getId()))
                .andExpect(status().isForbidden());
    }
}
