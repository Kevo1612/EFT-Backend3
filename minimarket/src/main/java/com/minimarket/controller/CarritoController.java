package com.minimarket.controller;

import com.minimarket.entity.Carrito;
import com.minimarket.service.CarritoService;
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
        name = "Carrito",
        description = "Operaciones para la gestión del carrito de compras de los clientes."
)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/carrito")
public class CarritoController {

    @Autowired
    private CarritoService carritoService;

    @Operation(
            summary = "Listar carrito",
            description = "Obtiene todos los productos agregados al carrito de compras."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carrito obtenido correctamente"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CLIENTE')")
    public List<Carrito> listarCarrito() {
        return carritoService.findAll();
    }

    @Operation(
            summary = "Buscar elemento del carrito",
            description = "Obtiene un elemento específico del carrito mediante su identificador."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Elemento encontrado"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Elemento no encontrado")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENTE')")
    public ResponseEntity<Carrito> obtenerCarritoPorId(
            @Parameter(description = "ID del elemento del carrito", example = "1")
            @PathVariable Long id) {

        Carrito carrito = carritoService.findById(id);

        return (carrito != null)
                ? ResponseEntity.ok(carrito)
                : ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "Agregar producto al carrito",
            description = "Agrega un nuevo producto al carrito de compras."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto agregado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CLIENTE')")
    public Carrito agregarProductoAlCarrito(@RequestBody Carrito carrito) {
        return carritoService.save(carrito);
    }

    @Operation(
            summary = "Actualizar carrito",
            description = "Actualiza la información de un producto del carrito."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carrito actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Elemento no encontrado")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENTE')")
    public ResponseEntity<Carrito> actualizarCarrito(
            @Parameter(description = "ID del elemento del carrito", example = "1")
            @PathVariable Long id,
            @RequestBody Carrito carrito) {

        Carrito existente = carritoService.findById(id);

        if (existente != null) {
            carrito.setId(id);
            return ResponseEntity.ok(carritoService.save(carrito));
        }

        return ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "Eliminar producto del carrito",
            description = "Elimina un producto del carrito de compras."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Producto eliminado correctamente"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Elemento no encontrado")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENTE')")
    public ResponseEntity<Void> eliminarProductoDelCarrito(
            @Parameter(description = "ID del elemento del carrito", example = "1")
            @PathVariable Long id) {

        Carrito carrito = carritoService.findById(id);

        if (carrito != null) {
            carritoService.deleteById(id);
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }
}