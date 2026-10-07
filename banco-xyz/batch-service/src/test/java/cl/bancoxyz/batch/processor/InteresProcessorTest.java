package cl.bancoxyz.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;

import cl.bancoxyz.batch.model.InteresCalculado;
import cl.bancoxyz.batch.model.InteresCsv;
import cl.bancoxyz.batch.util.InvalidRecordException;

/** Resultados esperados de semana_1 (calculados y verificados manualmente). */
class InteresProcessorTest {

    private final InteresProcessor p = new InteresProcessor("semana_1", "2024-12",
            Map.of("ahorro", new BigDecimal("0.0050"), "prestamo", new BigDecimal("0.0150"),
                   "hipoteca", new BigDecimal("0.0075")), 18, 99);

    private static InteresCsv linea(String cuenta, String saldo, String edad, String tipo) {
        InteresCsv i = new InteresCsv(cuenta, "Titular", saldo, edad, tipo);
        i.setLinea(2);
        i.setRaw(cuenta + ",Titular," + saldo + "," + edad + "," + tipo);
        return i;
    }

    @Test
    void ahorroAbonaInteres() {
        InteresCalculado r = p.process(linea("101", "5000", "30", "ahorro"));
        assertThat(r.interes()).isEqualByComparingTo("25.00");
        assertThat(r.saldoFinal()).isEqualByComparingTo("5025.00");
        assertThat(r.naturaleza()).isEqualTo("ABONO");
    }

    @Test
    void prestamoEHipotecaCarganInteres() {
        assertThat(p.process(linea("102", "8000", "25", "prestamo")).interes()).isEqualByComparingTo("120.00");
        InteresCalculado h = p.process(linea("105", "7000", "35", "hipoteca"));
        assertThat(h.interes()).isEqualByComparingTo("52.50");
        assertThat(h.naturaleza()).isEqualTo("CARGO");
    }

    @Test
    void saldoCeroGeneraInteresCero() {
        assertThat(p.process(linea("104", "0", "45", "ahorro")).interes()).isEqualByComparingTo("0");
    }

    @Test
    void registrosInvalidosSeOmiten() {
        assertThatThrownBy(() -> p.process(linea("104", "", "45", "ahorro"))).hasMessageContaining("SALDO_FALTANTE");
        assertThatThrownBy(() -> p.process(linea("103", "12000", "", "-1"))).hasMessageContaining("EDAD_FALTANTE");
        assertThatThrownBy(() -> p.process(linea("108", "10000", "100", "ahorro")))
                .isInstanceOf(InvalidRecordException.class).hasMessageContaining("EDAD_FUERA_DE_RANGO");
        assertThatThrownBy(() -> p.process(linea("124", "12000", "30", "-1")))
                .hasMessageContaining("TIPO_PRODUCTO_INVALIDO");
    }
}
