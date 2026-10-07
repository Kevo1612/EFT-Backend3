package cl.bancoxyz.batch.processor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.batch.item.ItemProcessor;

import cl.bancoxyz.batch.model.EstadoAnualCsv;
import cl.bancoxyz.batch.model.MovimientoAnual;
import cl.bancoxyz.batch.util.FechaParser;
import cl.bancoxyz.batch.util.InvalidRecordException;
import cl.bancoxyz.batch.util.Textos;

/**
 * Proceso 3 - Generacion de Estados de Cuenta Anuales (paso de normalizacion).
 *  - cuenta, fecha, tipo de transaccion y monto son obligatorios (si no, skip).
 *  - el legacy guardaba retiros/compras a veces con signo negativo y a veces positivo: se normaliza
 *    a monto positivo y el signo lo da el tipo (deposito suma; retiro, compra y pago restan).
 *  - deposito con monto negativo -> se registra observacion SIGNO_INCONSISTENTE.
 *  - monto cero -> observacion MONTO_CERO; descripcion vacia -> "SIN DESCRIPCION" + observacion.
 */
public class EstadoAnualProcessor implements ItemProcessor<EstadoAnualCsv, MovimientoAnual> {

    private static final Set<String> TIPOS = Set.of("deposito", "retiro", "compra", "pago");

    private final String semana;

    public EstadoAnualProcessor(String semana) {
        this.semana = semana;
    }

    @Override
    public MovimientoAnual process(EstadoAnualCsv in) {
        if (Textos.vacio(in.getRaw())) {
            return null;
        }
        long cuenta = Textos.entero(in.getCuentaId(), "cuenta_id");
        LocalDate fecha = FechaParser.parsear(in.getFecha())
                .orElseThrow(() -> new InvalidRecordException("fecha invalida: '" + Textos.limpio(in.getFecha()) + "'"));
        String tipo = Textos.limpio(in.getTransaccion()).toLowerCase();
        if (!TIPOS.contains(tipo)) {
            throw new InvalidRecordException("transaccion desconocida: '" + tipo + "'");
        }
        if (Textos.vacio(in.getMonto())) {
            throw new InvalidRecordException("monto vacio");
        }
        BigDecimal original = Textos.decimal(in.getMonto(), "monto");

        List<String> obs = new ArrayList<>();
        if (!FechaParser.esFormatoEstandar(in.getFecha())) {
            obs.add("FECHA_NORMALIZADA");
        }
        if (original.signum() == 0) {
            obs.add("MONTO_CERO");
        }
        boolean esIngreso = "deposito".equals(tipo);
        if (esIngreso && original.signum() < 0) {
            obs.add("SIGNO_INCONSISTENTE");
        }
        BigDecimal monto = original.abs();
        BigDecimal firmado = esIngreso ? monto : monto.negate();

        String descripcion = Textos.limpio(in.getDescripcion());
        if (descripcion.isEmpty()) {
            descripcion = "SIN DESCRIPCION";
            obs.add("DESCRIPCION_VACIA");
        }
        return new MovimientoAnual(semana, in.getLinea(), cuenta, fecha, tipo, monto, firmado, descripcion,
                String.join(";", obs));
    }
}
