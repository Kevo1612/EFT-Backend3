package cl.bancoxyz.batch.web;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta de los resultados generados por los jobs (equivalente a los listados del legacy). */
@RestController
@RequestMapping("/api/batch/reportes")
@PreAuthorize("hasRole('ADMIN')")
public class ReporteController {

    private final JdbcTemplate jdbc;

    public ReporteController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/transacciones/resumen")
    public List<Map<String, Object>> resumenTransacciones(@RequestParam(defaultValue = "semana_3") String semana) {
        return jdbc.queryForList("""
                SELECT fecha, tipo, cantidad, monto_valido, cantidad_anomalias
                  FROM resumen_transacciones_diarias WHERE semana = ? ORDER BY fecha, tipo""", semana);
    }

    @GetMapping("/transacciones/anomalias")
    public List<Map<String, Object>> anomalias(@RequestParam(defaultValue = "semana_3") String semana) {
        return jdbc.queryForList("""
                SELECT linea, id_origen, fecha, monto, tipo, motivo
                  FROM transacciones_diarias WHERE semana = ? AND anomalia ORDER BY linea""", semana);
    }

    @GetMapping("/intereses")
    public List<Map<String, Object>> intereses(@RequestParam(defaultValue = "semana_3") String semana,
                                               @RequestParam String periodo) {
        return jdbc.queryForList("""
                SELECT cuenta_id, nombre, tipo, saldo, tasa, interes, saldo_final, naturaleza, observacion
                  FROM intereses_calculados WHERE semana = ? AND periodo = ? ORDER BY cuenta_id""", semana, periodo);
    }

    @GetMapping("/estados-cuenta")
    public List<Map<String, Object>> estados(@RequestParam(defaultValue = "semana_3") String semana) {
        return jdbc.queryForList("""
                SELECT cuenta_id, anio, total_depositos, total_retiros, total_compras, total_pagos, saldo_neto,
                       cantidad_movimientos, movimientos_observados
                  FROM estado_cuenta_anual WHERE semana = ? ORDER BY cuenta_id, anio""", semana);
    }

    @GetMapping("/rechazados")
    public List<Map<String, Object>> rechazados(@RequestParam String job,
                                                @RequestParam(defaultValue = "semana_3") String semana) {
        return jdbc.queryForList("""
                SELECT linea, contenido, motivo, fecha
                  FROM registros_rechazados WHERE job = ? AND semana = ? ORDER BY linea""", job, semana);
    }

    /** Totales por semana: util para comparar contra los totales de control del sistema legacy. */
    @GetMapping("/control")
    public Map<String, Object> totalesControl(@RequestParam(defaultValue = "semana_3") String semana) {
        return jdbc.queryForMap("""
                SELECT
                  (SELECT COUNT(*) FROM transacciones_diarias WHERE semana = ?)                   AS transacciones_cargadas,
                  (SELECT COUNT(*) FROM transacciones_diarias WHERE semana = ? AND anomalia)      AS transacciones_anomalas,
                  (SELECT COUNT(*) FROM intereses_calculados WHERE semana = ?)                    AS cuentas_con_interes,
                  (SELECT COALESCE(SUM(interes),0) FROM intereses_calculados WHERE semana = ?)    AS total_intereses,
                  (SELECT COUNT(*) FROM movimientos_anuales WHERE semana = ?)                     AS movimientos_anuales,
                  (SELECT COUNT(*) FROM estado_cuenta_anual WHERE semana = ?)                     AS estados_generados,
                  (SELECT COUNT(*) FROM registros_rechazados WHERE semana = ?)                    AS registros_rechazados
                """, semana, semana, semana, semana, semana, semana, semana);
    }
}
