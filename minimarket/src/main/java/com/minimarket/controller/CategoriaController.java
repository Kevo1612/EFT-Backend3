package com.minimarket.controller;

import com.minimarket.entity.Categoria;
import com.minimarket.hateoas.CategoriaModelAssembler;
import com.minimarket.service.CategoriaService;
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

@Tag(name = "Categorías", description = "Operaciones para la administración de categorías de productos.")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

        @Autowired
        private CategoriaService categoriaService;
        @Autowired
        private CategoriaModelAssembler assembler;

        @Operation(summary = "Listar categorías", description = "Obtiene el listado completo de categorías registradas.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Categorías obtenidas correctamente"),
                        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
                        @ApiResponse(responseCode = "403", description = "Acceso denegado")
        })
        @GetMapping
        @PreAuthorize("hasAnyRole('ADMIN','CAJERO','CLIENTE')")
        public CollectionModel<EntityModel<Categoria>> listarCategorias() {

                List<EntityModel<Categoria>> categorias = categoriaService.findAll()

                                .stream()

                                .map(assembler::toModel)

                                .toList();

                return CollectionModel.of(

                                categorias,

                                linkTo(methodOn(CategoriaController.class)

                                                .listarCategorias())

                                                .withSelfRel()

                );

        }

        @Operation(summary = "Buscar categoría por ID", description = "Obtiene una categoría mediante su identificador.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Categoría encontrada"),
                        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
                        @ApiResponse(responseCode = "403", description = "Acceso denegado"),
                        @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
        })
        @GetMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN','CAJERO','CLIENTE')")
        public ResponseEntity<EntityModel<Categoria>> obtenerCategoriaPorId(
                        @Parameter(description = "ID de la categoría", example = "1") @PathVariable Long id) {

                Categoria categoria = categoriaService.findById(id);

                if (categoria == null) {
                        return ResponseEntity.notFound().build();
                }

                return ResponseEntity.ok(

                                assembler.toModel(categoria)

                );

        }

        @Operation(summary = "Crear categoría", description = "Registra una nueva categoría.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Categoría creada correctamente"),
                        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
                        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
                        @ApiResponse(responseCode = "403", description = "Solo administradores pueden crear categorías")
        })
        @PostMapping
        @PreAuthorize("hasRole('ADMIN')")
        public Categoria guardarCategoria(@RequestBody Categoria categoria) {
                return categoriaService.save(categoria);
        }

        @Operation(summary = "Actualizar categoría", description = "Actualiza la información de una categoría existente.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Categoría actualizada correctamente"),
                        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
                        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
                        @ApiResponse(responseCode = "403", description = "Solo administradores pueden actualizar categorías"),
                        @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
        })
        @PutMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<Categoria> actualizarCategoria(
                        @Parameter(description = "ID de la categoría", example = "1") @PathVariable Long id,
                        @RequestBody Categoria categoria) {

                Categoria categoriaExistente = categoriaService.findById(id);

                if (categoriaExistente != null) {
                        categoria.setId(id);
                        return ResponseEntity.ok(categoriaService.save(categoria));
                }

                return ResponseEntity.notFound().build();
        }

        @Operation(summary = "Eliminar categoría", description = "Elimina una categoría del sistema.")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Categoría eliminada correctamente"),
                        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
                        @ApiResponse(responseCode = "403", description = "Solo administradores pueden eliminar categorías"),
                        @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
        })
        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<Void> eliminarCategoria(
                        @Parameter(description = "ID de la categoría", example = "1") @PathVariable Long id) {

                Categoria categoria = categoriaService.findById(id);

                if (categoria != null) {
                        categoriaService.deleteById(id);
                        return ResponseEntity.noContent().build();
                }

                return ResponseEntity.notFound().build();
        }
}