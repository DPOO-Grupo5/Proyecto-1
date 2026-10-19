<div align="center">

# ⚽ Club Deportivo Séneca
### Entrega 2 · Programación Orientada a Objetos

**Gestión de un club deportivo desde un modelo Java con persistencia local y una consola de demostración.**

`Java` · `Eclipse` · `Serialización` · `PBKDF2`

</div>

---

## 🧭 Recorrido rápido

- [Qué incluye](#-qué-incluye)
- [Perfiles y consola](#-perfiles-y-consola)
- [Abrir en Eclipse](#-abrir-en-eclipse)
- [Compilar y ejecutar en PowerShell](#-compilar-y-ejecutar-en-powershell)
- [Verificaciones](#-verificaciones)
- [Persistencia y datos de demostración](#-persistencia-y-datos-de-demostración)
- [Diagramas y material](#-diagramas-y-material)

## ✨ Qué incluye

El proyecto modela áreas habituales de un club:

| Área | Elementos del modelo |
|---|---|
| 👥 Usuarios | Socios, entrenadores, fisioterapeutas y administradores |
| 🏆 Competencias | Equipos, deportes, categorías, inscripciones, partidos y sanciones |
| 🏋️ Entrenamiento | Sesiones, asistencias, disciplinas y partidos de práctica |
| 🏟️ Instalaciones | Canchas, reservas y espacios deportivos |
| 🛍️ Tienda y cafetería | Productos, existencias, transacciones, impuestos y puntos |

La aplicación ejecutable de esta entrega es una **consola básica**: permite iniciar sesión, consultar resúmenes, cambiar contraseña y, desde el perfil administrador, crear usuarios. Las demás operaciones del modelo no se presentan como opciones de esa consola.

## 👤 Perfiles y consola

| Perfil | Opciones visibles en la consola |
|---|---|
| Administrador | Resumen del sistema, creación de usuarios, cambio de contraseña y cierre de sesión |
| Socio | Resumen personal y cambio de contraseña |
| Entrenador | Resumen con sesiones planificadas y cambio de contraseña |
| Fisioterapeuta | Resumen de empleado y cambio de contraseña |

En una primera ejecución, la consola prepara cuentas y datos de demostración si el estado del club aún no existe. Credenciales iniciales del entorno demo:

| Usuario | Contraseña |
|---|---|
| `admin` | `admin123` |
| `socio.demo` | `demo123` |

Son credenciales públicas de demostración. Cámbialas antes de conservar o compartir un estado con datos propios.

## 🖥️ Abrir en Eclipse

1. Selecciona **File → Import → Existing Projects into Workspace**.
2. Elige `aplicacionClubDeportivo/ClubDeportivo` y completa la importación.
3. Abre `src/club/consola/Consola.java` y ejecuta **Run As → Java Application**.
4. Inicia sesión con una cuenta demo de la tabla anterior.

Para ejecutar las verificaciones, abre `src/club/pruebas/VerificacionEntrega.java` y selecciona **Run As → Java Application**.

## ⌨️ Compilar y ejecutar en PowerShell

Desde la raíz de este repositorio, compila todas las fuentes en una carpeta temporal:

```powershell
$src = 'aplicacionClubDeportivo/ClubDeportivo/src'
$out = Join-Path $env:TEMP 'seneca-classes'
New-Item -ItemType Directory -Force $out | Out-Null
$files = Get-ChildItem -Path $src -Recurse -Filter *.java | ForEach-Object FullName
javac -encoding UTF-8 -d $out $files
```

Ejecuta las verificaciones:

```powershell
java -cp $out club.pruebas.VerificacionEntrega
```

Ejecuta la consola usando un directorio temporal para que los datos de esta prueba queden separados del repositorio:

```powershell
$datos = Join-Path $env:TEMP 'seneca-demo'
java "-Dclub.datos=$datos" -cp $out club.consola.Consola
```

Se requiere un JDK instalado y disponible en `PATH` (`javac` y `java`). En Eclipse, importa la carpeta del proyecto indicada arriba.

## ✅ Verificaciones

`club.pruebas.VerificacionEntrega` es un verificador Java ligero, sin dependencia de JUnit. Comprueba estos ocho resultados:

1. Inicio de sesión válido sin distinguir mayúsculas en el login.
2. Rechazo de una contraseña incorrecta.
3. Almacenamiento de la contraseña con hash PBKDF2.
4. Rechazo de una inscripción que supera el cupo del equipo.
5. Rechazo de una reserva que se cruza con otra en la misma cancha.
6. Rechazo de una venta sin existencias suficientes, sin descontar stock.
7. Venta válida en cafetería, con impuesto del 8% y descuento de inventario.
8. Guardado y recarga de un usuario desde una carpeta temporal aislada.

El resultado esperado es `Verificaciones correctas: 8/8`. La prueba de persistencia crea una carpeta temporal nueva; no usa los datos habituales del club.

## 💾 Persistencia y datos de demostración

- Por defecto, el estado se guarda en `datos_seneca/club.ser`, relativo al directorio de trabajo del proceso.
- Puedes cambiar el directorio con la propiedad Java `-Dclub.datos=RUTA` (en Eclipse: **Run Configurations → Arguments → VM arguments**).
- La aplicación guarda el estado al salir y después de ciertas acciones del menú.
- Las contraseñas nuevas se almacenan con hash PBKDF2. El inicio de sesión también admite migrar contraseñas legadas en texto claro después de una validación correcta.
- El archivo serializado contiene datos de usuarios. No lo publiques ni lo compartas si utilizas información propia.

## 🗺️ Diagramas y material

- [Diagrama de alto nivel](Diagramas/Diagrama_alto_nivel.drawio.png)
- [Diagrama de diseño con métodos](Diagramas/Diagrama_de_diseño_con_m%C3%A9todos.drawio.png)
- [Modelo general](Diagramas/Proyecto_DPOO_Entrega2.drawio.png)
- [Diagramas de secuencia](Diagramas/Diagramas%20de%20secuencia/PNG/)
- [Presentación para grabar la entrega](aplicacionClubDeportivo/ClubDeportivo/docs/PRESENTACION_VIDEO_ENTREGA_2.html)

Los diagramas documentan el diseño. Para confirmar qué operaciones están disponibles actualmente, consulta la consola y el código fuente.

---

<div align="center">

Hecho para la Entrega 2 · Club Deportivo Séneca

</div>
