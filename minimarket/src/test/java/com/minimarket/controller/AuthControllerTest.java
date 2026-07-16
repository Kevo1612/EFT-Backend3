package com.minimarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.security.model.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración para el login y, de forma indirecta, para
 * JwtAuthenticationFilter y JwtUtil al usar el token generado contra un
 * endpoint protegido.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private LoginRequest credenciales(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    @Test
    void loginConCredencialesValidasRetornaToken() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credenciales("admin", "1234"))))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginConCredencialesInvalidasRetornaError() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credenciales("admin", "password-incorrecto"))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void loginConUsuarioInexistenteRetornaError() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credenciales("no-existe", "1234"))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void tokenObtenidoEnLoginPermiteAccederAEndpointProtegido() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credenciales("admin", "1234"))))
                .andExpect(status().isOk())
                .andReturn();

        String body = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(body).get("token").asText();

        mockMvc.perform(get("/api/productos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void endpointProtegidoSinTokenNoPermiteElAcceso() throws Exception {
        // El proyecto no define un AuthenticationEntryPoint explícito, por lo
        // que se valida contra 4xx en general (401 o 403 según el default de Spring Security).
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void endpointProtegidoConTokenMalformadoNoPermiteElAcceso() throws Exception {
        mockMvc.perform(get("/api/productos")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().is4xxClientError());
    }
}
