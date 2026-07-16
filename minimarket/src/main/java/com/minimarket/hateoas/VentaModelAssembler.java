package com.minimarket.hateoas;

import com.minimarket.controller.VentaController;
import com.minimarket.entity.Venta;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
public class VentaModelAssembler
        implements RepresentationModelAssembler<Venta, EntityModel<Venta>> {

    @Override
    public EntityModel<Venta> toModel(Venta venta) {

        return EntityModel.of(

                venta,

                linkTo(methodOn(VentaController.class)
                        .obtenerVentaPorId(venta.getId()))
                        .withSelfRel(),

                linkTo(methodOn(VentaController.class)
                        .listarVentas())
                        .withRel("ventas"),

                linkTo(methodOn(VentaController.class)
                        .guardarVenta(venta))
                        .withRel("crear")

        );

    }

}