package cl.bancoxyz.batch.model;

/** Linea de intereses_trimestrales.csv: cuenta_id,nombre,saldo,edad,tipo */
public class InteresCsv extends RegistroCsv {

    private String cuentaId;
    private String nombre;
    private String saldo;
    private String edad;
    private String tipo;

    public InteresCsv() {
    }

    public InteresCsv(String cuentaId, String nombre, String saldo, String edad, String tipo) {
        this.cuentaId = cuentaId;
        this.nombre = nombre;
        this.saldo = saldo;
        this.edad = edad;
        this.tipo = tipo;
    }

    public String getCuentaId() { return cuentaId; }
    public void setCuentaId(String cuentaId) { this.cuentaId = cuentaId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getSaldo() { return saldo; }
    public void setSaldo(String saldo) { this.saldo = saldo; }
    public String getEdad() { return edad; }
    public void setEdad(String edad) { this.edad = edad; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}
