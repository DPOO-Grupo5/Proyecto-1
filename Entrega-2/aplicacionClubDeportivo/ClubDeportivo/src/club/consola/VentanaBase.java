package club.consola;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.time.DateTimeException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import club.model.SistemaClub;
import club.modelo.tienda.DetalleTransaccion;
import club.modelo.tienda.Producto;
import club.modelo.tienda.ProductoCafeteria;
import club.modelo.tienda.ProductoTienda;
import club.modelo.tienda.Transaccion;
import club.modelo.usuarios.Empleado;
import club.modelo.usuarios.Usuario;
import club.persistencia.ArchivoPlano;

public abstract class VentanaBase extends JFrame implements ActionListener
{
    protected SistemaClub sistema;
    protected Usuario usuario;
    private final ArchivoPlano archivo = new ArchivoPlano();
    private JPanel centro;

    // opciones que tienen TODOS los usuarios
    private static final String TIENDA = "Comprar en la tienda";
    private static final String CAFETERIA = "Comprar en la cafetería";
    private static final String COMPRAS = "Mis compras y puntos";
    private static final String PASSWORD = "Cambiar contraseña";
    private static final String[] COMUNES = { TIENDA, CAFETERIA, COMPRAS, PASSWORD };

    // lo único que cambia entre los 3 mains
    protected abstract String nombreRol();
    protected abstract boolean admite(Usuario u);
    protected abstract String[] opciones();
    protected abstract JPanel crearPanel(String opcion);

