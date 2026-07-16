package com.minimarket.controller;

import com.minimarket.entity.Usuario;
import com.minimarket.hateoas.UsuarioModelAssembler;
import com.minimarket.service.UsuarioService;
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
import java.util.Optional;

@Tag(name = "Usuarios", description = "Operaciones para la administración de usuarios y asignación de roles.")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

        @Autowired
        private UsuarioService usuarioService;
        @Autowired
        private UsuarioModelAssembler assembler;

        @Operation(summary = "Listar usuarios", description = "Obtiene el listado completo de usuarios registrados en el sistema.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Usuarios obtenidos correctamente"),
                        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
                        @ApiResponse(responseCode = "403", description = "Solo administradores pueden consultar usuarios")
        })
        @GetMapping
        @PreAuthorize("hasRole('ADMIN')")
        public CollectionModel<EntityModel<Usuario>> listarUsuarios() {
                List<EntityModel<Usuario>> usuarios = usuarioService.findAll()
                                .stream()
                                .map(assembler::toModel)
                                .toList();

                return CollectionModel.of(
                                usuarios,
                                linkTo(methodOn(UsuarioController.class)
                                                .listarUsuarios())
                                                .withSelfRel());
        }

        @Operation(summary = "Buscar usuario por ID", description = "Obtiene la información de un usuario mediante su identificador.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
                        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
                        @ApiResponse(responseCode = "403", description = "Solo administradores pueden consultar usuarios"),
                        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
        })
        @GetMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<EntityModel<Usuario>> obtenerUsuarioPorId(
                        @Parameter(description = "ID del usuario", example = "1") @PathVariable Long id) {

                Optional<Usuario> usuario = usuarioService.findById(id);

                if (usuario.isEmpty()) {
                        return ResponseEntity.notFound().build();
                }

                return ResponseEntity.ok(
                                assembler.toModel(usuario.get()));
        }

        @Operation(summary = "Crear usuario", description = "Registra un nuevo usuario en el sistema.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Usuario registrado correctamente"),
                        @ApiResponse(responseCode = "400", description = "Datos del usuario inválidos"),
                        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
                        @ApiResponse(responseCode = "403", description = "Solo administradores pueden crear usuarios")
        })
        @PostMapping
        @PreAuthorize("hasRole('ADMIN')")
        public Usuario guardarUsuario(@RequestBody Usuario usuario) {
                return usuarioService.save(usuario);
        }

        @Operation(summary = "Actualizar usuario", description = "Actualiza la información de un usuario existente.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Usuario actualizado correctamente"),
                        @ApiResponse(responseCode = "400", description = "Datos del usuario inválidos"),
                        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
                        @ApiResponse(responseCode = "403", description = "Solo administradores pueden actualizar usuarios"),
                        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
        })
        @PutMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<Usuario> actualizarUsuario(
                        @Parameter(description = "ID del usuario", example = "1") @PathVariable Long id,
                        @RequestBody Usuario usuario) {

                Optional<Usuario> usuarioExistente = usuarioService.findById(id);

                if (usuarioExistente.isPresent()) {
                        usuario.setId(id);
                        return ResponseEntity.ok(usuarioService.save(usuario));
                }

                return ResponseEntity.notFound().build();
        }

        @Operation(summary = "Eliminar usuario", description = "Elimina un usuario del sistema.")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Usuario eliminado correctamente"),
                        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
                        @ApiResponse(responseCode = "403", description = "Solo administradores pueden eliminar usuarios"),
                        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
        })
        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<Void> eliminarUsuario(
                        @Parameter(description = "ID del usuario", example = "1") @PathVariable Long id) {

                Optional<Usuario> usuario = usuarioService.findById(id);

                if (usuario.isPresent()) {
                        usuarioService.deleteById(id);
                        return ResponseEntity.noContent().build();
                }

                return ResponseEntity.notFound().build();
        }
}