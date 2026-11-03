package club.consola;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import club.model.SistemaClub;
import club.modelo.competencias.Equipo;
import club.modelo.competencias.EstadisticaJugadorBaloncesto;
import club.modelo.competencias.EstadisticaJugadorFutbol;
import club.modelo.competencias.EstadisticaJugadorPartido;
import club.modelo.competencias.EstadisticaJugadorVoleibol;
import club.modelo.competencias.InscripcionEquipo;
import club.modelo.entrenamiento.Asistencia;
import club.modelo.entrenamiento.DisciplinaIndividual;
import club.modelo.entrenamiento.EstadisticaIndividual;
import club.modelo.entrenamiento.EstadisticaTenisMesa;
import club.modelo.entrenamiento.EstadisticaTenisPadel;
import club.modelo.entrenamiento.InscripcionDisciplina;
import club.modelo.entrenamiento.PartidoPractica;
import club.modelo.entrenamiento.TenisMesa;
import club.modelo.instalaciones.Instalacion;
import club.modelo.instalaciones.Reserva;
import club.modelo.usuarios.Entrenador;
import club.modelo.usuarios.Socio;
import club.modelo.usuarios.Usuario;

/** Aplicación del socio: la ventana, las opciones y todos sus paneles en un solo archivo. */
public class MainSocio extends VentanaBase
{
    private static final String EQUIPOS = "Mis equipos";
    private static final String DISCIPLINA = "Inscribirme en disciplina";
    private static final String RESERVAR = "Reservar instalación";
    private static final String PRACTICA = "Programar práctica";
    private static final String RESULTADO = "Resultado de práctica";
    private static final String DESEMPENO = "Mi desempeño";

    @Override protected String nombreRol() { return "Socio"; }

    @Override protected boolean admite(Usuario u) { return u instanceof Socio; }

    @Override protected String[] opciones()
    {
        return new String[] { EQUIPOS, DISCIPLINA, RESERVAR, PRACTICA, RESULTADO, DESEMPENO };
    }

    @Override protected JPanel crearPanel(String opcion)
    {
        Socio socio = (Socio) usuario;
        switch (opcion)
        {
            case EQUIPOS:    return new PanelMisEquipos(socio);
            case DISCIPLINA: return new PanelInscribirDisciplina(sistema, socio);
            case RESERVAR:   return new PanelReservar(sistema, socio);
            case PRACTICA:   return new PanelProgramarPractica(sistema, socio);
            case RESULTADO:  return new PanelResultadoPractica(socio);
            case DESEMPENO:  return new PanelDesempeno(socio);
            default:         return null;
        }
    }

    public static void main(String[] args)
    {
        new MainSocio().iniciar();
    }

    // ======================= Mis equipos =======================
    private static class PanelMisEquipos extends JPanel
    {
        PanelMisEquipos(Socio socio)
        {
            setLayout(new BorderLayout());
            JTextArea texto = new JTextArea();
            texto.setEditable(false);

            boolean alguno = false;
            for (InscripcionEquipo i : socio.getInscripcionesEquipo())
            {
                if (!i.isActiva()) continue;
                alguno = true;
                Equipo e = i.getEquipo();
                texto.append(e.getNombre() + " | " + e.getDeporte().getNombre()
                        + " | " + e.getCategoria().getNombre()
                        + " | posición: " + i.getPosicion()
                        + " | camiseta: " + i.getNumeroCamiseta() + "\n");
            }
            if (!alguno) texto.setText("No pertenece a ningún equipo.");

            add(new JLabel("  Mis equipos"), BorderLayout.NORTH);
            add(new JScrollPane(texto), BorderLayout.CENTER);
        }
    }

    // ======================= Inscribirme en disciplina =======================
    private static class PanelInscribirDisciplina extends JPanel implements ActionListener
    {
        private final Socio socio;
        private final JComboBox<DisciplinaIndividual> combo = new JComboBox<>();

