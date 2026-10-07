package cl.bancoxyz.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import cl.bancoxyz.batch.model.EstadoAnualCsv;
import cl.bancoxyz.batch.model.MovimientoAnual;

class EstadoAnualProcessorTest {

    private final EstadoAnualProcessor p = new EstadoAnualProcessor("semana_3");

    private static EstadoAnualCsv linea(String cuenta, String fecha, String tipo, String monto, String desc) {
        EstadoAnualCsv e = new EstadoAnualCsv(cuenta, fecha, tipo, monto, desc);
        e.setLinea(2);
        e.setRaw(String.join(",", cuenta, fecha, tipo, monto, desc));
        return e;
    }

    @Test
    void retiroSiempreResta() {
        // el legacy guardaba retiros con y sin signo: ambos deben restar lo mismo
        MovimientoAnual a = p.process(linea("101", "2024-03-15", "retiro", "-500", "Retiro parcial"));
        MovimientoAnual b = p.process(linea("103", "2024-05-09", "retiro", "500", "Retiro"));
        assertThat(a.montoFirmado()).isEqualByComparingTo("-500");
        assertThat(b.montoFirmado()).isEqualByComparingTo("-500");
        assertThat(a.observaciones()).isEmpty();
    }

    @Test
    void depositoNegativoSeCorrigeYSeObserva() {
        MovimientoAnual m = p.process(linea("105", "26-03-2024", "deposito", "-500", ""));
        assertThat(m.montoFirmado()).isEqualByComparingTo("500");
        assertThat(m.descripcion()).isEqualTo("SIN DESCRIPCION");
        assertThat(m.observaciones()).contains("FECHA_NORMALIZADA", "SIGNO_INCONSISTENTE", "DESCRIPCION_VACIA");
    }

    @Test
    void transaccionDesconocidaOMontoVacioSeOmiten() {
        assertThatThrownBy(() -> p.process(linea("110", "2024-08-28", "transferencia", "10", "x")))
                .hasMessageContaining("transaccion desconocida");
        assertThatThrownBy(() -> p.process(linea("110", "2024-08-28", "retiro", "", "x")))
                .hasMessageContaining("monto vacio");
    }
}
