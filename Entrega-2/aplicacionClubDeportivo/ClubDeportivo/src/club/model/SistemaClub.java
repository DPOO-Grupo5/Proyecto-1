package club.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import club.modelo.competencias.Competencia;
import club.modelo.competencias.Equipo;
import club.modelo.entrenamiento.DisciplinaIndividual;
import club.modelo.instalaciones.Instalacion;
import club.modelo.tienda.Producto;
import club.modelo.usuarios.Usuario;

public class SistemaClub implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Map<String, Usuario> usuarios = new HashMap<>();
    private List<Equipo> equipos = new ArrayList<>();
    private List<Competencia> competencias = new ArrayList<>();
    private List<Producto> productos = new ArrayList<>();
    private List<Instalacion> instalaciones = new ArrayList<>();
    private List<DisciplinaIndividual> disciplinas = new ArrayList<>();

    public void registrarUsuario(Usuario usuario) {
        if (usuario == null || usuario.getLogin() == null || usuario.getLogin().trim().isEmpty()) throw new IllegalArgumentException("El usuario debe tener login.");
        String login = usuario.getLogin().trim();
        usuario.setLogin(login);
        for (String registrado : usuarios.keySet()) if (registrado.equalsIgnoreCase(login)) throw new IllegalArgumentException("El login ya está registrado.");
        usuarios.put(login, usuario);
    }

    public Usuario autenticar(String login, String password) {
        if (login == null || password == null) return null;
        Usuario usuario = buscarUsuario(login);
        return usuario != null && usuario.validarPassword(password) ? usuario : null;
    }

    public Usuario buscarUsuario(String login) {
        if (login == null) return null;
        for (Map.Entry<String, Usuario> entry : usuarios.entrySet()) if (entry.getKey().equalsIgnoreCase(login)) return entry.getValue();
        return null;
    }

    public void darDeBajaUsuario(String login) {
        Usuario usuario = buscarUsuario(login);
        if (usuario == null) throw new IllegalArgumentException("El usuario no está registrado.");
        usuarios.remove(usuario.getLogin());
    }

    public void registrarEquipo(Equipo equipo) {
        validarNoRegistrado(equipos, equipo, "equipo");
        for (Equipo existente : equipos) if (existente.getNombre().equalsIgnoreCase(equipo.getNombre())) throw new IllegalArgumentException("Ya hay un equipo con ese nombre.");
        equipos.add(equipo);
    }
    public void registrarCompetencia(Competencia competencia) {
        validarNoRegistrado(competencias, competencia, "competencia");
        for (Competencia existente : competencias) if (existente.getNombre().equalsIgnoreCase(competencia.getNombre()) && existente.getTemporada().equalsIgnoreCase(competencia.getTemporada())) throw new IllegalArgumentException("Ya hay una competencia con ese nombre en esa temporada.");
        competencias.add(competencia);
    }
    public void registrarProducto(Producto producto) {
        if (producto == null) throw new IllegalArgumentException("El producto es obligatorio.");
        for (Producto existente : productos) if (existente.getNombre().equalsIgnoreCase(producto.getNombre())) throw new IllegalArgumentException("Ya hay un producto con ese nombre.");
        agregarUnico(productos, producto, "producto");
    }
    public void registrarInstalacion(Instalacion instalacion) {
        validarNoRegistrado(instalaciones, instalacion, "instalación");
        DisciplinaIndividual disciplina = instalacion.getDisciplina();
        if (disciplina != null && !disciplinas.contains(disciplina)) disciplinas.add(disciplina);
        instalaciones.add(instalacion);
    }
    public void registrarDisciplina(DisciplinaIndividual disciplina) {
        validarNoRegistrado(disciplinas, disciplina, "disciplina");
        for (Instalacion instalacion : disciplina.getInstalaciones()) {
            if (instalacion.getDisciplina() != null && instalacion.getDisciplina() != disciplina) throw new IllegalStateException("Una instalación asociada pertenece a otra disciplina.");
        }
        disciplinas.add(disciplina);
        for (Instalacion instalacion : disciplina.getInstalaciones()) if (!instalaciones.contains(instalacion)) instalaciones.add(instalacion);
    }

    private <T> void agregarUnico(List<T> lista, T elemento, String tipo) {
        if (elemento == null) throw new IllegalArgumentException("El " + tipo + " es obligatorio.");
        if (lista.contains(elemento)) throw new IllegalArgumentException("El " + tipo + " ya está registrado.");
        lista.add(elemento);
    }

    private <T> void validarNoRegistrado(List<T> lista, T elemento, String tipo) {
        if (elemento == null) throw new IllegalArgumentException("El " + tipo + " es obligatorio.");
        if (lista.contains(elemento)) throw new IllegalArgumentException("El " + tipo + " ya está registrado.");
    }

    public Map<String, Usuario> getUsuarios() { return usuarios; }
    public List<Equipo> getEquipos() { return equipos; }
    public List<Competencia> getCompetencias() { return competencias; }
    public List<Producto> getProductos() { return productos; }
    public List<Instalacion> getInstalaciones() { return instalaciones; }
    public List<DisciplinaIndividual> getDisciplinas() { return disciplinas; }
}
