package club.consola;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;

import club.model.SistemaClub;
import club.modelo.competencias.*;
import club.modelo.entrenamiento.*;
import club.modelo.instalaciones.*;
import club.modelo.tienda.*;
import club.modelo.usuarios.*;
import club.persistencia.ArchivoPlano;

/** Punto de entrada del club: inicio de sesión y un menú por rol con los requerimientos funcionales. */
public final class Consola {
    private Consola() { }

    private static final ArchivoPlano archivo = new ArchivoPlano();
    private static SistemaClub sistema;
    private static Scanner in;

    // ===================================================================== MAIN
    public static void main(String[] args) {
        boolean primeraEjecucion = !archivo.existeEstado();
        sistema = primeraEjecucion ? new SistemaClub() : archivo.cargarEstado();
        crearDatosDemo();
        archivo.guardarEstado(sistema);
        if (primeraEjecucion) {
            System.out.println("Se creó el estado inicial de demostración.");
            System.out.println("Usuarios: admin/admin123, entrenador.demo/entre123, fisio.demo/fisio123, socio.demo/demo123, socio.demo2/demo123");
        }
        try (Scanner entrada = new Scanner(System.in)) {
            in = entrada;
            while (true) {
                System.out.println("\n=== Club Deportivo Séneca ===");
                System.out.println("Acceso predeterminado: admin       admin123");
                String login = texto("Login (o 'salir'): ");
                if ("salir".equalsIgnoreCase(login)) break;
                String password = texto("Contraseña: ");
                Usuario usuario = sistema.autenticar(login, password);
                if (usuario == null) { System.out.println("Credenciales incorrectas."); continue; }
                System.out.println("Bienvenido, " + usuario.getNombre() + " (" + nombreRol(usuario) + ").");
                if (usuario instanceof Administrador) menuAdministrador((Administrador) usuario);
                else if (usuario instanceof Entrenador) menuEmpleado((Empleado) usuario, true);
                else if (usuario instanceof Empleado) menuEmpleado((Empleado) usuario, false);
                else if (usuario instanceof Socio) menuSocio((Socio) usuario);
                else System.out.println("Este tipo de usuario no tiene menú.");
            }
        }
        archivo.guardarEstado(sistema);
        System.out.println("Estado guardado. Hasta pronto.");
    }

    /** Ejecuta una acción de menú, muestra los errores de validación y guarda el estado. */
    private static void ejecutar(Runnable accion) {
        try {
            accion.run();
        } catch (IllegalArgumentException | IllegalStateException | DateTimeException | UnsupportedOperationException e) {
            System.out.println("No se completó la operación: " + e.getMessage());
        }
        archivo.guardarEstado(sistema);
    }

    // ===================================================================== SOCIO
    private static void menuSocio(Socio socio) {
        while (true) {
            System.out.println("\n--- Menú del socio ---");
            System.out.println("1) Mis equipos, partidos y sanciones   2) Inscribirme en una disciplina individual");
            System.out.println("3) Reservar instalación                4) Programar partido de práctica");
            System.out.println("5) Registrar resultado de práctica     6) Mi desempeño");
            System.out.println("7) Comprar en la tienda                8) Comprar en la cafetería");
            System.out.println("9) Mis compras y puntos                P) Cambiar contraseña   0) Cerrar sesión");
            String op = texto("Opción: ").toUpperCase();
            if ("0".equals(op)) return;
            switch (op) {
                case "1": ejecutar(() -> verMisEquipos(socio)); break;
                case "2": ejecutar(() -> inscribirseEnDisciplina(socio)); break;
                case "3": ejecutar(() -> reservarInstalacion(socio)); break;
                case "4": ejecutar(() -> programarPractica(socio)); break;
                case "5": ejecutar(() -> registrarResultadoPractica(socio)); break;
                case "6": ejecutar(() -> verDesempeno(socio)); break;
                case "7": ejecutar(() -> comprar(socio, "tienda")); break;
                case "8": ejecutar(() -> comprar(socio, "cafeteria")); break;
                case "9": ejecutar(() -> verCompras(socio)); break;
                case "P": ejecutar(() -> cambiarPassword(socio)); break;
                default: System.out.println("Opción no válida.");
            }
        }
    }

    private static void verMisEquipos(Socio socio) {
        System.out.println("--- Mis equipos ---");
        boolean alguno = false;
        for (InscripcionEquipo i : socio.getInscripcionesEquipo()) {
            if (!i.isActiva()) continue;
            alguno = true;
            Equipo e = i.getEquipo();
            System.out.printf("%s | %s | %s | posición: %s | camiseta: %d%n", e.getNombre(), e.getDeporte().getNombre(),
                    e.getCategoria().getNombre(), i.getPosicion(), i.getNumeroCamiseta());
        }
        if (!alguno) System.out.println("No pertenece a ningún equipo.");
        System.out.println("--- Calendario y resultados ---");
        boolean hayPartidos = false;
        for (InscripcionEquipo i : socio.getInscripcionesEquipo()) {
            if (!i.isActiva()) continue;
            Equipo e = i.getEquipo();
            for (PartidoInterno p : e.getPartidosInternos()) {
                hayPartidos = true;
                System.out.printf("[Interno] %s: %s vs %s | marcador: %s%n", p.getFechaHora(), p.getEquipoLocal().getNombre(),
                        p.getEquipoVisitante().getNombre(), p.getMarcadorFinal());
            }
            for (Competencia c : sistema.getCompetencias()) {
                for (PartidoCompetencia p : c.getPartidos()) {
                    if (!p.getEquipos().contains(e)) continue;
                    hayPartidos = true;
                    System.out.printf("[%s] %s: %s | marcador: %s | escenario: %s%n", c.getNombre(),
                            p.getFecha() == null ? "sin fecha" : p.getFecha().toString(), nombresEquipos(p), p.getMarcador(), p.getEscenario());
                }
            }
        }
        if (!hayPartidos) System.out.println("Sin partidos registrados.");
        System.out.println("--- Sanciones vigentes ---");
        if (socio.getSancionesVigentes().isEmpty()) System.out.println("Ninguna.");
        for (Sancion s : socio.getSancionesVigentes())
            System.out.printf("%s (suspensión de %d partido(s))%n", s.getMotivo(), s.getPartidosSuspension());
    }

    private static void inscribirseEnDisciplina(Socio socio) {
        DisciplinaIndividual d = elegir("Disciplinas disponibles:", sistema.getDisciplinas(), Consola::nombreDisciplina);
        InscripcionDisciplina i = socio.inscribirseEnDisciplina(d);
        System.out.printf("Inscripción exitosa en %s. Nivel: %s | ranking: %d%n", nombreDisciplina(d), i.getNivel(), i.getRanking());
    }

    private static void reservarInstalacion(Socio socio) {
        Instalacion inst = elegir("Instalaciones:", sistema.getInstalaciones(),
                i -> i.getNombre() + " (" + i.getClass().getSimpleName() + (i.isTechada() ? ", techada" : "") + ", "
                        + i.getJugadoresMin() + " a " + i.getJugadoresMax() + " jugadores)");
        LocalDateTime ini = fechaHora("Inicio (AAAA-MM-DD HH:mm): ");
        LocalDateTime fin = fechaHora("Fin (AAAA-MM-DD HH:mm): ");
        int jugadores = entero("Número de jugadores: ");
        String modalidad = texto("Modalidad (sencillos/dobles): ");
        Reserva r = socio.reservarInstalacion(inst, ini, fin, jugadores, modalidad);
        System.out.printf("Reserva creada en %s de %s a %s (%d jugadores, %s).%n", inst.getNombre(),
                r.getFechaHoraInicio(), r.getFechaHoraFin(), r.getCantidadJugadores(), r.getModalidad());
    }

