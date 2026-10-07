package cl.bancoxyz.cuentas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import cl.bancoxyz.cuentas.domain.MovimientoCuenta;

public interface MovimientoCuentaRepository extends JpaRepository<MovimientoCuenta, Long> {

    Optional<MovimientoCuenta> findByReferencia(String referencia);

    List<MovimientoCuenta> findByCuentaIdOrderByFechaDesc(Long cuentaId, Pageable pageable);
}
