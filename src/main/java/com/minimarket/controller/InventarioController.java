package com.minimarket.controller;

import com.minimarket.hateoas.InventarioModelAssembler;
import com.minimarket.entity.Inventario;
import com.minimarket.service.InventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/inventario")
@Tag(
        name = "Inventario",
        description = "API para la gestión de movimientos de inventario"
)
public class InventarioController {

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private InventarioModelAssembler assembler;

    @Operation(
            summary = "Listar movimientos de inventario",
            description = "Obtiene todos los movimientos registrados."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Movimientos obtenidos correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content)
    })
    @GetMapping
    public CollectionModel<EntityModel<Inventario>> listarMovimientosDeInventario() {

        List<EntityModel<Inventario>> inventario = inventarioService.findAll()
                .stream()
                .map(assembler::toModel)
                .toList();

        return CollectionModel.of(inventario,
                linkTo(methodOn(InventarioController.class)
                        .listarMovimientosDeInventario())
                        .withSelfRel());
    }

    @Operation(
            summary = "Buscar movimiento por ID",
            description = "Obtiene un movimiento de inventario por su ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Movimiento encontrado"),
            @ApiResponse(responseCode = "404", description = "Movimiento no encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Inventario>> obtenerMovimientoPorId(
            @Parameter(description = "ID del movimiento", example = "1")
            @PathVariable Long id) {

        Inventario inventario = inventarioService.findById(id);

        if (inventario == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(assembler.toModel(inventario));
    }

    @Operation(
            summary = "Registrar movimiento",
            description = "Registra un nuevo movimiento de inventario."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Movimiento registrado correctamente",
                    content = @Content(schema = @Schema(implementation = Inventario.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    })
    @PostMapping
    public EntityModel<Inventario> registrarMovimiento(@RequestBody Inventario inventario) {

        Inventario nuevo = inventarioService.save(inventario);

        return assembler.toModel(nuevo);
    }

    @Operation(
            summary = "Actualizar movimiento",
            description = "Actualiza un movimiento existente."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Movimiento actualizado correctamente"),
            @ApiResponse(responseCode = "404", description = "Movimiento no encontrado", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<Inventario>> actualizarMovimiento(
            @Parameter(description = "ID del movimiento", example = "1")
            @PathVariable Long id,
            @RequestBody Inventario inventario) {

        Inventario existente = inventarioService.findById(id);

        if (existente != null) {
            inventario.setId(id);

            Inventario actualizado = inventarioService.save(inventario);

            return ResponseEntity.ok(assembler.toModel(actualizado));
        }

        return ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "Eliminar movimiento",
            description = "Elimina un movimiento de inventario."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Movimiento eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Movimiento no encontrado", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarMovimiento(
            @Parameter(description = "ID del movimiento", example = "1")
            @PathVariable Long id) {

        Inventario inventario = inventarioService.findById(id);

        if (inventario != null) {
            inventarioService.deleteById(id);
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }
}