    private static void programarPractica(Socio socio) {
        List<DisciplinaIndividual> mias = new ArrayList<>();
        for (InscripcionDisciplina i : socio.getInscripcionesDisciplina()) if (i.isActiva()) mias.add(i.getDisciplina());
        DisciplinaIndividual d = elegir("Sus disciplinas:", mias, Consola::nombreDisciplina);
        List<Socio> rivales = new ArrayList<>();
        for (InscripcionDisciplina i : d.getInscripciones()) if (i.isActiva() && i.getSocio() != socio) rivales.add(i.getSocio());
        Socio rival = elegir("Rivales inscritos en la disciplina:", rivales, s -> s.getNombre() + " (" + s.getLogin() + ")");
        String modalidad = texto("Modalidad (sencillos/dobles): ");
        Entrenador entrenador = null;
        if (confirmar("¿Participa un entrenador en el partido?")) {
            entrenador = elegir("Entrenadores:", entrenadores(), e -> e.getNombre() + " (" + e.getLogin() + ")");
        }
        PartidoPractica p = socio.programarPartidoPractica(rival, d, modalidad, entrenador);
        System.out.println("Partido de práctica programado: " + etiquetaPractica(p));
    }

    private static void registrarResultadoPractica(Socio socio) {
        List<PartidoPractica> pendientes = new ArrayList<>();
        for (PartidoPractica p : socio.getPartidosPractica()) if (!p.isResultadoRegistrado()) pendientes.add(p);
        PartidoPractica p = elegir("Partidos de práctica pendientes:", pendientes, Consola::etiquetaPractica);
        EstadisticaIndividual a = pedirEstadisticaIndividual(p, 0);
        EstadisticaIndividual b = pedirEstadisticaIndividual(p, 1);
        p.registrarResultado(a, b);
        System.out.println("Resultado registrado. Rankings actualizados:");
        for (InscripcionDisciplina i : p.getParticipantes())
            System.out.printf("  %s: ranking %d%n", i.getSocio().getNombre(), i.getRanking());
    }

    private static EstadisticaIndividual pedirEstadisticaIndividual(PartidoPractica p, int indice) {
        InscripcionDisciplina participante = p.getParticipantes().get(indice);
        System.out.println("Estadísticas de " + participante.getSocio().getNombre() + ":");
        int sets = entero("  Sets ganados: ");
        if (p.getDisciplina() instanceof TenisMesa) return new EstadisticaTenisMesa(sets, entero("  Puntos totales: "));
        return new EstadisticaTenisPadel(sets, entero("  Games ganados: "), entero("  Aces: "), entero("  Errores no forzados: "));
    }

    private static void verDesempeno(Socio socio) {
        System.out.printf("Porcentaje de asistencia: %.1f%%%n", socio.getPorcentajeAsistencia());
        System.out.println("--- Asistencias y evaluaciones ---");
        if (socio.getAsistencias().isEmpty()) System.out.println("Sin registros.");
        for (Asistencia a : socio.getAsistencias()) {
            System.out.printf("%s | %s | nota: %s | %s%n", a.getSesion() == null ? "-" : a.getSesion().getFechaHora(), a.getEstado(),
                    a.getNotaDesempeno() == null ? "-" : a.getNotaDesempeno().toString(), a.getComentario() == null ? "" : a.getComentario());
        }
        System.out.println("--- Estadísticas de partidos de conjunto ---");
        int goles = 0, asistencias = 0, amarillas = 0, rojas = 0;
        for (EstadisticaJugadorPartido e : socio.getEstadisticas()) {
            if (e instanceof EstadisticaJugadorFutbol) {
                EstadisticaJugadorFutbol f = (EstadisticaJugadorFutbol) e;
                goles += f.getGoles(); asistencias += f.getAsistencias(); amarillas += f.getTarjetasAmarillas(); rojas += f.getTarjetasRojas();
            } else if (e instanceof EstadisticaJugadorBaloncesto) {
                System.out.printf("Baloncesto: %d puntos, %d rebotes%n", ((EstadisticaJugadorBaloncesto) e).getPuntos(), ((EstadisticaJugadorBaloncesto) e).getRebotes());
            } else if (e instanceof EstadisticaJugadorVoleibol) {
                System.out.printf("Voleibol: %d aces, %d bloqueos%n", ((EstadisticaJugadorVoleibol) e).getAces(), ((EstadisticaJugadorVoleibol) e).getBloqueos());
            }
        }
        System.out.printf("Fútbol: %d goles, %d asistencias, %d amarillas, %d rojas%n", goles, asistencias, amarillas, rojas);
        System.out.println("--- Disciplinas individuales ---");
        boolean alguna = false;
        for (InscripcionDisciplina i : socio.getInscripcionesDisciplina()) {
            if (!i.isActiva()) continue;
            alguna = true;
            System.out.printf("%s | nivel: %s | ranking: %d%n", nombreDisciplina(i.getDisciplina()), i.getNivel(), i.getRanking());
        }
        if (!alguna) System.out.println("Sin inscripciones.");
        for (EstadisticaIndividual e : socio.getEstadisticasIndividuales()) {
            if (e instanceof EstadisticaTenisPadel) {
                EstadisticaTenisPadel t = (EstadisticaTenisPadel) e;
                System.out.printf("Partido: sets %d, games %d, aces %d, errores %d%n", t.getSets(), t.getGamesGanados(), t.getAces(), t.getErroresNoForzados());
            } else if (e instanceof EstadisticaTenisMesa) {
                EstadisticaTenisMesa t = (EstadisticaTenisMesa) e;
                System.out.printf("Partido: sets %d, puntos totales %d%n", t.getSets(), t.getPuntosTotales());
            }
        }
    }

    private static void verCompras(Usuario usuario) {
        System.out.println("--- Historial de compras ---");
        int cuantas = 0;
        for (Transaccion t : usuario.consultarHistorialCompras()) {
            if (!t.isConfirmada()) continue;
            cuantas++;
            System.out.printf("%s | %s | total: $%.0f | puntos ganados: %d | puntos usados: %d%n", t.getFechaHora(), t.getRubro(),
                    t.getTotal(), t.getPuntosFidelidad(), t.getPuntosRedimidos());
            for (DetalleTransaccion d : t.getDetalles())
                System.out.printf("     %d x %s%n", d.getCantidad(), d.getProducto().getNombre());
        }
        if (cuantas == 0) System.out.println("Sin compras.");
        System.out.println("Saldo de puntos de fidelidad: " + usuario.getPuntosFidelidad() + " (1 punto = $1)");
    }

