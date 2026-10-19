package club.modelo.competencias;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import club.modelo.usuarios.Socio;

public class PartidoInterno implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private LocalDateTime fechaHora;
    private String marcadorFinal;
    private int marcadorLocal;
    private int marcadorVisitante;
    private boolean finalizado;
    private boolean validado;
    private Equipo equipoLocal;
    private Equipo equipoVisitante;
    private List<EstadisticaJugadorPartido> estadisticas = new ArrayList<>();

    public PartidoInterno(Equipo equipoLocal, Equipo equipoVisitante, LocalDateTime fechaHora) {
        this.equipoLocal = equipoLocal;
        this.equipoVisitante = equipoVisitante;
        this.fechaHora = fechaHora;
    }

    public void validarPuedeJugarse() {
        if (equipoLocal == null || equipoVisitante == null || equipoLocal == equipoVisitante) throw new IllegalArgumentException("Se requieren dos equipos distintos.");
        if (fechaHora == null) throw new IllegalArgumentException("La fecha del partido es obligatoria.");
        validarEquipo(equipoLocal, "local");
        validarEquipo(equipoVisitante, "visitante");
        if (equipoLocal.getDeporte() != equipoVisitante.getDeporte() || equipoLocal.getCategoria() != equipoVisitante.getCategoria()) {
            throw new IllegalStateException("Los equipos deben pertenecer al mismo deporte y categoría.");
        }
        validado = true;
    }

    private void validarEquipo(Equipo equipo, String etiqueta) {
        if (!equipo.isActivo() || equipo.getDeporte() == null || equipo.getCategoria() == null) throw new IllegalStateException("El equipo " + etiqueta + " debe estar activo y configurado.");
        int disponibles = 0;
        for (InscripcionEquipo inscripcion : equipo.getInscripcionesActivas()) {
            if (!inscripcion.getSocio().tieneSancionVigente()) disponibles++;
        }
        if (disponibles < equipo.getDeporte().getMinJugadores()) throw new IllegalStateException("El equipo " + etiqueta + " no cumple con la nómina mínima disponible.");
    }

    public void registrarMarcador(int golesA, int golesB) {
        if (!validado) throw new IllegalStateException("Se debe validar el partido antes de registrar el marcador.");
        validarPuedeJugarse();
        if (golesA < 0 || golesB < 0) throw new IllegalArgumentException("El marcador no puede ser negativo.");
        if (finalizado) throw new IllegalStateException("El resultado ya fue registrado.");
        marcadorLocal = golesA;
        marcadorVisitante = golesB;
        marcadorFinal = golesA + " - " + golesB;
        finalizado = true;
        consumirSuspensiones(equipoLocal);
        consumirSuspensiones(equipoVisitante);
        if (!equipoLocal.getPartidosInternos().contains(this)) equipoLocal.getPartidosInternos().add(this);
        if (!equipoVisitante.getPartidosInternos().contains(this)) equipoVisitante.getPartidosInternos().add(this);
    }

    public void registrarEstadistica(EstadisticaJugadorPartido estadistica) {
        if (!finalizado) throw new IllegalStateException("El marcador debe cerrarse antes de registrar estadísticas.");
        validarEstadistica(estadistica);
        Socio jugador = estadistica.getJugador();
        estadistica.setPartidoInterno(this);
        estadisticas.add(estadistica);
        jugador.registrarEstadistica(estadistica);
        if (estadistica instanceof EstadisticaJugadorFutbol) registrarSancionDisciplinaria((EstadisticaJugadorFutbol) estadistica, jugador);
    }

    public void validarEstadisticas(List<EstadisticaJugadorPartido> estadisticas) {
        if (estadisticas == null) throw new IllegalArgumentException("La lista de estadísticas es obligatoria.");
        Set<Socio> jugadores = new HashSet<>();
        for (EstadisticaJugadorPartido estadistica : estadisticas) {
            validarEstadistica(estadistica);
            if (!jugadores.add(estadistica.getJugador())) throw new IllegalArgumentException("Un jugador no puede tener dos estadísticas para el mismo partido.");
        }
    }

    private void validarEstadistica(EstadisticaJugadorPartido estadistica) {
        if (estadistica == null || estadistica.getJugador() == null) throw new IllegalArgumentException("La estadística debe estar asociada a un jugador.");
        Socio jugador = estadistica.getJugador();
        if (!perteneceActivo(equipoLocal, jugador) && !perteneceActivo(equipoVisitante, jugador)) throw new IllegalArgumentException("El jugador no pertenece a una de las nóminas activas del partido.");
        if (jugador.tieneSancionVigente()) throw new IllegalArgumentException("Un jugador sancionado no puede participar en el partido.");
        for (EstadisticaJugadorPartido existente : estadisticas) if (existente.getJugador() == jugador) throw new IllegalStateException("Ya hay estadísticas para este jugador.");
        validarTipoEstadistica(estadistica);
    }

    private boolean perteneceActivo(Equipo equipo, Socio jugador) {
        for (InscripcionEquipo inscripcion : equipo.getInscripcionesActivas()) if (inscripcion.getSocio() == jugador) return true;
        return false;
    }

    private void validarTipoEstadistica(EstadisticaJugadorPartido estadistica) {
        DeporteConjunto deporte = equipoLocal.getDeporte();
        boolean coincide = (deporte instanceof Futbol && estadistica instanceof EstadisticaJugadorFutbol)
                || (deporte instanceof Baloncesto && estadistica instanceof EstadisticaJugadorBaloncesto)
                || (deporte instanceof Voleibol && estadistica instanceof EstadisticaJugadorVoleibol);
        if (!coincide) throw new IllegalArgumentException("El tipo de estadística no corresponde al deporte.");
    }

    private void registrarSancionDisciplinaria(EstadisticaJugadorFutbol estadistica, Socio jugador) {
        int sanciones = estadistica.getTarjetasRojas() + (estadistica.getTarjetasAmarillas() >= 2 ? 1 : 0);
        if (sanciones > 0) {
            Sancion sancion = new Sancion(estadistica.getTarjetasRojas() > 0 ? "Tarjeta roja" : "Acumulación de tarjetas amarillas", sanciones, false, jugador);
            estadistica.setSancion(sancion);
            jugador.registrarSancion(sancion);
        }
    }

    private void consumirSuspensiones(Equipo equipo) {
        for (InscripcionEquipo inscripcion : equipo.getInscripcionesActivas()) {
            for (Sancion sancion : inscripcion.getSocio().getSancionesVigentes()) sancion.consumirPartido();
        }
    }

    public void agregarEstadistica(EstadisticaJugadorPartido estadistica) { registrarEstadistica(estadistica); }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { if (fechaHora == null) throw new IllegalArgumentException("La fecha es obligatoria."); this.fechaHora = fechaHora; }
    public String getMarcadorFinal() { return marcadorFinal; }
    public void setMarcadorFinal(String marcadorFinal) {
        if (marcadorFinal == null || !marcadorFinal.trim().matches("\\d+\\s*-\\s*\\d+")) throw new IllegalArgumentException("El marcador debe tener el formato 'n - n'.");
        String[] valores = marcadorFinal.trim().split("\\s*-\\s*");
        registrarMarcador(Integer.parseInt(valores[0]), Integer.parseInt(valores[1]));
    }
    public int getMarcadorLocal() { return marcadorLocal; }
    public int getMarcadorVisitante() { return marcadorVisitante; }
    public boolean isFinalizado() { return finalizado; }
    public Equipo getEquipoLocal() { return equipoLocal; }
    public void setEquipoLocal(Equipo equipoLocal) { this.equipoLocal = equipoLocal; }
    public Equipo getEquipoVisitante() { return equipoVisitante; }
    public void setEquipoVisitante(Equipo equipoVisitante) { this.equipoVisitante = equipoVisitante; }
    public List<EstadisticaJugadorPartido> getEstadisticas() { return estadisticas; }
    public void setEstadisticas(List<EstadisticaJugadorPartido> estadisticas) { this.estadisticas = estadisticas == null ? new ArrayList<>() : estadisticas; }
}
