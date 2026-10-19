package club.modelo.entrenamiento;

import java.util.ArrayList;
import java.util.List;
import club.modelo.instalaciones.Reserva;
import club.modelo.usuarios.Entrenador;

public class PartidoPractica implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String modalidad;

    private Reserva reserva;
    private DisciplinaIndividual disciplina;
    private List<EstadisticaIndividual> estadisticas;
    private List<InscripcionDisciplina> participantes = new ArrayList<>();
    private Entrenador entrenadorParticipante;
    private boolean resultadoRegistrado;

    public PartidoPractica(String modalidad, Reserva reserva, DisciplinaIndividual disciplina) {
        this.modalidad = modalidad;
        this.reserva = reserva;
        this.disciplina = disciplina;
        this.estadisticas = new ArrayList<>();
    }


    public void validarNiveles() {
        if (participantes.size() >= 2) {
            InscripcionDisciplina inscA = participantes.get(0);
            InscripcionDisciplina inscB = participantes.get(1);
            if (inscA.getDisciplina() != disciplina || inscB.getDisciplina() != disciplina) {
                throw new IllegalArgumentException("Los jugadores no están inscritos en la disciplina del partido.");
            }
            if (entrenadorParticipante == null && !inscA.puedeJugarCon(inscB)) {
                throw new IllegalArgumentException("Los niveles de los participantes no son compatibles.");
            }
            return;
        }
        if (estadisticas.size() < 2) {
            throw new IllegalStateException("Se deben registrar dos participantes antes de validar el partido.");
        }
        InscripcionDisciplina inscA = estadisticas.get(0).getInscripcion();
        InscripcionDisciplina inscB = estadisticas.get(1).getInscripcion();

        if (inscA != null && inscB != null && !inscA.puedeJugarCon(inscB)) {
            throw new IllegalArgumentException("Los niveles de los participantes no son compatibles para un partido de práctica.");
        }
    }

    public void agregarParticipante(InscripcionDisciplina participante) {
        if (participante == null || !participante.isActiva()) throw new IllegalArgumentException("La inscripción debe estar activa.");
        if (!participantes.contains(participante)) participantes.add(participante);
    }

    public void registrarResultado(EstadisticaIndividual statsA, EstadisticaIndividual statsB) {
        if (resultadoRegistrado) throw new IllegalStateException("El resultado ya fue registrado.");
        if (statsA == null || statsB == null) throw new IllegalArgumentException("Se requieren estadísticas de ambos jugadores.");
        validarNiveles();
        if (participantes.size() < 2) throw new IllegalStateException("Faltan participantes para registrar el resultado.");
        if (statsA.getInscripcion() == null) statsA.setInscripcion(participantes.get(0));
        if (statsB.getInscripcion() == null) statsB.setInscripcion(participantes.get(1));
        if (statsA.getInscripcion() != participantes.get(0) || statsB.getInscripcion() != participantes.get(1)) {
            throw new IllegalArgumentException("Las estadísticas deben corresponder a los participantes del partido.");
        }
        if (statsA.getClass() != statsB.getClass()) throw new IllegalArgumentException("Los dos resultados deben ser del mismo deporte.");
        boolean tipoCorrecto = (disciplina instanceof TenisMesa && statsA instanceof EstadisticaTenisMesa)
                || ((disciplina instanceof Tenis || disciplina instanceof Padel) && statsA instanceof EstadisticaTenisPadel);
        if (!tipoCorrecto) throw new IllegalArgumentException("Las estadísticas no corresponden a la disciplina.");
        if (statsA != null) {
            estadisticas.add(statsA);
            statsA.getInscripcion().getSocio().registrarEstadisticaIndividual(statsA);
        }
        if (statsB != null) {
            estadisticas.add(statsB);
            statsB.getInscripcion().getSocio().registrarEstadisticaIndividual(statsB);
        }
        int resultado = Integer.compare(statsA.getSets(), statsB.getSets());
        if (resultado == 0) resultado = Integer.compare(statsA.puntajeDesempate(), statsB.puntajeDesempate());
        int rankingA = statsA.getInscripcion().getRanking();
        int rankingB = statsB.getInscripcion().getRanking();
        statsA.getInscripcion().actualizarRanking(resultado > 0 ? 1.0 : resultado == 0 ? 0.5 : 0.0, rankingB);
        statsB.getInscripcion().actualizarRanking(resultado < 0 ? 1.0 : resultado == 0 ? 0.5 : 0.0, rankingA);
        resultadoRegistrado = true;
    }

    // --- GETTERS (Encapsulamiento seguro sin setters masivos) ---

    public String getModalidad() { return modalidad; }

    public void setModalidad(String modalidad) { this.modalidad = modalidad; }

    public Reserva getReserva() { return reserva; }

    public void setReserva(Reserva reserva) { this.reserva = reserva; }

    public DisciplinaIndividual getDisciplina() { return disciplina; }

    public void setDisciplina(DisciplinaIndividual disciplina) { this.disciplina = disciplina; }

    public List<EstadisticaIndividual> getEstadisticas() { return estadisticas; }

    public List<InscripcionDisciplina> getParticipantes() { return participantes; }
    public Entrenador getEntrenadorParticipante() { return entrenadorParticipante; }
    public void setEntrenadorParticipante(Entrenador entrenador) { this.entrenadorParticipante = entrenador; }
    public boolean isResultadoRegistrado() { return resultadoRegistrado; }
}