    // ===================================================================== COMPRAS (tienda y cafetería)
    private static void comprar(Usuario usuario, String rubro) {
        List<Producto> catalogo = new ArrayList<>();
        for (Producto p : sistema.getProductos()) {
            boolean delRubro = "tienda".equals(rubro) ? p instanceof ProductoTienda : p instanceof ProductoCafeteria;
            if (delRubro && p.getCantidadTotal() > 0) catalogo.add(p);
        }
        if (catalogo.isEmpty()) throw new IllegalStateException("No hay productos disponibles en " + rubro + ".");
        Transaccion t = usuario.iniciarTransaccion(null, rubro);
        try {
            while (true) {
                System.out.println("--- Productos de " + rubro + " ---");
                for (int i = 0; i < catalogo.size(); i++) System.out.printf("  %d) %s%n", i + 1, describirProducto(catalogo.get(i)));
                int n = entero("Número de producto (0 para terminar): ");
                if (n == 0) break;
                if (n < 1 || n > catalogo.size()) throw new IllegalArgumentException("Producto fuera de rango.");
                t.agregarDetalle(catalogo.get(n - 1), entero("Cantidad: "));
            }
            if (t.getDetalles().isEmpty()) throw new IllegalStateException("No agregó productos.");

            if ("tienda".equals(rubro)) {
                if (usuario instanceof Empleado) {
                    if (confirmar("¿Aplicar su descuento de empleado (15%)?")) t.aplicarDescuentoEmpleado((Empleado) usuario);
                } else {
                    String codigo = texto("Código de descuento compartido (Enter para omitir): ");
                    if (!codigo.isEmpty()) {
                        Empleado dueno = null;
                        for (Empleado e : empleados()) if (e.validarCodigo(codigo)) dueno = e;
                        t.aplicarDescuentoCompartido(codigo, dueno);
                    }
                }
            } else {
                List<String> alergenos = t.consultarAlergenos();
                if (!alergenos.isEmpty()) {
                    System.out.println("ATENCIÓN - alérgenos de su pedido: " + String.join(", ", alergenos));
                    if (!confirmar("¿Desea continuar con la compra?")) throw new IllegalStateException("Compra cancelada por alérgenos.");
                }
            }
            if (usuario.getPuntosFidelidad() > 0) {
                int puntos = entero("Puntos a redimir (tiene " + usuario.getPuntosFidelidad() + ", 0 para no usar): ");
                if (puntos > 0) t.redimirPuntos(puntos);
            }
            if ("cafeteria".equals(rubro)) {
                String pct = texto(String.format("Propina sugerida 10%% ($%.0f). Porcentaje (Enter para 10): ", t.getPropinaSugerida()));
                t.calcularPropina(pct.isEmpty() ? 10 : parsearDecimal(pct));
            }
            System.out.printf("Subtotal: $%.0f | descuento: $%.0f | impuesto: $%.0f | propina: $%.0f%n",
                    t.calcularSubtotal(), t.getDescuento(), t.calcularImpuestos(), t.getPropina());
            if (!confirmar("¿Confirmar la compra?")) throw new IllegalStateException("Compra cancelada.");
            t.confirmar();
            System.out.printf("Compra confirmada. Total: $%.0f | puntos ganados: %d | saldo de puntos: %d%n",
                    t.getTotal(), t.getPuntosFidelidad(), usuario.getPuntosFidelidad());
        } catch (RuntimeException e) {
            usuario.getTransacciones().remove(t);
            throw e;
        }
    }

    private static String describirProducto(Producto p) {
        String extra = "";
        if (p instanceof ProductoTienda) extra = " [" + ((ProductoTienda) p).getCategoria() + "]";
        else if (p instanceof Bebida) extra = ((Bebida) p).esCaliente() ? " [bebida caliente]" : " [bebida fría]";
        else if (p instanceof Snack) extra = " [snack, alérgenos: " + (((Snack) p).getAlergenos().isEmpty() ? "ninguno" : String.join(", ", ((Snack) p).getAlergenos())) + "]";
        return String.format("%s%s - $%.0f (disponibles: %d)", p.getNombre(), extra, p.getPrecio(), p.getCantidadTotal());
    }

    // ===================================================================== EMPLEADOS Y ENTRENADORES
    private static void menuEmpleado(Empleado empleado, boolean esEntrenador) {
        Entrenador entrenador = esEntrenador ? (Entrenador) empleado : null;
        while (true) {
            System.out.println("\n--- Menú del " + nombreRol(empleado) + " ---");
            System.out.println("1) Consultar mis turnos   2) Solicitar cambio de turno   3) Comprar en la tienda (descuento)");
            System.out.println("4) Compartir mi código    5) Sugerir producto");
            if (esEntrenador) {
                System.out.println("6) Planear sesión         7) Registrar asistencia y evaluación   8) Asignar nivel");
                System.out.println("9) Registrar partido interno   10) Registrar partido de competencia   11) Consultar analítica");
            }
            System.out.println("P) Cambiar contraseña   0) Cerrar sesión");
            String op = texto("Opción: ").toUpperCase();
            if ("0".equals(op)) return;
            switch (op) {
                case "1": ejecutar(() -> verTurnos(empleado)); break;
                case "2": ejecutar(() -> solicitarCambioTurno(empleado)); break;
                case "3": ejecutar(() -> comprar(empleado, "tienda")); break;
                case "4": ejecutar(() -> compartirCodigo(empleado)); break;
                case "5": ejecutar(() -> sugerirProducto(empleado)); break;
                case "6": if (esEntrenador) ejecutar(() -> planearSesion(entrenador)); else System.out.println("Opción no válida."); break;
                case "7": if (esEntrenador) ejecutar(() -> registrarAsistencia(entrenador)); else System.out.println("Opción no válida."); break;
                case "8": if (esEntrenador) ejecutar(() -> asignarNivel(entrenador)); else System.out.println("Opción no válida."); break;
                case "9": if (esEntrenador) ejecutar(() -> registrarPartidoInterno(entrenador)); else System.out.println("Opción no válida."); break;
                case "10": if (esEntrenador) ejecutar(() -> registrarPartidoCompetencia()); else System.out.println("Opción no válida."); break;
                case "11": if (esEntrenador) ejecutar(() -> consultarAnalitica(entrenador)); else System.out.println("Opción no válida."); break;
                case "P": ejecutar(() -> cambiarPassword(empleado)); break;
                default: System.out.println("Opción no válida.");
            }
        }
    }

    private static void verTurnos(Empleado empleado) {
        if (empleado.consultarTurnos().isEmpty()) System.out.println("No tiene turnos asignados.");
        for (Turno t : empleado.consultarTurnos()) System.out.println(etiquetaTurno(t));
        for (SolicitudCambioTurno s : empleado.getSolicitudes())
            System.out.printf("Solicitud: %s -> %s | estado: %s%n", etiquetaTurno(s.getTurnoActual()), etiquetaTurno(s.getTurnoDeseado()), s.getEstado());
    }

    private static void solicitarCambioTurno(Empleado empleado) {
        Turno actual = elegir("Su turno a cambiar:", empleado.getTurnos(), Consola::etiquetaTurno);
        DayOfWeek dia = dia(texto("Día deseado (lunes..domingo): "));
        LocalTime ini = hora("Hora de inicio deseada (HH:mm): ");
        LocalTime fin = hora("Hora de fin deseada (HH:mm): ");
        empleado.solicitarCambioTurno(actual, new Turno(dia, ini, fin));
        System.out.println("Solicitud registrada. Queda pendiente de aprobación del administrador.");
    }

    private static void compartirCodigo(Empleado empleado) {
        List<Socio> socios = socios();
        Socio socio = elegir("Socio que recibirá el descuento del 8%:", socios, s -> s.getNombre() + " (" + s.getLogin() + ")");
        empleado.autorizarDescuentoCompartido(socio);
        System.out.println("Comparta con " + socio.getNombre() + " su código: " + empleado.getCodigoDescuento());
    }

    private static void sugerirProducto(Empleado empleado) {
        String tipo = texto("Tipo (tienda/bebida/snack): ").toLowerCase();
        String nombre = texto("Nombre: ");
        double precio = decimal("Precio: ");
        Producto p;
        switch (tipo) {
            case "tienda": p = new ProductoTienda(nombre, precio, texto("Categoría: ")); break;
            case "bebida": p = new Bebida(nombre, precio, confirmar("¿Es caliente?")); break;
            case "snack":
                String lista = texto("Alérgenos separados por coma (Enter si no tiene): ");
                List<String> alergenos = new ArrayList<>();
                if (!lista.isEmpty()) for (String a : lista.split(",")) alergenos.add(a.trim());
                p = new Snack(nombre, precio, alergenos);
                break;
            default: throw new IllegalArgumentException("Tipo de producto no reconocido.");
        }
        empleado.sugerirProducto(p);
        System.out.println("Sugerencia enviada al administrador.");
    }

