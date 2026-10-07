package cl.bancoxyz.clientes.dto;

import cl.bancoxyz.clientes.domain.Cliente.Segmento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank @Pattern(regexp = "^\\d{7,8}-[\\dkK]$", message = "formato esperado 12345678-9") String rut,
        @NotBlank @Size(max = 60) String username,
        @NotBlank @Size(max = 80) String nombres,
        @NotBlank @Size(max = 80) String apellidos,
        @NotBlank @Email String email,
        @Size(max = 20) String telefono,
        @Size(max = 200) String direccion,
        Segmento segmento) {
}
