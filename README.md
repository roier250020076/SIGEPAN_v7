# SIGEPAN

Proyecto Eclipse de escritorio para ventas, pedidos e inventario de una panaderia.

## Instalacion obligatoria de la base de datos

1. Abre SQL Server Management Studio.
2. Abre y ejecuta el archivo `database/SIGEPAN_v2.sql` completo.
3. Revisa `src/properties/config.properties` y confirma el servidor, puerto, usuario y contrasena de tu SQL Server.
4. Importa el proyecto en Eclipse con un JDK 21 y ejecuta `Vista.Prueba`.

El script no borra las tablas existentes. Crea la base si no existe, completa las columnas requeridas y agrega ventas rapidas, precio y stock de productos.

## Datos iniciales

- Usuario: `sa`
- Contrasena: `admin123`

Al ejecutarlo por primera vez cambia la contrasena de SQL Server y despues crea usuarios de aplicacion desde la pantalla Usuarios.

## Funciones conectadas a SQL Server

- Inicio de sesion con contrasena SHA-256.
- Registro, listado y desactivacion logica de usuarios.
- Roles `ADMIN` y `EMPLEADO`; solo ADMIN puede registrar empleados.
- Catalogo e inventario: alta, modificacion y baja logica de productos, todo en SQL Server.
- Pedidos especiales: alta, modificacion y baja (cliente + productos), completamente persistentes.
- Stock de productos: se descuenta al guardar un pedido, se ajusta solo por la diferencia
  al modificarlo, y se regresa al cancelarlo/eliminarlo o al vaciar la tabla de pedidos;
  nunca queda en negativo (`ProductoDAO.descontarStock`/`restaurarStock`, usados desde
  `PedidoDAO`). Reforzado tambien con un `CHECK` en `Productos.stockActual` en SQL Server.
- Clientes de la panaderia (tabla `Clientes`, distinta de `Usuarios`): se registran solos
  al primer pedido, sin duplicarse (se detectan por telefono o correo), se listan/buscan/
  dan de baja desde la ventana **Clientes**, y se pueden elegir desde un combo en
  **Pedidos** para no volver a escribir sus datos.
- Boton **Pagar Pedido** en Inicio y estados **En proceso**, **Pagado** y **No pagado**, respaldados en `Pedidos` y `Pagos`.
- Reportes separados en **Ventas cobradas** y **Pedidos**, con actualizar/modificar, eliminar fila, vaciar tabla y borrar todo.
- Validacion de correo, fecha real, telefono, precios positivos, productos duplicados y busquedas seguras (`PreparedStatement` en todas las consultas).
- Ventanas adaptables: al maximizar o redimensionar, tablas, campos y botones se
  reacomodan proporcionalmente sin cortarse ni encimarse (`Vista.Redimensionable`); no
  aplica a las ventanas de login/registro, que conservan su tamano fijo original.

## Internacionalizacion (i18n)

Toda la interfaz (menus, botones, etiquetas, titulos y mensajes) se lee de archivos de
recursos en `src/idioma/`: `Mensajes_es.properties` (Español, por omision),
`Mensajes_en.properties` (English) y `Mensajes_pt.properties` (Português). El menu
**Configuracion**, presente en las 6 ventanas con menu principal, permite cambiar de
idioma en caliente sin reiniciar la aplicacion (ver `idioma.Idioma` y
`idioma.ActualizableIdioma`).

## Documentacion del codigo

Todas las clases de `datos` (los DAO: `ClienteDAO`, `PedidoDAO`, `ProductoDAO`,
`UsuarioDAO`, `VentaDAO`, `BaseDeDatos`, `Seguridad`) tienen comentarios JavaDoc
completos: proposito de la clase, que hace cada metodo, parametros, valor de retorno y
cuando lanzan `SQLException`.

## Cosas pendientes / conocidas

- Al marcar un pedido pagado como "No pagado" (`PedidoDAO.registrarPago`), el registro
  anterior en `Pagos` no se borra automaticamente; queda disponible para borrarse a mano
  desde el Reporte de Ventas si hace falta.
- `Proveedores`, `Compras` y `Detalle_Compra` existen en la base de datos pero todavia
  no tienen DAO ni pantalla en la aplicacion.
- La ventana **Clientes** solo permite dar de baja (baja logica); no tiene un formulario
  propio para registrar o modificar clientes a mano, porque eso ya se hace desde
  **Pedidos** (se registran solos, y se actualizan si se elige un cliente existente y se
  corrige algun dato antes de guardar).

