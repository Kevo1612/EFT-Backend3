package com.minimarket.controller;

import com.minimarket.hateoas.CarritoModelAssembler;
import com.minimarket.entity.Carrito;
import com.minimarket.service.CarritoService;
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
@RequestMapping("/api/carrito")
@Tag(
        name = "Carrito",
        description = "API para la gestión del carrito de compras"
)
public class CarritoController {

    @Autowired
    private CarritoService carritoService;

    @Autowired
    private CarritoModelAssembler assembler;

    @Operation(
            summary = "Listar carrito",
            description = "Obtiene todos los productos agregados al carrito."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carrito obtenido correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content)
    })
    @GetMapping
    public CollectionModel<EntityModel<Carrito>> listarCarrito() {

        List<EntityModel<Carrito>> carrito = carritoService.findAll()
                .stream()
                .map(assembler::toModel)
                .toList();

        return CollectionModel.of(carrito,
                linkTo(methodOn(CarritoController.class)
                        .listarCarrito())
                        .withSelfRel());
    }

    @Operation(
            summary = "Buscar elemento del carrito por ID",
            description = "Obtiene un elemento del carrito según su identificador."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Elemento encontrado"),
            @ApiResponse(responseCode = "404", description = "Elemento no encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Carrito>> obtenerCarritoPorId(
            @Parameter(description = "ID del carrito", example = "1")
            @PathVariable Long id) {

        Carrito carrito = carritoService.findById(id);

        if (carrito == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(assembler.toModel(carrito));
    }

    @Operation(
            summary = "Agregar producto al carrito",
            description = "Agrega un nuevo producto al carrito."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Producto agregado correctamente",
                    content = @Content(schema = @Schema(implementation = Carrito.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    })
    @PostMapping
    public EntityModel<Carrito> agregarProductoAlCarrito(
            @RequestBody Carrito carrito) {

        Carrito nuevo = carritoService.save(carrito);

        return assembler.toModel(nuevo);
    }

    @Operation(
            summary = "Actualizar carrito",
            description = "Actualiza un elemento del carrito."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carrito actualizado correctamente"),
            @ApiResponse(responseCode = "404", description = "Elemento no encontrado", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<Carrito>> actualizarCarrito(
            @Parameter(description = "ID del carrito", example = "1")
            @PathVariable Long id,
            @RequestBody Carrito carrito) {

        Carrito existente = carritoService.findById(id);

        if (existente != null) {
            carrito.setId(id);

            Carrito actualizado = carritoService.save(carrito);

            return ResponseEntity.ok(
                    assembler.toModel(actualizado));
        }

        return ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "Eliminar producto del carrito",
            description = "Elimina un producto del carrito."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Producto eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Elemento no encontrado", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProductoDelCarrito(
            @Parameter(description = "ID del carrito", example = "1")
            @PathVariable Long id) {

        Carrito carrito = carritoService.findById(id);

        if (carrito != null) {
            carritoService.deleteById(id);
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }
}