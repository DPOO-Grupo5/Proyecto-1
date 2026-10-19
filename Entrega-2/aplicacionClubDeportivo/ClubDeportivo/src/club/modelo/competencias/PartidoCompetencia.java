package club.modelo.competencias;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import club.modelo.usuarios.Socio;

public class PartidoCompetencia implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private LocalDate fecha;
    private String marcador = "Pendiente";
    private String escenario;
    private int marcadorLocal;
    private int marcadorVisitante;
    private boolean finalizado;
    private Competencia competencia;
    private Grupo grupo;
    private List<EquipoCompetencia> equipos = new ArrayList<>();
    private List<EstadisticaJugadorPartido> estadisticas = new ArrayList<>();
    private Set<Socio> sancionadosEnEstePartido = new HashSet<>();

    public PartidoCompetencia(LocalDate fecha, String marcador, String escenario) {
        this.fecha = fecha;
        this.escenario = escenario;
        if (marcador != null && !marcador.trim().isEmpty() && !"pendiente".equalsIgnoreCase(marcador)) setMarcador(marcador);
    }

    public void agregarEquipo(EquipoCompetencia equipo) {
        if (equipo == null) throw new IllegalArgumentException("El equipo es obligatorio.");
        if (finalizado) throw new IllegalStateException("El partido ya finalizó.");
        if (equipos.contains(equipo)) return;
        if (equipos.size() >= 2) throw new IllegalStateException("Un partido solo admite dos participantes.");
        equipos.add(equipo);
    }

    public void validarPuedeJugarse() {
        if (equipos.size() != 2 || equipos.get(0) == equipos.get(1)) throw new IllegalStateException("El partido requiere dos equipos distintos.");
        if (competencia != null) {
            for (EquipoCompetencia equipo : equipos) if (!competencia.getEquipos().contains(equipo)) throw new IllegalStateException("El equipo no está inscrito en la competencia.");
        }
        for (EquipoCompetencia participante : equipos) {
            if (!(participante instanceof Equipo)) continue;
            Equipo equipo = (Equipo) participante;
            if (!equipo.isActivo() || equipo.getDeporte() == null) throw new IllegalStateException("El equipo debe estar activo y configurado.");
            if (competencia != null && (equipo.getDeporte() != competencia.getDeporte() || equipo.getCategoria() != competencia.getCategoria())) throw new IllegalStateException("El equipo no corresponde a esta competencia.");
            int disponibles = 0;
            for (InscripcionEquipo inscripcion : equipo.getInscripcionesActivas()) if (!inscripcion.getSocio().tieneSancionVigente()) disponibles++;
            if (disponibles < equipo.getDeporte().getMinJugadores()) throw new IllegalStateException("El equipo no cumple la nómina mínima disponible.");
        }
    }

    public void registrarMarcador(int local, int visitante) {
        if (finalizado) throw new IllegalStateException("El resultado ya fue registrado.");
        if (local < 0 || visitante < 0) throw new IllegalArgumentException("Los marcadores no pueden ser negativos.");
        validarPuedeJugarse();
        capturarSancionados(equipos.get(0));
        capturarSancionados(equipos.get(1));
        marcadorLocal = local;
        marcadorVisitante = visitante;
        marcador = local + " - " + visitante;
        finalizado = true;
        if (competencia != null && competencia.getFormato() instanceof FormatoLiga) ((FormatoLiga) competencia.getFormato()).actualizarPosiciones();
        if (grupo != null) grupo.actualizarPosiciones();
        consumirSuspensiones(equipos.get(0));
        consumirSuspensiones(equipos.get(1));
    }

    public EquipoCompetencia getGanador() {
        if (!finalizado || marcadorLocal == marcadorVisitante) return null;
        return marcadorLocal > marcadorVisitante ? equipos.get(0) : equipos.get(1);
    }

    public void agregarEstadistica(EstadisticaJugadorPartido estadistica) {
        if (!finalizado) throw new IllegalStateException("Se debe registrar el resultado antes de las estadísticas.");
        if (competencia == null || estadistica == null || estadistica.getJugador() == null) throw new IllegalArgumentException("Partido, competencia y jugador son obligatorios.");
        Socio jugador = estadistica.getJugador();
        if (sancionadosEnEstePartido.contains(jugador)) throw new IllegalArgumentException("Un jugador sancionado antes del partido no puede registrar participación.");
        if (!perteneceAEquipoParticipante(jugador)) throw new IllegalArgumentException("El jugador no pertenece a un equipo participante.");
        validarTipoEstadistica(estadistica);
        for (EstadisticaJugadorPartido existente : estadisticas) if (existente.getJugador() == jugador) throw new IllegalStateException("Ya hay estadísticas para este jugador.");
        estadistica.setPartidoCompetencia(this);
        estadisticas.add(estadistica);
        jugador.registrarEstadistica(estadistica);
    }

    private boolean perteneceAEquipoParticipante(Socio jugador) {
        for (EquipoCompetencia participante : equipos) if (participante instanceof Equipo) {
            for (InscripcionEquipo inscripcion : ((Equipo) participante).getInscripcionesActivas()) if (inscripcion.getSocio() == jugador) return true;
        }
        return false;
    }

    private void validarTipoEstadistica(EstadisticaJugadorPartido estadistica) {
        DeporteConjunto deporte = competencia.getDeporte();
        boolean coincide = (deporte instanceof Futbol && estadistica instanceof EstadisticaJugadorFutbol)
                || (deporte instanceof Baloncesto && estadistica instanceof EstadisticaJugadorBaloncesto)
                || (deporte instanceof Voleibol && estadistica instanceof EstadisticaJugadorVoleibol);
        if (!coincide) throw new IllegalArgumentException("La estadística no corresponde al deporte de la competencia.");
    }

    private void capturarSancionados(EquipoCompetencia participante) {
        if (!(participante instanceof Equipo)) return;
        for (InscripcionEquipo inscripcion : ((Equipo) participante).getInscripcionesActivas()) {
            Socio socio = inscripcion.getSocio();
            if (socio.tieneSancionVigente()) sancionadosEnEstePartido.add(socio);
        }
    }

    private void consumirSuspensiones(EquipoCompetencia participante) {
        if (!(participante instanceof Equipo)) return;
        for (InscripcionEquipo inscripcion : ((Equipo) participante).getInscripcionesActivas()) {
            for (Sancion sancion : inscripcion.getSocio().getSancionesVigentes()) sancion.consumirPartido();
        }
    }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getMarcador() { return marcador; }
    public void setMarcador(String marcador) {
        if (finalizado) throw new IllegalStateException("El resultado ya fue registrado.");
        if (marcador == null || !marcador.trim().matches("\\d+\\s*-\\s*\\d+")) throw new IllegalArgumentException("El marcador debe tener el formato 'n - n'.");
        String[] valores = marcador.trim().split("\\s*-\\s*");
        registrarMarcador(Integer.parseInt(valores[0]), Integer.parseInt(valores[1]));
    }
    public String getEscenario() { return escenario; }
    public void setEscenario(String escenario) { this.escenario = escenario; }
    public int getMarcadorLocal() { return marcadorLocal; }
    public int getMarcadorVisitante() { return marcadorVisitante; }
    public boolean isFinalizado() { return finalizado; }
    public Competencia getCompetencia() { return competencia; }
    public void setCompetencia(Competencia competencia) { if (this.competencia != null && this.competencia != competencia) throw new IllegalStateException("El partido ya pertenece a otra competencia."); this.competencia = competencia; }
    public Grupo getGrupo() { return grupo; }
    public void setGrupo(Grupo grupo) { if (this.grupo != null && this.grupo != grupo) throw new IllegalStateException("El partido ya pertenece a otro grupo."); this.grupo = grupo; }
    public List<EquipoCompetencia> getEquipos() { return equipos; }
    public void setEquipos(List<EquipoCompetencia> equipos) {
        if (equipos == null || equipos.size() > 2 || new HashSet<>(equipos).size() != equipos.size()) throw new IllegalArgumentException("El partido admite hasta dos participantes distintos.");
        this.equipos = new ArrayList<>(equipos);
    }
    public List<EstadisticaJugadorPartido> getEstadisticas() { return estadisticas; }
    public void setEstadisticas(List<EstadisticaJugadorPartido> estadisticas) { this.estadisticas = estadisticas == null ? new ArrayList<>() : estadisticas; }
}
