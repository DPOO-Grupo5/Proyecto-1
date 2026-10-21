# Club Deportivo Séneca · Entrega 2

Proyecto 1 de Diseño y Programación Orientada a Objetos (Universidad de los Andes).
Modelo en Java del Club Deportivo Séneca, con persistencia en archivos y una consola
con un menú por rol.

**Integrantes:** Yury Dayana Velasquez Alvarez, Nelson Fabian Reyes Marquez y Juan José Sarmiento Bello.

## Contenido del repositorio

| Carpeta o archivo | Qué contiene |
|---|---|
| `aplicacionClubDeportivo/ClubDeportivo/src` | Código fuente Java (proyecto de Eclipse, sin Maven ni Gradle) |
| `aplicacionClubDeportivo/ClubDeportivo/docs` | Documento de diseño (PDF) y presentación para el video |
| `Diagramas/` | Diagramas de clases (alto nivel y detallado) y diagramas de secuencia |

### Paquetes principales

| Paquete | Responsabilidad |
|---|---|
| `club.consola` | `Consola`: punto de entrada, inicio de sesión y menús por rol |
| `club.model` | `SistemaClub`: registro en memoria de usuarios, equipos, competencias, productos, instalaciones y disciplinas |
| `club.modelo.usuarios` | Socio, empleados (entrenador y fisioterapeuta), administrador, turnos y solicitudes de cambio de turno |
| `club.modelo.competencias` | Deportes de conjunto, equipos, inscripciones, partidos, competencias, formatos, estadísticas y sanciones |
| `club.modelo.entrenamiento` | Disciplinas individuales, sesiones, asistencia, partidos de práctica y estadísticas |
| `club.modelo.instalaciones` | Instalaciones y reservas |
| `club.modelo.tienda` | Productos, existencias, sugerencias y transacciones |
| `club.persistencia` | `ArchivoPlano` (CSV, usado por la consola) y `ManejadorPersistencia` (serialización, alternativa no usada por la consola) |
| `club.pruebas` | `VerificacionEntrega`: comprobaciones ejecutables sin JUnit |

Los paquetes `club.model` y `club.modelo` son dos paquetes distintos; hay que respetarlo al importar clases.

## Funcionalidades por rol

Al iniciar sesión, la consola muestra el menú del rol del usuario.

| Rol | Opciones del menú | Requerimientos |
|---|---|---|
| Socio | 1 a 9: equipos y partidos, inscripción en disciplina individual, reservar instalación, partido de práctica, resultado de práctica, desempeño, compra en tienda, compra en cafetería, compras y puntos | RF-SOC-01 a 09 |
| Empleado (entrenador y fisioterapeuta) | 1 a 5: turnos, solicitar cambio de turno, compra con descuento, compartir código, sugerir producto | RF-EMP-01 a 05 |
| Entrenador (además de lo anterior) | 6 a 11: planear sesión, asistencia y evaluación, asignar nivel, partido interno, partido de competencia, analítica | RF-ENT-01 a 06 |
| Administrador | 1 a 19: usuarios, equipos, inscripción de jugadores, competencias, inventario, sugerencias, turnos, reportes de ventas y deportivos | RF-ADM-01 a 09 |

Todos los roles pueden cambiar su contraseña con la opción `P` y cerrar sesión con `0`.
El fisioterapeuta usa solo las opciones comunes de empleado.

Los requerimientos y las reglas de dominio están descritos en el documento de diseño
(`docs/Entrega2DPOO_actualizado-1.pdf`).

## Cómo ejecutarlo

Se necesita un **JDK** (17 o superior) con `javac` y `java` en el `PATH`.

### En Eclipse

1. File → Import → Existing Projects into Workspace.
2. Seleccionar la carpeta `aplicacionClubDeportivo/ClubDeportivo`.
3. Ejecutar `src/club/consola/Consola.java` con Run As → Java Application.

### En Visual Studio Code

1. Abrir la carpeta `aplicacionClubDeportivo/ClubDeportivo` (la que contiene `src`).
2. Tener instalado el paquete de extensiones de Java.
3. Ejecutar `club.consola.Consola`.