    private static void planearSesion(Entrenador entrenador) {
        Instalacion inst = elegir("Instalación:", sistema.getInstalaciones(), i -> i.getNombre() + " (" + i.getClass().getSimpleName() + ")");
        LocalDateTime fecha = fechaHora("Fecha y hora (AAAA-MM-DD HH:mm): ");
        int duracion = entero("Duración en minutos: ");
        String tipo = texto("Tipo (grupal/individual): ");
        List<Socio> convocados = new ArrayList<>();
        if (confirmar("¿Convocar un equipo completo? (n = socios de una disciplina)")) {
            Equipo e = elegir("Equipo:", equiposActivos(), Consola::etiquetaEquipo);
            for (InscripcionEquipo i : e.getInscripcionesActivas()) convocados.add(i.getSocio());
        } else {
            DisciplinaIndividual d = elegir("Disciplina:", sistema.getDisciplinas(), Consola::nombreDisciplina);
            List<Socio> inscritos = new ArrayList<>();
            for (InscripcionDisciplina i : d.getInscripciones()) if (i.isActiva()) inscritos.add(i.getSocio());
            for (Socio s : inscritos) System.out.println("  - " + s.getLogin() + " (" + s.getNombre() + ")");
            for (String login : texto("Logins a convocar separados por coma: ").split(",")) {
                Socio s = null;
                for (Socio x : inscritos) if (x.getLogin().equalsIgnoreCase(login.trim())) s = x;
                if (s == null) throw new IllegalArgumentException("El socio " + login.trim() + " no está inscrito en la disciplina.");
                convocados.add(s);
            }
        }
        SesionEntrenamiento s = entrenador.planearSesion(fecha, duracion, tipo, inst, convocados);
        System.out.printf("Sesión planeada el %s en %s con %d convocado(s).%n", s.getFechaHora(), inst.getNombre(), s.getConvocados().size());
    }

    private static void registrarAsistencia(Entrenador entrenador) {
        SesionEntrenamiento sesion = elegir("Sus sesiones:", entrenador.getSesiones(),
                s -> s.getFechaHora() + " en " + s.getInstalacion().getNombre() + " (" + s.getConvocados().size() + " convocados)");
        for (Socio socio : sesion.getConvocados()) {
            System.out.println("Convocado: " + socio.getNombre() + " (" + socio.getLogin() + ")");
            try {
                String estado = elegir("  Estado:", Arrays.asList("asistió", "faltó", "llegó tarde"), x -> x);
                String nota = texto("  Nota de 1 a 5 (Enter para omitir): ");
                String comentario = texto("  Comentario (Enter para omitir): ");
                entrenador.registrarAsistencia(sesion, socio, estado, nota.isEmpty() ? null : Integer.valueOf(nota), comentario.isEmpty() ? null : comentario);
                System.out.println("  Registrado.");
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("  No se registró: " + e.getMessage());
            }
        }
    }

    private static void asignarNivel(Entrenador entrenador) {
        DisciplinaIndividual d = elegir("Disciplina:", sistema.getDisciplinas(), Consola::nombreDisciplina);
        List<Socio> inscritos = new ArrayList<>();
        for (InscripcionDisciplina i : d.getInscripciones()) if (i.isActiva()) inscritos.add(i.getSocio());
        Socio socio = elegir("Socio:", inscritos, s -> s.getNombre() + " (" + s.getLogin() + ")");
        String nivel = elegir("Nivel:", Arrays.asList("Principiante", "Intermedio", "Avanzado"), x -> x);
        entrenador.asignarNivel(socio, d, nivel);
        System.out.println("Nivel actualizado a " + nivel + ".");
    }

    private static void registrarPartidoInterno(Entrenador entrenador) {
        Equipo a = elegir("Equipo local:", equiposActivos(), Consola::etiquetaEquipo);
        Equipo b = elegir("Equipo visitante:", equiposActivos(), Consola::etiquetaEquipo);
        LocalDateTime fecha = fechaHora("Fecha y hora (AAAA-MM-DD HH:mm): ");
        int ga = entero("Marcador del local: ");
        int gb = entero("Marcador del visitante: ");
        List<Socio> jugadores = new ArrayList<>();
        for (InscripcionEquipo i : a.getInscripcionesActivas()) jugadores.add(i.getSocio());
        for (InscripcionEquipo i : b.getInscripcionesActivas()) if (!jugadores.contains(i.getSocio())) jugadores.add(i.getSocio());
        List<EstadisticaJugadorPartido> stats = pedirEstadisticas(jugadores, a.getDeporte());
        PartidoInterno p = entrenador.registrarPartidoInterno(a, b, fecha, ga, gb, stats);
        System.out.printf("Partido registrado: %s vs %s, marcador %s (%d estadísticas).%n", a.getNombre(), b.getNombre(),
                p.getMarcadorFinal(), p.getEstadisticas().size());
    }

    private static void registrarPartidoCompetencia() {
        List<Competencia> conCalendario = new ArrayList<>();
        for (Competencia c : sistema.getCompetencias()) if (!c.getPartidos().isEmpty()) conCalendario.add(c);
        Competencia c = elegir("Competencia:", conCalendario, x -> x.getNombre() + " " + x.getTemporada());
        List<PartidoCompetencia> pendientes = new ArrayList<>();
        for (PartidoCompetencia p : c.getPartidos()) if (!p.isFinalizado() && p.getEquipos().size() == 2) pendientes.add(p);
        PartidoCompetencia p = elegir("Partidos pendientes:", pendientes, Consola::nombresEquipos);
        p.setFecha(fecha("Fecha (AAAA-MM-DD): "));
        p.setEscenario(texto("Escenario: "));
        int local = entero("Marcador de " + p.getEquipos().get(0).getNombre() + ": ");
        int visitante = entero("Marcador de " + p.getEquipos().get(1).getNombre() + ": ");
        List<Socio> jugadores = new ArrayList<>();
        for (InscripcionCompetencia ins : c.getInscripciones())
            if (p.getEquipos().contains(ins.getEquipo())) jugadores.addAll(ins.getNomina());
        List<EstadisticaJugadorPartido> stats = pedirEstadisticas(jugadores, c.getDeporte());
        p.registrarMarcador(local, visitante);
        for (EstadisticaJugadorPartido e : stats) p.agregarEstadistica(e);
        if (local == visitante && p.getGrupo() == null && c.getFormato() instanceof FormatoPorFases) {
            EquipoCompetencia ganador = elegir("Empate en eliminación directa. ¿Quién avanza?", p.getEquipos(), EquipoCompetencia::getNombre);
            p.resolverEmpate(ganador);
        }
        System.out.printf("Partido registrado: %s, marcador %s.%n", nombresEquipos(p), p.getMarcador());
        for (Socio s : jugadores) if (s.tieneSancionVigente()) System.out.println("Sanción vigente: " + s.getNombre());
    }

    /** Pide las estadísticas de los jugadores que pueden jugar (sin sanción vigente). */
    private static List<EstadisticaJugadorPartido> pedirEstadisticas(List<Socio> jugadores, DeporteConjunto deporte) {
        List<EstadisticaJugadorPartido> stats = new ArrayList<>();
        for (Socio s : jugadores) {
            if (s.tieneSancionVigente()) { System.out.println(s.getNombre() + " está suspendido y no puede jugar este partido."); continue; }
            if (!confirmar("¿Participó " + s.getNombre() + "?")) continue;
            if (deporte instanceof Futbol)
                stats.add(new EstadisticaJugadorFutbol(s, entero("  Goles: "), entero("  Asistencias: "), entero("  Tarjetas amarillas: "), entero("  Tarjetas rojas: ")));
            else if (deporte instanceof Baloncesto)
                stats.add(new EstadisticaJugadorBaloncesto(s, entero("  Puntos: "), entero("  Rebotes: ")));
            else
                stats.add(new EstadisticaJugadorVoleibol(s, entero("  Aces: "), entero("  Bloqueos: ")));
        }
        return stats;
    }

    private static void consultarAnalitica(Entrenador entrenador) {
        Socio socio = elegir("Jugador:", socios(), s -> s.getNombre() + " (" + s.getLogin() + ")");
        System.out.println(entrenador.consultarAnalitica(socio));
        verDesempeno(socio);
    }

