package cl.bancoxyz.batch.processor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.springframework.batch.item.ItemProcessor;

import cl.bancoxyz.batch.model.InteresCalculado;
import cl.bancoxyz.batch.model.InteresCsv;
import cl.bancoxyz.batch.util.InvalidRecordException;
import cl.bancoxyz.batch.util.Textos;

/**
 * Proceso 2 - Calculo de Intereses Mensuales.
 * Registros rechazados (skip): cuenta no numerica, saldo faltante o negativo,
 * edad faltante o fuera de rango [edadMinima, edadMaxima], tipo de producto desconocido.
 * Interes = saldo x tasa mensual del producto, redondeo HALF_EVEN a 2 decimales (redondeo bancario).
 *  - ahorro: el interes se ABONA al cliente.
 *  - prestamo / hipoteca: el interes se CARGA a la deuda.
 * Los registros con cuenta duplicada se eliminan en un step SQL posterior (se conserva la 1ra ocurrencia).
 */
public class InteresProcessor implements ItemProcessor<InteresCsv, InteresCalculado> {

    private final String semana;
    private final String periodo;
    private final Map<String, BigDecimal> tasas;
    private final int edadMin;
    private final int edadMax;

    public InteresProcessor(String semana, String periodo, Map<String, BigDecimal> tasas, int edadMin, int edadMax) {
        this.semana = semana;
        this.periodo = periodo;
        this.tasas = tasas;
        this.edadMin = edadMin;
        this.edadMax = edadMax;
    }

    @Override
    public InteresCalculado process(InteresCsv in) {
        if (Textos.vacio(in.getRaw())) {
            return null;
        }
        long cuenta = Textos.entero(in.getCuentaId(), "cuenta_id");

        if (Textos.vacio(in.getSaldo())) {
            throw new InvalidRecordException("SALDO_FALTANTE");
        }
        BigDecimal saldo = Textos.decimal(in.getSaldo(), "saldo");
        if (saldo.signum() < 0) {
            throw new InvalidRecordException("SALDO_NEGATIVO");
        }

        if (Textos.vacio(in.getEdad())) {
            throw new InvalidRecordException("EDAD_FALTANTE");
        }
        int edad = Textos.entero(in.getEdad(), "edad").intValue();
        if (edad < edadMin || edad > edadMax) {
            throw new InvalidRecordException("EDAD_FUERA_DE_RANGO (" + edad + ")");
        }

        String tipo = Textos.limpio(in.getTipo()).toLowerCase();
        BigDecimal tasa = tasas.get(tipo);
        if (tasa == null) {
            throw new InvalidRecordException("TIPO_PRODUCTO_INVALIDO ('" + tipo + "')");
        }

        BigDecimal interes = saldo.multiply(tasa).setScale(2, RoundingMode.HALF_EVEN);
        String naturaleza = "ahorro".equals(tipo) ? "ABONO" : "CARGO";
        return new InteresCalculado(semana, periodo, in.getLinea(), cuenta, Textos.limpio(in.getNombre()), tipo, edad,
                saldo, tasa, interes, saldo.add(interes), naturaleza);
    }
}
