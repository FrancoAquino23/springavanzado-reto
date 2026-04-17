package com.ejemplo.facturacion.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.ejemplo.facturacion.valueobjects.Articulo;
import com.ejemplo.facturacion.valueobjects.Factura;
import com.ejemplo.facturacion.valueobjects.Orden;

@Service
public class FacturaService {

    // Mapa compartido: mismo bean, misma instancia, sin proxies intermedios
    private final Map<String, Optional<Factura>> facturas = new ConcurrentHashMap<>();

    // Método síncrono para generar una factura a partir de una orden, bloqueando el hilo durante el proceso de cálculo 
    public Factura generarFactura(final Orden orden) throws InterruptedException {
        String idFactura = generarIdFactura();
        facturas.put(idFactura, Optional.empty());

        Thread.sleep(5000);

        Factura factura = construirFactura(idFactura, orden);
        facturas.put(idFactura, Optional.of(factura));
        return factura;
    }

    // Método asíncrono para iniciar el proceso de cálculo de una factura a partir de una orden, devolviendo inmediatamente un ID de factura y permitiendo consultar el resultado posteriormente
    public String iniciarFacturaAsincrona(final Orden orden) throws InterruptedException {
        String idFactura = generarIdFactura();

        // Registrar como PENDIENTE antes de lanzar el hilo
        facturas.put(idFactura, Optional.empty());

        // Lanzar el calculo en un hilo separado del pool comun
        CompletableFuture.runAsync(() -> {
            try {
                crearFacturaAsincrona(idFactura, orden);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Devolver el ID al instante (-> 202 Accepted)
        return idFactura;
    }

    // Método para consultar el estado de una factura asíncrona, devolviendo null si el ID no existe, empty si todavía se está procesando, o la factura completa si ya se calculó
    public Optional<Factura> obtenerFacturaAsincrona(final String idFactura) {
        // null  -> ID no existe en el mapa              -> 404 Not Found
        // empty -> procesando todavia                   -> 204 No Content
        // of(f) -> calculo completado                   -> 200 OK con factura
        if (!facturas.containsKey(idFactura)) {
            return null;
        }
        return facturas.get(idFactura);
    }

    // Método privado para simular el proceso de cálculo de una factura, bloqueando el hilo durante el proceso y actualizando el mapa compartido al finalizar
    public void crearFacturaAsincrona(final String idFactura, final Orden orden)
            throws InterruptedException {
        Thread.sleep(5000);
        Factura factura = construirFactura(idFactura, orden);
        facturas.put(idFactura, Optional.of(factura));
    }

    // Método privado para construir una factura a partir de una orden, calculando el subtotal, IVA y total, y asignando un ID único
    private Factura construirFactura(String idFactura, Orden orden) {
        BigDecimal subtotal = calcularSubtotal(orden.getArticulos());
        BigDecimal iva   = subtotal.multiply(BigDecimal.valueOf(0.16))
                                   .setScale(2, RoundingMode.UP);
        BigDecimal total = subtotal.add(iva);

        Factura factura = new Factura();
        factura.setId(idFactura);
        factura.setArticulos(orden.getArticulos());
        factura.setSubtotal(subtotal);
        factura.setIva(iva);
        factura.setTotal(total);
        return factura;
    }

    // Método privado para calcular el subtotal de una lista de artículos, multiplicando el precio unitario por la cantidad de cada artículo y sumando los resultados
    private BigDecimal calcularSubtotal(List<Articulo> articulos) {
        BigDecimal subtotal = BigDecimal.ZERO;
        if (articulos != null) {
            for (final Articulo articulo : articulos) {
                subtotal = subtotal.add(
                        articulo.getPrecioUnitario()
                                .multiply(BigDecimal.valueOf(articulo.getCantidad())));
            }
        }
        return subtotal;
    }

    // Método privado para generar un ID de factura único basado en el timestamp actual, garantizando que cada factura tenga un identificador distinto
    private String generarIdFactura() {
        return String.valueOf(Instant.now().toEpochMilli());
    }

    // Métodos getter y setter para el mapa de facturas, permitiendo acceder al estado actual de las facturas y modificarlo (usado principalmente en tests para inyectar estado previo)
    public Map<String, Optional<Factura>> getFacturas() {
        return facturas;
    }

    // Método setter para inyectar un mapa de facturas, utilizado principalmente en tests para establecer un estado previo antes de ejecutar las pruebas
    public void setFacturas(Map<String, Optional<Factura>> facturas) {
        this.facturas.clear();
        this.facturas.putAll(facturas);
    }
}
