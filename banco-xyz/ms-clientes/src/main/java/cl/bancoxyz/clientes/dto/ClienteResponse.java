package cl.bancoxyz.clientes.dto;

import java.time.Instant;

import cl.bancoxyz.clientes.domain.Cliente;

public record ClienteResponse(
        Long id,
        String rut,
        String username,
        String nombres,
        String apellidos,
        String email,
        String telefono,
        String direccion,
        String segmento,
        boolean activo,
        Instant fechaRegistro) {

    public static ClienteResponse de(Cliente c) {
        return new ClienteResponse(c.getId(), c.getRut(), c.getUsername(), c.getNombres(), c.getApellidos(),
                c.getEmail(), c.getTelefono(), c.getDireccion(), c.getSegmento().name(), c.isActivo(),
                c.getFechaRegistro());
    }
}
