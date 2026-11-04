package club.consola;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import club.model.SistemaClub;
import club.modelo.competencias.Equipo;
import club.modelo.competencias.InscripcionEquipo;
import club.modelo.entrenamiento.DisciplinaIndividual;
import club.modelo.entrenamiento.InscripcionDisciplina;
import club.modelo.entrenamiento.SesionEntrenamiento;
import club.modelo.instalaciones.Instalacion;
import club.modelo.tienda.Bebida;
import club.modelo.tienda.Producto;
import club.modelo.tienda.ProductoTienda;
import club.modelo.tienda.Snack;
import club.modelo.usuarios.Empleado;
import club.modelo.usuarios.Entrenador;
import club.modelo.usuarios.SolicitudCambioTurno;
import club.modelo.usuarios.Socio;
import club.modelo.usuarios.Turno;
import club.modelo.usuarios.Usuario;

/**
 * Aplicación del empleado (entrenador o fisioterapeuta): cada opción es un método que arma su panel.
 * Todos los empleados ven turnos, cambio de turno, código y sugerencias;
 * solo el entrenador ve además las opciones de entrenamiento.
 */
public class MainEmpleado extends VentanaBase
{
    private static final String TURNOS = "Mis turnos";
    private static final String CAMBIO = "Solicitar cambio de turno";
    private static final String CODIGO = "Compartir mi código";
    private static final String SUGERIR = "Sugerir producto";
    private static final String SESION = "Planear sesión";
    private static final String ASISTENCIA = "Registrar asistencia";
    private static final String NIVEL = "Asignar nivel";
    private static final String ANALITICA = "Analítica de jugadores";

    private static final String EQUIPO = "Un equipo completo";
    private static final String POR_DISCIPLINA = "Socios de una disciplina individual";
    private static final String[] ESTADOS = { "asistió", "faltó", "llegó tarde" };

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override protected String nombreRol()
    {
        return usuario instanceof Entrenador ? "Entrenador" : "Empleado";
    }

    @Override protected boolean admite(Usuario u) { return u instanceof Empleado; }

    @Override protected String[] opciones()
    {
        if (usuario instanceof Entrenador)
            return new String[] { TURNOS, CAMBIO, CODIGO, SUGERIR, SESION, ASISTENCIA, NIVEL, ANALITICA };
        return new String[] { TURNOS, CAMBIO, CODIGO, SUGERIR };
    }

    @Override protected JPanel crearPanel(String opcion)
    {
        Empleado emp = (Empleado) usuario;
        switch (opcion)
        {
            case TURNOS:     return panelTurnos(emp);
            case CAMBIO:     return panelSolicitarCambio(emp);
            case CODIGO:     return panelCompartirCodigo(emp);
            case SUGERIR:    return panelSugerirProducto(emp);
            case SESION:     return panelPlanearSesion((Entrenador) emp);
            case ASISTENCIA: return panelAsistencia((Entrenador) emp);
            case NIVEL:      return panelAsignarNivel((Entrenador) emp);
            case ANALITICA:  return panelAnalitica((Entrenador) emp);
            default:         return null;
        }
    }

    public static void main(String[] args)
    {
        new MainEmpleado().iniciar();
    }

    // ---------- utilidades ----------
    private static String etiqueta(Socio s) { return s.getNombre() + " (" + s.getLogin() + ")"; }

    private static String etiqueta(Turno t)
    {
        return t.getDia().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es")) + " "
                + t.getHoraInicio() + " - " + t.getHoraFin();
    }

