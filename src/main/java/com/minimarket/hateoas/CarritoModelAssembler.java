package com.minimarket.hateoas;

import com.minimarket.controller.CarritoController;
import com.minimarket.entity.Carrito;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
public class CarritoModelAssembler implements RepresentationModelAssembler<Carrito, EntityModel<Carrito>> {

    @Override
    public EntityModel<Carrito> toModel(Carrito carrito) {

        return EntityModel.of(carrito,

                linkTo(methodOn(CarritoController.class)
                        .obtenerCarritoPorId(carrito.getId()))
                        .withSelfRel(),

                linkTo(methodOn(CarritoController.class)
                        .listarCarrito())
                        .withRel("carrito"),

                linkTo(methodOn(CarritoController.class)
                        .actualizarCarrito(carrito.getId(), carrito))
                        .withRel("actualizar"),

                linkTo(methodOn(CarritoController.class)
                        .eliminarProductoDelCarrito(carrito.getId()))
                        .withRel("eliminar"));
    }
}