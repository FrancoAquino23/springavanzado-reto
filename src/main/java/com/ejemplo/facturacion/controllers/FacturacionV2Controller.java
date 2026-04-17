package com.ejemplo.facturacion.controllers;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ejemplo.facturacion.services.FacturaService;
import com.ejemplo.facturacion.valueobjects.Factura;
import com.ejemplo.facturacion.valueobjects.Orden;

// Controlador REST para la gestión de facturas en la versión 2 de la API
@RestController
public class FacturacionV2Controller {
    @Autowired FacturaService facturaService;

    // Endpoint para iniciar el proceso de cálculo de una factura de forma asíncrona, devolviendo un ID de factura y la ubicación para consultar el resultado
    @PostMapping("/v2/factura")
    public ResponseEntity<String> calcularFactura(@RequestBody Orden orden) throws InterruptedException {
        String idFactura = facturaService.iniciarFacturaAsincrona(orden);
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(HttpHeaders.LOCATION, "/v2/factura/" + idFactura);
        return new ResponseEntity<>(idFactura, headers, HttpStatus.ACCEPTED);
    }

    // Endpoint para consultar el resultado del cálculo de una factura utilizando su ID, devolviendo la factura si está disponible, un estado de no contenido si el cálculo aún no ha finalizado o un estado de no encontrado si el ID no existe
    @GetMapping("/v2/factura/{idFactura}")
    public ResponseEntity<Factura> buscarFactura(@PathVariable String idFactura) {
        Optional<Factura> resultado = facturaService.obtenerFacturaAsincrona(idFactura);
        if (resultado == null) {
            return ResponseEntity.notFound().build();
        }
        if (resultado.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(resultado.get());
    }
}
