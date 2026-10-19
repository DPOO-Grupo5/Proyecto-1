package club.modelo.usuarios;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class Turno implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private DayOfWeek dia;
    private LocalTime horaInicio;
    private LocalTime horaFin;

    public Turno(DayOfWeek dia, LocalTime horaInicio, LocalTime horaFin) {
        setDia(dia);
        setHoraInicio(horaInicio);
        setHoraFin(horaFin);
        validarRango();
    }

    private void validarRango() {
        if (horaInicio == null || horaFin == null || !horaInicio.isBefore(horaFin)) {
            throw new IllegalArgumentException("El inicio del turno debe ser anterior al fin.");
        }
    }

    public boolean seTraslapaCon(Turno otro) {
        return otro != null && dia == otro.dia && horaInicio.isBefore(otro.horaFin) && horaFin.isAfter(otro.horaInicio);
    }

    public boolean cubre(DayOfWeek d, LocalTime hora) {
        return d == dia && hora != null && !hora.isBefore(horaInicio) && hora.isBefore(horaFin);
    }

    public DayOfWeek getDia() { return dia; }
    public void setDia(DayOfWeek dia) { if (dia == null) throw new IllegalArgumentException("El día es obligatorio."); this.dia = dia; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { if (horaInicio == null) throw new IllegalArgumentException("La hora de inicio es obligatoria."); this.horaInicio = horaInicio; if (horaFin != null) validarRango(); }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { if (horaFin == null) throw new IllegalArgumentException("La hora de fin es obligatoria."); this.horaFin = horaFin; if (horaInicio != null) validarRango(); }
}
