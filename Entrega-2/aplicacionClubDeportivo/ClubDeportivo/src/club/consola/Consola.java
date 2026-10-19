package club.consola;

import java.time.LocalDate;
import java.util.Scanner;

import club.model.SistemaClub;
import club.modelo.competencias.Categoria;
import club.modelo.competencias.Equipo;
import club.modelo.competencias.Futbol;
import club.modelo.tienda.Existencia;
import club.modelo.tienda.ProductoTienda;
import club.modelo.usuarios.Administrador;
import club.modelo.usuarios.Empleado;
import club.modelo.usuarios.Entrenador;
import club.modelo.usuarios.Fisioterapeuta;
import club.modelo.usuarios.Socio;
import club.modelo.usuarios.Usuario;
import club.persistencia.ManejadorPersistencia;

/** Punto de entrada y demostración básica por consola del club. */
public final class Consola {
    private Consola() { }

    public static void main(String[] args) {
        boolean primeraEjecucion = !ManejadorPersistencia.existeEstado();
        SistemaClub sistema = ManejadorPersistencia.cargarEstado();
        if (primeraEjecucion) {
            crearDatosDemo(sistema);
            ManejadorPersistencia.guardarEstado(sistema);
            System.out.println("Se creó el estado inicial de demostración.");
            if (sistema.autenticar("admin", "admin123") != null)
                System.out.println("Acceso inicial: admin / admin123 (se recomienda cambiar la contraseña).");
            else
                System.out.println("Use las credenciales de administrador ya registradas.");
        }

        try (Scanner entrada = new Scanner(System.in)) {
            boolean continuar = true;
            while (continuar) {
                System.out.println("\n=== Club Deportivo Séneca ===");
                System.out.print("Login (o 'salir'): ");
                String login = entrada.nextLine().trim();
                if ("salir".equalsIgnoreCase(login)) break;
                System.out.print("Contraseña: ");
                String password = entrada.nextLine();
                Usuario usuario = sistema.autenticar(login, password);
                if (usuario == null) {
                    System.out.println("Credenciales incorrectas.");
                    continue;
                }
                System.out.println("Bienvenido, " + usuario.getNombre() + " (" + nombreRol(usuario) + ").");
                if (usuario instanceof Administrador) continuar = menuAdministrador(entrada, sistema, usuario);
                else menuUsuario(entrada, sistema, usuario);
            }
        }
        ManejadorPersistencia.guardarEstado(sistema);
        System.out.println("Estado guardado. Hasta pronto.");
    }

