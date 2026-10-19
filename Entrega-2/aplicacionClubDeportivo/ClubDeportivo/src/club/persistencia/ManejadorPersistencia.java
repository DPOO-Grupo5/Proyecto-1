package club.persistencia;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;

import club.model.SistemaClub;
import club.modelo.usuarios.Administrador;
import club.modelo.usuarios.Entrenador;
import club.modelo.usuarios.Fisioterapeuta;
import club.modelo.usuarios.Socio;
import club.modelo.usuarios.Usuario;

public final class ManejadorPersistencia {
    private static final String DIRECTORIO_PREDETERMINADO = "datos_seneca";

    private ManejadorPersistencia() { }

    private static String directorio() {
        String configurado = System.getProperty("club.datos");
        return configurado == null || configurado.trim().isEmpty() ? DIRECTORIO_PREDETERMINADO : configurado;
    }

    private static File archivoEstado() { return new File(directorio(), "club.ser"); }

    public static boolean existeEstado() {
        return archivoEstado().isFile();
    }

    public static void guardarEstado(SistemaClub sistema) {
        if (sistema == null) throw new IllegalArgumentException("El estado del club es obligatorio.");
        File directorio = new File(directorio());
        if (!directorio.exists() && !directorio.mkdirs()) throw new IllegalStateException("No se pudo crear el directorio de datos.");
        Path temporal = new File(directorio, "club.ser.tmp").toPath();
        Path destino = archivoEstado().toPath();
        try (FileOutputStream archivo = new FileOutputStream(temporal.toFile());
             ObjectOutputStream salida = new ObjectOutputStream(archivo)) {
            salida.writeObject(sistema);
            salida.flush();
            archivo.getFD().sync();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el estado completo del club.", e);
        }
        try {
            Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            try { Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING); }
            catch (IOException error) { throw new IllegalStateException("No se pudo reemplazar el archivo de estado.", error); }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo reemplazar el archivo de estado.", e);
        }
    }

    public static SistemaClub cargarEstado() {
        File estado = archivoEstado();
        if (estado.isFile()) {
            try (ObjectInputStream entrada = new ObjectInputStream(new FileInputStream(estado))) {
                Object objeto = entrada.readObject();
                if (!(objeto instanceof SistemaClub)) throw new IllegalStateException("El archivo de datos no contiene un estado válido del club.");
                return (SistemaClub) objeto;
            } catch (IOException | ClassNotFoundException e) {
                throw new IllegalStateException("No se pudo cargar el estado guardado del club.", e);
            }
        }

        SistemaClub sistema = cargarUsuariosLegados();
        if (sistema.getUsuarios().isEmpty()) sistema.registrarUsuario(new Administrador("admin", "admin123", "Super Administrador"));
        return sistema;
    }

    private static SistemaClub cargarUsuariosLegados() {
        SistemaClub sistema = new SistemaClub();
        File archivo = new File(directorio(), "usuarios.txt");
        if (!archivo.isFile()) return sistema;
        try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
            String linea;
            int numeroLinea = 0;
            while ((linea = lector.readLine()) != null) {
                numeroLinea++;
                if (linea.trim().isEmpty()) continue;
                String[] partes = linea.split(";", -1);
                if (partes.length < 4) throw new IllegalStateException("Registro incompleto en usuarios.txt, línea " + numeroLinea + ".");
                String tipo = partes[0];
                String login = partes[1];
                String password = partes[2];
                String nombre = partes[3];
                Usuario usuario;
                switch (tipo) {
                    case "Administrador": usuario = new Administrador(login, password, nombre); break;
                    case "Socio":
                        if (partes.length < 5) throw new IllegalStateException("Falta la fecha de nacimiento en línea " + numeroLinea + ".");
                        usuario = new Socio(login, password, nombre, LocalDate.parse(partes[4]));
                        break;
                    case "Entrenador":
                        if (partes.length < 5) throw new IllegalStateException("Falta el código de descuento en línea " + numeroLinea + ".");
                        usuario = new Entrenador(login, password, nombre, partes[4]);
                        break;
                    case "Fisioterapeuta":
                        if (partes.length < 5) throw new IllegalStateException("Falta el código de descuento en línea " + numeroLinea + ".");
                        usuario = new Fisioterapeuta(login, password, nombre, partes[4]);
                        break;
                    default: throw new IllegalStateException("Tipo de usuario no reconocido en línea " + numeroLinea + ".");
                }
                sistema.registrarUsuario(usuario);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudieron leer los usuarios guardados previamente.", e);
        }
        return sistema;
    }
}
