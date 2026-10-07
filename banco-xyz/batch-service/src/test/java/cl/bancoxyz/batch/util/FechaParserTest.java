package cl.bancoxyz.batch.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class FechaParserTest {

    @Test
    void aceptaLosCuatroFormatosDelLegacy() {
        LocalDate esperado = LocalDate.of(2024, 11, 25);
        assertThat(FechaParser.parsear("2024-11-25")).contains(esperado);
        assertThat(FechaParser.parsear("2024/11/25")).contains(esperado);
        assertThat(FechaParser.parsear("25-11-2024")).contains(esperado);
        assertThat(FechaParser.parsear("25/11/2024")).contains(esperado);
    }

    @Test
    void rechazaFechasImposiblesOVacias() {
        assertThat(FechaParser.parsear("2024-13-01")).isEmpty(); // mes 13 (aparece 49 veces en semana_3)
        assertThat(FechaParser.parsear("2024-02-30")).isEmpty();
        assertThat(FechaParser.parsear("")).isEmpty();
        assertThat(FechaParser.parsear(null)).isEmpty();
    }

    @Test
    void detectaFormatoEstandar() {
        assertThat(FechaParser.esFormatoEstandar(" 2024-01-05 ")).isTrue();
        assertThat(FechaParser.esFormatoEstandar("2024/01/05")).isFalse();
    }
}