### Desde la terminal

Ubicarse en la carpeta `aplicacionClubDeportivo/ClubDeportivo`, porque la carpeta de datos
se resuelve respecto al directorio de trabajo.

```bash
# Linux / macOS
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -Dfile.encoding=UTF-8 -cp out club.consola.Consola
```

```powershell
# Windows (PowerShell)
New-Item -ItemType Directory -Force out | Out-Null
$files = Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object FullName
javac -encoding UTF-8 -d out $files
java -Dfile.encoding=UTF-8 -cp out club.consola.Consola
```

Si las tildes se ven mal en la consola, es un tema de codificación del terminal y no afecta la lógica.

### Usuarios de demostración

En la primera ejecución, si no hay datos guardados, la consola crea un estado inicial
y muestra estos usuarios:

| Login | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | Administrador |
| `entrenador.demo` | `entre123` | Entrenador |
| `fisio.demo` | `fisio123` | Fisioterapeuta |
| `socio.demo` | `demo123` | Socio |
| `socio.demo2` | `demo123` | Socio |

Son credenciales de demostración. Conviene cambiarlas si se conservan los datos.

## Persistencia

- La consola guarda el estado con `ArchivoPlano` en varios archivos CSV (uno por clase concreta),
  más un `indice.csv`. Las referencias entre objetos se guardan por identificador.
- La carpeta predeterminada es `src/datos`, relativa al directorio de trabajo. Se crea sola en la
  primera ejecución. Al guardar, los archivos se escriben primero como temporales y luego
  reemplazan a los definitivos.
- El estado se guarda después de cada acción del menú y al salir.
- `ManejadorPersistencia` guarda todo el estado en un único archivo serializado (`club.ser`) y
  se conserva como mecanismo alterno; la consola no lo usa. La propiedad `-Dclub.datos=RUTA`
  solo afecta a esta clase.
- Las contraseñas nuevas se guardan con hash PBKDF2 con sal.
- Para reiniciar los datos de demostración, basta con borrar la carpeta `src/datos` y volver a ejecutar.

## Verificaciones

`club.pruebas.VerificacionEntrega` es un programa de comprobación ligero, sin JUnit.
Ejecutarlo con:

```bash
java -cp out club.pruebas.VerificacionEntrega
```

Comprueba ocho casos:

1. Inicio de sesión válido, sin distinguir mayúsculas en el login.
2. Rechazo de una contraseña incorrecta.
3. Contraseña almacenada como hash.
4. Rechazo de una inscripción que supera el cupo del equipo.
5. Rechazo de una reserva que se traslapa con otra en la misma cancha.
6. Rechazo de una venta sin existencias suficientes, sin descontar inventario.
7. Venta válida en cafetería, con impuesto del 8 % y descuento de inventario.
8. Guardado y recarga de un usuario en una carpeta temporal.

El resultado esperado es `Verificaciones correctas: 8/8`. Estas pruebas no cubren todos los
requerimientos; el resto se recorre desde los menús de la consola.

## Limitaciones conocidas

- La autorización por rol se aplica en los menús de la consola. Los métodos del modelo no
  comprueban el rol del llamador.
- La carpeta de datos por defecto está dentro de `src`, mientras que el enunciado pide una
  carpeta distinta a la del código. Se puede cambiar pasando otra ruta al constructor
  `ArchivoPlano(String carpeta)` en `Consola`.

## Diagramas

- [Diagrama de alto nivel](Diagramas/Diagrama_alto_nivel.drawio.png)
- [Diagrama de diseño con métodos](Diagramas/Diagrama_de_dise%C3%B1o_con_m%C3%A9todos.drawio.png)
- [Diagrama detallado adicional](Diagramas/Proyecto_DPOO_Entrega2.drawio.png)
- [Diagramas de secuencia](Diagramas/Diagramas%20de%20secuencia/PNG/)
- [Presentación para el video](aplicacionClubDeportivo/ClubDeportivo/docs/PRESENTACION_VIDEO_ENTREGA_2.html)