package club.modelo.usuarios;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import club.modelo.tienda.Producto;
import club.modelo.tienda.Transaccion;

public abstract class Usuario implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected String login;
    protected String nombre;
    protected int puntosFidelidad;
    protected String password;
    protected double descuento;

    private List<Transaccion> transacciones = new ArrayList<>();

    public Usuario(String login, String password, String nombre) {
        setLogin(login);
        setPassword(password);
        setNombre(nombre);
        this.puntosFidelidad = 0; 
    }

    public boolean validarPassword(String passwordIngresado) { 
        return this.password != null && this.password.equals(passwordIngresado); 
    }

    public void actualizarPassword(String actual, String nueva) { 
        if (validarPassword(actual)) {
            setPassword(nueva);
        } else {
            throw new IllegalArgumentException("La contraseña actual es incorrecta.");
        }
    }

    
    public Transaccion iniciarTransaccion(Map<Producto, Integer> productos, String rubro) {
        Transaccion t = new Transaccion(java.time.LocalDateTime.now(), rubro);
        t.setComprador(this);
        if (productos != null) {
            for (Map.Entry<Producto, Integer> item : productos.entrySet()) {
                if (item.getValue() == null) throw new IllegalArgumentException("La cantidad del producto es obligatoria.");
                t.agregarDetalle(item.getKey(), item.getValue());
            }
        }
        this.transacciones.add(t);
        return t; 
    }

    public List<Transaccion> consultarHistorialCompras() { 
        return this.transacciones; 
    }


    public void acumularPuntos(int puntos) {
        if (puntos < 0) throw new IllegalArgumentException("Los puntos a acumular no pueden ser negativos.");
        this.puntosFidelidad += puntos;
    }

    public void redimirPuntos(int puntos) {
        if (puntos <= 0 || this.puntosFidelidad < puntos) throw new IllegalArgumentException("El saldo de puntos es insuficiente.");
        this.puntosFidelidad -= puntos;
    }

   

    public String getLogin() { return login; }

    public void setLogin(String login) { if (login == null || login.trim().isEmpty()) throw new IllegalArgumentException("El login es obligatorio."); this.login = login.trim(); }

    public String getNombre() { return nombre; }

    public void setNombre(String nombre) { if (nombre == null || nombre.trim().isEmpty()) throw new IllegalArgumentException("El nombre es obligatorio."); this.nombre = nombre.trim(); }

    public int getPuntosFidelidad() { return puntosFidelidad; }

    public void setPuntosFidelidad(int puntosFidelidad) { if (puntosFidelidad < 0) throw new IllegalArgumentException("Los puntos no pueden ser negativos."); this.puntosFidelidad = puntosFidelidad; }

    public String getPassword() { return password; }

    public void setPassword(String password) { if (password == null || password.isEmpty()) throw new IllegalArgumentException("La contraseña es obligatoria."); this.password = password; }

    public double getDescuento() { return descuento; }

    public void setDescuento(double descuento) { this.descuento = descuento; }

    public List<Transaccion> getTransacciones() { return transacciones; }

    public void setTransacciones(List<Transaccion> transacciones) { this.transacciones = transacciones; }
}
