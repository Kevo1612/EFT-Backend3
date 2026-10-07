package cl.bancoxyz.clientes.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Notificacion generada a partir de eventos Kafka (transacciones y alertas).
 * La restriccion unica (referencia, tipo) hace que el consumidor sea idempotente:
 * si Kafka reentrega un mensaje no se duplica la notificacion.
 */
@Entity
@Table(name = "notificaciones",
       uniqueConstraints = @UniqueConstraint(name = "uk_notif_ref_tipo", columnNames = {"referencia", "tipo"}),
       indexes = @Index(name = "idx_notif_cliente", columnList = "cliente_id"))
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null = notificacion general (ej: alerta de calidad de datos del batch). */
    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(length = 10)
    private String severidad;

    @Column(nullable = false, length = 400)
    private String mensaje;

    @Column(nullable = false, length = 80)
    private String referencia;

    @Column(nullable = false)
    private Instant fecha = Instant.now();

    @Column(nullable = false)
    private boolean leida = false;

    public Notificacion() {
    }

    public Notificacion(Long clienteId, String tipo, String severidad, String mensaje, String referencia) {
        this.clienteId = clienteId;
        this.tipo = tipo;
        this.severidad = severidad;
        this.mensaje = mensaje;
        this.referencia = referencia;
    }

    public Long getId() { return id; }
    public Long getClienteId() { return clienteId; }
    public String getTipo() { return tipo; }
    public String getSeveridad() { return severidad; }
    public String getMensaje() { return mensaje; }
    public String getReferencia() { return referencia; }
    public Instant getFecha() { return fecha; }
    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
}