        PanelInscribirDisciplina(SistemaClub sistema, Socio socio)
        {
            this.socio = socio;
            setLayout(new BorderLayout());
            for (DisciplinaIndividual d : sistema.getDisciplinas())
                combo.addItem(d);
            combo.setRenderer((lista, d, i, sel, foco) ->
                    new JLabel(d == null ? "" : d.getClass().getSimpleName()));

            JButton boton = new JButton("Inscribirme");
            boton.addActionListener(this);

            JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT));
            fila.add(new JLabel("Disciplina:"));
            fila.add(combo);
            fila.add(boton);

            add(new JLabel("  Inscribirme en una disciplina individual"), BorderLayout.NORTH);
            add(fila, BorderLayout.CENTER);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            try
            {
                DisciplinaIndividual d = (DisciplinaIndividual) combo.getSelectedItem();
                InscripcionDisciplina i = socio.inscribirseEnDisciplina(d);
                JOptionPane.showMessageDialog(this, "Inscripción exitosa en "
                        + d.getClass().getSimpleName() + ". Nivel: " + i.getNivel()
                        + " | ranking: " + i.getRanking());
            }
            catch (IllegalArgumentException | IllegalStateException ex)
            {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // ======================= Reservar instalación =======================
    private static class PanelReservar extends JPanel implements ActionListener
    {
        private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        private final Socio socio;
        private final JComboBox<Instalacion> instalacion = new JComboBox<>();
        private final JTextField inicio = new JTextField();
        private final JTextField fin = new JTextField();
        private final JTextField jugadores = new JTextField("2");
        private final JComboBox<String> modalidad = new JComboBox<>(new String[] { "sencillos", "dobles" });

        PanelReservar(SistemaClub sistema, Socio socio)
        {
            this.socio = socio;
            setLayout(new GridLayout(0, 2, 6, 6));
            for (Instalacion i : sistema.getInstalaciones())
                instalacion.addItem(i);
            instalacion.setRenderer((lista, i, idx, sel, foco) ->
                    new JLabel(i == null ? "" : i.getNombre() + " (" + i.getClass().getSimpleName()
                            + (i.isTechada() ? ", techada" : "") + ")"));

            add(new JLabel("Instalación:"));               add(instalacion);
            add(new JLabel("Inicio (AAAA-MM-DD HH:mm):")); add(inicio);
            add(new JLabel("Fin (AAAA-MM-DD HH:mm):"));    add(fin);
            add(new JLabel("Jugadores:"));                 add(jugadores);
            add(new JLabel("Modalidad:"));                 add(modalidad);

            JButton boton = new JButton("Reservar");
            boton.addActionListener(this);
            add(new JLabel());                             add(boton);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            try
            {
                Instalacion inst = (Instalacion) instalacion.getSelectedItem();
                LocalDateTime ini = LocalDateTime.parse(inicio.getText().trim(), FORMATO);
                LocalDateTime fn = LocalDateTime.parse(fin.getText().trim(), FORMATO);
                int n = Integer.parseInt(jugadores.getText().trim());

                Reserva r = socio.reservarInstalacion(inst, ini, fn, n, (String) modalidad.getSelectedItem());
                JOptionPane.showMessageDialog(this, "Reserva creada en " + inst.getNombre()
                        + " de " + r.getFechaHoraInicio() + " a " + r.getFechaHoraFin() + ".");
            }
            catch (DateTimeParseException ex)
            {
                JOptionPane.showMessageDialog(this, "Use el formato AAAA-MM-DD HH:mm (ej. 2026-11-10 15:00).",
                        "Fecha inválida", JOptionPane.WARNING_MESSAGE);
            }
            catch (NumberFormatException ex)
            {
                JOptionPane.showMessageDialog(this, "Jugadores debe ser un número entero.",
                        "Dato inválido", JOptionPane.WARNING_MESSAGE);
            }
            catch (IllegalArgumentException | IllegalStateException | DateTimeException ex)
            {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // ======================= Programar partido de práctica =======================
    private static class PanelProgramarPractica extends JPanel implements ActionListener
    {
        private final Socio yo;
        private final JComboBox<DisciplinaIndividual> disciplina = new JComboBox<>();
        private final JComboBox<Socio> rival = new JComboBox<>();
        private final JComboBox<String> modalidad = new JComboBox<>(new String[] { "sencillos", "dobles" });
        private final JCheckBox conEntrenador = new JCheckBox("Participa un entrenador");
        private final JComboBox<Entrenador> entrenador = new JComboBox<>();

        PanelProgramarPractica(SistemaClub sistema, Socio yo)
        {
            this.yo = yo;
            setLayout(new BorderLayout());

            for (InscripcionDisciplina i : yo.getInscripcionesDisciplina())
                if (i.isActiva()) disciplina.addItem(i.getDisciplina());
            for (Usuario u : sistema.getUsuarios().values())
                if (u instanceof Entrenador) entrenador.addItem((Entrenador) u);

            disciplina.setRenderer((l, d, i, s, f) -> new JLabel(d == null ? "" : d.getClass().getSimpleName()));
            rival.setRenderer((l, r, i, s, f) -> new JLabel(r == null ? "" : r.getNombre() + " (" + r.getLogin() + ")"));
            entrenador.setRenderer((l, t, i, s, f) -> new JLabel(t == null ? "" : t.getNombre() + " (" + t.getLogin() + ")"));

            disciplina.setActionCommand("disciplina");
            disciplina.addActionListener(this);
            recargarRivales();

            JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
            form.add(new JLabel("Mi disciplina:")); form.add(disciplina);
            form.add(new JLabel("Rival:"));         form.add(rival);
            form.add(new JLabel("Modalidad:"));     form.add(modalidad);
            form.add(conEntrenador);                form.add(entrenador);
            JButton boton = new JButton("Programar partido");
            boton.setActionCommand("programar");
            boton.addActionListener(this);
            form.add(new JLabel());                 form.add(boton);

            add(new JLabel(disciplina.getItemCount() == 0
                    ? "  Primero debe inscribirse en una disciplina individual."
                    : "  Programar partido de práctica"), BorderLayout.NORTH);
            add(form, BorderLayout.CENTER);
        }

        private void recargarRivales()
        {
            rival.removeAllItems();
            DisciplinaIndividual d = (DisciplinaIndividual) disciplina.getSelectedItem();
            if (d == null) return;
            for (InscripcionDisciplina i : d.getInscripciones())
                if (i.isActiva() && i.getSocio() != yo) rival.addItem(i.getSocio());
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (e.getActionCommand().equals("disciplina")) { recargarRivales(); return; }
            try
            {
                if (disciplina.getSelectedItem() == null || rival.getSelectedItem() == null)
                    throw new IllegalStateException("Elija una disciplina y un rival.");
                Entrenador ent = conEntrenador.isSelected() ? (Entrenador) entrenador.getSelectedItem() : null;
                PartidoPractica p = yo.programarPartidoPractica((Socio) rival.getSelectedItem(),
                        (DisciplinaIndividual) disciplina.getSelectedItem(),
                        (String) modalidad.getSelectedItem(), ent);
                JOptionPane.showMessageDialog(this, "Partido de práctica programado: " + describir(p));
            }
            catch (IllegalArgumentException | IllegalStateException ex)
            {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private static String describir(PartidoPractica p)
    {
        StringBuilder sb = new StringBuilder(p.getDisciplina().getClass().getSimpleName())
                .append(" (").append(p.getModalidad()).append("): ");
        for (int i = 0; i < p.getParticipantes().size(); i++)
        {
            if (i > 0) sb.append(" vs ");
            sb.append(p.getParticipantes().get(i).getSocio().getNombre())
              .append(" [").append(p.getParticipantes().get(i).getNivel()).append("]");
        }
        return sb.toString();
    }

    // ======================= Registrar resultado de práctica =======================
    private static class PanelResultadoPractica extends JPanel implements ActionListener
    {
        private final Socio yo;
        private final JComboBox<PartidoPractica> partido = new JComboBox<>();
        private final JPanel estadisticas = new JPanel();
        private final List<List<JTextField>> campos = new ArrayList<>();

        PanelResultadoPractica(Socio yo)
        {
            this.yo = yo;
            setLayout(new BorderLayout());
            partido.setRenderer((l, p, i, s, f) -> new JLabel(p == null ? "" : describir(p)));
            partido.setActionCommand("partido");
            cargarPendientes();
            partido.addActionListener(this);

            estadisticas.setLayout(new BoxLayout(estadisticas, BoxLayout.Y_AXIS));
            JButton registrar = new JButton("Registrar resultado");
            registrar.setActionCommand("registrar");
            registrar.addActionListener(this);

            JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT));
            arriba.add(new JLabel("Partido pendiente:"));
            arriba.add(partido);

            add(arriba, BorderLayout.NORTH);
            add(estadisticas, BorderLayout.CENTER);
            add(registrar, BorderLayout.SOUTH);
            reconstruir();
        }

        private void cargarPendientes()
        {
            partido.removeAllItems();
            for (PartidoPractica p : yo.getPartidosPractica())
                if (!p.isResultadoRegistrado()) partido.addItem(p);
        }

        private void reconstruir()
        {
            estadisticas.removeAll();
            campos.clear();
            PartidoPractica p = (PartidoPractica) partido.getSelectedItem();
            if (p == null)
            {
                estadisticas.add(new JLabel("No tiene partidos de práctica pendientes de resultado."));
            }
            else
            {
                boolean mesa = p.getDisciplina() instanceof TenisMesa;
                for (InscripcionDisciplina part : p.getParticipantes())
                {
                    JPanel f = new JPanel(new GridLayout(0, 2, 4, 4));
                    f.setBorder(new TitledBorder("Estadísticas de " + part.getSocio().getNombre()));
                    List<JTextField> fila = new ArrayList<>();
                    agregarCampo(f, fila, "Sets ganados");
                    if (mesa) agregarCampo(f, fila, "Puntos totales");
                    else
                    {
                        agregarCampo(f, fila, "Games ganados");
                        agregarCampo(f, fila, "Aces");
                        agregarCampo(f, fila, "Errores no forzados");
                    }
                    campos.add(fila);
                    estadisticas.add(f);
                }
            }
            estadisticas.revalidate();
            estadisticas.repaint();
        }

        private void agregarCampo(JPanel f, List<JTextField> fila, String etiqueta)
        {
            JTextField c = new JTextField("0");
            f.add(new JLabel(etiqueta + ":"));
            f.add(c);
            fila.add(c);
        }

        private int num(JTextField c)
        {
            try
            {
                int v = Integer.parseInt(c.getText().trim());
                if (v < 0) throw new NumberFormatException();
                return v;
            }
            catch (NumberFormatException ex)
            {
                throw new IllegalArgumentException("Todas las estadísticas deben ser enteros no negativos.");
            }
        }

        private EstadisticaIndividual leer(PartidoPractica p, List<JTextField> f)
        {
            if (p.getDisciplina() instanceof TenisMesa)
                return new EstadisticaTenisMesa(num(f.get(0)), num(f.get(1)));
            return new EstadisticaTenisPadel(num(f.get(0)), num(f.get(1)), num(f.get(2)), num(f.get(3)));
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (e.getActionCommand().equals("partido")) { reconstruir(); return; }
            try
            {
                PartidoPractica p = (PartidoPractica) partido.getSelectedItem();
                if (p == null) throw new IllegalStateException("No hay partido seleccionado.");
                p.registrarResultado(leer(p, campos.get(0)), leer(p, campos.get(1)));

                StringBuilder sb = new StringBuilder("Resultado registrado. Rankings actualizados:\n");
                for (InscripcionDisciplina i : p.getParticipantes())
                    sb.append(String.format("  %s: ranking %d%n", i.getSocio().getNombre(), i.getRanking()));
                JOptionPane.showMessageDialog(this, sb.toString());

                partido.removeActionListener(this);
                cargarPendientes();
                partido.addActionListener(this);
                reconstruir();
            }
            catch (IllegalArgumentException | IllegalStateException ex)
            {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // ======================= Mi desempeño =======================
    private static class PanelDesempeno extends JPanel
    {
        PanelDesempeno(Socio socio)
        {
            setLayout(new BorderLayout());
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Porcentaje de asistencia: %.1f%%%n", socio.getPorcentajeAsistencia()));

            sb.append("\n--- Asistencias y evaluaciones ---\n");
            if (socio.getAsistencias().isEmpty()) sb.append("Sin registros.\n");
            for (Asistencia a : socio.getAsistencias())
                sb.append(String.format("%s | %s | nota: %s | %s%n",
                        a.getSesion() == null ? "-" : a.getSesion().getFechaHora(), a.getEstado(),
                        a.getNotaDesempeno() == null ? "-" : a.getNotaDesempeno().toString(),
                        a.getComentario() == null ? "" : a.getComentario()));

            sb.append("\n--- Estadísticas de partidos de conjunto ---\n");
            int goles = 0, asist = 0, amarillas = 0, rojas = 0;
            for (EstadisticaJugadorPartido e : socio.getEstadisticas())
            {
                if (e instanceof EstadisticaJugadorFutbol)
                {
                    EstadisticaJugadorFutbol fb = (EstadisticaJugadorFutbol) e;
                    goles += fb.getGoles(); asist += fb.getAsistencias();
                    amarillas += fb.getTarjetasAmarillas(); rojas += fb.getTarjetasRojas();
                }
                else if (e instanceof EstadisticaJugadorBaloncesto)
                    sb.append(String.format("Baloncesto: %d puntos, %d rebotes%n",
                            ((EstadisticaJugadorBaloncesto) e).getPuntos(), ((EstadisticaJugadorBaloncesto) e).getRebotes()));
                else if (e instanceof EstadisticaJugadorVoleibol)
                    sb.append(String.format("Voleibol: %d aces, %d bloqueos%n",
                            ((EstadisticaJugadorVoleibol) e).getAces(), ((EstadisticaJugadorVoleibol) e).getBloqueos()));
            }
            sb.append(String.format("Fútbol: %d goles, %d asistencias, %d amarillas, %d rojas%n", goles, asist, amarillas, rojas));

            sb.append("\n--- Disciplinas individuales ---\n");
            boolean alguna = false;
            for (InscripcionDisciplina i : socio.getInscripcionesDisciplina())
            {
                if (!i.isActiva()) continue;
                alguna = true;
                sb.append(String.format("%s | nivel: %s | ranking: %d%n",
                        i.getDisciplina().getClass().getSimpleName(), i.getNivel(), i.getRanking()));
            }
            if (!alguna) sb.append("Sin inscripciones.\n");
            for (EstadisticaIndividual e : socio.getEstadisticasIndividuales())
            {
                if (e instanceof EstadisticaTenisPadel)
                {
                    EstadisticaTenisPadel t = (EstadisticaTenisPadel) e;
                    sb.append(String.format("Partido: sets %d, games %d, aces %d, errores %d%n",
                            t.getSets(), t.getGamesGanados(), t.getAces(), t.getErroresNoForzados()));
                }
                else if (e instanceof EstadisticaTenisMesa)
                {
                    EstadisticaTenisMesa t = (EstadisticaTenisMesa) e;
                    sb.append(String.format("Partido: sets %d, puntos totales %d%n", t.getSets(), t.getPuntosTotales()));
                }
            }

            JTextArea texto = new JTextArea(sb.toString());
            texto.setEditable(false);
            add(new JLabel("  Mi desempeño"), BorderLayout.NORTH);
            add(new JScrollPane(texto), BorderLayout.CENTER);
        }
    }
}