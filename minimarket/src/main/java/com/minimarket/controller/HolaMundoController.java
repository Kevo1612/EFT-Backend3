package com.minimarket.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Pruebas", description = "Endpoints públicos para verificar el funcionamiento del sistema.")
public class HolaMundoController {

    
    @GetMapping("/public/hola")
    public String holaMundo() {
        return "¡Hola Mundo!";
    }
}
