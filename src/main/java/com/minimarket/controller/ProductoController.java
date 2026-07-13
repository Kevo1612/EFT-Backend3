package com.minimarket.controller;

import com.minimarket.hateoas.ProductoModelAssembler;
import com.minimarket.entity.Producto;
import com.minimarket.service.ProductoService;
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
@RequestMapping("/api/productos")
@Tag(
        name = "Productos",
        description = "API para la gestión de productos del Minimarket"
)
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ProductoModelAssembler assembler;

    @Operation(
            summary = "Listar productos",
            description = "Obtiene la lista completa de productos registrados."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Productos obtenidos correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content)
    })
    @GetMapping
    public CollectionModel<EntityModel<Producto>> listarProductos() {

        List<EntityModel<Producto>> productos = productoService.findAll()
                .stream()
                .map(assembler::toModel)
                .toList();

        return CollectionModel.of(productos,
                linkTo(methodOn(ProductoController.class)
                        .listarProductos())
                        .withSelfRel());
    }

    @Operation(
            summary = "Buscar producto por ID",
            description = "Obtiene un producto específico según su identificador."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Producto>> obtenerProductoPorId(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long id) {

        Producto producto = productoService.findById(id);

        if (producto == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(assembler.toModel(producto));
    }

    @Operation(
            summary = "Registrar producto",
            description = "Crea un nuevo producto en el sistema."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Producto registrado correctamente",
                    content = @Content(schema = @Schema(implementation = Producto.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    })
    @PostMapping
    public EntityModel<Producto> guardarProducto(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos del nuevo producto",
                    required = true)
            @RequestBody Producto producto) {

        Producto nuevo = productoService.save(producto);

        return assembler.toModel(nuevo);
    }

    @Operation(
            summary = "Actualizar producto",
            description = "Actualiza la información de un producto existente."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Producto actualizado correctamente"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<Producto>> actualizarProducto(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long id,
            @RequestBody Producto producto) {

        Producto productoExistente = productoService.findById(id);

        if (productoExistente != null) {
            producto.setId(id);
            Producto actualizado = productoService.save(producto);
            return ResponseEntity.ok(assembler.toModel(actualizado));
        }

        return ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "Eliminar producto",
            description = "Elimina un producto registrado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Producto eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long id) {

        Producto producto = productoService.findById(id);

        if (producto != null) {
            productoService.deleteById(id);
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }
}