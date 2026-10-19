package club.modelo.competencias;

import java.time.LocalDate;

public class InscripcionCompetencia implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Competencia competencia;
    private EquipoCompetencia equipo;
    private LocalDate fechaInscripcion = LocalDate.now();
    private boolean activa = true;

    public InscripcionCompetencia(Competencia competencia, EquipoCompetencia equipo) {
        if (competencia == null || equipo == null) throw new IllegalArgumentException("La competencia y el equipo son obligatorios.");
        this.competencia = competencia;
        this.equipo = equipo;
    }

    public void retirar() { activa = false; }
    public Competencia getCompetencia() { return competencia; }
    public EquipoCompetencia getEquipo() { return equipo; }
    public LocalDate getFechaInscripcion() { return fechaInscripcion; }
    public boolean isActiva() { return activa; }
}
