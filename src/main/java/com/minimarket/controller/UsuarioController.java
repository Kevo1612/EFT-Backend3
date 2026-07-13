package com.minimarket.controller;

import com.minimarket.hateoas.UsuarioModelAssembler;
import com.minimarket.entity.Usuario;
import com.minimarket.service.UsuarioService;
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
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/usuarios")
@Tag(
        name = "Usuarios",
        description = "API para la administración de usuarios y roles"
)
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioModelAssembler assembler;

    @Operation(
            summary = "Listar usuarios",
            description = "Obtiene todos los usuarios registrados."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuarios obtenidos correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content)
    })
    @GetMapping
    public CollectionModel<EntityModel<Usuario>> listarUsuarios() {

        List<EntityModel<Usuario>> usuarios = usuarioService.findAll()
                .stream()
                .map(assembler::toModel)
                .toList();

        return CollectionModel.of(usuarios,
                linkTo(methodOn(UsuarioController.class)
                        .listarUsuarios())
                        .withSelfRel());
    }

    @Operation(
            summary = "Buscar usuario por ID",
            description = "Obtiene un usuario por su ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Usuario>> obtenerUsuarioPorId(
            @Parameter(description = "ID del usuario", example = "1")
            @PathVariable Long id) {

        Optional<Usuario> usuario = usuarioService.findById(id);

        return usuario
                .map(assembler::toModel)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Registrar usuario",
            description = "Registra un nuevo usuario."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario registrado correctamente",
                    content = @Content(schema = @Schema(implementation = Usuario.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    })
    @PostMapping
    public EntityModel<Usuario> guardarUsuario(@RequestBody Usuario usuario) {

        Usuario nuevo = usuarioService.save(usuario);

        return assembler.toModel(nuevo);
    }

    @Operation(
            summary = "Actualizar usuario",
            description = "Actualiza un usuario existente."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario actualizado correctamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<Usuario>> actualizarUsuario(
            @Parameter(description = "ID del usuario", example = "1")
            @PathVariable Long id,
            @RequestBody Usuario usuario) {

        Optional<Usuario> existente = usuarioService.findById(id);

        if (existente.isPresent()) {

            usuario.setId(id);

            Usuario actualizado = usuarioService.save(usuario);

            return ResponseEntity.ok(assembler.toModel(actualizado));
        }

        return ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "Eliminar usuario",
            description = "Elimina un usuario."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuario eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(
            @Parameter(description = "ID del usuario", example = "1")
            @PathVariable Long id) {

        Optional<Usuario> usuario = usuarioService.findById(id);

        if (usuario.isPresent()) {
            usuarioService.deleteById(id);
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }
}