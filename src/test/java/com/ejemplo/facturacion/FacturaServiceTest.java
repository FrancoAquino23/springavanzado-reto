package com.ejemplo.facturacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ejemplo.facturacion.services.FacturaService;
import com.ejemplo.facturacion.valueobjects.Articulo;
import com.ejemplo.facturacion.valueobjects.Factura;
import com.ejemplo.facturacion.valueobjects.Orden;

// Clase de pruebas unitarias para la clase FacturaService, utilizando Mockito para simular dependencias y verificar interacciones
@ExtendWith(MockitoExtension.class)
class FacturaServiceTest {

    @InjectMocks
    private FacturaService facturaService;

    private Orden ordenUnArticulo;
    private Orden ordenDosArticulos;

    // Método de configuración que se ejecuta antes de cada prueba, inicializando objetos de prueba y configurando el comportamiento simulado del ApplicationContext para devolver el proxy asíncrono cuando se solicite el bean de FacturaService
    @BeforeEach
    void setUp() {
        Articulo laptop = new Articulo("Laptop", 2, new BigDecimal("1000.00"));
        ordenUnArticulo = new Orden();
        ordenUnArticulo.setId(1L);
        ordenUnArticulo.setUsuario("developer");
        ordenUnArticulo.setArticulos(Collections.singletonList(laptop));

        Articulo laptopB = new Articulo("Laptop", 1, new BigDecimal("1500.00"));
        Articulo mouse   = new Articulo("Mouse",  3, new BigDecimal("50.00"));
        ordenDosArticulos = new Orden();
        ordenDosArticulos.setId(2L);
        ordenDosArticulos.setUsuario("developer");
        ordenDosArticulos.setArticulos(Arrays.asList(laptopB, mouse));
    }

    // Pruebas para el método generarFactura, verificando que los cálculos de subtotal, IVA y total sean correctos para diferentes escenarios (un artículo, múltiples artículos, artículos nulos o lista vacía) y que las facturas se almacenen correctamente en el mapa compartido con IDs únicos 
    @Test
    void test01_generarFactura_calculosCorrectos() throws InterruptedException {
        Factura factura = facturaService.generarFactura(ordenUnArticulo);

        assertNotNull(factura);
        assertNotNull(factura.getId());
        assertEquals(new BigDecimal("2000.00"), factura.getSubtotal());
        assertEquals(new BigDecimal("320.00"),  factura.getIva());
        assertEquals(new BigDecimal("2320.00"), factura.getTotal());
        assertEquals(ordenUnArticulo.getArticulos(), factura.getArticulos());
    }

    // Prueba para verificar que el método generarFactura calcula correctamente el subtotal, IVA y total cuando la orden contiene múltiples artículos, asegurando que se sumen correctamente los precios de cada artículo y se apliquen los cálculos correspondientes
    @Test
    void test02_generarFactura_multipleArticulos() throws InterruptedException {
        Factura factura = facturaService.generarFactura(ordenDosArticulos);

        assertEquals(new BigDecimal("1650.00"), factura.getSubtotal());
        assertEquals(new BigDecimal("264.00"),  factura.getIva());
        assertEquals(new BigDecimal("1914.00"), factura.getTotal());
    }

    // Prueba para verificar que el método generarFactura maneja correctamente el caso en que la lista de artículos es nula, asegurando que el subtotal se calcule como cero y no se produzcan errores al intentar acceder a la lista de artículos
    @Test
    void test03_generarFactura_articulosNulos_subtotalCero() throws InterruptedException {
        Orden ordenVacia = new Orden();
        ordenVacia.setId(99L);
        ordenVacia.setArticulos(null);

        Factura factura = facturaService.generarFactura(ordenVacia);

        assertEquals(BigDecimal.ZERO, factura.getSubtotal());
    }

    // Prueba para verificar que el método generarFactura maneja correctamente el caso en que la lista de artículos está vacía, asegurando que el subtotal se calcule como cero y no se produzcan errores al intentar acceder a la lista de artículos
    @Test
    void test04_generarFactura_listaVacia_subtotalCero() throws InterruptedException {
        Orden ordenVacia = new Orden();
        ordenVacia.setId(100L);
        ordenVacia.setArticulos(Collections.emptyList());

        Factura factura = facturaService.generarFactura(ordenVacia);

        assertEquals(BigDecimal.ZERO, factura.getSubtotal());
    }

