package com.minimarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.entity.*;
import com.minimarket.repository.*;
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
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DetalleVentaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Venta venta;
    private Producto producto;

    @BeforeEach
    void setUp() {
        detalleVentaRepository.deleteAll();
        ventaRepository.deleteAll();
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();

        Rol rolCliente = rolRepository.findByNombre("ROLE_CLIENTE")
                .orElseGet(() -> {
                    Rol rol = new Rol();
                    rol.setNombre("ROLE_CLIENTE");
                    return rolRepository.save(rol);
                });

        Usuario usuario = new Usuario();
        usuario.setUsername("compradorDetalle_" + System.nanoTime());
        usuario.setPassword("clave123");
        usuario.setRoles(Set.of(rolCliente));
        usuario = usuarioRepository.save(usuario);

        venta = new Venta();
        venta.setUsuario(usuario);
        venta.setFecha(new Date());
        venta = ventaRepository.save(venta);

        Categoria categoria = new Categoria();
        categoria.setNombre("Bebidas_" + System.nanoTime());
        categoria = categoriaRepository.save(categoria);

        producto = new Producto();
        producto.setNombre("Coca Cola");
        producto.setPrecio(1990.0);
        producto.setStock(20);
        producto.setCategoria(categoria);
        producto = productoRepository.save(producto);
    }

    private DetalleVenta crearDetalle() {
        DetalleVenta detalle = new DetalleVenta();
        detalle.setVenta(venta);
        detalle.setProducto(producto);
        detalle.setCantidad(2);
        detalle.setPrecio(1990.0);
        return detalle;
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroPuedeListarDetallesDeVenta() throws Exception {
        detalleVentaRepository.save(crearDetalle());

        mockMvc.perform(get("/api/detalle-ventas"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNoPuedeListarDetallesDeVenta() throws Exception {
        mockMvc.perform(get("/api/detalle-ventas"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeObtenerDetallePorId() throws Exception {
        DetalleVenta detalle = detalleVentaRepository.save(crearDetalle());

        mockMvc.perform(get("/api/detalle-ventas/{id}", detalle.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void obtenerDetalleInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/detalle-ventas/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroPuedeRegistrarDetalle() throws Exception {
        mockMvc.perform(post("/api/detalle-ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearDetalle())))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroPuedeActualizarDetalleExistente() throws Exception {
        DetalleVenta detalle = detalleVentaRepository.save(crearDetalle());
        detalle.setCantidad(5);

        mockMvc.perform(put("/api/detalle-ventas/{id}", detalle.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(detalle)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(5));
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void actualizarDetalleInexistenteRetorna404() throws Exception {
        mockMvc.perform(put("/api/detalle-ventas/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearDetalle())))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeEliminarDetalle() throws Exception {
        DetalleVenta detalle = detalleVentaRepository.save(crearDetalle());

        mockMvc.perform(delete("/api/detalle-ventas/{id}", detalle.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroNoPuedeEliminarDetalle() throws Exception {
        DetalleVenta detalle = detalleVentaRepository.save(crearDetalle());

        mockMvc.perform(delete("/api/detalle-ventas/{id}", detalle.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminarDetalleInexistenteRetorna404() throws Exception {
        mockMvc.perform(delete("/api/detalle-ventas/{id}", 999999L))
                .andExpect(status().isNotFound());
    }
}
