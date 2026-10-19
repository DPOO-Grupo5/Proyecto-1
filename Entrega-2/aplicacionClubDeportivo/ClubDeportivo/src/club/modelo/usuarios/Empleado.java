package club.modelo.usuarios;

import java.util.ArrayList;
import java.util.List;

import club.modelo.tienda.Producto;
import club.modelo.tienda.SugerenciaProducto;

public abstract class Empleado extends Usuario implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected static final double DESCUENTO = 0.15;
    protected static final double DESCUENTOCOMPARTIDO = 0.08;
    protected String codigoDescuento;

    private List<Turno> turnos = new ArrayList<>();
    private List<SolicitudCambioTurno> solicitudes = new ArrayList<>();
    private List<SugerenciaProducto> sugerencias = new ArrayList<>();
    private List<Socio> sociosConDescuento = new ArrayList<>();

    public Empleado(String login, String password, String nombre, String codigoDescuento) {
        super(login, password, nombre);
        this.codigoDescuento = codigoDescuento;
    }

    // --- MÉTODOS DE LÓGICA Y REGLAS DE NEGOCIO AGREGADOS ---

    public double getPorcentajeDescuentoPropio() {
        return DESCUENTO;
    }

    public double getPorcentajeDescuentoCompartido() {
        return DESCUENTOCOMPARTIDO;
    }

    public boolean validarCodigo(String codigo) {
        return this.codigoDescuento != null && this.codigoDescuento.equals(codigo);
    }

    public void autorizarDescuentoCompartido(Socio socio) {
        if (socio == null) throw new IllegalArgumentException("El socio es obligatorio.");
        if (!sociosConDescuento.contains(socio)) sociosConDescuento.add(socio);
    }

    public void retirarDescuentoCompartido(Socio socio) {
        if (socio == null || !sociosConDescuento.remove(socio)) throw new IllegalArgumentException("El socio no tiene descuento compartido.");
    }

    public SolicitudCambioTurno solicitarCambioTurno(Turno actual, Turno deseado) {
        if (actual == null || deseado == null || !turnos.contains(actual)) throw new IllegalArgumentException("El turno actual debe pertenecer al empleado.");
        SolicitudCambioTurno solicitud = new SolicitudCambioTurno(this, actual, deseado);
        this.solicitudes.add(solicitud);
        return solicitud;
    }

    public void agregarTurno(Turno turno) {
        if (turno == null) throw new IllegalArgumentException("El turno es obligatorio.");
        for (Turno existente : turnos) if (existente.seTraslapaCon(turno)) throw new IllegalArgumentException("Los turnos del empleado no pueden traslaparse.");
        if (!turnos.contains(turno)) turnos.add(turno);
    }

    public void retirarTurno(Turno turno) {
        if (!turnos.remove(turno)) throw new IllegalArgumentException("El turno no pertenece al empleado.");
    }

    public SugerenciaProducto sugerirProducto(Producto p) {
        if (p == null) throw new IllegalArgumentException("El producto es obligatorio.");
        SugerenciaProducto sugerencia = new SugerenciaProducto("Pendiente"); 
        sugerencia.setProducto(p);
        this.sugerencias.add(sugerencia);
        return sugerencia;
     
    }

    public List<Turno> consultarTurnos() {
        return this.turnos;
    }

   
    public String getCodigoDescuento() { return codigoDescuento; }

    public void setCodigoDescuento(String codigoDescuento) { this.codigoDescuento = codigoDescuento; }

    public List<Turno> getTurnos() { return turnos; }

    public List<SolicitudCambioTurno> getSolicitudes() { return solicitudes; }

    public List<SugerenciaProducto> getSugerencias() { return sugerencias; }

    public List<Socio> getSociosConDescuento() { return sociosConDescuento; }
}