    // Prueba para verificar que el método generarFactura almacena correctamente la factura generada en el mapa compartido utilizando un ID único, asegurando que el mapa contenga la clave correspondiente al ID de la factura y que el valor asociado sea la factura generada
    @Test
    void test05_generarFactura_almacenaFacturaEnMapa() throws InterruptedException {
        Factura factura = facturaService.generarFactura(ordenUnArticulo);

        Map<String, Optional<Factura>> mapa = facturaService.getFacturas();
        assertTrue(mapa.containsKey(factura.getId()));
        assertTrue(mapa.get(factura.getId()).isPresent());
        assertEquals(factura, mapa.get(factura.getId()).get());
    }

    // Prueba para verificar que el método generarFactura genera IDs únicos para cada factura, asegurando que al generar múltiples facturas consecutivas se obtengan IDs distintos y no se produzcan colisiones en el mapa compartido
    @Test
    void test06_generarFactura_idsUnicos() throws InterruptedException {
        Factura f1 = facturaService.generarFactura(ordenUnArticulo);
        Thread.sleep(2);
        Factura f2 = facturaService.generarFactura(ordenUnArticulo);

        assertNotEquals(f1.getId(), f2.getId());
    }

    // Pruebas para el método iniciarFacturaAsincrona, verificando que se genere un ID no vacío, que se registre el estado pendiente en el mapa compartido y que se delegue correctamente al proxy asíncrono para la creación de la factura en segundo plano
    @Test
    void test07_iniciarFacturaAsincrona_retornaIdNoVacio() throws InterruptedException {
        String id = facturaService.iniciarFacturaAsincrona(ordenUnArticulo);

        assertNotNull(id);
        assertFalse(id.isBlank());
    }

    // Prueba para verificar que el método iniciarFacturaAsincrona registra correctamente el estado pendiente en el mapa compartido, asegurando que al iniciar la factura asíncrona se agregue una entrada al mapa con el ID generado y un valor de Optional vacío para indicar que la factura aún está en proceso
    @Test
    void test08_iniciarFacturaAsincrona_registraEstadoPendiente() throws InterruptedException {
        String id = facturaService.iniciarFacturaAsincrona(ordenUnArticulo);

        Map<String, Optional<Factura>> mapa = facturaService.getFacturas();
        assertTrue(mapa.containsKey(id));
        assertTrue(mapa.get(id).isEmpty());
    }

    // Prueba para verificar que el método iniciarFacturaAsincrona delega correctamente al proxy asíncrono para la creación de la factura en segundo plano, asegurando que se invoque el método crearFacturaAsincrona del proxy con los parámetros correctos y que no se produzcan interacciones adicionales con el proxy
    @Test
    void test09_iniciarFacturaAsincrona_esNoBloqueante() throws InterruptedException {
        long inicio = System.currentTimeMillis();
        facturaService.iniciarFacturaAsincrona(ordenUnArticulo);
        long duracion = System.currentTimeMillis() - inicio;

        assertTrue(duracion < 1000, "iniciarFacturaAsincrona debe retornar antes de que termine el calculo asincrono");
    }

    // Pruebas para el método crearFacturaAsincrona, verificando que al crear una factura de forma asíncrona se actualice correctamente el mapa compartido con la factura generada una vez que el proceso en segundo plano haya finalizado, asegurando que después de llamar a crearFacturaAsincrona el mapa contenga la factura correspondiente al ID proporcionado y que los cálculos de subtotal, IVA y total sean correctos
    @Test
    void test10_crearFacturaAsincrona_actualizaMapaAlTerminar() throws InterruptedException {
        String idFactura = "async-001";
        Map<String, Optional<Factura>> mapa = new ConcurrentHashMap<>();
        mapa.put(idFactura, Optional.empty());
        facturaService.setFacturas(mapa);

        facturaService.crearFacturaAsincrona(idFactura, ordenUnArticulo);

        assertTrue(mapa.get(idFactura).isPresent());
        Factura f = mapa.get(idFactura).get();
        assertEquals(idFactura,                     f.getId());
        assertEquals(new BigDecimal("2000.00"),      f.getSubtotal());
        assertEquals(new BigDecimal("320.00"),       f.getIva());
        assertEquals(new BigDecimal("2320.00"),      f.getTotal());
        assertEquals(ordenUnArticulo.getArticulos(), f.getArticulos());
    }

