package cl.bancoxyz.cuentas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.bancoxyz.cuentas.domain.Cuenta;
import jakarta.persistence.LockModeType;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    List<Cuenta> findByClienteIdOrderByIdAsc(Long clienteId);

    List<Cuenta> findByEstado(Cuenta.Estado estado);

    boolean existsByNumeroCuenta(String numeroCuenta);

    /**
     * SELECT ... FOR UPDATE: bloquea la fila mientras se modifica el saldo.
     * Garantiza consistencia aunque haya varias replicas de ms-cuentas atendiendo en paralelo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cuenta c where c.id = :id")
    Optional<Cuenta> findByIdForUpdate(@Param("id") Long id);
}
