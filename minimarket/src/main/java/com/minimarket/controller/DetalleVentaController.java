package com.minimarket.controller;

import com.minimarket.entity.DetalleVenta;
import com.minimarket.service.DetalleVentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Detalle de Ventas",
        description = "Operaciones para la administración del detalle de las ventas registradas."
)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/detalle-ventas")
public class DetalleVentaController {

    @Autowired
    private DetalleVentaService detalleVentaService;

    @Operation(
            summary = "Listar detalles de venta",
            description = "Obtiene el listado completo de los detalles de ventas registrados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalles obtenidos correctamente"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public List<DetalleVenta> listarDetalleVentas() {
        return detalleVentaService.findAll();
    }

    @Operation(
            summary = "Buscar detalle de venta por ID",
            description = "Obtiene un detalle de venta mediante su identificador."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle encontrado"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Detalle no encontrado")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public ResponseEntity<DetalleVenta> obtenerDetalleVentaPorId(
            @Parameter(description = "ID del detalle de venta", example = "1")
            @PathVariable Long id) {

        DetalleVenta detalleVenta = detalleVentaService.findById(id);

        return (detalleVenta != null)
                ? ResponseEntity.ok(detalleVenta)
                : ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "Registrar detalle de venta",
            description = "Registra un nuevo detalle asociado a una venta."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "403", description = "Solo administradores y cajeros pueden registrar detalles")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public DetalleVenta guardarDetalleVenta(@RequestBody DetalleVenta detalleVenta) {
        return detalleVentaService.save(detalleVenta);
    }

    @Operation(
            summary = "Actualizar detalle de venta",
            description = "Actualiza la información de un detalle de venta existente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "403", description = "Solo administradores y cajeros pueden actualizar detalles"),
            @ApiResponse(responseCode = "404", description = "Detalle no encontrado")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public ResponseEntity<DetalleVenta> actualizarDetalleVenta(
            @Parameter(description = "ID del detalle de venta", example = "1")
            @PathVariable Long id,
            @RequestBody DetalleVenta detalleVenta) {

        DetalleVenta existente = detalleVentaService.findById(id);

        if (existente != null) {
            detalleVenta.setId(id);
            return ResponseEntity.ok(detalleVentaService.save(detalleVenta));
        }

        return ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "Eliminar detalle de venta",
            description = "Elimina un detalle de venta del sistema."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Detalle eliminado correctamente"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "403", description = "Solo administradores pueden eliminar detalles"),
            @ApiResponse(responseCode = "404", description = "Detalle no encontrado")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarDetalleVenta(
            @Parameter(description = "ID del detalle de venta", example = "1")
            @PathVariable Long id) {

        DetalleVenta detalleVenta = detalleVentaService.findById(id);

        if (detalleVenta != null) {
            detalleVentaService.deleteById(id);
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }
}