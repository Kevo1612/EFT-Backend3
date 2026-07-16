package com.minimarket.security.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String SECRET = "MiClaveUltraSecretaParaMiniMarketPlusJWT2026SeguridadMuyFuerte123456789";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
    }

    private UserDetails usuario(String username) {
        return new User(username, "password", Collections.emptyList());
    }

    @Test
    void generarTokenProduceUnTokenNoVacio() {
        String token = jwtUtil.generarToken(usuario("admin"));

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void obtenerUsernameExtraeElUsuarioCorrecto() {
        String token = jwtUtil.generarToken(usuario("admin"));

        assertThat(jwtUtil.obtenerUsername(token)).isEqualTo("admin");
    }

    @Test
    void tokenRecienGeneradoNoEstaExpirado() {
        String token = jwtUtil.generarToken(usuario("admin"));

        assertThat(jwtUtil.tokenExpirado(token)).isFalse();
    }

    @Test
    void tokenConExpiracionEnElPasadoLanzaExcepcionAlParsear() {
        // JJWT valida la expiración durante el parseo, por lo que un token ya
        // vencido lanza ExpiredJwtException en lugar de retornar false en silencio.
        ReflectionTestUtils.setField(jwtUtil, "expiration", -100000L);

        String token = jwtUtil.generarToken(usuario("admin"));

        assertThrows(io.jsonwebtoken.ExpiredJwtException.class,
                () -> jwtUtil.tokenExpirado(token));
    }

    @Test
    void validarTokenRetornaTrueParaUsuarioYTokenCorrectos() {
        UserDetails userDetails = usuario("admin");
        String token = jwtUtil.generarToken(userDetails);

        assertThat(jwtUtil.validarToken(token, userDetails)).isTrue();
    }

    @Test
    void validarTokenRetornaFalseParaUsuarioDistinto() {
        String token = jwtUtil.generarToken(usuario("admin"));

        assertThat(jwtUtil.validarToken(token, usuario("otro"))).isFalse();
    }

    @Test
    void validarTokenConTokenExpiradoLanzaExcepcionAlParsear() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -100000L);
        UserDetails userDetails = usuario("admin");
        String token = jwtUtil.generarToken(userDetails);

        assertThrows(io.jsonwebtoken.ExpiredJwtException.class,
                () -> jwtUtil.validarToken(token, userDetails));
    }

    @Test
    void obtenerUsernameConTokenMalformadoLanzaExcepcion() {
        assertThrows(Exception.class, () -> jwtUtil.obtenerUsername("token-invalido"));
    }
}
