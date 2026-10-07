package cl.bancoxyz.batch.processor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.batch.item.ItemProcessor;

import cl.bancoxyz.batch.model.MovimientoDiarioCsv;
import cl.bancoxyz.batch.model.TransaccionDiaria;
import cl.bancoxyz.batch.util.FechaParser;
import cl.bancoxyz.batch.util.InvalidRecordException;
import cl.bancoxyz.batch.util.Textos;

/**
 * Proceso 1 - Reporte de Transacciones Diarias.
 * Reglas (equivalentes al programa legacy):
 *  - id y fecha son obligatorios; si no se pueden interpretar el registro se RECHAZA (skip).
 *  - fechas en formatos no estandar se normalizan a yyyy-MM-dd.
 *  - monto vacio, negativo o cero y tipo distinto de debito/credito se marcan como ANOMALIA
 *    (se conservan para el reporte, pero no suman en los totales).
 *  - los duplicados (misma fecha, monto y tipo) se marcan en un step posterior con SQL,
 *    para que el resultado sea deterministico aunque el step sea multihilo.
 */
public class MovimientoDiarioProcessor implements ItemProcessor<MovimientoDiarioCsv, TransaccionDiaria> {

    private static final Set<String> TIPOS_VALIDOS = Set.of("debito", "credito");

    private final String semana;

    public MovimientoDiarioProcessor(String semana) {
        this.semana = semana;
    }

    @Override
    public TransaccionDiaria process(MovimientoDiarioCsv in) {
        if (Textos.vacio(in.getRaw())) {
            return null; // linea en blanco: se filtra
        }
        if (Textos.vacio(in.getId())) {
            throw new InvalidRecordException("id vacio");
        }
        long id = Textos.entero(in.getId(), "id");
        LocalDate fecha = FechaParser.parsear(in.getFecha())
                .orElseThrow(() -> new InvalidRecordException("fecha invalida: '" + Textos.limpio(in.getFecha()) + "'"));
        boolean normalizada = !FechaParser.esFormatoEstandar(in.getFecha());

        List<String> motivos = new ArrayList<>();
        BigDecimal monto = null;
        if (Textos.vacio(in.getMonto())) {
            motivos.add("MONTO_VACIO");
        } else {
            monto = Textos.decimal(in.getMonto(), "monto");
            if (monto.signum() < 0) {
                motivos.add("MONTO_NEGATIVO");
            } else if (monto.signum() == 0) {
                motivos.add("MONTO_CERO");
            }
        }
        String tipo = Textos.limpio(in.getTipo()).toLowerCase();
        if (!TIPOS_VALIDOS.contains(tipo)) {
            motivos.add("TIPO_INVALIDO");
        }
        return new TransaccionDiaria(semana, in.getLinea(), id, fecha, monto, tipo.isEmpty() ? "desconocido" : tipo,
                !motivos.isEmpty(), String.join(";", motivos), normalizada);
    }
}
