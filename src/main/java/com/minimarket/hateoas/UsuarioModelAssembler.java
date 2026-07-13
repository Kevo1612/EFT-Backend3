package com.minimarket.hateoas;

import com.minimarket.controller.UsuarioController;
import com.minimarket.entity.Usuario;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
public class UsuarioModelAssembler implements RepresentationModelAssembler<Usuario, EntityModel<Usuario>> {

    @Override
    public EntityModel<Usuario> toModel(Usuario usuario) {

        return EntityModel.of(usuario,

                linkTo(methodOn(UsuarioController.class)
                        .obtenerUsuarioPorId(usuario.getId()))
                        .withSelfRel(),

                linkTo(methodOn(UsuarioController.class)
                        .listarUsuarios())
                        .withRel("usuarios"),

                linkTo(methodOn(UsuarioController.class)
                        .actualizarUsuario(usuario.getId(), usuario))
                        .withRel("actualizar"),

                linkTo(methodOn(UsuarioController.class)
                        .eliminarUsuario(usuario.getId()))
                        .withRel("eliminar"));
    }
}