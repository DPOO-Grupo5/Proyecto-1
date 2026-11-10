package club.modelo.usuarios;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import club.modelo.competencias.Equipo;
import club.modelo.competencias.EstadisticaJugadorPartido;
import club.modelo.competencias.PartidoCompetencia;
import club.modelo.competencias.PartidoInterno;
import club.modelo.entrenamiento.Asistencia;
import club.modelo.entrenamiento.DisciplinaIndividual;
import club.modelo.entrenamiento.SesionEntrenamiento;
import club.modelo.instalaciones.Instalacion;
import club.modelo.instalaciones.Reserva;

public class Entrenador extends Empleado implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private List<SesionEntrenamiento> sesiones = new ArrayList<>();
    private List<DisciplinaIndividual> disciplinas = new ArrayList<>();
    private List<Reserva> reservas = new ArrayList<>();
    private List<PartidoInterno> partidosProgramados = new ArrayList<>();

    public Entrenador(String login, String password, String nombre, String codigoDescuento) {
        super(login, password, nombre, codigoDescuento);
    }


    public SesionEntrenamiento planearSesion(LocalDateTime fechaHora, int duracionMin, String tipo, 
                                             Instalacion inst, List<Socio> convocados) {
        if (convocados == null || convocados.isEmpty()) throw new IllegalArgumentException("La sesión debe tener convocados.");
        if (fechaHora == null || !fechaHora.isAfter(java.time.LocalDateTime.now())) throw new IllegalArgumentException("La fecha de la sesión debe ser futura.");
        if (duracionMin <= 0) throw new IllegalArgumentException("La duración debe ser mayor que 0.");
        java.time.LocalDateTime finNueva = fechaHora.plusMinutes(duracionMin);
        for (SesionEntrenamiento otra : sesiones)
            if (otra.seTraslapaCon(fechaHora, finNueva)) throw new IllegalStateException("Ya tienes otra sesión en ese horario.");
        SesionEntrenamiento sesion = new SesionEntrenamiento(fechaHora, duracionMin, tipo, inst, this);
        sesion.validarDisponibilidad();
        sesion.validarConvocados(convocados);
        for(Socio s : convocados) {
            sesion.agregarConvocado(s);
        }
        this.sesiones.add(sesion);
        inst.registrarSesion(sesion);
        return sesion;
   
    }

    public Asistencia registrarAsistencia(SesionEntrenamiento s, Socio j, String estado, Integer nota, String comentario) {
       
        if (s == null || s.getEntrenador() != this) throw new IllegalArgumentException("La sesión debe pertenecer a este entrenador.");
        if (!s.haFinalizado(java.time.LocalDateTime.now())) throw new IllegalStateException("La sesión aún no ha finalizado.");
        Asistencia asistencia = new Asistencia(estado, nota, comentario, j, s);

        if (s != null) {
            s.registrarAsistencia(asistencia);
        }
        if (j != null && j.getAsistencias() != null) {
            j.getAsistencias().add(asistencia);
        }
        return asistencia;
    }
    public void asignarNivel(Socio s, DisciplinaIndividual d, String nivel) {
        if (s == null || d == null) throw new IllegalArgumentException("Socio y disciplina son obligatorios.");
        for (club.modelo.entrenamiento.InscripcionDisciplina insc : s.getInscripcionesDisciplina()) {
            if (insc.getDisciplina() == d && insc.isActiva()) { insc.cambiarNivel(nivel); return; }
        }
        throw new IllegalArgumentException("El socio no tiene una inscripción activa en la disciplina.");
    }
    
    public PartidoInterno programarPartidoInterno(Equipo a, Equipo b, LocalDateTime fecha) {
        if (fecha == null || !fecha.isAfter(LocalDateTime.now())) throw new IllegalArgumentException("La fecha del partido debe ser futura.");
        PartidoInterno partido = new PartidoInterno(a, b, fecha);
        partido.validarPuedeJugarse();
        partido.setProgramadoPor(this);
        partidosProgramados.add(partido);
        return partido;
    }

    public void definirAlineacion(PartidoInterno partido, List<Socio> jugadores) {
        if (partido == null || partido.getProgramadoPor() != this) throw new IllegalArgumentException("El partido no fue programado por ti.");
        partido.definirAlineacion(jugadores);
    }

    public void registrarResultadoPartidoInterno(PartidoInterno partido, int golesA, int golesB, List<EstadisticaJugadorPartido> stats) {
        if (partido == null || partido.getProgramadoPor() != this) throw new IllegalArgumentException("El partido no fue programado por ti.");
        if (partido.isFinalizado()) throw new IllegalStateException("El partido ya tiene resultado.");
        if (partido.getFechaHora().isAfter(LocalDateTime.now())) throw new IllegalStateException("El partido aún no se ha disputado.");
        partido.validarEstadisticas(stats);
        partido.registrarMarcador(golesA, golesB);
        for (EstadisticaJugadorPartido s : stats) partido.registrarEstadistica(s);
    }
    
    public void registrarResultadoCompetencia(PartidoCompetencia partido, int local, int visitante, List<EstadisticaJugadorPartido> stats) {
        if (partido == null || partido.getEntrenadorResponsable() != this)
            throw new IllegalArgumentException("No eres el entrenador responsable de este partido.");
        partido.validarPuedeJugarse();
        partido.registrarMarcador(local, visitante);
        if (stats != null) for (EstadisticaJugadorPartido s : stats) partido.agregarEstadistica(s);
    }

    public List<PartidoInterno> getPartidosProgramados() { return partidosProgramados; }

    public String consultarAnalitica(Socio j) {
        if (j == null) throw new IllegalArgumentException("El socio es obligatorio.");
        StringBuilder reporte = new StringBuilder();
        reporte.append("=== Analítica del Jugador: ").append(j.getLogin()).append(" ===\n");
        reporte.append("Porcentaje de asistencia: ").append(j.getPorcentajeAsistencia()).append("%\n");
        reporte.append("Partidos oficiales: ").append(j.getEstadisticas().size()).append("\n");
        reporte.append("Partidos de práctica: ").append(j.getPartidosPractica().size()).append("\n");
        // Aquí se agregarán más detalles del rendimiento cuando completemos las estadísticas
        return reporte.toString();
    }

    // --- GETTERS (Se eliminaron los SETTERS por encapsulamiento) ---

    public List<SesionEntrenamiento> getSesiones() { return sesiones; }

    public List<DisciplinaIndividual> getDisciplinas() { return disciplinas; }

    public List<Reserva> getReservas() { return reservas; }
}
