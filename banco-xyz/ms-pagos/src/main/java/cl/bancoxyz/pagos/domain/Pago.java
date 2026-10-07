package cl.bancoxyz.pagos.domain;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Operacion monetaria. Actua como registro de la SAGA: guarda que pasos ya se aplicaron
 * (debito, credito) para poder reintentar o compensar sin duplicar movimientos.
 */
@Entity
@Table(name = "pagos", indexes = {
        @Index(name = "idx_pago_origen", columnList = "cuenta_origen_id, fecha_creacion"),
        @Index(name = "idx_pago_destino", columnList = "cuenta_destino_id, fecha_creacion"),
        @Index(name = "idx_pago_estado", columnList = "estado")})
public class Pago {

    public enum Tipo { TRANSFERENCIA, DEPOSITO, RETIRO, PAGO_SERVICIO }

    public enum Estado { PROCESANDO, PENDIENTE_REINTENTO, COMPLETADO, RECHAZADO, REVERTIDO, REQUIERE_REVISION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String referencia;

    /** Header Idempotency-Key enviado por el canal: evita cobrar dos veces si el cliente reintenta. */
    @Column(name = "idempotency_key", unique = true, length = 80)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tipo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.PROCESANDO;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "cuenta_origen_id")
    private Long cuentaOrigenId;

    @Column(name = "cuenta_destino_id")
    private Long cuentaDestinoId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal monto;

    @Column(length = 200)
    private String descripcion;

    @Column(nullable = false, length = 20)
    private String canal;

    /** Usuario que origino la operacion (para validar propiedad incluso en reintentos diferidos). */
    @Column(nullable = false, length = 60)
    private String solicitante;

    @Column(name = "solicitante_staff", nullable = false)
    private boolean solicitanteStaff;

    @Column(length = 300)
    private String motivo;

    @Column(nullable = false)
    private int intentos;

    @Column(nullable = false)
    private boolean validado;

    @Column(name = "debito_aplicado", nullable = false)
    private boolean debitoAplicado;

    @Column(name = "credito_aplicado", nullable = false)
    private boolean creditoAplicado;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion = Instant.now();

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion = Instant.now();

    @Version
    private Long version;

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = Instant.now();
    }

    public Long getId() { return id; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public Long getCuentaOrigenId() { return cuentaOrigenId; }
    public void setCuentaOrigenId(Long cuentaOrigenId) { this.cuentaOrigenId = cuentaOrigenId; }
    public Long getCuentaDestinoId() { return cuentaDestinoId; }
    public void setCuentaDestinoId(Long cuentaDestinoId) { this.cuentaDestinoId = cuentaDestinoId; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }
    public String getSolicitante() { return solicitante; }
    public void setSolicitante(String solicitante) { this.solicitante = solicitante; }
    public boolean isSolicitanteStaff() { return solicitanteStaff; }
    public void setSolicitanteStaff(boolean solicitanteStaff) { this.solicitanteStaff = solicitanteStaff; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public int getIntentos() { return intentos; }
    public void setIntentos(int intentos) { this.intentos = intentos; }
    public boolean isValidado() { return validado; }
    public void setValidado(boolean validado) { this.validado = validado; }
    public boolean isDebitoAplicado() { return debitoAplicado; }
    public void setDebitoAplicado(boolean debitoAplicado) { this.debitoAplicado = debitoAplicado; }
    public boolean isCreditoAplicado() { return creditoAplicado; }
    public void setCreditoAplicado(boolean creditoAplicado) { this.creditoAplicado = creditoAplicado; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaActualizacion() { return fechaActualizacion; }
    public Long getVersion() { return version; }

    /** Cuenta cuya propiedad se valida: origen (cargos) o destino (depositos). */
    public Long cuentaPrincipal() {
        return cuentaOrigenId != null ? cuentaOrigenId : cuentaDestinoId;
    }
}
