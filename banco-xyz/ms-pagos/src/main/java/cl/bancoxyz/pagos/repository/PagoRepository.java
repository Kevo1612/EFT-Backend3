package cl.bancoxyz.pagos.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import cl.bancoxyz.pagos.domain.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByReferencia(String referencia);

    Optional<Pago> findByIdempotencyKey(String idempotencyKey);

    List<Pago> findTop50ByEstadoOrderByFechaCreacionAsc(Pago.Estado estado);

    @Query("select p from Pago p where p.cuentaOrigenId = :cuentaId or p.cuentaDestinoId = :cuentaId "
            + "order by p.fechaCreacion desc")
    List<Pago> historialCuenta(@Param("cuentaId") Long cuentaId, Pageable pageable);

    long countByCuentaOrigenIdAndTipoAndEstadoAndFechaCreacionAfter(Long cuentaOrigenId, Pago.Tipo tipo,
                                                                     Pago.Estado estado, Instant desde);

    /**
     * "Reclama" un pago pendiente para reprocesarlo. Con varias replicas de ms-pagos solo una
     * logra el UPDATE (devuelve 1); las demas lo omiten. Evita trabajo duplicado.
     */
    @Transactional
    @Modifying
    @Query("update Pago p set p.estado = :nuevo, p.version = p.version + 1 where p.id = :id and p.estado = :actual")
    int cambiarEstadoSi(@Param("id") Long id, @Param("actual") Pago.Estado actual, @Param("nuevo") Pago.Estado nuevo);

    default boolean reclamar(Long id) {
        return cambiarEstadoSi(id, Pago.Estado.PENDIENTE_REINTENTO, Pago.Estado.PROCESANDO) == 1;
    }
}
