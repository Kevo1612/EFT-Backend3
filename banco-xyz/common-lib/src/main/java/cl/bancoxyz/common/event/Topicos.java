package cl.bancoxyz.common.event;

/**
 * Nombres de los topicos Kafka usados por toda la plataforma.
 * Centralizarlos evita errores de tipeo entre productores y consumidores.
 */
public final class Topicos {

    /** Pagos, transferencias, depositos y retiros que terminaron correctamente. */
    public static final String TRANSACCIONES_COMPLETADAS = "banco.transacciones.completadas";
    /** Alertas de seguridad / fraude / calidad de datos. */
    public static final String ALERTAS_SEGURIDAD = "banco.alertas.seguridad";
    /** Ciclo de vida de las cuentas (apertura, bloqueo, cierre). */
    public static final String CUENTAS_EVENTOS = "banco.cuentas.eventos";
    /** Resultado de cada ejecucion de un job batch. */
    public static final String BATCH_RESULTADOS = "banco.batch.resultados";

    private Topicos() {
    }
}
