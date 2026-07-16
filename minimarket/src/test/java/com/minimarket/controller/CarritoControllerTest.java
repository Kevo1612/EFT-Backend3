package com.minimarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.entity.Carrito;
import com.minimarket.entity.Categoria;
import com.minimarket.entity.Producto;
import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
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

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CarritoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CarritoRepository carritoRepository;

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

    private Usuario usuario;
    private Producto producto;

    @BeforeEach
    void setUp() {
        carritoRepository.deleteAll();
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();

        Rol rolCliente = rolRepository.findByNombre("ROLE_CLIENTE")
                .orElseGet(() -> {
                    Rol rol = new Rol();
                    rol.setNombre("ROLE_CLIENTE");
                    return rolRepository.save(rol);
                });

        usuario = new Usuario();
        usuario.setUsername("compradorCarrito_" + System.nanoTime());
        usuario.setPassword("clave123");
        usuario.setRoles(Set.of(rolCliente));
        usuario = usuarioRepository.save(usuario);

        Categoria categoria = new Categoria();
        categoria.setNombre("Limpieza_" + System.nanoTime());
        categoria = categoriaRepository.save(categoria);

        producto = new Producto();
        producto.setNombre("Detergente");
        producto.setPrecio(3500.0);
        producto.setStock(15);
        producto.setCategoria(categoria);
        producto = productoRepository.save(producto);
    }

    private Carrito crearItemCarrito() {
        Carrito carrito = new Carrito();
        carrito.setUsuario(usuario);
        carrito.setProducto(producto);
        carrito.setCantidad(1);
        return carrito;
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clientePuedeListarSuCarrito() throws Exception {
        carritoRepository.save(crearItemCarrito());

        mockMvc.perform(get("/api/carrito"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeObtenerElementoDelCarritoPorId() throws Exception {
        Carrito item = carritoRepository.save(crearItemCarrito());

        mockMvc.perform(get("/api/carrito/{id}", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void obtenerElementoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/carrito/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clientePuedeAgregarProductoAlCarrito() throws Exception {
        mockMvc.perform(post("/api/carrito")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearItemCarrito())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(1));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clientePuedeActualizarSuCarrito() throws Exception {
        Carrito item = carritoRepository.save(crearItemCarrito());
        item.setCantidad(4);

        mockMvc.perform(put("/api/carrito/{id}", item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(4));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void actualizarCarritoInexistenteRetorna404() throws Exception {
        mockMvc.perform(put("/api/carrito/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearItemCarrito())))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clientePuedeEliminarProductoDeSuCarrito() throws Exception {
        Carrito item = carritoRepository.save(crearItemCarrito());

        mockMvc.perform(delete("/api/carrito/{id}", item.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void eliminarElementoInexistenteRetorna404() throws Exception {
        mockMvc.perform(delete("/api/carrito/{id}", 999999L))
                .andExpect(status().isNotFound());
    }
}
