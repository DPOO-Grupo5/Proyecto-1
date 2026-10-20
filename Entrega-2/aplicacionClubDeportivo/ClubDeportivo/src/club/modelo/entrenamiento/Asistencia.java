package club.modelo.entrenamiento;

import club.modelo.usuarios.Socio;
import java.util.Locale;

public class Asistencia implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String estado;
    private Integer notaDesempeno;
    private String comentario;

    private Socio socio;
    private SesionEntrenamiento sesion;

    public Asistencia(String estado, Integer notaDesempeno, String comentario, Socio socio, SesionEntrenamiento sesion) {
        setEstado(estado);
        setNotaDesempeno(notaDesempeno);
        this.comentario = comentario;
        this.socio = socio;
        this.sesion = sesion;
    }
   
    


	public String getEstado() { return estado; }

    public void setEstado(String estado) {
        if (estado == null) throw new IllegalArgumentException("El estado de asistencia es obligatorio.");
        String normalizado = estado.trim().toLowerCase(Locale.ROOT);
        if (!(normalizado.equals("asistió") || normalizado.equals("faltó") || normalizado.equals("llegó tarde"))) {
            throw new IllegalArgumentException("El estado debe ser asistió, faltó o llegó tarde.");
        }
        this.estado = normalizado;
    }

    public Integer getNotaDesempeno() { return notaDesempeno; }

    public void setNotaDesempeno(Integer notaDesempeno) {
        if (notaDesempeno != null && (notaDesempeno < 1 || notaDesempeno > 5)) {
            throw new IllegalArgumentException("La nota de desempeño debe estar entre 1 y 5.");
        }
        this.notaDesempeno = notaDesempeno;
    }

    public String getComentario() { return comentario; }

    public void setComentario(String comentario) { this.comentario = comentario; }

    public Socio getSocio() { return socio; }

    public void setSocio(Socio socio) { this.socio = socio; }

    public SesionEntrenamiento getSesion() { return sesion; }

    public void setSesion(SesionEntrenamiento sesion) { this.sesion = sesion; }
}
