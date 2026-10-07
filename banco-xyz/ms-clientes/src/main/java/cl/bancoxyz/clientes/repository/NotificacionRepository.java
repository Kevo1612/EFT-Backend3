package cl.bancoxyz.clientes.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import cl.bancoxyz.clientes.domain.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByClienteIdOrderByFechaDesc(Long clienteId, Pageable pageable);

    boolean existsByReferenciaAndTipo(String referencia, String tipo);
}
