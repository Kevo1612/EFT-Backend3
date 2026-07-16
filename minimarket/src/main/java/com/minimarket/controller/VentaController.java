package com.minimarket.controller;

import com.minimarket.entity.Venta;
import com.minimarket.hateoas.VentaModelAssembler;
import com.minimarket.service.VentaService;
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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import java.util.List;

@Tag(name = "Ventas", description = "Operaciones para el registro y consulta de ventas realizadas en el sistema.")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/ventas")
public class VentaController {

        @Autowired
        private VentaService ventaService;
        @Autowired
        private VentaModelAssembler assembler;

        @Operation(summary = "Listar ventas", description = "Obtiene el listado completo de ventas registradas.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Ventas obtenidas correctamente"),
                        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
                        @ApiResponse(responseCode = "403", description = "Acceso denegado")
        })
        @GetMapping
        @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
        public CollectionModel<EntityModel<Venta>> listarVentas() {

                List<EntityModel<Venta>> ventas = ventaService.findAll()
                                .stream()
                                .map(assembler::toModel)
                                .toList();

                return CollectionModel.of(
                                ventas,
                                linkTo(methodOn(VentaController.class)
                                                .listarVentas())
                                                .withSelfRel());
        }

        @Operation(summary = "Buscar venta por ID", description = "Obtiene la información de una venta específica mediante su identificador.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Venta encontrada"),
                        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
                        @ApiResponse(responseCode = "403", description = "Acceso denegado"),
                        @ApiResponse(responseCode = "404", description = "Venta no encontrada")
        })
        @GetMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
        public ResponseEntity<EntityModel<Venta>> obtenerVentaPorId(
                        @Parameter(description = "ID de la venta", example = "1") @PathVariable Long id) {

                Venta venta = ventaService.findById(id);

                if (venta == null) {
                        return ResponseEntity.notFound().build();
                }

                return ResponseEntity.ok(
                                assembler.toModel(venta));
        }

        @Operation(summary = "Registrar venta", description = "Registra una nueva venta en el sistema.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Venta registrada correctamente"),
                        @ApiResponse(responseCode = "400", description = "Datos de la venta inválidos"),
                        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
                        @ApiResponse(responseCode = "403", description = "Solo administradores y cajeros pueden registrar ventas")
        })
        @PostMapping
        @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
        public Venta guardarVenta(@RequestBody Venta venta) {
                return ventaService.save(venta);
        }
}