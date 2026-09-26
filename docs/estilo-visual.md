# Estilo visual Swing

Las ocho vistas usan `Vista.EstiloUI`: Inicio, Inventario, pedidos, usuarios,
Clientes, reportes, Prueba y Registro. No requiere nuevas bibliotecas; se mantiene
JDK 21, como indica el README del proyecto.

- Paleta de crema, espresso y terracota centralizada en constantes.
- Tarjetas con esquinas suaves, borde fino y sombra discreta.
- Botones con hover, pulsacion, estado deshabilitado y foco de teclado visible.
  Las acciones destructivas conservan el rojo.
- Tablas con filas alternadas, seleccion contrastada, filas de 32 px y cabeceras
  de 36 px. Se mantienen sus modelos, filtros y renderers por tipo de dato.
- Campos con margen interior, borde de foco y fondo distinto cuando son de lectura.
- Navegacion con indicador de la pantalla activa y respuesta al pasar el cursor.
- Se conservan el logo y las ilustraciones de `src/imagenes`; el login tiene
  campos y boton mas altos. Las ventanas con menu reservan su espacio al abrirse.

Para otra vista, usar `EstiloUI.Tarjeta` y `EstiloUI.Tabla` donde corresponda y
llamar una sola vez a `EstiloUI.aplicar(ventana, menuActivo)` al terminar su
construccion, antes de `Redimensionable.activar`. Pasar `null` si no tiene menu.
Los componentes internos del calendario, combos y spinners mantienen sus editores.

## Verificacion

Se compilaron todos los fuentes con el compilador JDK 21 y las bibliotecas que ya
incluye el repositorio. Se renderizaron las ocho vistas en una copia de trabajo
aislada, omitiendo la construccion de controladores y la carga SQL exclusivamente
en esa copia temporal, para revisar la presentacion sin acceder a la base de datos.
Esas sustituciones no forman parte del cambio. No se probaron operaciones SQL.

Revision manual sugerida en Eclipse con la base configurada: abrir las ocho
pantallas, cambiar entre espanol/ingles/portugues, maximizar y restaurar las
ventanas, navegar con Tab y comprobar seleccion, filtros y desplazamiento de tablas.
Los formularios conservan el posicionamiento proporcional existente; los textos
largos pueden requerir ampliar columnas o la ventana.
