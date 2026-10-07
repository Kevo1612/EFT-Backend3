package cl.bancoxyz.pagos.service;

/** ms-cuentas no responde (caido, timeout o circuito abierto): la operacion se reintentara mas tarde. */
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
