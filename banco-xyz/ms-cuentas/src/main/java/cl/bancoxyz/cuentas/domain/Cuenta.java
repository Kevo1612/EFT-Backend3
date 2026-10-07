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
import jakarta.persistence.Version;

@Entity
@Table(name = "cuentas", indexes = @Index(name = "idx_cuenta_cliente", columnList = "cliente_id"))
public class Cuenta {

    public enum Tipo { AHORRO, CORRIENTE, VISTA }

    public enum Estado { PENDIENTE_VALIDACION, ACTIVA, BLOQUEADA, CERRADA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_cuenta", nullable = false, unique = true, length = 20)
    private String numeroCuenta;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    /** Username del titular: permite validar propiedad sin llamar a ms-clientes en cada lectura. */
    @Column(name = "titular_username", nullable = false, length = 60)
    private String titularUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tipo tipo;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private Estado estado = Estado.ACTIVA;

    @Column(name = "fecha_apertura", nullable = false)
    private Instant fechaApertura = Instant.now();

    @Column(name = "fecha_cierre")
    private Instant fechaCierre;

    @Version
    private Long version;

    public Long getId() { return id; }
    public String getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(String numeroCuenta) { this.numeroCuenta = numeroCuenta; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public String getTitularUsername() { return titularUsername; }
    public void setTitularUsername(String titularUsername) { this.titularUsername = titularUsername; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public Instant getFechaApertura() { return fechaApertura; }
    public Instant getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(Instant fechaCierre) { this.fechaCierre = fechaCierre; }
    public Long getVersion() { return version; }
}
