# Cronos

App Android para cronometrar controles de piragüismo (200, 500, 1000, 1500 y 3000 m) a partir de las fotos de los palistas. Funciona sin conexión a internet.

## Funciones

- Biblioteca de fotos: se cargan una sola vez y quedan guardadas en el móvil. El nombre sale del nombre del archivo y se puede cambiar.
- Parrilla de salida de 4 columnas: la primera casilla sale la primera y se sigue por filas. Las fotos que no se colocan no participan.
- Salida individual con intervalo (30″, 1′ u otro) o salida conjunta.
- Al iniciar, el palista al que le toca salir se marca en verde; al cumplirse el intervalo pasa a amarillo (en ruta) y el siguiente a verde. Al llegar, se marca en rojo.
- Cada llegada se registra tocando la foto. El tiempo descuenta la salida escalonada, y la tabla se ordena de menor a mayor tiempo con el día y la diferencia con el ganador.
- La pantalla no se apaga mientras la app está abierta.

## Obtener el .apk sin Android Studio

1. GitHub compila la app en cada push a `main` y en cada PR.
2. Entra en la pestaña **Actions** del repositorio, abre la última ejecución y descarga el artefacto `Cronos-apk`.
3. En las versiones publicadas en `main`, el .apk también aparece en **Releases**.
4. Copia el .apk al móvil y ábrelo. Android pedirá permiso para instalar apps desconocidas la primera vez.

El .apk se firma con una clave de depuración fija incluida en el repositorio, así que cada versión nueva se instala encima de la anterior y conserva las fotos y los datos.

## Estructura

- `app/src/main/assets/index.html`: la app completa (HTML, CSS y JavaScript en un solo archivo).
- `app/src/main/java/com/cronos/MainActivity.kt`: muestra la página en un WebView, abre el selector de fotos y bloquea cualquier acceso a internet.
- `.github/workflows/build-apk.yml`: compilación automática.

## Datos

Las fotos, la parrilla y los tiempos se guardan en el almacenamiento del propio móvil (IndexedDB y localStorage del WebView). Si se borran los datos de la app desde los ajustes de Android, habrá que cargar las fotos de nuevo.