    private static boolean menuAdministrador(Scanner entrada, SistemaClub sistema, Usuario usuario) {
        while (true) {
            System.out.println("\n1) Resumen del sistema  2) Crear usuario  3) Cambiar contraseña  4) Cerrar sesión  0) Salir");
            String opcion = entrada.nextLine().trim();
            try {
                switch (opcion) {
                    case "1":
                        System.out.printf("Usuarios: %d | equipos: %d | competencias: %d | productos: %d | instalaciones: %d%n",
                                sistema.getUsuarios().size(), sistema.getEquipos().size(), sistema.getCompetencias().size(),
                                sistema.getProductos().size(), sistema.getInstalaciones().size());
                        break;
                    case "2": crearUsuario(entrada, sistema); ManejadorPersistencia.guardarEstado(sistema); break;
                    case "3": cambiarPassword(entrada, usuario); ManejadorPersistencia.guardarEstado(sistema); break;
                    case "4": return true;
                    case "0": return false;
                    default: System.out.println("Opción no válida.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("No se completó la operación: " + e.getMessage());
            }
        }
    }

    private static void menuUsuario(Scanner entrada, SistemaClub sistema, Usuario usuario) {
        while (true) {
            System.out.println("\n1) Ver mi resumen  2) Cambiar contraseña  3) Cerrar sesión");
            String opcion = entrada.nextLine().trim();
            if ("1".equals(opcion)) {
                if (usuario instanceof Socio) {
                    Socio socio = (Socio) usuario;
                    System.out.printf("Socio: %s | equipos activos: %d | puntos: %d | reservas: %d%n",
                            socio.getNombre(), socio.getEquipos().size(), socio.getPuntosFidelidad(), socio.getReservas().size());
                } else if (usuario instanceof Entrenador) {
                    System.out.println("Entrenador: " + usuario.getNombre() + " | sesiones planificadas: " + ((Entrenador) usuario).getSesiones().size());
                } else if (usuario instanceof Empleado) {
                    System.out.println("Empleado: " + usuario.getNombre());
                } else {
                    System.out.println("Usuario: " + usuario.getNombre());
                }
            } else if ("2".equals(opcion)) {
                cambiarPassword(entrada, usuario);
                ManejadorPersistencia.guardarEstado(sistema);
            } else if ("3".equals(opcion)) {
                return;
            } else {
                System.out.println("Opción no válida.");
            }
        }
    }

    private static void crearUsuario(Scanner entrada, SistemaClub sistema) {
        System.out.print("Tipo (socio/entrenador/fisioterapeuta/administrador): ");
        String tipo = entrada.nextLine().trim().toLowerCase();
        System.out.print("Login: "); String login = entrada.nextLine().trim();
        System.out.print("Nombre: "); String nombre = entrada.nextLine().trim();
        System.out.print("Contraseña: "); String password = entrada.nextLine();
        Usuario nuevo;
        switch (tipo) {
            case "socio":
                System.out.print("Fecha de nacimiento (AAAA-MM-DD): ");
                nuevo = new Socio(login, password, nombre, LocalDate.parse(entrada.nextLine().trim()));
                break;
            case "entrenador": case "fisioterapeuta":
                System.out.print("Código de descuento: "); String codigo = entrada.nextLine().trim();
                nuevo = "entrenador".equals(tipo) ? new Entrenador(login, password, nombre, codigo) : new Fisioterapeuta(login, password, nombre, codigo);
                break;
            case "administrador": nuevo = new Administrador(login, password, nombre); break;
            default: throw new IllegalArgumentException("Tipo de usuario no reconocido.");
        }
        sistema.registrarUsuario(nuevo);
        System.out.println("Usuario creado.");
    }

    private static void cambiarPassword(Scanner entrada, Usuario usuario) {
        System.out.print("Contraseña actual: "); String actual = entrada.nextLine();
        System.out.print("Nueva contraseña: "); String nueva = entrada.nextLine();
        usuario.actualizarPassword(actual, nueva);
        System.out.println("Contraseña actualizada.");
    }

    private static String nombreRol(Usuario usuario) {
        if (usuario instanceof Administrador) return "administrador";
        if (usuario instanceof Entrenador) return "entrenador";
        if (usuario instanceof Fisioterapeuta) return "fisioterapeuta";
        if (usuario instanceof Socio) return "socio";
        return "usuario";
    }

    private static void crearDatosDemo(SistemaClub sistema) {
        if (sistema.buscarUsuario("admin") == null) sistema.registrarUsuario(new Administrador("admin", "admin123", "Administrador de demostración"));
        Socio demo = (Socio) sistema.buscarUsuario("socio.demo");
        if (demo == null) {
            demo = new Socio("socio.demo", "demo123", "Socio de demostración", LocalDate.of(2000, 1, 1));
            sistema.registrarUsuario(demo);
        }
        if (sistema.getEquipos().isEmpty()) {
            Equipo equipo = new Equipo("Seneca Fútbol Demo", new Futbol("Fútbol", 7), new Categoria("Abierta", 0, 150), 12);
            sistema.registrarEquipo(equipo);
            equipo.inscribirJugador(demo, "delantero", 10);
        }
        if (sistema.getProductos().isEmpty()) {
            ProductoTienda balon = new ProductoTienda("Balón demo", 50000, "deporte");
            balon.agregarExistencia(new Existencia(5, "tienda"));
            sistema.registrarProducto(balon);
        }
    }
}
