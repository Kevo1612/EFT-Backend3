package com.minimarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.RolRepository;
import com.minimarket.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Usuario usuarioExistente;

    @BeforeEach
    void setUp() {
        Rol rolCliente = rolRepository.findByNombre("ROLE_CLIENTE")
                .orElseGet(() -> {
                    Rol rol = new Rol();
                    rol.setNombre("ROLE_CLIENTE");
                    return rolRepository.save(rol);
                });

        usuarioExistente = new Usuario();
        usuarioExistente.setUsername("pruebaUsuario_" + System.nanoTime());
        usuarioExistente.setPassword("clave123");
        usuarioExistente.setRoles(Set.of(rolCliente));
        usuarioExistente = usuarioRepository.save(usuarioExistente);
    }

    private Usuario nuevoUsuario(String username) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword("clave123");
        usuario.setRoles(Set.of());
        return usuario;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeListarUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNoPuedeListarUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeObtenerUsuarioPorId() throws Exception {
        mockMvc.perform(get("/api/usuarios/{id}", usuarioExistente.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(usuarioExistente.getUsername()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void obtenerUsuarioInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/usuarios/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeCrearUsuario() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevoUsuario("nuevoUsuario_" + System.nanoTime()))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeActualizarUsuarioExistente() throws Exception {
        usuarioExistente.setPassword("nuevaClave456");

        mockMvc.perform(put("/api/usuarios/{id}", usuarioExistente.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(usuarioExistente)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void actualizarUsuarioInexistenteRetorna404() throws Exception {
        mockMvc.perform(put("/api/usuarios/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevoUsuario("otro"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeEliminarUsuario() throws Exception {
        mockMvc.perform(delete("/api/usuarios/{id}", usuarioExistente.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminarUsuarioInexistenteRetorna404() throws Exception {
        mockMvc.perform(delete("/api/usuarios/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroNoPuedeCrearUsuario() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevoUsuario("otroMas"))))
                .andExpect(status().isForbidden());
    }
}