    protected final void iniciar()
    {
        if (!archivo.existeEstado())
        {
            JOptionPane.showMessageDialog(null, "No se encontraron los CSV en la carpeta " + archivo.getCarpeta(),
                    "Club Séneca", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        sistema = archivo.cargarEstado();
        sistema.completarDatosFaltantes();
        usuario = autenticar();
        if (usuario == null) System.exit(0);
        construirVentana();
    }

    private Usuario autenticar()
    {
        JTextField login = new JTextField();
        JPasswordField clave = new JPasswordField();
        Object[] campos = { "Usuario:", login, "Contraseña:", clave };
        while (true)
        {
            int r = JOptionPane.showConfirmDialog(null, campos, "Ingreso - " + nombreRol(), JOptionPane.OK_CANCEL_OPTION);
            if (r != JOptionPane.OK_OPTION) return null;
            try
            {
                Usuario u = sistema.autenticar(login.getText().trim(), new String(clave.getPassword()));
                if (u == null) JOptionPane.showMessageDialog(null, "Usuario o contraseña incorrectos.");
                else if (!admite(u)) JOptionPane.showMessageDialog(null, "Este acceso es solo para " + nombreRol() + ".");
                else return u;
            }
            catch (IllegalArgumentException | IllegalStateException ex)
            {
                JOptionPane.showMessageDialog(null, ex.getMessage());
            }
            clave.setText("");
        }
    }

    private void construirVentana()
    {
        setTitle("Club Deportivo Séneca - " + nombreRol());
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);   // al cerrar se llama processWindowEvent

        JLabel norte = new JLabel("  " + usuario.getNombre() + "  (" + nombreRol() + ")");
        norte.setOpaque(true);
        norte.setBackground(new Color(30, 60, 110));
        norte.setForeground(Color.WHITE);
        norte.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(norte, BorderLayout.NORTH);

        centro = new JPanel(new BorderLayout());
        add(centro, BorderLayout.CENTER);

        JPanel botones = new JPanel(new GridLayout(0, 1, 4, 4));
        botones.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        for (String opcion : opciones())
            botones.add(crearBoton(opcion));
        botones.add(new JLabel("— Para todos —", JLabel.CENTER));
        for (String opcion : COMUNES)
            botones.add(crearBoton(opcion));
        JPanel oeste = new JPanel(new BorderLayout());
        oeste.add(botones, BorderLayout.NORTH);
        add(oeste, BorderLayout.WEST);

        setVisible(true);
    }

    private JButton crearBoton(String texto)
    {
        JButton b = new JButton(texto);
        b.setActionCommand(texto);
        b.addActionListener(this);
        return b;
    }

    /** La ventana se entera de que la cierran: guarda los CSV y sale. */
    @Override
    protected void processWindowEvent(WindowEvent e)
    {
        if (e.getID() == WindowEvent.WINDOW_CLOSING)
        {
            archivo.guardarEstado(sistema);
            dispose();
            System.exit(0);
        }
        super.processWindowEvent(e);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        try
        {
            JPanel panel = crearPanelComun(e.getActionCommand());
            if (panel == null) panel = crearPanel(e.getActionCommand());
            if (panel == null) return;
            centro.removeAll();
            centro.add(panel, BorderLayout.CENTER);
            centro.revalidate();
            centro.repaint();
        }
        catch (IllegalArgumentException | IllegalStateException | DateTimeException ex)
        {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Paneles que son iguales para cualquier usuario; null si la opción no es común. */
    private JPanel crearPanelComun(String opcion)
    {
        switch (opcion)
        {
            case TIENDA:    return panelComprar(true);
            case CAFETERIA: return panelComprar(false);
            case COMPRAS:   return panelMisCompras();
            case PASSWORD:  return panelCambiarPassword();
            default:        return null;
        }
    }

    // ---------- utilidades ----------
    /** Ejecuta la acción de un botón; si el modelo lanza un error de negocio, lo muestra. */
    private static void protegido(Component padre, Runnable accion)
    {
        try
        {
            accion.run();
        }
        catch (IllegalArgumentException | IllegalStateException | DateTimeException ex)
        {
            JOptionPane.showMessageDialog(padre, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static int entero(JTextField campo, String nombre, int min, int max)
    {
        try
        {
            int v = Integer.parseInt(campo.getText().trim());
            if (v < min || v > max)
                throw new IllegalArgumentException(nombre + " debe estar entre " + min + " y " + max + ".");
            return v;
        }
        catch (NumberFormatException ex)
        {
            throw new IllegalArgumentException(nombre + " debe ser un número entero.");
        }
    }

    private static String resumen(Transaccion t)
    {
        return String.format("Subtotal: $%.0f%nDescuento: $%.0f%nImpuesto: $%.0f%nPropina: $%.0f",
                t.calcularSubtotal(), t.getDescuento(), t.calcularImpuestos(), t.getPropina());
    }

    // ======================= Mis compras y puntos =======================
    private JPanel panelMisCompras()
    {
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea texto = new JTextArea();
        texto.setEditable(false);

        int cuantas = 0;
        for (Transaccion t : usuario.consultarHistorialCompras())
        {
            if (!t.isConfirmada()) continue;
            cuantas++;
            texto.append(String.format("%s | %s | total: $%.0f | puntos ganados: %d | puntos usados: %d%n",
                    t.getFechaHora(), t.getRubro(), t.getTotal(), t.getPuntosFidelidad(), t.getPuntosRedimidos()));
            for (DetalleTransaccion d : t.getDetalles())
                texto.append(String.format("     %d x %s%n", d.getCantidad(), d.getProducto().getNombre()));
        }
        if (cuantas == 0) texto.append("Sin compras.\n");
        texto.append(String.format("%nSaldo de puntos de fidelidad: %d (1 punto = $1)", usuario.getPuntosFidelidad()));

        panel.add(new JLabel("  Mis compras y puntos"), BorderLayout.NORTH);
        panel.add(new JScrollPane(texto), BorderLayout.CENTER);
        return panel;
    }

    // ======================= Cambiar contraseña =======================
    private JPanel panelCambiarPassword()
    {
        JPanel panel = new JPanel(new BorderLayout());
        JPasswordField actual = new JPasswordField();
        JPasswordField nueva = new JPasswordField();
        JPasswordField repetida = new JPasswordField();

        JButton boton = new JButton("Cambiar contraseña");
        boton.addActionListener(e -> protegido(panel, () ->
        {
            String a = new String(actual.getPassword());
            String n = new String(nueva.getPassword());
            if (a.isEmpty() || n.isEmpty())
                throw new IllegalArgumentException("Complete la contraseña actual y la nueva.");
            if (!n.equals(new String(repetida.getPassword())))
                throw new IllegalArgumentException("La nueva contraseña y su repetición no coinciden.");
            usuario.actualizarPassword(a, n);
            actual.setText(""); nueva.setText(""); repetida.setText("");
            JOptionPane.showMessageDialog(panel, "Contraseña actualizada.");
        }));

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Contraseña actual:"));        form.add(actual);
        form.add(new JLabel("Nueva contraseña:"));         form.add(nueva);
        form.add(new JLabel("Repetir nueva contraseña:")); form.add(repetida);
        form.add(new JLabel());                            form.add(boton);

        panel.add(new JLabel("  Cambiar contraseña"), BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        return panel;
    }

    // ======================= Comprar (tienda o cafetería) =======================
    private JPanel panelComprar(boolean tienda)
    {
        String rubro = tienda ? "tienda" : "cafeteria";
        Map<Producto, Integer> carrito = new LinkedHashMap<>();

        JPanel panel = new JPanel(new BorderLayout());
        JComboBox<Producto> productos = new JComboBox<>();
        JTextField cantidad = new JTextField("1", 5);
        JTextArea salida = new JTextArea();
        salida.setEditable(false);

        // campos opcionales: solo existen según el tipo de usuario y de compra
        JTextField codigo = null;
        JCheckBox descuentoEmpleado = null;
        JTextField puntos = null;
        JTextField propina = null;

        productos.setRenderer((l, p, i, s, f) -> new JLabel(p == null ? ""
                : String.format("%s - $%.0f (disponibles: %d)", p.getNombre(), p.getPrecio(), p.getCantidadTotal())));

        Runnable cargarProductos = () ->
        {
            productos.removeAllItems();
            for (Producto p : sistema.getProductos())
            {
                boolean delRubro = tienda ? p instanceof ProductoTienda : p instanceof ProductoCafeteria;
                if (delRubro && p.getCantidadTotal() > 0) productos.addItem(p);
            }
        };
        cargarProductos.run();

        Runnable mostrarCarrito = () ->
        {
            StringBuilder sb = new StringBuilder("--- Carrito ---\n");
            if (carrito.isEmpty()) sb.append("Vacío.\n");
            double subtotal = 0;
            for (Map.Entry<Producto, Integer> e : carrito.entrySet())
            {
                double s = e.getKey().getPrecio() * e.getValue();
                subtotal += s;
                sb.append(String.format("%d x %-30s $%.0f%n", e.getValue(), e.getKey().getNombre(), s));
            }
            sb.append(String.format("%nSubtotal (sin descuentos ni impuestos): $%.0f%n", subtotal));
            salida.setText(sb.toString());
        };

        // ---- fila 1: elegir productos ----
        JPanel fila1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton agregar = new JButton("Agregar al carrito");
        JButton vaciar = new JButton("Vaciar");
        fila1.add(new JLabel("Producto:")); fila1.add(productos);
        fila1.add(new JLabel("Cantidad:")); fila1.add(cantidad);
        fila1.add(agregar);
        fila1.add(vaciar);

        agregar.addActionListener(e -> protegido(panel, () ->
        {
            Producto p = (Producto) productos.getSelectedItem();
            if (p == null) throw new IllegalStateException("No hay productos disponibles.");
            int total = carrito.getOrDefault(p, 0) + entero(cantidad, "La cantidad", 1, 1000);
            if (total > p.getCantidadTotal())
                throw new IllegalStateException("No hay existencias suficientes de " + p.getNombre()
                        + " (disponibles: " + p.getCantidadTotal() + ").");
            carrito.put(p, total);
            mostrarCarrito.run();
        }));
        vaciar.addActionListener(e -> { carrito.clear(); mostrarCarrito.run(); });

        // ---- fila 2: descuentos, puntos, propina y pago ----
        JPanel fila2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        if (tienda && usuario instanceof Empleado)
        {
            descuentoEmpleado = new JCheckBox("Aplicar mi descuento de empleado (15 %)");
            fila2.add(descuentoEmpleado);
        }
        else if (tienda)
        {
            codigo = new JTextField(10);
            fila2.add(new JLabel("Código de descuento (opcional):")); fila2.add(codigo);
        }
        if (usuario.getPuntosFidelidad() > 0)
        {
            puntos = new JTextField("0", 5);
            fila2.add(new JLabel("Puntos a redimir (tiene " + usuario.getPuntosFidelidad() + "):"));
            fila2.add(puntos);
        }
        if (!tienda)
        {
            propina = new JTextField("10", 5);
            fila2.add(new JLabel("Propina % (sugerida 10):")); fila2.add(propina);
        }
        JButton verResumen = new JButton("Ver resumen");
        JButton confirmar = new JButton("Confirmar compra");
        fila2.add(verResumen);
        fila2.add(confirmar);

        // las variables capturadas por lambdas deben ser finales: copias finales
        final JTextField campoCodigo = codigo;
        final JCheckBox checkEmpleado = descuentoEmpleado;
        final JTextField campoPuntos = puntos;
        final JTextField campoPropina = propina;

        // arma la transacción con todo lo elegido; si algo falla, la saca del historial
        Supplier<Transaccion> armar = () ->
        {
            if (carrito.isEmpty()) throw new IllegalStateException("Agregue al menos un producto al carrito.");
            Transaccion t = usuario.iniciarTransaccion(null, rubro);
            try
            {
                for (Map.Entry<Producto, Integer> e : carrito.entrySet())
                    t.agregarDetalle(e.getKey(), e.getValue());
                if (checkEmpleado != null && checkEmpleado.isSelected())
                    t.aplicarDescuentoEmpleado((Empleado) usuario);
                if (campoCodigo != null && !campoCodigo.getText().trim().isEmpty())
                {
                    String cod = campoCodigo.getText().trim();
                    Empleado dueno = null;
                    for (Usuario u : sistema.getUsuarios().values())
                        if (u instanceof Empleado && ((Empleado) u).validarCodigo(cod)) dueno = (Empleado) u;
                    t.aplicarDescuentoCompartido(cod, dueno);
                }
                if (campoPuntos != null)
                {
                    int p = entero(campoPuntos, "Los puntos", 0, Integer.MAX_VALUE);
                    if (p > 0) t.redimirPuntos(p);
                }
                if (campoPropina != null)
                    t.calcularPropina(entero(campoPropina, "La propina", 0, 100));
                return t;
            }
            catch (RuntimeException ex)
            {
                usuario.getTransacciones().remove(t);
                throw ex;
            }
        };

        verResumen.addActionListener(e -> protegido(panel, () ->
        {
            Transaccion t = armar.get();
            try
            {
                StringBuilder sb = new StringBuilder("--- Resumen (aún sin confirmar) ---\n").append(resumen(t)).append('\n');
                List<String> alergenos = t.consultarAlergenos();
                if (!tienda && !alergenos.isEmpty())
                    sb.append("\nAlérgenos de su pedido: ").append(String.join(", ", alergenos)).append('\n');
                salida.setText(sb.toString());
            }
            finally
            {
                usuario.getTransacciones().remove(t);
            }
        }));

        confirmar.addActionListener(e -> protegido(panel, () ->
        {
            Transaccion t = armar.get();
            try
            {
                if (!tienda)
                {
                    List<String> alergenos = t.consultarAlergenos();
                    if (!alergenos.isEmpty() && JOptionPane.showConfirmDialog(panel,
                            "ATENCIÓN - alérgenos de su pedido: " + String.join(", ", alergenos)
                            + "\n¿Desea continuar con la compra?", "Confirmar", JOptionPane.YES_NO_OPTION)
                            != JOptionPane.YES_OPTION)
                        throw new IllegalStateException("Compra cancelada por alérgenos.");
                }
                if (JOptionPane.showConfirmDialog(panel, resumen(t) + "\n\n¿Confirmar la compra?",
                        "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                    throw new IllegalStateException("Compra cancelada.");
                t.confirmar();
            }
            catch (RuntimeException ex)
            {
                if (!t.isConfirmada()) usuario.getTransacciones().remove(t);
                throw ex;
            }
            String mensaje = String.format("Compra confirmada. Total: $%.0f | puntos ganados: %d | saldo de puntos: %d",
                    t.getTotal(), t.getPuntosFidelidad(), usuario.getPuntosFidelidad());
            carrito.clear();
            cargarProductos.run();
            salida.setText(mensaje);
            JOptionPane.showMessageDialog(panel, mensaje);
        }));

        JPanel arriba = new JPanel();
        arriba.setLayout(new BoxLayout(arriba, BoxLayout.Y_AXIS));
        arriba.add(new JLabel(tienda ? "  Comprar en la tienda" : "  Comprar en la cafetería"));
        arriba.add(fila1);
        arriba.add(fila2);

        panel.add(arriba, BorderLayout.NORTH);
        panel.add(new JScrollPane(salida), BorderLayout.CENTER);
        mostrarCarrito.run();
        return panel;
    }
}