    // ===================================================================== ADMINISTRADOR
    private static void menuAdministrador(Administrador admin) {
        while (true) {
            System.out.println("\n--- Menú del administrador ---");
            System.out.println("Usuarios:      1) Crear   2) Dar de baja   3) Listar");
            System.out.println("Equipos:       4) Crear   5) Dar de baja   6) Inscribir jugador   7) Listar");
            System.out.println("Competencias:  8) Crear   9) Inscribir equipo y nómina   10) Calendario y eliminación   11) Tabla y clasificaciones");
            System.out.println("Inventario:    12) Consultar inventario e historial   13) Mover/reabastecer   14) Resolver sugerencias");
            System.out.println("Turnos:        15) Asignar turno   16) Resolver solicitudes de cambio   17) Ver turnos");
            System.out.println("Reportes:      18) Ventas   19) Deportivo");
            System.out.println("P) Cambiar contraseña   0) Cerrar sesión");
            String op = texto("Opción: ").toUpperCase();
            if ("0".equals(op)) return;
            switch (op) {
                case "1": ejecutar(() -> crearUsuario(admin)); break;
                case "2": ejecutar(() -> darDeBajaUsuario(admin)); break;
                case "3": ejecutar(Consola::listarUsuarios); break;
                case "4": ejecutar(() -> crearEquipo(admin)); break;
                case "5": ejecutar(() -> darDeBajaEquipo(admin)); break;
                case "6": ejecutar(() -> inscribirJugador(admin)); break;
                case "7": ejecutar(Consola::listarEquipos); break;
                case "8": ejecutar(() -> crearCompetencia(admin)); break;
                case "9": ejecutar(Consola::inscribirEnCompetencia); break;
                case "10": ejecutar(Consola::gestionarCalendario); break;
                case "11": ejecutar(Consola::verCompetencia); break;
                case "12": ejecutar(() -> consultarInventario(admin)); break;
                case "13": ejecutar(() -> gestionarInventario(admin)); break;
                case "14": ejecutar(() -> resolverSugerencias(admin)); break;
                case "15": ejecutar(() -> asignarTurno(admin)); break;
                case "16": ejecutar(() -> resolverSolicitudes(admin)); break;
                case "17": ejecutar(Consola::listarTurnos); break;
                case "18": ejecutar(() -> reporteVentas(admin)); break;
                case "19": ejecutar(() -> reporteDeportivo(admin)); break;
                case "P": ejecutar(() -> cambiarPassword(admin)); break;
                default: System.out.println("Opción no válida.");
            }
        }
    }

    private static void crearUsuario(Administrador admin) {
        String tipo = texto("Tipo (socio/entrenador/fisioterapeuta/administrador): ").toLowerCase();
        String login = texto("Login: ");
        String nombre = texto("Nombre: ");
        String password = texto("Contraseña: ");
        Usuario nuevo;
        switch (tipo) {
            case "socio": nuevo = new Socio(login, password, nombre, fecha("Fecha de nacimiento (AAAA-MM-DD): ")); break;
            case "entrenador": nuevo = new Entrenador(login, password, nombre, texto("Código de descuento: ")); break;
            case "fisioterapeuta": nuevo = new Fisioterapeuta(login, password, nombre, texto("Código de descuento: ")); break;
            case "administrador": nuevo = new Administrador(login, password, nombre); break;
            default: throw new IllegalArgumentException("Tipo de usuario no reconocido.");
        }
        admin.crearUsuario(nuevo, sistema);
        System.out.println("Usuario creado.");
    }

    private static void darDeBajaUsuario(Administrador admin) {
        List<Usuario> candidatos = new ArrayList<>();
        for (Usuario u : sistema.getUsuarios().values()) if (u != admin) candidatos.add(u);
        Usuario u = elegir("Usuario a dar de baja:", candidatos, x -> x.getLogin() + " - " + x.getNombre() + " (" + nombreRol(x) + ")");
        sistema.darDeBajaUsuario(u.getLogin());
        System.out.println("Usuario dado de baja.");
    }

    private static void listarUsuarios() {
        for (Usuario u : sistema.getUsuarios().values()) System.out.printf("%-15s %-30s %s%n", u.getLogin(), u.getNombre(), nombreRol(u));
    }

    private static void crearEquipo(Administrador admin) {
        String nombre = texto("Nombre del equipo: ");
        DeporteConjunto deporte = deporte(elegir("Deporte:", Arrays.asList("Fútbol", "Baloncesto", "Voleibol"), x -> x));
        Categoria categoria = categoria(elegir("Categoría:", Arrays.asList("Sub-13", "Sub-15", "Sub-17", "Mayores"), x -> x));
        int cupo = entero("Cupo máximo de jugadores: ");
        Equipo e = admin.crearEquipo(nombre, deporte, categoria, cupo, sistema);
        System.out.println("Equipo creado: " + etiquetaEquipo(e));
    }

    private static void darDeBajaEquipo(Administrador admin) {
        Equipo e = elegir("Equipo a dar de baja:", equiposActivos(), Consola::etiquetaEquipo);
        admin.darDeBajaEquipo(e);
        System.out.println("Equipo dado de baja (se conserva su historial).");
    }

    private static void inscribirJugador(Administrador admin) {
        Equipo e = elegir("Equipo:", equiposActivos(), Consola::etiquetaEquipo);
        Socio s = elegir("Socio:", socios(), x -> x.getNombre() + " (" + x.getLogin() + ", " + x.calcularEdad(LocalDate.now()) + " años)");
        String posicion = texto("Posición: ");
        int camiseta = entero("Número de camiseta: ");
        admin.inscribirJugadorEnEquipo(s, e, posicion, camiseta);
        System.out.println("Jugador inscrito.");
    }

    private static void listarEquipos() {
        for (Equipo e : sistema.getEquipos()) {
            System.out.println(etiquetaEquipo(e) + (e.isActivo() ? "" : " [de baja]"));
            for (InscripcionEquipo i : e.getInscripciones())
                System.out.printf("     #%d %s (%s)%s%n", i.getNumeroCamiseta(), i.getSocio().getNombre(), i.getPosicion(), i.isActiva() ? "" : " [retirado]");
        }
    }

    private static void crearCompetencia(Administrador admin) {
        String nombre = texto("Nombre (ej. ASCUN, Torneo de los Cerros): ");
        String organizador = texto("Entidad organizadora: ");
        String temporada = texto("Año o temporada: ");
        DeporteConjunto deporte = deporte(elegir("Deporte:", Arrays.asList("Fútbol", "Baloncesto", "Voleibol"), x -> x));
        Categoria categoria = categoria(elegir("Categoría habilitada:", Arrays.asList("Sub-13", "Sub-15", "Sub-17", "Mayores"), x -> x));
        ReglaPuntuacion regla = deporte instanceof Futbol ? new ReglaFutbol(3, 0)
                : deporte instanceof Baloncesto ? new ReglaBaloncesto(2, 0) : new ReglaVoleibol(2, 0);
        Formato formato;
        if (entero("Formato: 1) Liga todos contra todos   2) Fase de grupos + eliminación directa: ") == 1) {
            formato = new FormatoLiga(nombre, regla);
        } else {
            int grupos = entero("Número de grupos: ");
            int porGrupo = entero("Equipos por grupo: ");
            FormatoPorFases fases = new FormatoPorFases(nombre);
            FaseGrupos fg = new FaseGrupos(regla);
            for (int i = 0; i < grupos; i++) fg.getGrupos().add(new Grupo("Grupo " + (char) ('A' + i), porGrupo));
            fases.setFaseGrupos(fg);
            fases.setFaseEliminacion(new FaseEliminacion());
            formato = fases;
        }
        admin.crearCompetencia(nombre, organizador, temporada, deporte, categoria, formato, sistema);
        System.out.println("Competencia creada.");
    }

