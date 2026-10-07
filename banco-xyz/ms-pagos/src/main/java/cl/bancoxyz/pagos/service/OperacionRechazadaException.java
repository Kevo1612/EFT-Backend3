package cl.bancoxyz.pagos.service;

/** ms-cuentas rechazo la operacion por una regla de negocio (saldo insuficiente, cuenta bloqueada, etc.). */
public class OperacionRechazadaException extends RuntimeException {

    private final int status;

    public OperacionRechazadaException(int status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
