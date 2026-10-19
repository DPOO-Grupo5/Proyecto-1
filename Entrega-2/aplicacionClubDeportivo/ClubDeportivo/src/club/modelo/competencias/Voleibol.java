package club.modelo.competencias;

public class Voleibol extends DeporteConjunto implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public Voleibol(String nombre, int minJugadores) {
        super(nombre, minJugadores);
    }
}
