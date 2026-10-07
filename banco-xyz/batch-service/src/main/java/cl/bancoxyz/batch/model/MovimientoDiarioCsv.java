package cl.bancoxyz.batch.model;

/** Linea de movimientos_financieros_diarios.csv: id,fecha,monto,tipo */
public class MovimientoDiarioCsv extends RegistroCsv {

    private String id;
    private String fecha;
    private String monto;
    private String tipo;

    public MovimientoDiarioCsv() {
    }

    public MovimientoDiarioCsv(String id, String fecha, String monto, String tipo) {
        this.id = id;
        this.fecha = fecha;
        this.monto = monto;
        this.tipo = tipo;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getMonto() { return monto; }
    public void setMonto(String monto) { this.monto = monto; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}
