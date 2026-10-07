package cl.bancoxyz.bffweb.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import org.springframework.stereotype.Service;

import cl.bancoxyz.bffweb.dto.Dtos;
import cl.bancoxyz.bffweb.dto.Requests;
import cl.bancoxyz.bffweb.dto.Vistas;
import cl.bancoxyz.common.web.NegocioException;

/**
 * Orquesta varias llamadas y arma vistas completas para el navegador.
 * Las consultas independientes se hacen EN PARALELO (CompletableFuture) para que el tiempo
 * de respuesta sea el de la llamada mas lenta y no la suma de todas.
 */
@Service
public class WebService {

    private static final int OPERACIONES_POR_CUENTA = 10;

    private final Backend backend;
    private final Executor executor;

    public WebService(Backend backend, Executor bffExecutor) {
        this.backend = backend;
        this.executor = bffExecutor;
    }

    public Vistas.Dashboard dashboard() {
        Dtos.Cliente cliente = backend.perfil();
        List<String> advertencias = new ArrayList<>();

        var fCuentas = CompletableFuture.supplyAsync(() -> backend.cuentas(cliente.id()), executor);
        var fNotif = CompletableFuture.supplyAsync(() -> backend.notificaciones(cliente.id(), 10), executor);
        List<Dtos.Cuenta> cuentas = esperar(fCuentas);

        // historial de todas las cuentas en paralelo
        List<CompletableFuture<List<Dtos.Pago>>> fHistoriales = cuentas.stream()
                .map(c -> CompletableFuture.supplyAsync(() -> backend.historial(c.id(), OPERACIONES_POR_CUENTA),
                        executor))
                .toList();

        List<Dtos.Pago> operaciones = new ArrayList<>();
        boolean historialCaido = false;
        for (var f : fHistoriales) {
            List<Dtos.Pago> h = esperar(f);
            if (h == null) {
                historialCaido = true;
            } else {
                operaciones.addAll(h);
            }
        }
        if (historialCaido) {
            advertencias.add("El historial de operaciones no esta disponible temporalmente");
        }
        List<Dtos.Pago> ultimas = operaciones.stream()
                .filter(distintoPorReferencia())
                .sorted(Comparator.comparing(Dtos.Pago::fechaCreacion).reversed())
                .limit(15)
                .toList();

        List<Dtos.Notificacion> notificaciones = esperar(fNotif);
        if (notificaciones == null) {
            advertencias.add("Las notificaciones no estan disponibles temporalmente");
            notificaciones = List.of();
        }

        BigDecimal total = cuentas.stream()
                .filter(c -> !"CERRADA".equals(c.estado()))
                .map(Dtos.Cuenta::saldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Vistas.Dashboard(cliente, cuentas, total, ultimas, notificaciones, advertencias);
    }

    public Vistas.DetalleCuenta detalleCuenta(Long cuentaId) {
        // ms-cuentas valida que la cuenta pertenezca al usuario del token
        Dtos.Cuenta cuenta = backend.cuenta(cuentaId);
        var fMov = CompletableFuture.supplyAsync(() -> backend.movimientos(cuentaId, 50), executor);
        var fPagos = CompletableFuture.supplyAsync(() -> backend.historial(cuentaId, 50), executor);
        List<String> advertencias = new ArrayList<>();
        List<Dtos.Movimiento> mov = esperar(fMov);
        List<Dtos.Pago> pagos = esperar(fPagos);
        if (mov == null) {
            advertencias.add("Cartola no disponible temporalmente");
        }
        if (pagos == null) {
            advertencias.add("Historial de operaciones no disponible temporalmente");
        }
        return new Vistas.DetalleCuenta(cuenta, mov == null ? List.of() : mov, pagos == null ? List.of() : pagos,
                advertencias);
    }

    public Dtos.Pago transferir(Requests.Transferencia req, String idempotencyKey) {
        return backend.transferir(new Dtos.PagoInterno(req.cuentaOrigenId(), req.cuentaDestinoId(), req.monto(),
                req.descripcion(), "WEB"), idempotencyKey);
    }

    public Dtos.Pago pagarServicio(Requests.PagoServicio req, String idempotencyKey) {
        String descripcion = "Pago " + req.servicio() + " N° " + req.numeroCliente();
        return backend.pagarServicio(new Dtos.PagoInterno(req.cuentaOrigenId(), null, req.monto(), descripcion, "WEB"),
                idempotencyKey);
    }

    public Dtos.Cuenta abrirCuenta(Requests.AbrirCuenta req) {
        Dtos.Cliente cliente = backend.perfil();
        return backend.abrirCuenta(new Dtos.AbrirCuentaInterno(cliente.id(), req.tipo()));
    }

    public Map<String, Object> listarClientes(int page, int size) {
        return backend.listarClientes(page, Math.min(size, 100));
    }

    // ---------------------------------------------------------------- helpers

    private static <T> T esperar(CompletableFuture<T> f) {
        try {
            return f.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw NegocioException.noDisponible("Error obteniendo datos");
        }
    }

    private static java.util.function.Predicate<Dtos.Pago> distintoPorReferencia() {
        Collection<String> vistos = new java.util.HashSet<>();
        return p -> Objects.nonNull(p.referencia()) && vistos.add(p.referencia());
    }
}