    private static void aviso(Component padre, String mensaje)
    {
        JOptionPane.showMessageDialog(padre, mensaje, "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    private static List<Socio> inscritos(DisciplinaIndividual d)
    {
        List<Socio> lista = new ArrayList<>();
        if (d == null) return lista;
        for (InscripcionDisciplina i : d.getInscripciones())
            if (i.isActiva()) lista.add(i.getSocio());
        return lista;
    }

    private List<Socio> todosLosSocios()
    {
        List<Socio> lista = new ArrayList<>();
        for (Usuario u : sistema.getUsuarios().values())
            if (u instanceof Socio) lista.add((Socio) u);
        return lista;
    }

    // ======================= Mis turnos =======================
    private JPanel panelTurnos(Empleado emp)
    {
        JPanel panel = new JPanel(new BorderLayout());
        StringBuilder sb = new StringBuilder("--- Mis turnos ---\n");
        if (emp.consultarTurnos().isEmpty()) sb.append("No tiene turnos asignados.\n");
        for (Turno t : emp.consultarTurnos()) sb.append(etiqueta(t)).append('\n');
        sb.append("\n--- Mis solicitudes de cambio ---\n");
        if (emp.getSolicitudes().isEmpty()) sb.append("Ninguna.\n");
        for (SolicitudCambioTurno s : emp.getSolicitudes())
            sb.append(etiqueta(s.getTurnoActual())).append(" -> ").append(etiqueta(s.getTurnoDeseado()))
              .append(" | estado: ").append(s.getEstado()).append('\n');

        JTextArea texto = new JTextArea(sb.toString());
        texto.setEditable(false);
        panel.add(new JLabel("  Mis turnos"), BorderLayout.NORTH);
        panel.add(new JScrollPane(texto), BorderLayout.CENTER);
        return panel;
    }

    // ======================= Solicitar cambio de turno =======================
    private JPanel panelSolicitarCambio(Empleado emp)
    {
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        JComboBox<Turno> actual = new JComboBox<>();
        JComboBox<DayOfWeek> dia = new JComboBox<>(DayOfWeek.values());
        JTextField inicio = new JTextField();
        JTextField fin = new JTextField();

        for (Turno t : emp.getTurnos()) actual.addItem(t);
        actual.setRenderer((l, t, i, s, f) -> new JLabel(t == null ? "" : etiqueta(t)));
        dia.setRenderer((l, d, i, s, f) -> new JLabel(d == null ? ""
                : d.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"))));

        JButton boton = new JButton("Enviar solicitud");
        boton.addActionListener(e ->
        {
            try
            {
                if (actual.getSelectedItem() == null) throw new IllegalStateException("No tiene turnos para cambiar.");
                LocalTime ini = LocalTime.parse(inicio.getText().trim());
                LocalTime fn = LocalTime.parse(fin.getText().trim());
                emp.solicitarCambioTurno((Turno) actual.getSelectedItem(), new Turno((DayOfWeek) dia.getSelectedItem(), ini, fn));
                JOptionPane.showMessageDialog(panel, "Solicitud registrada. Queda pendiente de aprobación del administrador.");
            }
            catch (DateTimeParseException ex)
            {
                aviso(panel, "Las horas deben tener el formato HH:mm (ej. 14:30).");
            }
            catch (IllegalArgumentException | IllegalStateException | DateTimeException ex)
            {
                aviso(panel, ex.getMessage());
            }
        });

        panel.add(new JLabel("Turno a cambiar:"));        panel.add(actual);
        panel.add(new JLabel("Día deseado:"));            panel.add(dia);
        panel.add(new JLabel("Hora de inicio (HH:mm):")); panel.add(inicio);
        panel.add(new JLabel("Hora de fin (HH:mm):"));    panel.add(fin);
        panel.add(new JLabel());                          panel.add(boton);
        return panel;
    }

    // ======================= Compartir código de descuento =======================
    private JPanel panelCompartirCodigo(Empleado emp)
    {
        JPanel panel = new JPanel(new BorderLayout());
        JComboBox<Socio> socio = new JComboBox<>();
        for (Socio s : todosLosSocios()) socio.addItem(s);
        socio.setRenderer((l, s, i, sel, f) -> new JLabel(s == null ? "" : etiqueta(s)));

        JButton boton = new JButton("Compartir");
        boton.addActionListener(e ->
        {
            try
            {
                Socio s = (Socio) socio.getSelectedItem();
                if (s == null) throw new IllegalStateException("No hay socios registrados.");
                emp.autorizarDescuentoCompartido(s);
                JOptionPane.showMessageDialog(panel, "Comparta con " + s.getNombre()
                        + " su código: " + emp.getCodigoDescuento());
            }
            catch (IllegalArgumentException | IllegalStateException ex)
            {
                aviso(panel, ex.getMessage());
            }
        });

        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fila.add(new JLabel("Socio que recibirá el descuento del 8 %:"));
        fila.add(socio);
        fila.add(boton);

        panel.add(new JLabel("  Compartir mi código de descuento"), BorderLayout.NORTH);
        panel.add(fila, BorderLayout.CENTER);
        return panel;
    }

    // ======================= Sugerir producto =======================
    private JPanel panelSugerirProducto(Empleado emp)
    {
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        JComboBox<String> tipo = new JComboBox<>(new String[] { "tienda", "bebida", "snack" });
        JTextField nombre = new JTextField();
        JTextField precio = new JTextField();
        JTextField categoria = new JTextField();
        JCheckBox caliente = new JCheckBox("Es una bebida caliente");
        JTextField alergenos = new JTextField();

        // habilita solo los campos que corresponden al tipo elegido
        Runnable habilitar = () ->
        {
            String t = (String) tipo.getSelectedItem();
            categoria.setEnabled("tienda".equals(t));
            caliente.setEnabled("bebida".equals(t));
            alergenos.setEnabled("snack".equals(t));
        };
        tipo.addActionListener(e -> habilitar.run());

        JButton boton = new JButton("Enviar sugerencia");
        boton.addActionListener(e ->
        {
            try
            {
                String n = nombre.getText().trim();
                if (n.isEmpty()) throw new IllegalArgumentException("El nombre es obligatorio.");
                double p;
                try { p = Double.parseDouble(precio.getText().trim().replace(',', '.')); }
                catch (NumberFormatException ex) { throw new IllegalArgumentException("El precio debe ser un número."); }
                if (p <= 0) throw new IllegalArgumentException("El precio debe ser positivo.");

                Producto producto;
                switch ((String) tipo.getSelectedItem())
                {
                    case "tienda":
                        if (categoria.getText().trim().isEmpty())
                            throw new IllegalArgumentException("La categoría es obligatoria para productos de tienda.");
                        producto = new ProductoTienda(n, p, categoria.getText().trim());
                        break;
                    case "bebida":
                        producto = new Bebida(n, p, caliente.isSelected());
                        break;
                    default:
                        List<String> lista = new ArrayList<>();
                        if (!alergenos.getText().trim().isEmpty())
                            for (String a : alergenos.getText().split(",")) lista.add(a.trim());
                        producto = new Snack(n, p, lista);
                }
                emp.sugerirProducto(producto);
                nombre.setText(""); precio.setText("");
                JOptionPane.showMessageDialog(panel, "Sugerencia enviada al administrador.");
            }
            catch (IllegalArgumentException | IllegalStateException ex)
            {
                aviso(panel, ex.getMessage());
            }
        });

        panel.add(new JLabel("Tipo:"));                                 panel.add(tipo);
        panel.add(new JLabel("Nombre:"));                               panel.add(nombre);
        panel.add(new JLabel("Precio:"));                               panel.add(precio);
        panel.add(new JLabel("Categoría (solo tienda):"));              panel.add(categoria);
        panel.add(new JLabel("Bebida caliente (solo bebida):"));        panel.add(caliente);
        panel.add(new JLabel("Alérgenos, separados por coma (solo snack):")); panel.add(alergenos);
        panel.add(new JLabel());                                        panel.add(boton);
        habilitar.run();
        return panel;
    }

    // ======================= Planear sesión (entrenador) =======================
    private JPanel panelPlanearSesion(Entrenador yo)
    {
        JPanel panel = new JPanel(new BorderLayout());
        JComboBox<Instalacion> instalacion = new JComboBox<>();
        JTextField fecha = new JTextField();
        JTextField duracion = new JTextField("60");
        JComboBox<String> tipo = new JComboBox<>(new String[] { "grupal", "individual" });
        JComboBox<String> convocar = new JComboBox<>(new String[] { EQUIPO, POR_DISCIPLINA });
        JComboBox<Equipo> equipo = new JComboBox<>();
        JComboBox<DisciplinaIndividual> disciplina = new JComboBox<>();
        JList<Socio> socios = new JList<>();

        for (Instalacion i : sistema.getInstalaciones()) instalacion.addItem(i);
        for (Equipo e : sistema.getEquipos()) if (e.isActivo()) equipo.addItem(e);
        for (DisciplinaIndividual d : sistema.getDisciplinas()) disciplina.addItem(d);
        instalacion.setRenderer((l, i, idx, s, f) -> new JLabel(i == null ? "" : i.getNombre()));
        equipo.setRenderer((l, e, i, s, f) -> new JLabel(e == null ? "" : e.getNombre()));
        disciplina.setRenderer((l, d, i, s, f) -> new JLabel(d == null ? "" : d.getClass().getSimpleName()));
        socios.setCellRenderer((l, s, i, sel, f) ->
        {
            JLabel lb = new JLabel(etiqueta(s));
            lb.setOpaque(true);
            if (sel) lb.setBackground(l.getSelectionBackground());
            return lb;
        });
        socios.setVisibleRowCount(4);

        Runnable cargarSocios = () ->
                socios.setListData(inscritos((DisciplinaIndividual) disciplina.getSelectedItem()).toArray(new Socio[0]));
        Runnable habilitar = () ->
        {
            boolean porEquipo = EQUIPO.equals(convocar.getSelectedItem());
            equipo.setEnabled(porEquipo);
            disciplina.setEnabled(!porEquipo);
            socios.setEnabled(!porEquipo);
        };
        disciplina.addActionListener(e -> cargarSocios.run());
        convocar.addActionListener(e -> habilitar.run());
        cargarSocios.run();
        habilitar.run();

        JButton boton = new JButton("Planear sesión");
        boton.addActionListener(e ->
        {
            try
            {
                List<Socio> convocados = new ArrayList<>();
                if (EQUIPO.equals(convocar.getSelectedItem()))
                {
                    if (equipo.getSelectedItem() == null) throw new IllegalStateException("No hay equipos activos.");
                    for (InscripcionEquipo i : ((Equipo) equipo.getSelectedItem()).getInscripcionesActivas())
                        convocados.add(i.getSocio());
                }
                else
                {
                    convocados.addAll(socios.getSelectedValuesList());
                    if (convocados.isEmpty()) throw new IllegalArgumentException("Seleccione al menos un socio para convocar.");
                }
                Instalacion inst = (Instalacion) instalacion.getSelectedItem();
                if (inst == null) throw new IllegalStateException("No hay instalaciones.");

                LocalDateTime cuando = LocalDateTime.parse(fecha.getText().trim(), FECHA_HORA);
                int minutos = Integer.parseInt(duracion.getText().trim());
                SesionEntrenamiento s = yo.planearSesion(cuando, minutos, (String) tipo.getSelectedItem(), inst, convocados);
                JOptionPane.showMessageDialog(panel, "Sesión planeada el " + s.getFechaHora() + " en "
                        + inst.getNombre() + " con " + s.getConvocados().size() + " convocado(s).");
            }
            catch (DateTimeParseException ex)
            {
                aviso(panel, "Use el formato AAAA-MM-DD HH:mm (ej. 2026-11-10 15:00).");
            }
            catch (NumberFormatException ex)
            {
                aviso(panel, "La duración debe ser un número entero de minutos.");
            }
            catch (IllegalArgumentException | IllegalStateException | DateTimeException ex)
            {
                aviso(panel, ex.getMessage());
            }
        });

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Instalación:"));                     form.add(instalacion);
        form.add(new JLabel("Fecha y hora (AAAA-MM-DD HH:mm):")); form.add(fecha);
        form.add(new JLabel("Duración (minutos):"));              form.add(duracion);
        form.add(new JLabel("Tipo:"));                            form.add(tipo);
        form.add(new JLabel("Convocar a:"));                      form.add(convocar);
        form.add(new JLabel("Equipo:"));                          form.add(equipo);
        form.add(new JLabel("Disciplina:"));                      form.add(disciplina);
        form.add(new JLabel("Socios (Ctrl para varios):"));       form.add(new JScrollPane(socios));
        form.add(new JLabel());                                   form.add(boton);

        panel.add(new JLabel("  Planear sesión de entrenamiento"), BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        return panel;
    }

    // ======================= Registrar asistencia (entrenador) =======================
    private JPanel panelAsistencia(Entrenador yo)
    {
        JPanel panel = new JPanel(new BorderLayout());
        JComboBox<SesionEntrenamiento> sesion = new JComboBox<>();
        JPanel filas = new JPanel();
        filas.setLayout(new BoxLayout(filas, BoxLayout.Y_AXIS));
        List<Socio> socios = new ArrayList<>();
        List<JComboBox<String>> estados = new ArrayList<>();
        List<JTextField> notas = new ArrayList<>();
        List<JTextField> comentarios = new ArrayList<>();

        for (SesionEntrenamiento s : yo.getSesiones()) sesion.addItem(s);
        sesion.setRenderer((l, s, i, sel, f) -> new JLabel(s == null ? "" : s.getFechaHora() + " en "
                + s.getInstalacion().getNombre() + " (" + s.getConvocados().size() + " convocados)"));

        Runnable reconstruir = () ->
        {
            filas.removeAll();
            socios.clear(); estados.clear(); notas.clear(); comentarios.clear();
            SesionEntrenamiento s = (SesionEntrenamiento) sesion.getSelectedItem();
            if (s == null)
                filas.add(new JLabel("No tiene sesiones planeadas."));
            else
                for (Socio so : s.getConvocados())
                {
                    JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    JComboBox<String> estado = new JComboBox<>(ESTADOS);
                    JTextField nota = new JTextField(3);
                    JTextField comentario = new JTextField(20);
                    fila.add(new JLabel(etiqueta(so)));
                    fila.add(estado);
                    fila.add(new JLabel("Nota 1-5:")); fila.add(nota);
                    fila.add(new JLabel("Comentario:")); fila.add(comentario);
                    socios.add(so); estados.add(estado); notas.add(nota); comentarios.add(comentario);
                    filas.add(fila);
                }
            filas.revalidate();
            filas.repaint();
        };
        sesion.addActionListener(e -> reconstruir.run());

        JButton boton = new JButton("Registrar asistencia");
        boton.addActionListener(e ->
        {
            try
            {
                SesionEntrenamiento s = (SesionEntrenamiento) sesion.getSelectedItem();
                if (s == null) throw new IllegalStateException("No hay sesión seleccionada.");

                // validar todas las notas antes de registrar, para no dejar la sesión a medias
                List<Integer> valores = new ArrayList<>();
                for (JTextField n : notas)
                {
                    String t = n.getText().trim();
                    if (t.isEmpty()) { valores.add(null); continue; }
                    try
                    {
                        int v = Integer.parseInt(t);
                        if (v < 1 || v > 5) throw new NumberFormatException();
                        valores.add(v);
                    }
                    catch (NumberFormatException ex)
                    {
                        throw new IllegalArgumentException("Las notas deben ser enteros entre 1 y 5.");
                    }
                }

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < socios.size(); i++)
                {
                    String c = comentarios.get(i).getText().trim();
                    try
                    {
                        yo.registrarAsistencia(s, socios.get(i), (String) estados.get(i).getSelectedItem(),
                                valores.get(i), c.isEmpty() ? null : c);
                        sb.append(socios.get(i).getNombre()).append(": registrado.\n");
                    }
                    catch (IllegalArgumentException | IllegalStateException ex)
                    {
                        sb.append(socios.get(i).getNombre()).append(": no se registró (").append(ex.getMessage()).append(").\n");
                    }
                }
                JOptionPane.showMessageDialog(panel, sb.toString());
            }
            catch (IllegalArgumentException | IllegalStateException ex)
            {
                aviso(panel, ex.getMessage());
            }
        });

        JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT));
        arriba.add(new JLabel("Sesión:"));
        arriba.add(sesion);

        panel.add(arriba, BorderLayout.NORTH);
        panel.add(new JScrollPane(filas), BorderLayout.CENTER);
        panel.add(boton, BorderLayout.SOUTH);
        reconstruir.run();
        return panel;
    }

    // ======================= Asignar nivel (entrenador) =======================
    private JPanel panelAsignarNivel(Entrenador yo)
    {
        JPanel panel = new JPanel(new BorderLayout());
        JComboBox<DisciplinaIndividual> disciplina = new JComboBox<>();
        JComboBox<Socio> socio = new JComboBox<>();
        JComboBox<String> nivel = new JComboBox<>(new String[] { "Principiante", "Intermedio", "Avanzado" });

        for (DisciplinaIndividual d : sistema.getDisciplinas()) disciplina.addItem(d);
        disciplina.setRenderer((l, d, i, s, f) -> new JLabel(d == null ? "" : d.getClass().getSimpleName()));
        socio.setRenderer((l, s, i, sel, f) -> new JLabel(s == null ? "" : etiqueta(s)));

        Runnable recargar = () ->
        {
            socio.removeAllItems();
            for (Socio s : inscritos((DisciplinaIndividual) disciplina.getSelectedItem())) socio.addItem(s);
        };
        disciplina.addActionListener(e -> recargar.run());
        recargar.run();

        JButton boton = new JButton("Asignar nivel");
        boton.addActionListener(e ->
        {
            try
            {
                if (socio.getSelectedItem() == null) throw new IllegalStateException("No hay socios inscritos en esa disciplina.");
                yo.asignarNivel((Socio) socio.getSelectedItem(), (DisciplinaIndividual) disciplina.getSelectedItem(),
                        (String) nivel.getSelectedItem());
                JOptionPane.showMessageDialog(panel, "Nivel actualizado a " + nivel.getSelectedItem() + ".");
            }
            catch (IllegalArgumentException | IllegalStateException ex)
            {
                aviso(panel, ex.getMessage());
            }
        });

        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fila.add(new JLabel("Disciplina:")); fila.add(disciplina);
        fila.add(new JLabel("Socio:"));      fila.add(socio);
        fila.add(new JLabel("Nivel:"));      fila.add(nivel);
        fila.add(boton);

        panel.add(new JLabel("  Asignar nivel"), BorderLayout.NORTH);
        panel.add(fila, BorderLayout.CENTER);
        return panel;
    }

    // ======================= Analítica de jugadores (entrenador) =======================
    private JPanel panelAnalitica(Entrenador yo)
    {
        JPanel panel = new JPanel(new BorderLayout());
        JComboBox<Socio> jugador = new JComboBox<>();
        JTextArea salida = new JTextArea();
        salida.setEditable(false);
        for (Socio s : todosLosSocios()) jugador.addItem(s);
        jugador.setRenderer((l, s, i, sel, f) -> new JLabel(s == null ? "" : etiqueta(s)));

        JButton boton = new JButton("Consultar");
        boton.addActionListener(e ->
        {
            try
            {
                Socio s = (Socio) jugador.getSelectedItem();
                if (s == null) throw new IllegalStateException("No hay socios registrados.");
                salida.setText(yo.consultarAnalitica(s));
                salida.setCaretPosition(0);
            }
            catch (IllegalArgumentException | IllegalStateException ex)
            {
                aviso(panel, ex.getMessage());
            }
        });

        JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT));
        arriba.add(new JLabel("Jugador:")); arriba.add(jugador); arriba.add(boton);

        panel.add(arriba, BorderLayout.NORTH);
        panel.add(new JScrollPane(salida), BorderLayout.CENTER);
        return panel;
    }
}