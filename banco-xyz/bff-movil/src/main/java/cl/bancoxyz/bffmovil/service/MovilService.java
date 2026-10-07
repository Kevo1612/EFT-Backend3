package cl.bancoxyz.bffmovil.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cl.bancoxyz.bffmovil.client.Clients.ClientesClient;
import cl.bancoxyz.bffmovil.client.Clients.CuentasClient;
import cl.bancoxyz.bffmovil.client.Clients.PagosClient;
import cl.bancoxyz.bffmovil.dto.Movil;
import cl.bancoxyz.common.security.SecurityUtils;
import cl.bancoxyz.common.web.NegocioException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * Logica del canal movil.
 * Optimizaciones: respuestas reducidas, numero de cuenta enmascarado, maximo 10 movimientos,
 * y cache del ultimo resumen por usuario: si los microservicios fallan la app sigue mostrando
 * el ultimo saldo conocido marcado como "cache" (mejor experiencia en redes moviles inestables).
 */
@Service
public class MovilService {

    private static final Logger log = LoggerFactory.getLogger(MovilService.class);
    private static final ZoneId CHILE = ZoneId.of("America/Santiago");

    private final ClientesClient clientes;
    private final CuentasClient cuentas;
    private final PagosClient pagos;
    private final Duration ttlCache;
    private final Map<String, Movil.Resumen> cache = new ConcurrentHashMap<>();

    public MovilService(ClientesClient clientes, CuentasClient cuentas, PagosClient pagos,
                        @Value("${banco.movil.cache-ttl-min:30}") long ttlMin) {
        this.clientes = clientes;
        this.cuentas = cuentas;
        this.pagos = pagos;
        this.ttlCache = Duration.ofMinutes(ttlMin);
    }

    @CircuitBreaker(name = "movil-resumen", fallbackMethod = "resumenDesdeCache")
    @Retry(name = "lecturas")
    public Movil.Resumen resumen() {
        Movil.ClienteDto cliente = clientes.yo();
        List<Movil.CuentaCorta> lista = cuentas.porCliente(cliente.id()).stream()
                .filter(c -> "ACTIVA".equals(c.estado()) || "BLOQUEADA".equals(c.estado()))
                .map(c -> new Movil.CuentaCorta(c.id(), enmascarar(c.numeroCuenta()), c.tipo(), c.saldo()))
                .toList();
        BigDecimal total = lista.stream().map(Movil.CuentaCorta::saldo).reduce(BigDecimal.ZERO, BigDecimal::add);
        Movil.Resumen r = new Movil.Resumen(cliente.nombres(), total, lista, null, Instant.now());
        cache.put(SecurityUtils.usuarioActual(), r);
        return r;
    }

    @SuppressWarnings("unused")
    private Movil.Resumen resumenDesdeCache(Throwable e) {
        if (e instanceof FeignException.FeignClientException fce) {
            throw fce;
        }
        Movil.Resumen previo = cache.get(SecurityUtils.usuarioActual());
        if (previo != null && previo.actualizado().isAfter(Instant.now().minus(ttlCache))) {
            log.warn("Backend no disponible, se entrega resumen en cache para {}", SecurityUtils.usuarioActual());
            return new Movil.Resumen(previo.nombre(), previo.saldoTotal(), previo.cuentas(), true,
                    previo.actualizado());
        }
        throw NegocioException.noDisponible("Servicio no disponible, intenta en unos minutos");
    }

    @CircuitBreaker(name = "ms-cuentas", fallbackMethod = "movimientosFallback")
    @Retry(name = "lecturas")
    public List<Movil.MovCorto> movimientos(Long cuentaId) {
        return cuentas.movimientos(cuentaId, 10).stream()
                .map(m -> new Movil.MovCorto(
                        m.fecha().atZone(CHILE).toLocalDate(),
                        recortar(m.descripcion(), 30),
                        "DEBITO".equals(m.tipo()) ? m.monto().negate() : m.monto()))
                .toList();
    }

    @SuppressWarnings("unused")
    private List<Movil.MovCorto> movimientosFallback(Long cuentaId, Throwable e) {
        if (e instanceof FeignException.FeignClientException fce) {
            throw fce;
        }
        throw NegocioException.noDisponible("Movimientos no disponibles, intenta en unos minutos");
    }

    @CircuitBreaker(name = "ms-pagos", fallbackMethod = "transferirFallback")
    public Movil.Comprobante transferir(Movil.Transferencia t, String idempotencyKey) {
        Movil.PagoDto p = pagos.transferir(new Movil.PagoInterno(t.origen(), t.destino(), t.monto(),
                t.glosa(), "MOVIL"), idempotencyKey);
        cache.remove(SecurityUtils.usuarioActual()); // el saldo cambio
        return new Movil.Comprobante(p.referencia(), p.estado(), p.motivo());
    }

    @SuppressWarnings("unused")
    private Movil.Comprobante transferirFallback(Movil.Transferencia t, String key, Throwable e) {
        if (e instanceof FeignException.FeignClientException fce) {
            throw fce;
        }
        throw NegocioException.noDisponible("No se pudo transferir. No se realizo ningun cargo.");
    }

    static String enmascarar(String numero) {
        if (numero == null || numero.length() < 4) {
            return numero;
        }
        return "****" + numero.substring(numero.length() - 4);
    }

    private static String recortar(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