    private static void inscribirEnCompetencia() {
        Competencia c = elegir("Competencia:", sistema.getCompetencias(), x -> x.getNombre() + " " + x.getTemporada());
        if (entero("1) Equipo del club   2) Institución rival: ") == 2) {
            String nombre = texto("Nombre de la institución: ");
            EquipoCompetencia rival = null;
            for (EquipoCompetencia e : c.getEquipos()) if (e instanceof InstitucionRival && e.getNombre().equalsIgnoreCase(nombre)) rival = e;
            c.agregarEquipo(rival != null ? rival : new InstitucionRival(nombre));
            System.out.println("Institución inscrita.");
            return;
        }
        List<Equipo> elegibles = new ArrayList<>();
        for (Equipo e : equiposActivos())
            if (e.getDeporte() == c.getDeporte() && e.getCategoria() == c.getCategoria() && !c.getEquipos().contains(e)) elegibles.add(e);
        Equipo equipo = elegir("Equipos elegibles (mismo deporte y categoría):", elegibles, Consola::etiquetaEquipo);
        List<Socio> disponibles = new ArrayList<>();
        for (InscripcionEquipo i : equipo.getInscripcionesActivas()) disponibles.add(i.getSocio());
        for (Socio s : disponibles) System.out.println("  - " + s.getLogin() + " (" + s.getNombre() + ")");
        String respuesta = texto("Nómina: logins separados por coma, o 'todos': ");
        List<Socio> nomina = new ArrayList<>();
        if (respuesta.equalsIgnoreCase("todos")) nomina.addAll(disponibles);
        else for (String login : respuesta.split(",")) {
            Socio s = null;
            for (Socio x : disponibles) if (x.getLogin().equalsIgnoreCase(login.trim())) s = x;
            if (s == null) throw new IllegalArgumentException("El socio " + login.trim() + " no pertenece al equipo.");
            nomina.add(s);
        }
        if (nomina.size() < c.getDeporte().getMinJugadores())
            throw new IllegalStateException("La nómina mínima es de " + c.getDeporte().getMinJugadores() + " jugadores.");
        c.agregarEquipo(equipo);
        c.registrarNomina(equipo, nomina);
        System.out.println("Equipo inscrito con una nómina de " + nomina.size() + " jugadores.");
    }

    private static void gestionarCalendario() {
        Competencia c = elegir("Competencia:", sistema.getCompetencias(), x -> x.getNombre() + " " + x.getTemporada());
        int op = entero("1) Generar calendario   2) Generar fase de eliminación   3) Avanzar ronda de eliminación: ");
        if (op == 1) {
            c.generarCalendario();
            System.out.println("Calendario generado: " + c.getPartidos().size() + " partidos.");
        } else if (op == 2) {
            List<PartidoCompetencia> nuevos = c.generarFaseEliminacion(entero("Clasificados por grupo: "));
            System.out.println("Fase de eliminación generada: " + nuevos.size() + " partidos.");
        } else if (op == 3) {
            RondaEliminacion ronda = c.avanzarFaseEliminacion();
            if (ronda == null) System.out.println("Campeón: " + ((FormatoPorFases) c.getFormato()).getFaseEliminacion().getCampeon().getNombre());
            else System.out.println("Nueva ronda generada: " + ronda.getNombre() + " con " + ronda.getPartidos().size() + " partidos.");
        } else throw new IllegalArgumentException("Opción no válida.");
    }

    private static void verCompetencia() {
        Competencia c = elegir("Competencia:", sistema.getCompetencias(), x -> x.getNombre() + " " + x.getTemporada());
        System.out.printf("%s (%s) | %s | %s | formato: %s%n", c.getNombre(), c.getOrganizador(), c.getDeporte().getNombre(),
                c.getCategoria().getNombre(), c.getFormato().getNombreFormato());
        if (c.getFormato() instanceof FormatoLiga) {
            FormatoLiga liga = (FormatoLiga) c.getFormato();
            liga.actualizarPosiciones();
            imprimirTabla("Tabla de posiciones", liga.getPosiciones());
        } else if (c.getFormato() instanceof FormatoPorFases) {
            FormatoPorFases fases = (FormatoPorFases) c.getFormato();
            if (fases.getFaseGrupos() != null) for (Grupo g : fases.getFaseGrupos().getGrupos()) {
                g.actualizarPosiciones();
                imprimirTabla(g.getNombre(), g.getPosiciones());
            }
            if (fases.getFaseEliminacion() != null) {
                for (RondaEliminacion r : fases.getFaseEliminacion().getRondas()) {
                    System.out.println(r.getNombre() + ":");
                    for (PartidoCompetencia p : r.getPartidos()) System.out.println("   " + nombresEquipos(p) + " | " + p.getMarcador());
                }
                if (fases.getFaseEliminacion().getCampeon() != null) System.out.println("Campeón: " + fases.getFaseEliminacion().getCampeon().getNombre());
            }
        }
        System.out.println("--- Partidos ---");
        for (PartidoCompetencia p : c.getPartidos())
            System.out.printf("%s | %s | %s | escenario: %s%n", p.getFecha() == null ? "sin fecha" : p.getFecha().toString(), nombresEquipos(p), p.getMarcador(), p.getEscenario());
        if (c.getDeporte() instanceof Futbol) {
            System.out.println("--- Goleadores ---");
            for (Map.Entry<Socio, Integer> e : c.clasificacionGoleadores()) System.out.printf("%s: %d%n", e.getKey().getNombre(), e.getValue());
        }
        System.out.println("--- Jugadores más destacados ---");
        for (Map.Entry<Socio, Integer> e : c.clasificacionDestacados()) System.out.printf("%s: %d%n", e.getKey().getNombre(), e.getValue());
    }

    private static void imprimirTabla(String titulo, List<PosicionEquipo> posiciones) {
        System.out.println("--- " + titulo + " ---");
        System.out.println("Pos Equipo                         PJ  G  E  P  GF  GC  Pts");
        for (PosicionEquipo p : posiciones)
            System.out.printf("%-3d %-30s %2d %2d %2d %2d %3d %3d %4d%n", p.getPosicion(), p.getEquipo().getNombre(), p.getPartidosJugados(),
                    p.getVictorias(), p.getEmpates(), p.getDerrotas(), p.getTantosFavor(), p.getTantosContra(), p.getPuntuacion());
    }

    private static void consultarInventario(Administrador admin) {
        int op = entero("1) Inventario   2) Reservas   3) Partidos   4) Sesiones de entrenamiento: ");
        if (op == 1) System.out.println(admin.estadoInventario(sistema));
        else if (op == 2) {
            List<Reserva> reservas = admin.historialReservas(sistema);
            if (reservas.isEmpty()) System.out.println("Sin reservas.");
            for (Reserva r : reservas) System.out.printf("%s a %s | %s | %s | %d jugadores%n", r.getFechaHoraInicio(), r.getFechaHoraFin(),
                    r.getInstalacion().getNombre(), r.getSocioResponsable() == null ? "-" : r.getSocioResponsable().getNombre(), r.getCantidadJugadores());
        } else if (op == 3) {
            for (PartidoInterno p : admin.historialPartidosInternos(sistema))
                System.out.printf("[Interno] %s | %s vs %s | %s%n", p.getFechaHora(), p.getEquipoLocal().getNombre(), p.getEquipoVisitante().getNombre(), p.getMarcadorFinal());
            for (PartidoCompetencia p : admin.historialPartidosCompetencia(sistema))
                System.out.printf("[Competencia] %s | %s | %s%n", p.getFecha(), nombresEquipos(p), p.getMarcador());
        } else if (op == 4) {
            List<SesionEntrenamiento> sesiones = admin.historialSesiones(sistema);
            if (sesiones.isEmpty()) System.out.println("Sin sesiones.");
            for (SesionEntrenamiento s : sesiones) System.out.printf("%s | %s | entrenador: %s | %d convocados%n", s.getFechaHora(),
                    s.getInstalacion().getNombre(), s.getEntrenador().getNombre(), s.getConvocados().size());
        } else throw new IllegalArgumentException("Opción no válida.");
    }

