package com.minimarket.controller;

import com.minimarket.entity.Inventario;
import com.minimarket.hateoas.InventarioModelAssembler;
import com.minimarket.service.InventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(
        name = "Inventario",
        description = "Operaciones para la administración del inventario y control de stock."
)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private InventarioModelAssembler assembler;

    @Operation(
            summary = "Listar movimientos de inventario",
            description = "Obtiene todos los movimientos de inventario registrados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movimientos obtenidos correctamente"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public CollectionModel<EntityModel<Inventario>> listarMovimientosDeInventario() {

        List<EntityModel<Inventario>> inventario = inventarioService.findAll()
                .stream()
                .map(assembler::toModel)
                .toList();

        return CollectionModel.of(
                inventario,
                linkTo(methodOn(InventarioController.class)
                        .listarMovimientosDeInventario())
                        .withSelfRel()
        );
    }

    @Operation(
            summary = "Buscar movimiento por ID",
            description = "Obtiene un movimiento de inventario mediante su identificador."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movimiento encontrado"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Movimiento no encontrado")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public ResponseEntity<EntityModel<Inventario>> obtenerMovimientoPorId(
            @Parameter(description = "ID del movimiento de inventario", example = "1")
            @PathVariable Long id) {

        Inventario inventario = inventarioService.findById(id);

        if (inventario == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                assembler.toModel(inventario)
        );
    }

    @Operation(
            summary = "Registrar movimiento de inventario",
            description = "Registra un nuevo movimiento de entrada o salida de inventario."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movimiento registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "403", description = "Solo administradores y cajeros pueden registrar movimientos")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public Inventario registrarMovimiento(@RequestBody Inventario inventario) {
        return inventarioService.save(inventario);
    }

    @Operation(
            summary = "Actualizar movimiento de inventario",
            description = "Actualiza la información de un movimiento de inventario existente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movimiento actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "403", description = "Solo administradores y cajeros pueden actualizar movimientos"),
            @ApiResponse(responseCode = "404", description = "Movimiento no encontrado")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public ResponseEntity<Inventario> actualizarMovimiento(
            @Parameter(description = "ID del movimiento de inventario", example = "1")
            @PathVariable Long id,
            @RequestBody Inventario inventario) {

        Inventario existente = inventarioService.findById(id);

        if (existente != null) {
            inventario.setId(id);
            return ResponseEntity.ok(inventarioService.save(inventario));
        }

        return ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "Eliminar movimiento de inventario",
            description = "Elimina un movimiento de inventario del sistema."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Movimiento eliminado correctamente"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "403", description = "Solo administradores pueden eliminar movimientos"),
            @ApiResponse(responseCode = "404", description = "Movimiento no encontrado")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarMovimiento(
            @Parameter(description = "ID del movimiento de inventario", example = "1")
            @PathVariable Long id) {

        Inventario inventario = inventarioService.findById(id);

        if (inventario != null) {
            inventarioService.deleteById(id);
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }
}