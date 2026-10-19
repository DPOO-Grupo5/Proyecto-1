package club.modelo.competencias;

import java.util.ArrayList;
import java.util.List;

public class Competencia implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String nombre;
    private String organizador;
    private String temporada;
    private DeporteConjunto deporte;
    private Categoria categoria;
    private Formato formato;
    private List<EquipoCompetencia> equipos = new ArrayList<>();
    private List<InscripcionCompetencia> inscripciones = new ArrayList<>();
    private List<PartidoCompetencia> partidos = new ArrayList<>();

    public Competencia(String nombre, String organizador, String temporada,
                       DeporteConjunto deporte, Categoria categoria, Formato formato) {
        setNombre(nombre);
        setOrganizador(organizador);
        setTemporada(temporada);
        setDeporte(deporte);
        setCategoria(categoria);
        setFormato(formato);
    }

    public void agregarEquipo(EquipoCompetencia equipo) {
        if (equipo == null) throw new IllegalArgumentException("El equipo es obligatorio.");
        if (equipos.contains(equipo)) throw new IllegalArgumentException("El equipo ya participa en esta competencia.");
        if (equipo instanceof Equipo) {
            Equipo club = (Equipo) equipo;
            if (!club.isActivo() || club.getDeporte() != deporte || club.getCategoria() != categoria) {
                throw new IllegalArgumentException("El equipo no está activo o no corresponde al deporte y categoría.");
            }
            int elegibles = 0;
            for (InscripcionEquipo inscripcion : club.getInscripcionesActivas()) {
                if (!inscripcion.getSocio().tieneSancionVigente()) elegibles++;
            }
            if (elegibles < deporte.getMinJugadores()) throw new IllegalStateException("El equipo no cuenta con la nómina mínima disponible.");
        } else if (!(equipo instanceof InstitucionRival)) {
            throw new IllegalArgumentException("Solo se admiten equipos del club o instituciones rivales.");
        }
        equipos.add(equipo);
        inscripciones.add(new InscripcionCompetencia(this, equipo));
    }

    public void generarCalendario() {
        if (equipos.size() < 2) throw new IllegalStateException("Se requieren al menos dos equipos para generar el calendario.");
        formato.generarEncuentros(new ArrayList<>(equipos));
        partidos.clear();
        if (formato instanceof FormatoLiga) partidos.addAll(((FormatoLiga) formato).getEncuentros());
        else if (formato instanceof FormatoPorFases) partidos.addAll(((FormatoPorFases) formato).getEncuentros());
        for (PartidoCompetencia partido : partidos) partido.setCompetencia(this);
    }

    public void registrarPartido(PartidoCompetencia partido) {
        if (partido == null || !partidos.contains(partido)) throw new IllegalArgumentException("El partido debe pertenecer al calendario de esta competencia.");
        partido.setCompetencia(this);
    }

    public List<PartidoCompetencia> generarFaseEliminacion(int clasificadosPorGrupo) {
        if (!(formato instanceof FormatoPorFases)) throw new IllegalStateException("La competencia no tiene formato por fases.");
        List<PartidoCompetencia> nuevos = ((FormatoPorFases) formato).generarEliminacionDesdeGrupos(clasificadosPorGrupo);
        for (PartidoCompetencia partido : nuevos) {
            partido.setCompetencia(this);
            if (!partidos.contains(partido)) partidos.add(partido);
        }
        return nuevos;
    }

    public RondaEliminacion avanzarFaseEliminacion() {
        if (!(formato instanceof FormatoPorFases) || ((FormatoPorFases) formato).getFaseEliminacion() == null) throw new IllegalStateException("La competencia no tiene fase de eliminación.");
        RondaEliminacion nueva = ((FormatoPorFases) formato).getFaseEliminacion().avanzarGanadores();
        if (nueva == null || nueva.getPartidos().isEmpty()) return null;
        for (PartidoCompetencia partido : nueva.getPartidos()) {
            partido.setCompetencia(this);
            if (!partidos.contains(partido)) partidos.add(partido);
        }
        return nueva;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { if (nombre == null || nombre.trim().isEmpty()) throw new IllegalArgumentException("El nombre es obligatorio."); this.nombre = nombre.trim(); }
    public String getOrganizador() { return organizador; }
    public void setOrganizador(String organizador) { if (organizador == null || organizador.trim().isEmpty()) throw new IllegalArgumentException("El organizador es obligatorio."); this.organizador = organizador.trim(); }
    public String getTemporada() { return temporada; }
    public void setTemporada(String temporada) { if (temporada == null || temporada.trim().isEmpty()) throw new IllegalArgumentException("La temporada es obligatoria."); this.temporada = temporada.trim(); }
    public DeporteConjunto getDeporte() { return deporte; }
    public void setDeporte(DeporteConjunto deporte) { if (deporte == null) throw new IllegalArgumentException("El deporte es obligatorio."); this.deporte = deporte; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { if (categoria == null) throw new IllegalArgumentException("La categoría es obligatoria."); this.categoria = categoria; }
    public Formato getFormato() { return formato; }
    public void setFormato(Formato formato) { if (formato == null) throw new IllegalArgumentException("El formato es obligatorio."); this.formato = formato; }
    public List<EquipoCompetencia> getEquipos() { return equipos; }
    public List<InscripcionCompetencia> getInscripciones() { return inscripciones; }
    public List<PartidoCompetencia> getPartidos() { return partidos; }
}
