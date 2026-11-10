package club.modelo.usuarios;

import java.time.LocalDate;
import club.modelo.competencias.PartidoCompetencia;
import club.modelo.competencias.PartidoInterno;
import club.modelo.entrenamiento.SesionEntrenamiento;
import club.modelo.instalaciones.Instalacion;
import club.modelo.instalaciones.Reserva;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import club.modelo.competencias.Categoria;
import club.modelo.competencias.Competencia;
import club.modelo.competencias.DeporteConjunto;
import club.modelo.competencias.Equipo;
import club.modelo.competencias.Formato;
import club.modelo.competencias.InscripcionEquipo;
import club.modelo.competencias.InstitucionRival;
import club.model.SistemaClub;
import club.modelo.tienda.Existencia;
import club.modelo.tienda.Producto;
import club.modelo.tienda.SugerenciaProducto;
import club.modelo.tienda.Transaccion;

public class Administrador extends Usuario implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private List<SugerenciaProducto> sugerenciasAprobadas = new ArrayList<>();
    private List<SolicitudCambioTurno> solicitudesAprobadas = new ArrayList<>();
    private List<Producto> productosCatalogo = new ArrayList<>();

    public Administrador(String login, String password, String nombre) { super(login, password, nombre); }

    public void crearUsuario(Usuario usuario, List<Usuario> existentes) {
        if (usuario == null || existentes == null) throw new IllegalArgumentException("El usuario y el registro son obligatorios.");
        for (Usuario existente : existentes) {
            if (existente.getLogin().equalsIgnoreCase(usuario.getLogin())) throw new IllegalArgumentException("El login ya está en uso.");
        }
        existentes.add(usuario);
    }

    public void crearUsuario(Usuario usuario, SistemaClub sistema) {
        if (sistema == null) throw new IllegalArgumentException("El sistema es obligatorio.");
        sistema.registrarUsuario(usuario);
    }

    public void darDeBajaUsuario(Usuario usuario, SistemaClub sistema) {
        if (sistema == null || usuario == null) throw new IllegalArgumentException("El usuario y el sistema son obligatorios.");
        sistema.darDeBajaUsuario(usuario.getLogin());
    }

    public Equipo crearEquipo(String nombre, DeporteConjunto deporte, Categoria categoria, int cupo) {
        return new Equipo(nombre, deporte, categoria, cupo);
    }

    public Equipo crearEquipo(String nombre, DeporteConjunto deporte, Categoria categoria, int cupo, SistemaClub sistema) {
        if (sistema == null) throw new IllegalArgumentException("El sistema es obligatorio.");
        Equipo equipo = crearEquipo(nombre, deporte, categoria, cupo);
        sistema.registrarEquipo(equipo);
        return equipo;
    }


    public InscripcionEquipo inscribirJugadorEnEquipo(Socio socio, Equipo equipo, String posicion, int camiseta) {
        if (equipo == null) throw new IllegalArgumentException("El equipo es obligatorio.");
        return equipo.inscribirJugador(socio, posicion, camiseta);
    }

    public Competencia crearCompetencia(String nombre, String organizador, String temporada,
                                        DeporteConjunto deporte, Categoria categoria, Formato formato) {
        return new Competencia(nombre, organizador, temporada, deporte, categoria, formato);
    }

    public Competencia crearCompetencia(String nombre, String organizador, String temporada,
                                        DeporteConjunto deporte, Categoria categoria, Formato formato, SistemaClub sistema) {
        if (sistema == null) throw new IllegalArgumentException("El sistema es obligatorio.");
        Competencia competencia = crearCompetencia(nombre, organizador, temporada, deporte, categoria, formato);
        sistema.registrarCompetencia(competencia);
        return competencia;
    }

    public void moverInventario(Producto producto, String origen, String destino, int cantidad) {
        if (producto == null || origen == null || destino == null || origen.equalsIgnoreCase(destino) || cantidad <= 0) {
            throw new IllegalArgumentException("Producto, ubicaciones distintas y cantidad positiva son obligatorios.");
        }
        Existencia fuente = producto.buscarExistencia(origen);
        if (fuente == null || !fuente.hayDisponible(cantidad)) throw new IllegalStateException("No hay suficiente inventario en la ubicación de origen.");
        Existencia destinoExistente = producto.buscarExistencia(destino);
        fuente.disminuir(cantidad);
        if (destinoExistente == null) producto.agregarExistencia(new Existencia(cantidad, destino));
        else destinoExistente.aumentar(cantidad);
    }

    public void reabastecer(Producto producto, String ubicacion, int cantidad) {
        if (producto == null || ubicacion == null || cantidad <= 0) throw new IllegalArgumentException("Producto, ubicación y cantidad positiva son obligatorios.");
        Existencia existencia = producto.buscarExistencia(ubicacion);
        if (existencia == null) producto.agregarExistencia(new Existencia(cantidad, ubicacion));
        else existencia.aumentar(cantidad);
    }

    public void resolverSugerencia(SugerenciaProducto sugerencia, boolean aprobar) {
        if (sugerencia == null || sugerencia.getProducto() == null) throw new IllegalArgumentException("La sugerencia y el producto son obligatorios.");
        if (!"Pendiente".equalsIgnoreCase(sugerencia.getEstado())) throw new IllegalStateException("La sugerencia ya fue resuelta.");
        if (aprobar) {
            sugerencia.aprobar();
            if (!sugerenciasAprobadas.contains(sugerencia)) sugerenciasAprobadas.add(sugerencia);
            if (!productosCatalogo.contains(sugerencia.getProducto())) productosCatalogo.add(sugerencia.getProducto());
        } else sugerencia.rechazar();
    }

    public void resolverSugerencia(SugerenciaProducto sugerencia, boolean aprobar, SistemaClub sistema) {
        if (sistema == null || sugerencia == null || sugerencia.getProducto() == null) throw new IllegalArgumentException("El sistema, la sugerencia y el producto son obligatorios.");
        if (aprobar) {
            for (Producto existente : sistema.getProductos()) if (existente.getNombre().equalsIgnoreCase(sugerencia.getProducto().getNombre())) throw new IllegalArgumentException("Ya existe un producto con ese nombre.");
        }
        resolverSugerencia(sugerencia, aprobar);
        if (aprobar) sistema.registrarProducto(sugerencia.getProducto());
    }

    public void resolverCambioTurno(SolicitudCambioTurno solicitud, List<Empleado> empleados, boolean aprobar) {
        if (solicitud == null) throw new IllegalArgumentException("La solicitud es obligatoria.");
        if (!"Pendiente".equals(solicitud.getEstado())) throw new IllegalStateException("La solicitud ya fue resuelta.");
        if (!aprobar) { solicitud.rechazar(); return; }
        if (solicitud.getSolicitante() == null || !solicitud.cumpleCobertura(empleados)) {
            solicitud.rechazar();
            return;
        }
        solicitud.aprobar();
        solicitud.aplicarCambio();
        if (!solicitudesAprobadas.contains(solicitud)) solicitudesAprobadas.add(solicitud);
    }
    
    public double reporteVentas(SistemaClub sistema, String rubro, LocalDate desde, LocalDate hasta) {
        if (sistema == null || rubro == null || desde == null || hasta == null)
            throw new IllegalArgumentException("Sistema, rubro y periodo son obligatorios.");
        double total = 0.0;
        for (Usuario usuario : sistema.getUsuarios().values()) {
            for (Transaccion t : usuario.consultarHistorialCompras()) {
                LocalDate dia = t.getFechaHora().toLocalDate();
                if (t.isConfirmada() && t.getRubro().equalsIgnoreCase(rubro) && !dia.isBefore(desde) && !dia.isAfter(hasta))
                    total += t.getTotal();
            }
        }
        return total;
    }

    public String estadoInventario(SistemaClub sistema) {
        StringBuilder sb = new StringBuilder("--- Instalaciones ---\n");
        for (Instalacion i : sistema.getInstalaciones())
            sb.append(i.getNombre()).append(" | reservas: ").append(i.getReservas().size()).append('\n');
        sb.append("--- Artículos ---\n");
        for (Producto p : sistema.getProductos()) {
            sb.append(p.getNombre());
            for (Existencia e : p.getExistencias())
                sb.append(" | ").append(e.getUbicacionInventario()).append(": ").append(e.getCantidad());
            sb.append('\n');
        }
        return sb.toString();
    }

    public void asignarTurno(Empleado empleado, Turno turno) {
        if (empleado == null || turno == null) throw new IllegalArgumentException("El empleado y el turno son obligatorios.");
        empleado.agregarTurno(turno);
    }

    public Map<String, Double> generarReporteVentas(List<Transaccion> transacciones, String rubro,
                                                     String periodo, LocalDate fechaRef) {
        Map<String, Double> reporte = new HashMap<>();
        if (transacciones == null || periodo == null || fechaRef == null) return reporte;
        String alcance = periodo.trim().toLowerCase();
        for (Transaccion transaccion : transacciones) {
            if (transaccion == null || !transaccion.isConfirmada()) continue;
            if (rubro != null && !rubro.trim().isEmpty() && !rubro.equalsIgnoreCase(transaccion.getRubro())) continue;
            LocalDate fecha = transaccion.getFechaHora().toLocalDate();
            boolean coincide;
            if (alcance.equals("dia") || alcance.equals("diario")) coincide = fecha.equals(fechaRef);
            else if (alcance.equals("semana") || alcance.equals("semanal")) {
                WeekFields semanas = WeekFields.ISO;
                coincide = fecha.getYear() == fechaRef.getYear() && fecha.get(semanas.weekOfWeekBasedYear()) == fechaRef.get(semanas.weekOfWeekBasedYear());
            } else if (alcance.equals("mes") || alcance.equals("mensual")) coincide = fecha.getYear() == fechaRef.getYear() && fecha.getMonth() == fechaRef.getMonth();
            else if (alcance.equals("anio") || alcance.equals("año") || alcance.equals("anual")) coincide = fecha.getYear() == fechaRef.getYear();
            else throw new IllegalArgumentException("El periodo debe ser día, semana, mes o año.");
            if (coincide) reporte.merge(transaccion.getRubro(), transaccion.getTotal(), Double::sum);
        }
        return reporte;
    }
    
    public List<Empleado> listarEmpleados(SistemaClub sistema) {
        List<Empleado> empleados = new ArrayList<>();
        for (Usuario u : sistema.getUsuarios().values())
            if (u instanceof Empleado) empleados.add((Empleado) u);
        return empleados;
    }
    public PartidoCompetencia programarPartidoOficial(Competencia c, Equipo propio, InstitucionRival rival,
            LocalDate fecha, String escenario, Entrenador responsable) {
if (c == null || propio == null || rival == null || fecha == null || responsable == null || escenario == null || escenario.trim().isEmpty())
throw new IllegalArgumentException("Competencia, equipo, rival, fecha, escenario y entrenador responsable son obligatorios.");
if (!c.getEquipos().contains(propio)) throw new IllegalStateException("El equipo no está inscrito en la competencia.");
if (!c.getEquipos().contains(rival)) c.agregarEquipo(rival);
PartidoCompetencia partido = new PartidoCompetencia(fecha, null, escenario.trim());
partido.agregarEquipo(propio);
partido.agregarEquipo(rival);
partido.setEntrenadorResponsable(responsable);
c.getPartidos().add(partido);
partido.setCompetencia(c);
return partido;
}

    public List<SolicitudCambioTurno> consultarSolicitudesPendientes(List<Empleado> empleados) {
        List<SolicitudCambioTurno> pendientes = new ArrayList<>();
        for (Empleado e : empleados)
            for (SolicitudCambioTurno s : e.getSolicitudes())
                if ("Pendiente".equals(s.getEstado())) pendientes.add(s);
        return pendientes;
    }

    public String generarReporteDeportivo(List<Socio> socios) {
        StringBuilder reporte = new StringBuilder("--- Reporte Deportivo Global ---\n");
        if (socios == null) return reporte.toString();
        for (Socio socio : socios) {
            if (socio == null) continue;
            reporte.append(socio.getNombre()).append(" (").append(socio.getLogin()).append(")")
                    .append(" | asistencia: ").append(String.format(java.util.Locale.ROOT, "%.1f", socio.getPorcentajeAsistencia())).append("%")
                    .append(" | equipos activos: ").append(socio.getEquipos().size())
                    .append(" | partidos de práctica: ").append(socio.getPartidosPractica().size())
                    .append(" | sanciones vigentes: ").append(socio.getSancionesVigentes().size()).append('\n');
        }
        return reporte.toString();
    }

    public List<SugerenciaProducto> getSugerenciasAprobadas() { return sugerenciasAprobadas; }
    public List<SolicitudCambioTurno> getSolicitudesAprobadas() { return solicitudesAprobadas; }
    public List<Producto> getProductosCatalogo() { return productosCatalogo; }
    
    public void darDeBajaJugador(Equipo equipo, Socio socio, SistemaClub sistema) {
        if (equipo == null || socio == null || sistema == null) throw new IllegalArgumentException("Equipo, socio y sistema son obligatorios.");
        boolean activo = false;
        for (InscripcionEquipo i : equipo.getInscripcionesActivas()) if (i.getSocio() == socio) activo = true;
        if (!activo) throw new IllegalStateException("El jugador no está activo en el equipo.");
        for (Competencia c : sistema.getCompetencias())
            if (c.getEquipos().contains(equipo) && c.estaEnNomina(socio))
                throw new IllegalStateException("El jugador está en la nómina de la competencia " + c.getNombre() + ".");
        equipo.retirarJugador(socio);
    }

    public void darDeBajaEquipo(Equipo equipo, SistemaClub sistema) {
        if (equipo == null || sistema == null) throw new IllegalArgumentException("El equipo y el sistema son obligatorios.");
        for (Competencia c : sistema.getCompetencias())
            if (c.getEquipos().contains(equipo)) throw new IllegalStateException("El equipo está inscrito en la competencia " + c.getNombre() + ".");
        equipo.darDeBaja();
    }
    
    public void programarTurnos(List<Empleado> empleados, java.time.LocalTime apertura, java.time.LocalTime cierre) {
        if (empleados == null || apertura == null || cierre == null || !apertura.isBefore(cierre))
            throw new IllegalArgumentException("Empleados y horario de atención válidos son obligatorios.");
        for (java.time.DayOfWeek dia : java.time.DayOfWeek.values()) {
            for (java.time.LocalTime h = apertura; h.isBefore(cierre); h = h.plusMinutes(30)) {
                int entrenadores = 0, fisios = 0;
                for (Empleado e : empleados) {
                    for (Turno t : e.getTurnos()) {
                        if (t.cubre(dia, h)) {
                            if (e instanceof Entrenador) entrenadores++;
                            else if (e instanceof Fisioterapeuta) fisios++;
                            break;
                        }
                    }
                }
                if (entrenadores < 2 || fisios < 1)
                    throw new IllegalStateException("Cobertura insuficiente el " + dia + " a las " + h + ": " + entrenadores + " entrenadores y " + fisios + " fisioterapeutas.");
            }
        }
    }
    
    private void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) throw new IllegalArgumentException("El rango de fechas es inválido.");
    }

    public List<Reserva> historialReservas(SistemaClub sistema, LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        List<Reserva> lista = new ArrayList<>();
        for (Instalacion i : sistema.getInstalaciones())
            for (Reserva r : i.getReservas()) {
                LocalDate d = r.getFechaHoraInicio().toLocalDate();
                if (!d.isBefore(desde) && !d.isAfter(hasta)) lista.add(r);
            }
        return lista;
    }

    public List<SesionEntrenamiento> historialSesiones(SistemaClub sistema, LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        List<SesionEntrenamiento> lista = new ArrayList<>();
        for (Instalacion i : sistema.getInstalaciones())
            for (SesionEntrenamiento s : i.getSesiones()) {
                LocalDate d = s.getFechaHora().toLocalDate();
                if (!d.isBefore(desde) && !d.isAfter(hasta)) lista.add(s);
            }
        return lista;
    }

    public List<PartidoInterno> historialPartidosInternos(SistemaClub sistema, LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        List<PartidoInterno> lista = new ArrayList<>();
        for (Equipo e : sistema.getEquipos())
            for (PartidoInterno p : e.getPartidosInternos()) {
                LocalDate d = p.getFechaHora().toLocalDate();
                if (!lista.contains(p) && !d.isBefore(desde) && !d.isAfter(hasta)) lista.add(p);
            }
        return lista;
    }

    public List<PartidoCompetencia> historialPartidosCompetencia(SistemaClub sistema, LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        List<PartidoCompetencia> lista = new ArrayList<>();
        for (Competencia c : sistema.getCompetencias())
            for (PartidoCompetencia p : c.getPartidos())
                if (p.getFecha() != null && !p.getFecha().isBefore(desde) && !p.getFecha().isAfter(hasta)) lista.add(p);
        return lista;
    }
    
    public Transaccion registrarMensualidad(Socio socio, java.time.YearMonth mes, double valor) {
        if (socio == null || mes == null) throw new IllegalArgumentException("El socio y el mes son obligatorios.");
        if (valor <= 0) throw new IllegalArgumentException("El valor debe ser mayor que 0.");
        String nombre = "Mensualidad " + mes;
        for (Transaccion t : socio.getTransacciones())
            if (t.isConfirmada() && "mensualidad".equals(t.getRubro()) && !t.getDetalles().isEmpty()
                    && nombre.equals(t.getDetalles().get(0).getProducto().getNombre()))
                throw new IllegalStateException("El mes " + mes + " ya está pagado.");
        club.modelo.tienda.ProductoTienda concepto = new club.modelo.tienda.ProductoTienda(nombre, valor, "Mensualidad");
        concepto.agregarExistencia(new Existencia(1, "N/A"));
        Map<Producto, Integer> items = new HashMap<>();
        items.put(concepto, 1);
        Transaccion t = socio.iniciarTransaccion(items, "mensualidad");
        t.confirmar();
        return t;
    }
    
}
