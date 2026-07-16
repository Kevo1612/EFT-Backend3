package com.minimarket.controller;

import com.minimarket.entity.Producto;
import com.minimarket.hateoas.ProductoModelAssembler;
import com.minimarket.service.ProductoService;
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
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
import java.util.List;

@Tag(name = "Productos", description = "Operaciones para la administración de productos del sistema MiniMarket Plus.")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

        @Autowired
        private ProductoService productoService;
        @Autowired
        private ProductoModelAssembler assembler;

        @Operation(summary = "Listar productos", description = "Obtiene el listado completo de productos registrados.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Productos obtenidos correctamente"),
                        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
                        @ApiResponse(responseCode = "403", description = "Acceso denegado")
        })
        @GetMapping
        @PreAuthorize("hasAnyRole('ADMIN','CAJERO','CLIENTE')")
        public CollectionModel<EntityModel<Producto>> listarProductos() {

                List<EntityModel<Producto>> productos = productoService.findAll()
                                .stream()
                                .map(assembler::toModel)
                                .toList();

                return CollectionModel.of(
                                productos,
                                linkTo(methodOn(ProductoController.class)
                                                .listarProductos())
                                                .withSelfRel());
        }

        @Operation(summary = "Buscar producto por ID", description = "Obtiene la información de un producto específico.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Producto encontrado"),
                        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
                        @ApiResponse(responseCode = "403", description = "Acceso denegado"),
                        @ApiResponse(responseCode = "404", description = "Producto no encontrado")
        })
        @GetMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN','CAJERO','CLIENTE')")
        public ResponseEntity<EntityModel<Producto>> obtenerProductoPorId(
                        @Parameter(description = "ID del producto", example = "1") @PathVariable Long id) {

                Producto producto = productoService.findById(id);

                if (producto == null) {
                        return ResponseEntity.notFound().build();
                }

                return ResponseEntity.ok(
                                assembler.toModel(producto));
        }

        @Operation(summary = "Crear producto", description = "Registra un nuevo producto en el sistema.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Producto registrado correctamente"),
                        @ApiResponse(responseCode = "400", description = "Datos del producto inválidos"),
                        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
                        @ApiResponse(responseCode = "403", description = "Solo administradores pueden crear productos")
        })
        @PostMapping
        @PreAuthorize("hasRole('ADMIN')")
        public Producto guardarProducto(@RequestBody Producto producto) {
                return productoService.save(producto);
        }

        @Operation(summary = "Actualizar producto", description = "Modifica la información de un producto existente.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Producto actualizado correctamente"),
                        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
                        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
                        @ApiResponse(responseCode = "403", description = "Solo administradores pueden actualizar productos"),
                        @ApiResponse(responseCode = "404", description = "Producto no encontrado")
        })
        @PutMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<Producto> actualizarProducto(
                        @Parameter(description = "ID del producto", example = "1") @PathVariable Long id,
                        @RequestBody Producto producto) {

                Producto productoExistente = productoService.findById(id);

                if (productoExistente != null) {
                        producto.setId(id);
                        return ResponseEntity.ok(productoService.save(producto));
                }

                return ResponseEntity.notFound().build();
        }

        @Operation(summary = "Eliminar producto", description = "Elimina un producto del sistema.")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Producto eliminado correctamente"),
                        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
                        @ApiResponse(responseCode = "403", description = "Solo administradores pueden eliminar productos"),
                        @ApiResponse(responseCode = "404", description = "Producto no encontrado")
        })
        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<Void> eliminarProducto(
                        @Parameter(description = "ID del producto", example = "1") @PathVariable Long id) {

                Producto producto = productoService.findById(id);

                if (producto != null) {
                        productoService.deleteById(id);
                        return ResponseEntity.noContent().build();
                }

                return ResponseEntity.notFound().build();
        }
}