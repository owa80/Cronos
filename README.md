# Cronos

App Android para cronometrar controles de piragüismo (200, 500, 1000, 1500 y 3000 m) a partir de las fotos de los palistas. Funciona sin conexión a internet.

## Funciones

- Biblioteca de fotos: se cargan una sola vez y quedan guardadas en el móvil. El nombre sale del nombre del archivo; tocándolo se abre una ventana para poner el del palista.
- Parrilla de salida de 4 columnas: la primera casilla sale la primera y se sigue por filas. Al tocar un hueco con + se eligen fotos de la biblioteca. Las fotos que no se colocan no participan.
- Salida individual con intervalo (30″, 1′ u otro) o salida conjunta.
- Al pulsar Iniciar sale el primero (amarillo, en ruta) y el siguiente se marca en verde. Con cada intervalo sale el verde y se marca el siguiente. Al llegar, se marca en rojo.
- Cada llegada se registra tocando la foto. El tiempo descuenta la salida escalonada, y la tabla se ordena de menor a mayor tiempo con el día y la diferencia con el ganador.
- Guardar resultados: crea la hoja de cálculo `temporadaXX.xlsx` en Descargas/Cronos (XX lo elige el usuario) con una hoja por día (día-mes-año). Si el archivo ya existe, añade la hoja; varios controles del mismo día van uno debajo de otro. «Nueva temporada» empieza un archivo nuevo.
- La pantalla no se apaga mientras la app está abierta.

## Obtener el .apk sin Android Studio

1. GitHub compila la app en cada push a `main` y en cada PR.
2. Entra en la pestaña **Actions** del repositorio, abre la última ejecución y descarga el artefacto `Cronos-apk`.
3. En las versiones publicadas en `main`, el .apk también aparece en **Releases**.
4. Copia el .apk al móvil y ábrelo. Android pedirá permiso para instalar apps desconocidas la primera vez.

El .apk se firma con una clave de depuración fija incluida en el repositorio, así que cada versión nueva se instala encima de la anterior y conserva las fotos y los datos.

## Estructura

- `app/src/main/assets/index.html`: la app completa (HTML, CSS y JavaScript en un solo archivo).
- `app/src/main/assets/xlsx.mini.min.js`: SheetJS 0.18.5 (Apache 2.0), para leer y escribir las hojas de cálculo sin internet.
- `app/src/main/java/com/cronos/SeasonFiles.kt`: guarda y lee los archivos de temporada en Descargas/Cronos.
- `app/src/main/java/com/cronos/MainActivity.kt`: muestra la página en un WebView, abre el selector de fotos y bloquea cualquier acceso a internet.
- `.github/workflows/build-apk.yml`: compilación automática.

## Datos

Las fotos, la parrilla y los tiempos se guardan en el almacenamiento del propio móvil (IndexedDB y localStorage del WebView). Si se borran los datos de la app desde los ajustes de Android, habrá que cargar las fotos de nuevo.
