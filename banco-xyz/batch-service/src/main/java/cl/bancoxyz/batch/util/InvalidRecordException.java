package cl.bancoxyz.batch.util;

/**
 * Registro que no puede procesarse (dato estructuralmente invalido).
 * Es "skippable": Spring Batch lo omite, lo registra en registros_rechazados y continua.
 */
public class InvalidRecordException extends RuntimeException {

    public InvalidRecordException(String motivo) {
        super(motivo);
    }
}
