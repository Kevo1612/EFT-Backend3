package com.minimarket.hateoas;

import com.minimarket.controller.CategoriaController;
import com.minimarket.controller.ProductoController;
import com.minimarket.entity.Categoria;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
public class CategoriaModelAssembler
        implements RepresentationModelAssembler<Categoria, EntityModel<Categoria>> {

    @Override
    public EntityModel<Categoria> toModel(Categoria categoria) {

        return EntityModel.of(

                categoria,

                linkTo(methodOn(CategoriaController.class)
                        .obtenerCategoriaPorId(categoria.getId()))
                        .withSelfRel(),

                linkTo(methodOn(CategoriaController.class)
                        .listarCategorias())
                        .withRel("categorias"),

                linkTo(methodOn(CategoriaController.class)
                        .actualizarCategoria(categoria.getId(), categoria))
                        .withRel("actualizar"),

                linkTo(methodOn(CategoriaController.class)
                        .eliminarCategoria(categoria.getId()))
                        .withRel("eliminar"),
                linkTo(methodOn(ProductoController.class)
                        .listarProductos())
                        .withRel("productos")

        );

    }

}