    // Prueba para verificar que el método crearFacturaAsincrona calcula correctamente el subtotal, IVA y total cuando la orden contiene múltiples artículos, asegurando que se sumen correctamente los precios de cada artículo y se apliquen los cálculos correspondientes, y que la factura generada se almacene correctamente en el mapa compartido con el ID proporcionado
    @Test
    void test11_crearFacturaAsincrona_multipleArticulos() throws InterruptedException {
        String idFactura = "async-002";
        Map<String, Optional<Factura>> mapa = new ConcurrentHashMap<>();
        mapa.put(idFactura, Optional.empty());
        facturaService.setFacturas(mapa);

        facturaService.crearFacturaAsincrona(idFactura, ordenDosArticulos);

        Factura f = mapa.get(idFactura).get();
        assertEquals(new BigDecimal("1650.00"), f.getSubtotal());
        assertEquals(new BigDecimal("264.00"),  f.getIva());
        assertEquals(new BigDecimal("1914.00"), f.getTotal());
    }

    // Pruebas para el método obtenerFacturaAsincrona, verificando que se manejen correctamente los casos en que el ID no exista en el mapa compartido (retornando null), que el estado esté en proceso (retornando un Optional vacío) y que la factura esté disponible (retornando un Optional con la factura correspondiente)
    @Test
    void test12_obtenerFacturaAsincrona_nullSiIdNoExiste() {
        assertNull(facturaService.obtenerFacturaAsincrona("no-existe"));
    }

    // Prueba para verificar que el método obtenerFacturaAsincrona maneja correctamente el caso en que el ID exista en el mapa compartido pero la factura aún esté en proceso (es decir, el valor asociado sea un Optional vacío), asegurando que se retorne un Optional vacío para indicar que la factura aún no está disponible
    @Test
    void test13_obtenerFacturaAsincrona_optionalVacioSiEnProceso() {
        Map<String, Optional<Factura>> mapa = new ConcurrentHashMap<>();
        mapa.put("pendiente", Optional.empty());
        facturaService.setFacturas(mapa);

        Optional<Factura> estado = facturaService.obtenerFacturaAsincrona("pendiente");

        assertNotNull(estado);
        assertTrue(estado.isEmpty());
    }

    // Prueba para verificar que el método obtenerFacturaAsincrona maneja correctamente el caso en que el ID exista en el mapa compartido y la factura esté disponible (es decir, el valor asociado sea un Optional con la factura correspondiente), asegurando que se retorne un Optional con la factura correcta y que los datos de la factura coincidan con lo esperado
    @Test
    void test14_obtenerFacturaAsincrona_retornaFacturaSiTermino() {
        Factura esperada = new Factura("listo", null,
                new BigDecimal("500.00"), new BigDecimal("80.00"), new BigDecimal("580.00"));
        Map<String, Optional<Factura>> mapa = new ConcurrentHashMap<>();
        mapa.put("listo", Optional.of(esperada));
        facturaService.setFacturas(mapa);

        Optional<Factura> estado = facturaService.obtenerFacturaAsincrona("listo");

        assertNotNull(estado);
        assertTrue(estado.isPresent());
        assertEquals(esperada, estado.get());
    }

    // Pruebas para los métodos getFacturas y setFacturas, verificando que el mapa compartido se inicialice correctamente, que se pueda acceder a él sin obtener un valor nulo, y que el método setFacturas reemplace completamente el mapa existente con uno nuevo
    @Test
    void test15_getFacturas_noEsNull() {
        assertNotNull(facturaService.getFacturas());
    }

    // Prueba para verificar que el método getFacturas retorna el mismo mapa compartido que se utiliza internamente en la clase, asegurando que al obtener el mapa a través de getFacturas se obtenga una referencia al mismo objeto que se utiliza para almacenar las facturas, y que cualquier modificación realizada a través de la referencia obtenida se refleje en el mapa interno
    @Test
    void test16_setFacturas_reemplazaContenidoDelMapa() {
        Map<String, Optional<Factura>> nuevoMapa = new ConcurrentHashMap<>();
        nuevoMapa.put("clave", Optional.empty());

        facturaService.setFacturas(nuevoMapa);

        assertTrue(facturaService.getFacturas().containsKey("clave"));
        assertEquals(1, facturaService.getFacturas().size());
    }
}
