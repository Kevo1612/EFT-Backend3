package cl.bancoxyz.cuentas.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Asiento en la cartola de la cuenta. La referencia es unica: aplicar dos veces el mismo
 * movimiento (reintento de ms-pagos) devuelve el asiento existente en vez de duplicar el cargo.
 */
@Entity
@Table(name = "movimientos_cuenta", indexes = @Index(name = "idx_mov_cuenta_fecha", columnList = "cuenta_id, fecha"))
public class MovimientoCuenta {

    public enum Tipo { DEBITO, CREDITO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_id", nullable = false)
    private Long cuentaId;

    @Column(nullable = false, unique = true, length = 80)
    private String referencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Tipo tipo;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal monto;

    @Column(name = "saldo_resultante", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoResultante;

    @Column(length = 200)
    private String descripcion;

    @Column(nullable = false)
    private Instant fecha = Instant.now();

    public MovimientoCuenta() {
    }

    public MovimientoCuenta(Long cuentaId, String referencia, Tipo tipo, BigDecimal monto,
                            BigDecimal saldoResultante, String descripcion) {
        this.cuentaId = cuentaId;
        this.referencia = referencia;
        this.tipo = tipo;
        this.monto = monto;
        this.saldoResultante = saldoResultante;
        this.descripcion = descripcion;
    }

    public Long getId() { return id; }
    public Long getCuentaId() { return cuentaId; }
    public String getReferencia() { return referencia; }
    public Tipo getTipo() { return tipo; }
    public BigDecimal getMonto() { return monto; }
    public BigDecimal getSaldoResultante() { return saldoResultante; }
    public String getDescripcion() { return descripcion; }
    public Instant getFecha() { return fecha; }
}