    private static void gestionarInventario(Administrador admin) {
        Producto p = elegir("Producto:", sistema.getProductos(), x -> x.getNombre() + " (total: " + x.getCantidadTotal() + ")");
        for (Existencia e : p.getExistencias()) System.out.printf("   %s: %d%n", e.getUbicacionInventario(), e.getCantidad());
        if (entero("1) Mover entre ubicaciones   2) Reabastecer: ") == 1) {
            admin.moverInventario(p, texto("Ubicación de origen: "), texto("Ubicación de destino: "), entero("Cantidad: "));
            System.out.println("Inventario movido.");
        } else {
            admin.reabastecer(p, texto("Ubicación (tienda/cafeteria/bodega): "), entero("Cantidad: "));
            System.out.println("Inventario reabastecido.");
        }
    }

    private static void resolverSugerencias(Administrador admin) {
        List<SugerenciaProducto> pendientes = new ArrayList<>();
        for (Empleado e : empleados()) for (SugerenciaProducto s : e.getSugerencias()) if ("Pendiente".equalsIgnoreCase(s.getEstado())) pendientes.add(s);
        SugerenciaProducto s = elegir("Sugerencias pendientes:", pendientes,
                x -> x.getProducto().getNombre() + " - $" + (long) x.getProducto().getPrecio() + " (" + x.getProducto().getClass().getSimpleName() + ")");
        boolean aprobar = confirmar("¿Aprobar la sugerencia?");
        admin.resolverSugerencia(s, aprobar, sistema);
        System.out.println(aprobar ? "Aprobada: el producto entró al catálogo (reabastézcalo desde la opción 13)." : "Sugerencia rechazada.");
    }

    private static void asignarTurno(Administrador admin) {
        Empleado e = elegir("Empleado:", empleados(), x -> x.getNombre() + " (" + nombreRol(x) + ")");
        DayOfWeek dia = dia(texto("Día (lunes..domingo): "));
        LocalTime ini = hora("Hora de inicio (HH:mm): ");
        LocalTime fin = hora("Hora de fin (HH:mm): ");
        admin.asignarTurno(e, new Turno(dia, ini, fin));
        System.out.println("Turno asignado.");
    }

    private static void resolverSolicitudes(Administrador admin) {
        List<Empleado> empleados = admin.listarEmpleados(sistema);
        List<SolicitudCambioTurno> pendientes = admin.consultarSolicitudesPendientes(empleados);
        SolicitudCambioTurno s = elegir("Solicitudes pendientes:", pendientes,
                x -> x.getSolicitante().getNombre() + ": " + etiquetaTurno(x.getTurnoActual()) + " -> " + etiquetaTurno(x.getTurnoDeseado()));
        admin.resolverCambioTurno(s, empleados, confirmar("¿Aprobar la solicitud?"));
        System.out.println("Estado final de la solicitud: " + s.getEstado()
                + ("Rechazada".equals(s.getEstado()) ? " (si se intentó aprobar, no se cumple la cobertura mínima de 2 entrenadores y 1 fisioterapeuta)" : ""));
    }

    private static void listarTurnos() {
        for (Empleado e : empleados()) {
            System.out.println(e.getNombre() + " (" + nombreRol(e) + "):");
            if (e.getTurnos().isEmpty()) System.out.println("   sin turnos");
            for (Turno t : e.getTurnos()) System.out.println("   " + etiquetaTurno(t));
        }
    }

    private static void reporteVentas(Administrador admin) {
        String rubro = elegir("Rubro:", Arrays.asList("tienda", "cafeteria", "mensualidad", "todos"), x -> x);
        String periodo = elegir("Periodo:", Arrays.asList("diario", "semanal", "mensual"), x -> x);
        String ref = texto("Fecha de referencia AAAA-MM-DD (Enter para hoy): ");
        LocalDate fecha = ref.isEmpty() ? LocalDate.now() : LocalDate.parse(ref);
        List<Transaccion> todas = new ArrayList<>();
        for (Usuario u : sistema.getUsuarios().values()) todas.addAll(u.consultarHistorialCompras());
        Map<String, Double> reporte = admin.generarReporteVentas(todas, "todos".equals(rubro) ? null : rubro, periodo, fecha);
        if (reporte.isEmpty()) System.out.println("Sin ventas en el periodo.");
        for (Map.Entry<String, Double> e : reporte.entrySet()) System.out.printf("%s: $%.0f%n", e.getKey(), e.getValue());
    }

    private static void reporteDeportivo(Administrador admin) {
        System.out.println(admin.generarReporteDeportivo(socios()));
        System.out.println("--- Resultados de partidos ---");
        for (PartidoInterno p : admin.historialPartidosInternos(sistema))
            System.out.printf("[Interno] %s vs %s: %s%n", p.getEquipoLocal().getNombre(), p.getEquipoVisitante().getNombre(), p.getMarcadorFinal());
        for (PartidoCompetencia p : admin.historialPartidosCompetencia(sistema))
            if (p.isFinalizado()) System.out.printf("[Competencia] %s: %s%n", nombresEquipos(p), p.getMarcador());
        System.out.println("--- Rankings de disciplinas individuales ---");
        for (DisciplinaIndividual d : sistema.getDisciplinas()) {
            System.out.println(nombreDisciplina(d) + ":");
            List<InscripcionDisciplina> orden = new ArrayList<>();
            for (InscripcionDisciplina i : d.getInscripciones()) if (i.isActiva()) orden.add(i);
            orden.sort((x, y) -> Integer.compare(y.getRanking(), x.getRanking()));
            for (InscripcionDisciplina i : orden) System.out.printf("   %-25s %-12s %d%n", i.getSocio().getNombre(), i.getNivel(), i.getRanking());
        }
    }

