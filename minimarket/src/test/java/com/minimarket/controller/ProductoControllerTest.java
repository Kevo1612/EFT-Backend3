package com.minimarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.entity.Categoria;
import com.minimarket.entity.Producto;
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
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

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

    private Producto crearProducto() {

        Producto producto = new Producto();

        producto.setNombre("Coca Cola");

        producto.setPrecio(1990.0);

        producto.setStock(20);

        producto.setCategoria(categoria);

        return producto;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeCrearProducto() throws Exception {

        Producto producto = crearProducto();

        mockMvc.perform(post("/api/productos")

                        .contentType(MediaType.APPLICATION_JSON)

                        .content(objectMapper.writeValueAsString(producto)))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.nombre").value("Coca Cola"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeListarProductos() throws Exception {

        productoRepository.save(crearProducto());

        mockMvc.perform(get("/api/productos"))

                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clientePuedeListarProductos() throws Exception {

        productoRepository.save(crearProducto());

        mockMvc.perform(get("/api/productos"))

                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroNoPuedeCrearProducto() throws Exception {

        Producto producto = crearProducto();

        mockMvc.perform(post("/api/productos")

                        .contentType(MediaType.APPLICATION_JSON)

                        .content(objectMapper.writeValueAsString(producto)))

                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNoPuedeEliminarProducto() throws Exception {

        mockMvc.perform(delete("/api/productos/1"))

                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clientePuedeObtenerProductoPorId() throws Exception {

        Producto producto = productoRepository.save(crearProducto());

        mockMvc.perform(get("/api/productos/{id}", producto.getId()))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.nombre").value("Coca Cola"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void obtenerProductoInexistenteRetorna404() throws Exception {

        mockMvc.perform(get("/api/productos/{id}", 9999L))

                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeActualizarProductoExistente() throws Exception {

        Producto producto = productoRepository.save(crearProducto());
        producto.setPrecio(2500.0);

        mockMvc.perform(put("/api/productos/{id}", producto.getId())

                        .contentType(MediaType.APPLICATION_JSON)

                        .content(objectMapper.writeValueAsString(producto)))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.precio").value(2500.0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void actualizarProductoInexistenteRetorna404() throws Exception {

        Producto producto = crearProducto();

        mockMvc.perform(put("/api/productos/{id}", 9999L)

                        .contentType(MediaType.APPLICATION_JSON)

                        .content(objectMapper.writeValueAsString(producto)))

                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeEliminarProducto() throws Exception {

        Producto producto = productoRepository.save(crearProducto());

        mockMvc.perform(delete("/api/productos/{id}", producto.getId()))

                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminarProductoInexistenteRetorna404() throws Exception {

        mockMvc.perform(delete("/api/productos/{id}", 9999L))

                .andExpect(status().isNotFound());
    }

}