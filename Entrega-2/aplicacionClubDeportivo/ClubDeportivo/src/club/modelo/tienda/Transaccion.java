package club.modelo.tienda;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import club.modelo.usuarios.Empleado;
import club.modelo.usuarios.Socio;
import club.modelo.usuarios.Usuario;

public class Transaccion implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private LocalDateTime fechaHora;
    private String rubro;
    private double subtotal;
    private double descuento;
    private double descuentoBeneficio;
    private double porcentajeBeneficio;
    private double descuentoPuntos;
    private double impuestos;
    private double propina;
    private double total;
    private int puntosFidelidad;
    private int puntosRedimidos;
    private boolean confirmada;
    private Usuario comprador;
    private List<DetalleTransaccion> detalles = new ArrayList<>();

    public Transaccion() { this(LocalDateTime.now(), "general"); }

    public Transaccion(LocalDateTime fechaHora, String rubro) {
        if (fechaHora == null || rubro == null || rubro.trim().isEmpty()) throw new IllegalArgumentException("La fecha y el rubro son obligatorios.");
        this.fechaHora = fechaHora;
        this.rubro = rubro.trim().toLowerCase();
    }

    private void validarPendiente() {
        if (confirmada) throw new IllegalStateException("La transacción ya fue confirmada.");
    }

    public DetalleTransaccion agregarDetalle(Producto producto, int cantidad) {
        validarPendiente();
        DetalleTransaccion detalle = new DetalleTransaccion(producto, cantidad);
        detalles.add(detalle);
        calcularSubtotal();
        actualizarDescuento();
        return detalle;
    }

    public double calcularSubtotal() {
        subtotal = 0.0;
        for (DetalleTransaccion detalle : detalles) subtotal += detalle.calcularSubtotal();
        return subtotal;
    }

    public void aplicarDescuentoEmpleado(Empleado empleado) {
        validarPendiente();
        if (empleado == null) throw new IllegalArgumentException("El empleado es obligatorio.");
        if (comprador != empleado) throw new IllegalArgumentException("El descuento de empleado solo aplica a una compra propia.");
        double nuevoDescuento = calcularSubtotal() * empleado.getPorcentajeDescuentoPropio();
        if (nuevoDescuento + descuentoPuntos > subtotal) throw new IllegalArgumentException("El beneficio supera el saldo de la compra junto con los puntos redimidos.");
        porcentajeBeneficio = empleado.getPorcentajeDescuentoPropio();
        descuentoBeneficio = nuevoDescuento;
        actualizarDescuento();
    }

    public void aplicarDescuentoCompartido(String codigo, Empleado empleado) {
        validarPendiente();
        if (empleado == null || !empleado.validarCodigo(codigo)) throw new IllegalArgumentException("El código de descuento no es válido.");
        if (!(comprador instanceof Socio) || !empleado.getSociosConDescuento().contains((Socio) comprador)) throw new IllegalArgumentException("El socio no está autorizado para este descuento compartido.");
        double nuevoDescuento = calcularSubtotal() * empleado.getPorcentajeDescuentoCompartido();
        if (nuevoDescuento + descuentoPuntos > subtotal) throw new IllegalArgumentException("El beneficio supera el saldo de la compra junto con los puntos redimidos.");
        porcentajeBeneficio = empleado.getPorcentajeDescuentoCompartido();
        descuentoBeneficio = nuevoDescuento;
        actualizarDescuento();
    }

    public void redimirPuntos(int puntos) {
        validarPendiente();
        if (comprador == null || puntos <= 0 || puntos > comprador.getPuntosFidelidad()) throw new IllegalArgumentException("No se pueden redimir esos puntos.");
        int nuevosPuntos = puntosRedimidos + puntos;
        if (nuevosPuntos > comprador.getPuntosFidelidad()) throw new IllegalArgumentException("El saldo de puntos es insuficiente.");
        if (descuentoBeneficio + descuentoPuntos + puntos * 100.0 > calcularSubtotal()) throw new IllegalArgumentException("Los puntos superan el saldo de la compra.");
        puntosRedimidos = nuevosPuntos;
        descuentoPuntos += puntos * 100.0;
        actualizarDescuento();
    }

    private void actualizarDescuento() {
        double actual = calcularSubtotal();
        if (porcentajeBeneficio > 0) descuentoBeneficio = actual * porcentajeBeneficio;
        descuento = Math.min(actual, descuentoBeneficio + descuentoPuntos);
    }

    public double calcularImpuestos() {
        double baseImponible = Math.max(0.0, calcularSubtotal() - descuento);
        if ("tienda".equals(rubro)) impuestos = baseImponible * 0.19;
        else if ("cafeteria".equals(rubro)) impuestos = baseImponible * 0.08;
        else if ("general".equals(rubro) || "mensualidad".equals(rubro)) impuestos = 0.0;
        else throw new IllegalArgumentException("El rubro no está reconocido.");
        return impuestos;
    }

    public double calcularPropina(double porcentaje) {
        validarPendiente();
        if (porcentaje < 0 || porcentaje > 100 || Double.isNaN(porcentaje)) throw new IllegalArgumentException("El porcentaje de propina debe estar entre 0 y 100.");
        propina = Math.max(0.0, calcularSubtotal() - descuento) * porcentaje / 100.0;
        return propina;
    }

    public int calcularPuntosGenerados() {
        double neto = Math.max(0.0, calcularSubtotal() - descuento) + impuestos;
        puntosFidelidad = (int) (neto / 1000.0);
        return puntosFidelidad;
    }

    public void confirmar() {
        validarPendiente();
        if (comprador == null) throw new IllegalStateException("La transacción debe tener un comprador.");
        if (detalles.isEmpty()) throw new IllegalStateException("La transacción no tiene productos.");
        if (descuentoBeneficio + descuentoPuntos > calcularSubtotal()) throw new IllegalStateException("El descuento supera el subtotal.");
        actualizarDescuento();
        validarProductosDelRubro();
        validarExistencias();
        if (contieneBebidaCaliente() && comprador instanceof Socio
                && ((Socio) comprador).estaEnZonaRestringidaParaBebidasCalientes(fechaHora)) {
            throw new IllegalStateException("No se puede vender una bebida caliente para consumo en esta instalación.");
        }
        if (puntosRedimidos > comprador.getPuntosFidelidad()) throw new IllegalStateException("El saldo de puntos ya no es suficiente.");
        calcularImpuestos();
        calcularPuntosGenerados();

        descontarExistencias();
        if (puntosRedimidos > 0) comprador.redimirPuntos(puntosRedimidos);
        comprador.acumularPuntos(puntosFidelidad);
        total = Math.max(0.0, subtotal - descuento) + impuestos + propina;
        confirmada = true;
    }

    private void validarProductosDelRubro() {
        for (DetalleTransaccion detalle : detalles) {
            Producto producto = detalle.getProducto();
            if ("tienda".equals(rubro) && !(producto instanceof ProductoTienda)) throw new IllegalStateException("La tienda solo puede vender productos de tienda.");
            if ("cafeteria".equals(rubro) && !(producto instanceof ProductoCafeteria)) throw new IllegalStateException("La cafetería solo puede vender productos de cafetería.");
        }
    }

    private void validarExistencias() {
        for (int i = 0; i < detalles.size(); i++) {
            DetalleTransaccion detalle = detalles.get(i);
            boolean yaValidado = false;
            for (int anterior = 0; anterior < i; anterior++) {
                if (detalles.get(anterior).getProducto() == detalle.getProducto()) { yaValidado = true; break; }
            }
            if (yaValidado) continue;
            int requerida = 0;
            for (DetalleTransaccion repetido : detalles) if (repetido.getProducto() == detalle.getProducto()) requerida += repetido.getCantidad();
            int disponible = 0;
            for (Existencia existencia : detalle.getProducto().getExistencias()) disponible += existencia.getCantidad();
            if (disponible < requerida) throw new IllegalStateException("No hay existencia suficiente de " + detalle.getProducto().getNombre() + ".");
        }
    }

    private void descontarExistencias() {
        for (DetalleTransaccion detalle : detalles) {
            int pendiente = detalle.getCantidad();
            for (Existencia existencia : detalle.getProducto().getExistencias()) {
                int cantidad = Math.min(pendiente, existencia.getCantidad());
                if (cantidad > 0) existencia.disminuir(cantidad);
                pendiente -= cantidad;
                if (pendiente == 0) break;
            }
        }
    }

    public boolean contieneBebidaCaliente() {
        for (DetalleTransaccion detalle : detalles) {
            if (detalle.getProducto() instanceof Bebida && ((Bebida) detalle.getProducto()).esCaliente()) return true;
        }
        return false;
    }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { validarPendiente(); if (fechaHora == null) throw new IllegalArgumentException("La fecha es obligatoria."); this.fechaHora = fechaHora; }
    public String getRubro() { return rubro; }
    public void setRubro(String rubro) { validarPendiente(); if (rubro == null || rubro.trim().isEmpty()) throw new IllegalArgumentException("El rubro es obligatorio."); this.rubro = rubro.trim().toLowerCase(); }
    public double getSubtotal() { return subtotal; }
    public double getDescuento() { return descuento; }
    public void setDescuento(double descuento) { validarPendiente(); if (descuento < 0 || descuento + descuentoPuntos > calcularSubtotal()) throw new IllegalArgumentException("El descuento no puede superar el subtotal."); this.porcentajeBeneficio = 0; this.descuentoBeneficio = descuento; actualizarDescuento(); }
    public double getImpuestos() { return impuestos; }
    public double getPropina() { return propina; }
    public void setPropina(double propina) { validarPendiente(); if (propina < 0) throw new IllegalArgumentException("La propina no puede ser negativa."); this.propina = propina; }
    public double getTotal() { return total; }
    public int getPuntosFidelidad() { return puntosFidelidad; }
    public int getPuntosRedimidos() { return puntosRedimidos; }
    public boolean isConfirmada() { return confirmada; }
    public Usuario getComprador() { return comprador; }
    public void setComprador(Usuario comprador) { validarPendiente(); if (comprador == null) throw new IllegalArgumentException("El comprador es obligatorio."); this.comprador = comprador; }
    public List<DetalleTransaccion> getDetalles() { return detalles; }
}