    // ===================================================================== UTILIDADES DE DOMINIO
    private static void cambiarPassword(Usuario usuario) {
        String actual = texto("Contraseña actual: ");
        String nueva = texto("Nueva contraseña: ");
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

    private static List<Socio> socios() {
        List<Socio> lista = new ArrayList<>();
        for (Usuario u : sistema.getUsuarios().values()) if (u instanceof Socio) lista.add((Socio) u);
        return lista;
    }

    private static List<Empleado> empleados() {
        List<Empleado> lista = new ArrayList<>();
        for (Usuario u : sistema.getUsuarios().values()) if (u instanceof Empleado) lista.add((Empleado) u);
        return lista;
    }

    private static List<Entrenador> entrenadores() {
        List<Entrenador> lista = new ArrayList<>();
        for (Usuario u : sistema.getUsuarios().values()) if (u instanceof Entrenador) lista.add((Entrenador) u);
        return lista;
    }

    private static List<Equipo> equiposActivos() {
        List<Equipo> lista = new ArrayList<>();
        for (Equipo e : sistema.getEquipos()) if (e.isActivo()) lista.add(e);
        return lista;
    }

    private static String etiquetaEquipo(Equipo e) {
        return e.getNombre() + " (" + e.getDeporte().getNombre() + ", " + e.getCategoria().getNombre() + ", "
                + e.getInscripcionesActivas().size() + "/" + e.getCupoMaximo() + " jugadores)";
    }

    private static String nombresEquipos(PartidoCompetencia p) {
        if (p.getEquipos().size() < 2) return "partido sin definir";
        return p.getEquipos().get(0).getNombre() + " vs " + p.getEquipos().get(1).getNombre();
    }

    private static String etiquetaPractica(PartidoPractica p) {
        StringBuilder sb = new StringBuilder(nombreDisciplina(p.getDisciplina())).append(" (").append(p.getModalidad()).append("): ");
        for (int i = 0; i < p.getParticipantes().size(); i++) {
            if (i > 0) sb.append(" vs ");
            sb.append(p.getParticipantes().get(i).getSocio().getNombre()).append(" [").append(p.getParticipantes().get(i).getNivel()).append("]");
        }
        return sb.toString();
    }

    private static String nombreDisciplina(DisciplinaIndividual d) { return d.getClass().getSimpleName(); }

    private static String etiquetaTurno(Turno t) {
        return t.getDia() + " " + t.getHoraInicio() + " - " + t.getHoraFin();
    }

    /** Reutiliza el deporte que ya existe en el sistema, porque los equipos comparan deporte y categoría por identidad. */
    private static DeporteConjunto deporte(String nombre) {
        List<DeporteConjunto> conocidos = new ArrayList<>();
        for (Equipo e : sistema.getEquipos()) if (e.getDeporte() != null) conocidos.add(e.getDeporte());
        for (Competencia c : sistema.getCompetencias()) conocidos.add(c.getDeporte());
        for (DeporteConjunto d : conocidos) if (sinTildes(d.getNombre()).equals(sinTildes(nombre))) return d;
        if (sinTildes(nombre).equals("futbol")) return new Futbol("Fútbol", 7);
        if (sinTildes(nombre).equals("baloncesto")) return new Baloncesto("Baloncesto", 5);
        return new Voleibol("Voleibol", 6);
    }

    private static Categoria categoria(String nombre) {
        List<Categoria> conocidas = new ArrayList<>();
        for (Equipo e : sistema.getEquipos()) if (e.getCategoria() != null) conocidas.add(e.getCategoria());
        for (Competencia c : sistema.getCompetencias()) conocidas.add(c.getCategoria());
        for (Categoria c : conocidas) if (c.getNombre().equalsIgnoreCase(nombre)) return c;
        switch (nombre) {
            case "Sub-13": return new Categoria("Sub-13", 0, 12);
            case "Sub-15": return new Categoria("Sub-15", 13, 14);
            case "Sub-17": return new Categoria("Sub-17", 15, 17);
            default: return new Categoria("Mayores", 18, 150);
        }
    }

    private static String sinTildes(String s) {
        return java.text.Normalizer.normalize(s.toLowerCase(), java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    // ===================================================================== ENTRADA POR CONSOLA
    private static String texto(String mensaje) {
        System.out.print(mensaje);
        return in.hasNextLine() ? in.nextLine().trim() : "salir";
    }

    private static int entero(String mensaje) {
        String t = texto(mensaje);
        try { return Integer.parseInt(t); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Debe ingresar un número entero."); }
    }

    private static double decimal(String mensaje) { return parsearDecimal(texto(mensaje)); }

    private static double parsearDecimal(String t) {
        try { return Double.parseDouble(t.replace(',', '.')); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Debe ingresar un número."); }
    }

    private static boolean confirmar(String mensaje) {
        String r = texto(mensaje + " (s/n): ").toLowerCase();
        return r.startsWith("s") || r.startsWith("y");
    }

    private static LocalDate fecha(String mensaje) { return LocalDate.parse(texto(mensaje)); }

    private static LocalDateTime fechaHora(String mensaje) { return LocalDateTime.parse(texto(mensaje).replace(' ', 'T')); }

    private static LocalTime hora(String mensaje) { return LocalTime.parse(texto(mensaje)); }

    private static DayOfWeek dia(String texto) {
        switch (sinTildes(texto.trim())) {
            case "lunes": return DayOfWeek.MONDAY;
            case "martes": return DayOfWeek.TUESDAY;
            case "miercoles": return DayOfWeek.WEDNESDAY;
            case "jueves": return DayOfWeek.THURSDAY;
            case "viernes": return DayOfWeek.FRIDAY;
            case "sabado": return DayOfWeek.SATURDAY;
            case "domingo": return DayOfWeek.SUNDAY;
            default: throw new IllegalArgumentException("Día no reconocido (use lunes, martes, ...).");
        }
    }

    private static <T> T elegir(String titulo, List<T> opciones, Function<T, String> etiqueta) {
        if (opciones.isEmpty()) throw new IllegalStateException("No hay opciones disponibles. (" + titulo + ")");
        System.out.println(titulo);
        for (int i = 0; i < opciones.size(); i++) System.out.printf("  %d) %s%n", i + 1, etiqueta.apply(opciones.get(i)));
        int n = entero("Opción: ");
        if (n < 1 || n > opciones.size()) throw new IllegalArgumentException("Opción fuera de rango.");
        return opciones.get(n - 1);
    }

    // ===================================================================== DATOS DE DEMOSTRACIÓN
    /** Crea solo lo que falta, para poder ejecutarse también sobre datos ya guardados. */
    private static void crearDatosDemo() {
        if (sistema.buscarUsuario("admin") == null) sistema.registrarUsuario(new Administrador("admin", "admin123", "Administrador de demostración"));
        Socio demo = (Socio) sistema.buscarUsuario("socio.demo");
        if (demo == null) { demo = new Socio("socio.demo", "demo123", "Socio de demostración", LocalDate.of(2000, 1, 1)); sistema.registrarUsuario(demo); }
        if (sistema.buscarUsuario("socio.demo2") == null) sistema.registrarUsuario(new Socio("socio.demo2", "demo123", "Segundo socio de demostración", LocalDate.of(2001, 5, 20)));
        if (sistema.buscarUsuario("entrenador.demo") == null) sistema.registrarUsuario(new Entrenador("entrenador.demo", "entre123", "Entrenador de demostración", "ENTRE15"));
        if (sistema.buscarUsuario("entrenador2.demo") == null) sistema.registrarUsuario(new Entrenador("entrenador2.demo", "entre123", "Segundo entrenador", "ENTRE2"));
        if (sistema.buscarUsuario("fisio.demo") == null) sistema.registrarUsuario(new Fisioterapeuta("fisio.demo", "fisio123", "Fisioterapeuta de demostración", "FISIO15"));
        for (String login : new String[] { "entrenador.demo", "entrenador2.demo", "fisio.demo" }) {
            Empleado e = (Empleado) sistema.buscarUsuario(login);
            if (e.getTurnos().isEmpty()) e.agregarTurno(new Turno(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        }
        if (sistema.getEquipos().isEmpty()) {
            Equipo equipo = new Equipo("Seneca Fútbol Demo", new Futbol("Fútbol", 7), new Categoria("Abierta", 0, 150), 12);
            sistema.registrarEquipo(equipo);
            equipo.inscribirJugador(demo, "delantero", 10);
        }
        if (sistema.getDisciplinas().isEmpty()) {
            Tenis tenis = new Tenis(2);
            tenis.registrarInstalacion(new CanchaTenis(2, 4, "Cancha de tenis 1", false));
            Padel padel = new Padel(2);
            padel.registrarInstalacion(new CanchaPadel(4, 4, "Cancha de pádel 1", true));
            TenisMesa mesa = new TenisMesa(2);
            mesa.registrarInstalacion(new MesaTenisMesa(2, 4, "Mesa de tenis de mesa 1", true));
            for (DisciplinaIndividual d : new DisciplinaIndividual[] { tenis, padel, mesa })
                for (Instalacion i : d.getInstalaciones()) sistema.registrarInstalacion(i);
        }
        boolean hayCanchaConjunto = false;
        for (Instalacion i : sistema.getInstalaciones()) if (i instanceof CanchaConjunto) hayCanchaConjunto = true;
        if (!hayCanchaConjunto) sistema.registrarInstalacion(new CanchaConjunto(10, 22, "Cancha de conjunto 1", false));
        agregarProducto(new ProductoTienda("Balón demo", 50000, "deporte"), 5, "tienda");
        agregarProducto(new ProductoTienda("Raqueta de tenis demo", 180000, "raquetas"), 8, "tienda");
        agregarProducto(new Bebida("Agua demo", 3000, false), 30, "cafeteria");
        agregarProducto(new Bebida("Café demo", 4000, true), 30, "cafeteria");
        agregarProducto(new Snack("Barra de maní demo", 3500, new ArrayList<>(Collections.singletonList("maní"))), 20, "cafeteria");
    }

    private static void agregarProducto(Producto producto, int cantidad, String ubicacion) {
        for (Producto p : sistema.getProductos()) if (p.getNombre().equalsIgnoreCase(producto.getNombre())) return;
        producto.agregarExistencia(new Existencia(cantidad, ubicacion));
        sistema.registrarProducto(producto);
    }
}