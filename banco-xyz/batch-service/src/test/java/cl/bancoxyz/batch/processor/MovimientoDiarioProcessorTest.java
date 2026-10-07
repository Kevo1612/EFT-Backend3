package cl.bancoxyz.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import cl.bancoxyz.batch.model.MovimientoDiarioCsv;
import cl.bancoxyz.batch.model.TransaccionDiaria;
import cl.bancoxyz.batch.util.InvalidRecordException;

/** Casos tomados de semana_1 y semana_2 del repositorio legacy. */
class MovimientoDiarioProcessorTest {

    private final MovimientoDiarioProcessor p = new MovimientoDiarioProcessor("semana_2");

    private static MovimientoDiarioCsv linea(String id, String fecha, String monto, String tipo) {
        MovimientoDiarioCsv m = new MovimientoDiarioCsv(id, fecha, monto, tipo);
        m.setLinea(2);
        m.setRaw(id + "," + fecha + "," + monto + "," + tipo);
        return m;
    }

    @Test
    void registroValido() {
        TransaccionDiaria t = p.process(linea("1", "2024-01-01", "1000", "debito"));
        assertThat(t.anomalia()).isFalse();
        assertThat(t.monto()).isEqualByComparingTo("1000");
        assertThat(t.fechaNormalizada()).isFalse();
    }

    @Test
    void montoNegativoEsAnomalia() {
        TransaccionDiaria t = p.process(linea("3", "2024-01-03", "-200", "debito       "));
        assertThat(t.anomalia()).isTrue();
        assertThat(t.motivo()).isEqualTo("MONTO_NEGATIVO");
        assertThat(t.tipo()).isEqualTo("debito");
    }

    @Test
    void montoVacioYTipoInvalido() {
        assertThat(p.process(linea("4", "2024-01-03", "", "debito")).motivo()).isEqualTo("MONTO_VACIO");
        assertThat(p.process(linea("10", "2024-01-08", "1000", "invalido")).motivo()).isEqualTo("TIPO_INVALIDO");
    }

    @Test
    void fechaAlternativaSeNormaliza() {
        TransaccionDiaria t = p.process(linea("5", "2024/01/04", "800", "credito"));
        assertThat(t.fecha()).isEqualTo(LocalDate.of(2024, 1, 4));
        assertThat(t.fechaNormalizada()).isTrue();
        assertThat(t.anomalia()).isFalse();
    }

    @Test
    void fechaImposibleSeOmite() {
        assertThatThrownBy(() -> p.process(linea("9", "2024-13-01", "100", "debito")))
                .isInstanceOf(InvalidRecordException.class);
    }

    @Test
    void montoCero() {
        assertThat(p.process(linea("4", "2024-01-03", "0", "debito")).motivo()).isEqualTo("MONTO_CERO");
    }
}
