package cl.bancoxyz.common.event;

import java.time.Instant;

/**
 * Resumen de una ejecucion de job batch, publicado al terminar el job.
 */
public record BatchResultadoEvent(
        String job,
        String semana,
        String estado,
        long leidos,
        long escritos,
        long omitidos,
        long duracionMs,
        Instant fecha) {
}
