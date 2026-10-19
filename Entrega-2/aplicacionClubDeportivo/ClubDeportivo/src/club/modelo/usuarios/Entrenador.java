package club.modelo.usuarios;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import club.modelo.competencias.Equipo;
import club.modelo.competencias.EstadisticaJugadorPartido;
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

    public Entrenador(String login, String password, String nombre, String codigoDescuento) {
        super(login, password, nombre, codigoDescuento);
    }


    public SesionEntrenamiento planearSesion(LocalDateTime fechaHora, int duracionMin, String tipo, 
                                             Instalacion inst, List<Socio> convocados) {
        if (convocados == null || convocados.isEmpty()) throw new IllegalArgumentException("La sesión debe tener convocados.");
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

    public PartidoInterno registrarPartidoInterno(Equipo a, Equipo b, LocalDateTime fecha, 
                                                  int golesA, int golesB, List<EstadisticaJugadorPartido> stats) {
        
        PartidoInterno partido = new PartidoInterno(a, b, fecha);
        if (stats == null) throw new IllegalArgumentException("La lista de estadísticas es obligatoria.");
        partido.validarPuedeJugarse();
        partido.validarEstadisticas(stats);
        partido.registrarMarcador(golesA, golesB);
        
        for (EstadisticaJugadorPartido stat : stats) {
            partido.registrarEstadistica(stat);
        }
        return partido;
        
    }

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
