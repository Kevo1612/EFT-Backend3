package com.minimarket.hateoas;


import com.minimarket.controller.InventarioController;
import com.minimarket.entity.Inventario;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
public class InventarioModelAssembler implements RepresentationModelAssembler<Inventario, EntityModel<Inventario>> {

    @Override
    public EntityModel<Inventario> toModel(Inventario inventario) {

        return EntityModel.of(inventario,

                linkTo(methodOn(InventarioController.class)
                        .obtenerMovimientoPorId(inventario.getId()))
                        .withSelfRel(),

                linkTo(methodOn(InventarioController.class)
                        .listarMovimientosDeInventario())
                        .withRel("inventario"),

                linkTo(methodOn(InventarioController.class)
                        .actualizarMovimiento(inventario.getId(), inventario))
                        .withRel("actualizar"),

                linkTo(methodOn(InventarioController.class)
                        .eliminarMovimiento(inventario.getId()))
                        .withRel("eliminar"));
    }
}