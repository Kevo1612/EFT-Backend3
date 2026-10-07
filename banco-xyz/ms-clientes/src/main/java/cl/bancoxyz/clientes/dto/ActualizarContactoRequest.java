package cl.bancoxyz.clientes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Datos que el propio cliente puede modificar (no RUT ni segmento). */
public record ActualizarContactoRequest(
        @Email String email,
        @Size(max = 20) String telefono,
        @Size(max = 200) String direccion) {
}
