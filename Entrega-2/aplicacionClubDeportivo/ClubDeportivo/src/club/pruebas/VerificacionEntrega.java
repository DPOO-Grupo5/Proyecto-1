package club.pruebas;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import club.model.SistemaClub;
import club.modelo.competencias.Categoria;
import club.modelo.competencias.Equipo;
import club.modelo.competencias.Futbol;
import club.modelo.instalaciones.CanchaPadel;
import club.modelo.tienda.Existencia;
import club.modelo.tienda.ProductoTienda;
import club.modelo.tienda.Transaccion;
import club.modelo.usuarios.Socio;
import club.persistencia.ManejadorPersistencia;

/** Comprobaciones repetibles sin JUnit ni datos del club del usuario. */
public final class VerificacionEntrega {
    private int correctas;

    private VerificacionEntrega() { }

    public static void main(String[] args) throws Exception {
        new VerificacionEntrega().ejecutar();
    }

    private void ejecutar() throws Exception {
        autenticacionYHash();
        cupoDeEquipo();
        conflictoDeReserva();
        ventaSinInventario();
        ventaExitosa();
        persistenciaAislada();
        System.out.println("Verificaciones correctas: " + correctas + "/8");
    }

    private void autenticacionYHash() {
        SistemaClub sistema = new SistemaClub();
        Socio socio = socio("prueba.login");
        sistema.registrarUsuario(socio);
        comprobar(sistema.autenticar("PRUEBA.LOGIN", "clave123") == socio, "autenticación válida sin distinguir mayúsculas");
        comprobar(sistema.autenticar("prueba.login", "incorrecta") == null, "rechazo de contraseña incorrecta");
        comprobar(!"clave123".equals(socio.getPassword()) && socio.getPassword().startsWith("pbkdf2$"), "contraseña almacenada como hash");
    }

    private void cupoDeEquipo() {
        Equipo equipo = new Equipo("Equipo prueba", new Futbol("Futbol", 7), new Categoria("Abierta", 0, 150), 1);
        equipo.inscribirJugador(socio("jugador.uno"), "delantero", 9);
        boolean rechazado = false;
        try { equipo.inscribirJugador(socio("jugador.dos"), "portero", 1); }
        catch (IllegalStateException e) { rechazado = true; }
        comprobar(rechazado && equipo.getInscripcionesActivas().size() == 1, "rechazo al superar el cupo del equipo");
    }

    private void ventaSinInventario() {
        Socio comprador = socio("comprador");
        ProductoTienda producto = new ProductoTienda("Balón de prueba", 10000, "deporte");
        producto.agregarExistencia(new Existencia(1, "tienda"));
        Transaccion venta = comprador.iniciarTransaccion(java.util.Collections.emptyMap(), "tienda");
        venta.agregarDetalle(producto, 2);
        boolean rechazada = false;
        try { venta.confirmar(); }
        catch (IllegalStateException e) { rechazada = true; }
        comprobar(rechazada && !venta.isConfirmada() && producto.getCantidadTotal() == 1,
                "venta con inventario insuficiente sin alterar existencias");
    }

    private void conflictoDeReserva() {
        Socio responsable = socio("reservas");
        CanchaPadel cancha = new CanchaPadel(4, 4, "Cancha demo", true);
        java.time.LocalDateTime inicio = java.time.LocalDateTime.of(2030, 5, 10, 10, 0);
        responsable.reservarInstalacion(cancha, inicio, inicio.plusHours(1), 4, "dobles");
        boolean rechazada = false;
        try { socio("segundo").reservarInstalacion(cancha, inicio.plusMinutes(30), inicio.plusHours(2), 4, "dobles"); }
        catch (IllegalStateException e) { rechazada = true; }
        comprobar(rechazada && cancha.getReservas().size() == 1, "rechazo de reserva de cancha en horario traslapado");
    }

    private void ventaExitosa() {
        Socio comprador = socio("venta.valida");
        club.modelo.tienda.ProductoCafeteria producto = new club.modelo.tienda.ProductoCafeteria("Agua demo", 1000) { };
        producto.agregarExistencia(new Existencia(1, "cafeteria"));
        Transaccion venta = comprador.iniciarTransaccion(java.util.Collections.emptyMap(), "cafeteria");
        venta.agregarDetalle(producto, 1);
        venta.confirmar();
        comprobar(venta.isConfirmada() && producto.getCantidadTotal() == 0 && venta.getTotal() == 1080.0,
                "venta exitosa con impuesto y descuento correcto de inventario");
    }

    private void persistenciaAislada() throws Exception {
        Path carpetaTemporal = Files.createTempDirectory("seneca-verificacion-");
        System.setProperty("club.datos", carpetaTemporal.toString());
        SistemaClub original = new SistemaClub();
        original.registrarUsuario(socio("persistido"));
        ManejadorPersistencia.guardarEstado(original);
        SistemaClub recuperado = ManejadorPersistencia.cargarEstado();
        comprobar(recuperado.autenticar("persistido", "clave123") != null, "guardado y recarga de usuario en carpeta temporal");
    }

    private static Socio socio(String login) {
        return new Socio(login, "clave123", login, LocalDate.of(2000, 1, 1));
    }

    private void comprobar(boolean condicion, String descripcion) {
        if (!condicion) throw new AssertionError("FALLÓ: " + descripcion);
        correctas++;
        System.out.println("OK: " + descripcion);
    }
}
