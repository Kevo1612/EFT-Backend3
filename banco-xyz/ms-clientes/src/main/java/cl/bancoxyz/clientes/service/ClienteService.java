package cl.bancoxyz.clientes.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.bancoxyz.clientes.domain.Cliente;
import cl.bancoxyz.clientes.dto.ActualizarContactoRequest;
import cl.bancoxyz.clientes.dto.ClienteRequest;
import cl.bancoxyz.clientes.dto.ClienteResponse;
import cl.bancoxyz.clientes.dto.NotificacionResponse;
import cl.bancoxyz.clientes.repository.ClienteRepository;
import cl.bancoxyz.clientes.repository.NotificacionRepository;
import cl.bancoxyz.common.security.SecurityUtils;
import cl.bancoxyz.common.web.NegocioException;

@Service
public class ClienteService {

    private final ClienteRepository clientes;
    private final NotificacionRepository notificaciones;

    public ClienteService(ClienteRepository clientes, NotificacionRepository notificaciones) {
        this.clientes = clientes;
        this.notificaciones = notificaciones;
    }

    @Transactional(readOnly = true)
    public Page<ClienteResponse> listar(Pageable pageable) {
        return clientes.findAll(pageable).map(ClienteResponse::de);
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long id) {
        Cliente cliente = buscar(id);
        verificarAcceso(cliente);
        return ClienteResponse.de(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorUsername(String username) {
        return clientes.findByUsername(username)
                .map(ClienteResponse::de)
                .orElseThrow(() -> NegocioException.noEncontrado("No existe cliente para el usuario " + username));
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest req) {
        if (clientes.existsByRut(req.rut())) {
            throw NegocioException.conflicto("Ya existe un cliente con RUT " + req.rut());
        }
        if (clientes.existsByUsername(req.username())) {
            throw NegocioException.conflicto("El usuario " + req.username() + " ya esta asociado a un cliente");
        }
        Cliente c = new Cliente();
        c.setRut(req.rut().toUpperCase());
        c.setUsername(req.username());
        c.setNombres(req.nombres());
        c.setApellidos(req.apellidos());
        c.setEmail(req.email());
        c.setTelefono(req.telefono());
        c.setDireccion(req.direccion());
        if (req.segmento() != null) {
            c.setSegmento(req.segmento());
        }
        return ClienteResponse.de(clientes.save(c));
    }

    @Transactional
    public ClienteResponse actualizarContacto(Long id, ActualizarContactoRequest req) {
        Cliente c = buscar(id);
        verificarAcceso(c);
        if (req.email() != null) {
            c.setEmail(req.email());
        }
        if (req.telefono() != null) {
            c.setTelefono(req.telefono());
        }
        if (req.direccion() != null) {
            c.setDireccion(req.direccion());
        }
        return ClienteResponse.de(c);
    }

    @Transactional
    public ClienteResponse desactivar(Long id) {
        Cliente c = buscar(id);
        c.setActivo(false);
        return ClienteResponse.de(c);
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponse> notificaciones(Long clienteId, int limite) {
        verificarAcceso(buscar(clienteId));
        return notificaciones.findByClienteIdOrderByFechaDesc(clienteId, PageRequest.of(0, Math.min(limite, 100)))
                .stream().map(NotificacionResponse::de).toList();
    }

    private Cliente buscar(Long id) {
        return clientes.findById(id)
                .orElseThrow(() -> NegocioException.noEncontrado("Cliente " + id + " no existe"));
    }

    /** Un usuario con rol CLIENTE solo puede ver/modificar su propio perfil. */
    private void verificarAcceso(Cliente cliente) {
        if (SecurityUtils.tieneRol("ADMIN") || SecurityUtils.tieneRol("SERVICIO")) {
            return;
        }
        if (!cliente.getUsername().equals(SecurityUtils.usuarioActual())) {
            throw NegocioException.prohibido("No puede acceder a datos de otro cliente");
        }
    }
}
