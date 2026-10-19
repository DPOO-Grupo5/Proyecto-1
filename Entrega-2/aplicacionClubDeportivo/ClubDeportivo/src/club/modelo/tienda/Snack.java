package club.modelo.tienda;

import java.util.List;

public class Snack extends ProductoCafeteria implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private List<String> alergenos;

    public Snack(String nombre, double precio, List<String> alergenos) {
        super(nombre, precio);
        this.alergenos = alergenos;
    }

    public List<String> getAlergenos() { return alergenos; }

    public void setAlergenos(List<String> alergenos) { this.alergenos = alergenos; }
}
