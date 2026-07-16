package com.minimarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.entity.Venta;
import com.minimarket.repository.DetalleVentaRepository;
import com.minimarket.repository.RolRepository;
import com.minimarket.repository.UsuarioRepository;
import com.minimarket.repository.VentaRepository;
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
class VentaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        detalleVentaRepository.deleteAll();
        ventaRepository.deleteAll();

        Rol rolCliente = rolRepository.findByNombre("ROLE_CLIENTE")
                .orElseGet(() -> {
                    Rol rol = new Rol();
                    rol.setNombre("ROLE_CLIENTE");
                    return rolRepository.save(rol);
                });

        usuario = new Usuario();
        usuario.setUsername("compradorVentas_" + System.nanoTime());
        usuario.setPassword("clave123");
        usuario.setRoles(Set.of(rolCliente));
        usuario = usuarioRepository.save(usuario);
    }

    private Venta crearVenta() {
        Venta venta = new Venta();
        venta.setUsuario(usuario);
        venta.setFecha(new Date());
        return venta;
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroPuedeListarVentas() throws Exception {
        ventaRepository.save(crearVenta());

        mockMvc.perform(get("/api/ventas"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNoPuedeListarVentas() throws Exception {
        mockMvc.perform(get("/api/ventas"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeObtenerVentaPorId() throws Exception {
        Venta venta = ventaRepository.save(crearVenta());

        mockMvc.perform(get("/api/ventas/{id}", venta.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void obtenerVentaInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/ventas/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroPuedeRegistrarVenta() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearVenta())))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNoPuedeRegistrarVenta() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearVenta())))
                .andExpect(status().isForbidden());
    }
}
