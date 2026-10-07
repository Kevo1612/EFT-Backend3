package cl.bancoxyz.batch.model;

/**
 * Base de toda linea leida desde CSV: conserva numero de linea y contenido original
 * para trazabilidad (registros rechazados y deduplicacion deterministica).
 */
public abstract class RegistroCsv {

    private int linea;
    private String raw;

    public int getLinea() { return linea; }
    public void setLinea(int linea) { this.linea = linea; }
    public String getRaw() { return raw; }
    public void setRaw(String raw) { this.raw = raw; }
}
