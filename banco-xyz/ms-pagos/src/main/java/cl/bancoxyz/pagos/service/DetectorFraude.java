package cl.bancoxyz.pagos.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cl.bancoxyz.common.event.AlertaSeguridadEvent.Severidad;
import cl.bancoxyz.pagos.domain.Pago;
import cl.bancoxyz.pagos.kafka.PublicadorEventos;
import cl.bancoxyz.pagos.repository.PagoRepository;

/**
 * Reglas simples de deteccion de fraude evaluadas en linea tras cada operacion completada.
 *  - Monto elevado              -> alerta ALTA (notificacion al cliente).
 *  - N retiros en cajero en X min -> alerta CRITICA (ms-cuentas bloquea la cuenta automaticamente).
 */
@Component
public class DetectorFraude {

    private final PagoRepository repo;
    private final PublicadorEventos eventos;
    private final BigDecimal umbralMonto;
    private final int maxRetirosVentana;
    private final Duration ventana;

    public DetectorFraude(PagoRepository repo, PublicadorEventos eventos,
                          @Value("${banco.pagos.fraude.umbral-monto:5000000}") BigDecimal umbralMonto,
                          @Value("${banco.pagos.fraude.max-retiros-ventana:3}") int maxRetirosVentana,
                          @Value("${banco.pagos.fraude.ventana-minutos:10}") long ventanaMinutos) {
        this.repo = repo;
        this.eventos = eventos;
        this.umbralMonto = umbralMonto;
        this.maxRetirosVentana = maxRetirosVentana;
        this.ventana = Duration.ofMinutes(ventanaMinutos);
    }

    public void evaluar(Pago p) {
        if (p.getMonto().compareTo(umbralMonto) >= 0) {
            eventos.alerta(p, "MONTO_ELEVADO", Severidad.ALTA,
                    "Operacion %s por $%s supera el umbral de $%s".formatted(p.getTipo(),
                            p.getMonto().toPlainString(), umbralMonto.toPlainString()));
        }
        if (p.getTipo() == Pago.Tipo.RETIRO && "CAJERO".equals(p.getCanal())) {
            long retiros = repo.countByCuentaOrigenIdAndTipoAndEstadoAndFechaCreacionAfter(
                    p.getCuentaOrigenId(), Pago.Tipo.RETIRO, Pago.Estado.COMPLETADO, Instant.now().minus(ventana));
            if (retiros >= maxRetirosVentana) {
                eventos.alerta(p, "RETIROS_REPETIDOS", Severidad.CRITICA,
                        "%d retiros en cajero en menos de %d minutos. Cuenta bloqueada preventivamente"
                                .formatted(retiros, ventana.toMinutes()));
            }
        }
    }
}
