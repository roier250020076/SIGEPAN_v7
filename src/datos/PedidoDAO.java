package datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import modelo.DetallePedido;
import modelo.Pedido;

/**
 * DAO (Data Access Object) para los "pedidos especiales" (tablas {@code Pedidos} y
 * {@code Detalles_Pedidos} de SQL Server): alta, edición, consulta, cobro y baja.
 * <p>
 * A diferencia de una venta rápida de mostrador (ver {@link VentaDAO}), un pedido
 * especial se puede guardar SIN estar pagado (nace con {@code estado = 'En proceso'})
 * y su pago se registra después, por separado, en la tabla {@code Pagos}
 * (ver {@link #registrarPago(String, boolean)}). El folio con el que se identifica un
 * pedido en toda la aplicación tiene el formato {@code "P-" + IDPedido} (por ejemplo
 * {@code "P-42"}); los métodos de esta clase convierten entre ese texto y el
 * {@code IDPedido} numérico real según lo necesiten.
 * <p>
 * <b>STOCK (SIGEPAN v6):</b> antes esta clase NO tocaba {@code Productos.stockActual}
 * en ningún lado (a diferencia de {@link VentaDAO}, que sí lo hacía). Ahora
 * {@link #guardar}, {@link #actualizar}, {@link #eliminar} y {@link #vaciar} sí lo
 * hacen, siempre a través de la misma lógica centralizada:
 * {@link #ajustarStockPorDiferencia}, que compara las cantidades que el pedido tenía
 * ANTES contra las que va a tener DESPUÉS y solo mueve la diferencia, apoyándose en los
 * métodos atómicos de {@link ProductoDAO#descontarStock} / {@link ProductoDAO#restaurarStock}.
 * Así se evita el riesgo (que se pidió cuidar especialmente) de descontar el mismo
 * pedido dos veces: no hay otra función en todo el proyecto que reste o sume
 * {@code stockActual} por su cuenta.
 */
public class PedidoDAO {

    /** Unico punto de acceso a las operaciones atomicas de stock (ver la nota de clase). */
    private final ProductoDAO productoDAO = new ProductoDAO();

    /**
     * Guarda un pedido especial NUEVO junto con todos sus renglones de producto, en una
     * sola transacción (si un producto del detalle no existe, o si no hay stock
     * suficiente de alguno, no se guarda nada). Al terminar, actualiza
     * {@code pedido.setIdPedido(...)} con el folio recién generado por SQL Server (con
     * el prefijo {@code "P-"}).
     * <p>
     * <b>STOCK (corregido en v6):</b> igual que {@link VentaDAO#registrarVentaRapida},
     * este método SÍ descuenta el inventario ({@code Productos.stockActual}) de los
     * productos del pedido (ver la nota de clase). Antes NO lo hacía; era una diferencia
     * de comportamiento entre los dos flujos de venta que ya no existe.
     *
     * @param pedido    pedido a guardar, con su lista de {@link DetallePedido} ya cargada;
     *                  el total se calcula sumando esos detalles ({@link Pedido#getTotal()})
     * @param idCliente IDCliente ya existente (obtenido con {@link ClienteDAO#obtenerOCrear})
     * @param idUsuario IDUsuario del empleado que capturó el pedido (sesión activa)
     * @throws SQLException si algún producto del detalle no existe/está inactivo, si no
     *                       hay stock suficiente de alguno, si falla la conexión, o si no
     *                       se pudo generar el folio; en cualquiera de estos casos la
     *                       transacción se revierte completa (no se guarda nada a medias
     *                       ni se descuenta stock de solo algunos productos)
     */
    public void guardar(Pedido pedido, int idCliente, int idUsuario) throws SQLException {
        String encabezado = "INSERT INTO Pedidos (IDCliente, IDUsuario, fechaPedido, total, estado) VALUES (?, ?, SYSDATETIME(), ?, 'En proceso')";
        try (Connection conexion = BaseDeDatos.abrirConexion()) {
            conexion.setAutoCommit(false);
            try (PreparedStatement ps = conexion.prepareStatement(encabezado, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, idCliente); ps.setInt(2, idUsuario); ps.setBigDecimal(3, java.math.BigDecimal.valueOf(pedido.getTotal()));
                ps.executeUpdate();
                try (ResultSet claves = ps.getGeneratedKeys()) {
                    if (!claves.next()) throw new SQLException("No se genero el folio del pedido.");
                    int idPedido = claves.getInt(1);

                    // STOCK: un pedido nuevo no tenia nada reservado antes ("anteriores"
                    // vacio), asi que TODA la cantidad de cada renglon se descuenta del
                    // inventario ahora mismo. Si algun producto no tiene stock suficiente,
                    // esto lanza SQLException y el catch de abajo revierte TODO.
                    Map<Integer, Integer> cantidadesNuevas = resolverCantidadesPorProducto(conexion, pedido.getDetalles());
                    ajustarStockPorDiferencia(conexion, java.util.Collections.emptyMap(), cantidadesNuevas);

                    for (DetallePedido detalle : pedido.getDetalles()) insertarDetalle(conexion, idPedido, detalle);
                    pedido.setIdPedido("P-" + idPedido);
                }
                conexion.commit();
            } catch (SQLException ex) {
                conexion.rollback();
                throw ex;
            } finally { conexion.setAutoCommit(true); }
        }
    }

    /** Inserta un renglón de {@code Detalles_Pedidos}, resolviendo el nombre de producto a su IDProducto. Usado por {@link #guardar} y {@link #actualizar}. */
    private void insertarDetalle(Connection conexion, int idPedido, DetallePedido detalle) throws SQLException {
        int idProducto;
        try (PreparedStatement buscar = conexion.prepareStatement("SELECT IDProducto FROM Productos WHERE nombreProducto = ? AND estado = 'activo'")) {
            buscar.setString(1, detalle.getProducto());
            try (ResultSet rs = buscar.executeQuery()) {
                if (!rs.next()) throw new SQLException("Producto no encontrado: " + detalle.getProducto());
                idProducto = rs.getInt(1);
            }
        }
        String sql = "INSERT INTO Detalles_Pedidos (IDPedido, IDProducto, cantidad, precioUnitario, subtotal) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idPedido); ps.setInt(2, idProducto); ps.setInt(3, detalle.getCantidad());
            ps.setBigDecimal(4, java.math.BigDecimal.valueOf(detalle.getPrecio())); ps.setBigDecimal(5, java.math.BigDecimal.valueOf(detalle.getSubtotal()));
            ps.executeUpdate();
        }
    }

    /**
     * Lista TODOS los pedidos (de cualquier estado), con su cliente y el detalle completo
     * de productos ya cargado, para la pestaña "Pedidos" de Reportes.
     *
     * @return lista de pedidos, del folio más alto (más reciente) al más antiguo
     * @throws SQLException si falla la conexión o alguna de las consultas
     */
    public List<Pedido> listar() throws SQLException {
        List<Pedido> pedidos = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT p.IDPedido, p.fechaPedido, p.estado, c.nombre, c.telefono FROM Pedidos p INNER JOIN Clientes c ON c.IDCliente = p.IDCliente ORDER BY p.IDPedido DESC";
        try (Connection conexion = BaseDeDatos.abrirConexion()) {
            try (PreparedStatement ps = conexion.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Pedido pedido = new Pedido("P-" + rs.getInt("IDPedido"), rs.getString("nombre"), rs.getString("telefono"), rs.getTimestamp("fechaPedido").toLocalDateTime().toString());
                    pedido.setEstado(rs.getString("estado"));
                    pedidos.add(pedido);
                    ids.add(rs.getInt("IDPedido"));
                }
            }
            for (int i = 0; i < pedidos.size(); i++) cargarDetalles(conexion, ids.get(i), pedidos.get(i));
        }
        return pedidos;
    }

    /**
     * Trae UN pedido con TODOS sus datos, incluyendo {@code IDCliente}, apellido,
     * dirección y correo del cliente -- lo que {@link #listar()} no trae, porque solo
     * lo necesita el formulario de "Modificar Pedido" para precargarse. Con el
     * {@code IDCliente} real ya en mano, {@code ControladorPedido} puede actualizar
     * después los datos del cliente sin tener que volver a buscarlo por teléfono.
     *
     * @param idPedido folio del pedido a consultar (con o sin el prefijo "P-", se limpia igual)
     * @return el pedido completo, con su lista de {@link DetallePedido} ya cargada
     * @throws SQLException si el pedido no existe, o si falla la conexión o la consulta
     */
    public Pedido obtenerCompleto(String idPedido) throws SQLException {
        int id = Integer.parseInt(idPedido.replace("P-", ""));
        String sql = "SELECT p.IDPedido, p.fechaPedido, p.estado, c.IDCliente, c.nombre, c.apellido, c.telefono, c.direccion, c.correo "
                   + "FROM Pedidos p INNER JOIN Clientes c ON c.IDCliente = p.IDCliente WHERE p.IDPedido = ?";
        try (Connection conexion = BaseDeDatos.abrirConexion()) {
            Pedido pedido;
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("El pedido " + idPedido + " ya no existe.");
                    pedido = new Pedido("P-" + rs.getInt("IDPedido"), rs.getString("nombre"), rs.getString("telefono"), rs.getTimestamp("fechaPedido").toLocalDateTime().toString());
                    pedido.setEstado(rs.getString("estado"));
                    pedido.setIdCliente(rs.getInt("IDCliente"));
                    pedido.setApellido(rs.getString("apellido"));
                    pedido.setDireccion(rs.getString("direccion"));
                    pedido.setCorreo(rs.getString("correo"));
                }
            }
            cargarDetalles(conexion, id, pedido);
            return pedido;
        }
    }

    /** Carga en {@code pedido} sus renglones de {@code Detalles_Pedidos}, con el nombre de producto ya resuelto. Usado por {@link #listar} y {@link #obtenerCompleto}. */
    private void cargarDetalles(Connection conexion, int idPedido, Pedido pedido) throws SQLException {
        String sql = "SELECT pr.nombreProducto, d.cantidad, d.precioUnitario FROM Detalles_Pedidos d INNER JOIN Productos pr ON pr.IDProducto = d.IDProducto WHERE d.IDPedido = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) pedido.getDetalles().add(new DetallePedido(rs.getString(1), rs.getInt(2), rs.getBigDecimal(3).doubleValue()));
            }
        }
    }

    /**
     * MODIFICAR PEDIDO: actualiza el cliente y el total de un pedido YA EXISTENTE
     * (identificado por {@code pedido.getIdPedido()}) y REEMPLAZA por completo sus
     * renglones de {@code Detalles_Pedidos} por los que trae {@code pedido.getDetalles()}.
     * <p>
     * Se hace "borrar todo e insertar de nuevo" en lugar de calcular qué renglón cambió,
     * se agregó o se quitó, porque es mucho más simple y confiable para una lista de
     * productos que el usuario pudo reordenar, agregar o quitar libremente en el
     * formulario. El {@code IDUsuario} y la fecha original del pedido NO se tocan; el
     * {@code estado} (Pagado/No pagado/En proceso) tampoco cambia aquí -- para eso está
     * {@link #registrarPago(String, boolean)}.
     *
     * @param pedido    pedido con su folio ya existente y la lista de detalles NUEVA
     *                  (reemplaza por completo a la anterior)
     * @param idCliente IDCliente al que debe quedar asociado el pedido (puede ser el
     *                  mismo de antes, o uno distinto si el usuario cambió los datos)
     * @throws SQLException si el pedido ya no existe, si algún producto del nuevo
     *                       detalle no existe, si la cantidad aumentó para algún
     *                       producto y ya no hay stock suficiente para cubrir esa
     *                       diferencia, o si falla la conexión; la transacción se
     *                       revierte completa si algo falla
     */
    public void actualizar(Pedido pedido, int idCliente) throws SQLException {
        int id = Integer.parseInt(pedido.getIdPedido().replace("P-", ""));
        try (Connection conexion = BaseDeDatos.abrirConexion()) {
            conexion.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conexion.prepareStatement(
                        "UPDATE Pedidos SET IDCliente = ?, total = ? WHERE IDPedido = ?")) {
                    ps.setInt(1, idCliente);
                    ps.setBigDecimal(2, java.math.BigDecimal.valueOf(pedido.getTotal()));
                    ps.setInt(3, id);
                    int filas = ps.executeUpdate();
                    if (filas == 0) throw new SQLException("El pedido " + pedido.getIdPedido() + " ya no existe.");
                }

                // STOCK: se compara lo que el pedido tenia ANTES (todavia en
                // Detalles_Pedidos, se lee AQUI, antes de borrarlo) contra lo que va a
                // tener DESPUES (el detalle nuevo que trae "pedido"), y solo se mueve la
                // diferencia por producto -- nunca se vuelve a descontar todo. Si un
                // producto subio de 5 a 8, solo se descuentan 3; si bajo de 8 a 5,
                // regresan 3 al inventario; si se quito por completo, regresa todo.
                Map<Integer, Integer> cantidadesAntes = cantidadesActualesDelPedido(conexion, id);
                Map<Integer, Integer> cantidadesDespues = resolverCantidadesPorProducto(conexion, pedido.getDetalles());
                ajustarStockPorDiferencia(conexion, cantidadesAntes, cantidadesDespues);

                // Se eliminan los renglones anteriores y se insertan los nuevos, ya
                // limpios y consistentes con lo que el usuario dejo en el formulario.
                ejecutar(conexion, "DELETE FROM Detalles_Pedidos WHERE IDPedido = ?", id);
                for (DetallePedido detalle : pedido.getDetalles()) insertarDetalle(conexion, id, detalle);
                conexion.commit();
            } catch (SQLException ex) {
                conexion.rollback();
                throw ex;
            } finally { conexion.setAutoCommit(true); }
        }
    }

    /**
     * Marca un pedido como pagado o no pagado, botón "Pagar Pedido" de Inicio y
     * "Modificar Pedido"/estado en Reportes.
     * <ul>
     *   <li>Si {@code pagado} es {@code true}: se busca el total actual del pedido, se
     *       inserta un renglón en {@code Pagos} (con una referencia única generada a
     *       partir del folio y la hora exacta, para que nunca choque con la restricción
     *       UNIQUE de esa columna aunque el mismo pedido se pague/despague varias veces),
     *       y el pedido pasa a {@code estado = 'Pagado'}.</li>
     *   <li>Si {@code pagado} es {@code false}: el pedido pasa a {@code estado = 'No pagado'}.
     *       <b>Ojo:</b> este caso NO borra el registro de {@code Pagos} que se haya
     *       generado anteriormente para este pedido; si se necesita revertir también ese
     *       pago, hay que borrarlo aparte (por ejemplo, con {@link VentaDAO#eliminar}
     *       desde el Reporte de Ventas, donde también aparece listado).</li>
     * </ul>
     *
     * @param idPedido folio del pedido a marcar (con o sin el prefijo "P-")
     * @param pagado   {@code true} para registrar el cobro, {@code false} para marcarlo
     *                 como no pagado
     * @throws SQLException si el pedido no existe, o si falla la conexión; la
     *                       transacción se revierte completa si algo falla
     */
    public void registrarPago(String idPedido, boolean pagado) throws SQLException {
        int id = Integer.parseInt(idPedido.replace("P-", ""));
        try (Connection conexion = BaseDeDatos.abrirConexion()) {
            conexion.setAutoCommit(false);
            try {
                if (pagado) {
                    java.math.BigDecimal total;
                    try (PreparedStatement consultar = conexion.prepareStatement("SELECT total FROM Pedidos WHERE IDPedido = ?")) {
                        consultar.setInt(1, id);
                        try (ResultSet rs = consultar.executeQuery()) { if (!rs.next()) throw new SQLException("Pedido no encontrado."); total = rs.getBigDecimal(1); }
                    }
                    try (PreparedStatement pago = conexion.prepareStatement("INSERT INTO Pagos (IDPedido, monto, metodoPago, fechaPago, referencia, estadoPago) VALUES (?, ?, 'Efectivo', SYSDATETIME(), ?, 'Pagado')")) {
                        pago.setInt(1, id); pago.setBigDecimal(2, total); pago.setString(3, "PAGO-" + id + "-" + System.currentTimeMillis()); pago.executeUpdate();
                    }
                }
                try (PreparedStatement actualizar = conexion.prepareStatement("UPDATE Pedidos SET estado = ? WHERE IDPedido = ?")) {
                    actualizar.setString(1, pagado ? "Pagado" : "No pagado"); actualizar.setInt(2, id); actualizar.executeUpdate();
                }
                conexion.commit();
            } catch (SQLException ex) { conexion.rollback(); throw ex; }
            finally { conexion.setAutoCommit(true); }
        }
    }

    /**
     * Elimina POR COMPLETO un pedido: su pago (si existe), sus renglones de detalle, y
     * el encabezado del pedido, en ese orden (para respetar las llaves foráneas), dentro
     * de una sola transacción.
     *
     * @param idPedido folio del pedido a eliminar (con o sin el prefijo "P-")
     * @throws SQLException si falla la conexión o cualquiera de los DELETE; la
     *                       transacción se revierte completa si algo falla (incluyendo
     *                       la devolución de stock)
     */
    public void eliminar(String idPedido) throws SQLException {
        int id = Integer.parseInt(idPedido.replace("P-", ""));
        try (Connection conexion = BaseDeDatos.abrirConexion()) {
            conexion.setAutoCommit(false);
            try {
                // STOCK: antes de borrar los renglones, se regresan al inventario todas
                // las cantidades que este pedido tenia reservadas (si el pedido no tenia
                // ningun renglon, el mapa sale vacio y esto simplemente no hace nada).
                Map<Integer, Integer> cantidadesActuales = cantidadesActualesDelPedido(conexion, id);
                ajustarStockPorDiferencia(conexion, cantidadesActuales, java.util.Collections.emptyMap());

                ejecutar(conexion, "DELETE FROM Pagos WHERE IDPedido = ?", id);
                ejecutar(conexion, "DELETE FROM Detalles_Pedidos WHERE IDPedido = ?", id);
                ejecutar(conexion, "DELETE FROM Pedidos WHERE IDPedido = ?", id);
                conexion.commit();
            } catch (SQLException ex) { conexion.rollback(); throw ex; }
            finally { conexion.setAutoCommit(true); }
        }
    }

    /**
     * Vacía POR COMPLETO la tabla de pedidos: borra todos los pagos, todos los detalles
     * y todos los encabezados de pedido, en ese orden, dentro de una sola transacción.
     * Se usa desde el botón "Vaciar tabla actual"/"Borrar ambas tablas" de Reportes.
     *
     * @throws SQLException si falla la conexión o cualquiera de los DELETE; la
     *                       transacción se revierte completa si algo falla (incluyendo
     *                       la devolución de stock)
     */
    public void vaciar() throws SQLException {
        try (Connection conexion = BaseDeDatos.abrirConexion()) {
            conexion.setAutoCommit(false);
            try (Statement sentencia = conexion.createStatement()) {
                // STOCK: al vaciar TODOS los pedidos de una sola vez, se le regresa a
                // cada producto la suma de todo lo que estuviera reservado en
                // Detalles_Pedidos, por la misma razon que eliminar(...) lo hace para un
                // solo pedido (es la misma operacion -- borrar pedidos -- aplicada a
                // todas las filas a la vez, asi que debe seguir la misma regla de stock).
                sentencia.executeUpdate(
                    "UPDATE p SET p.stockActual = p.stockActual + dp.cantidad " +
                    "FROM Productos p INNER JOIN (" +
                    "  SELECT IDProducto, SUM(cantidad) AS cantidad FROM Detalles_Pedidos GROUP BY IDProducto" +
                    ") dp ON dp.IDProducto = p.IDProducto");
                sentencia.executeUpdate("DELETE FROM Pagos");
                sentencia.executeUpdate("DELETE FROM Detalles_Pedidos");
                sentencia.executeUpdate("DELETE FROM Pedidos");
                conexion.commit();
            } catch (SQLException ex) { conexion.rollback(); throw ex; }
            finally { conexion.setAutoCommit(true); }
        }
    }

    /** Ejecuta un UPDATE/DELETE parametrizado con un único entero (ID), como parte de una transacción ya abierta. */
    private void ejecutar(Connection conexion, String sql, int id) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(sql)) { ps.setInt(1, id); ps.executeUpdate(); }
    }

    // =========================================================================
    // LOGICA UNICA DE STOCK PARA PEDIDOS (SIGEPAN v6)
    // -------------------------------------------------------------------------
    // Estos 3 metodos privados son los UNICOS de esta clase que deciden CUANTO hay
    // que sumar/restar de stock; guardar(), actualizar() y eliminar() solo los llaman,
    // nunca tocan Productos.stockActual por su cuenta. Asi, si algun dia hay que
    // ajustar la regla de stock, solo hay que tocar ajustarStockPorDiferencia().
    // =========================================================================

    /**
     * Ajusta {@code Productos.stockActual} comparando las cantidades ANTERIORES de un
     * pedido (antes de la operación) contra las NUEVAS que va a tener DESPUÉS,
     * producto por producto, y solo mueve la diferencia:
     * <ul>
     *   <li>Si un producto aumentó (nueva &gt; anterior), se DESCUENTA la diferencia del
     *       inventario ({@link ProductoDAO#descontarStock}).</li>
     *   <li>Si un producto disminuyó o se quitó por completo (nueva &lt; anterior), se
     *       REGRESA la diferencia al inventario ({@link ProductoDAO#restaurarStock}).</li>
     *   <li>Si no cambió, no se hace nada.</li>
     * </ul>
     * La usan {@link #guardar} (con {@code anteriores} vacío: todo el detalle se
     * descuenta por primera vez), {@link #actualizar} (con {@code anteriores} = lo que
     * el pedido tenía antes de la edición) y {@link #eliminar} (con {@code nuevas}
     * vacío: todo lo que tenía se regresa).
     *
     * @param conexion   conexión/transacción ya abierta por quien llama
     * @param anteriores IDProducto -&gt; cantidad que el pedido tenía ANTES (vacío si es un pedido nuevo)
     * @param nuevas     IDProducto -&gt; cantidad que el pedido debe tener DESPUÉS (vacío si se está eliminando)
     * @throws SQLException si algún producto necesita más stock del que hay disponible
     */
    private void ajustarStockPorDiferencia(Connection conexion, Map<Integer, Integer> anteriores, Map<Integer, Integer> nuevas) throws SQLException {
        Set<Integer> productosAfectados = new LinkedHashSet<>();
        productosAfectados.addAll(anteriores.keySet());
        productosAfectados.addAll(nuevas.keySet());
        for (int idProducto : productosAfectados) {
            int antes = anteriores.getOrDefault(idProducto, 0);
            int despues = nuevas.getOrDefault(idProducto, 0);
            int diferencia = despues - antes;
            if (diferencia > 0) {
                productoDAO.descontarStock(conexion, idProducto, diferencia);
            } else if (diferencia < 0) {
                productoDAO.restaurarStock(conexion, idProducto, -diferencia);
            }
        }
    }

    /**
     * Convierte la lista de {@link DetallePedido} (identificados por NOMBRE de
     * producto, como los maneja la ventana de Pedidos) en un mapa IDProducto -&gt;
     * cantidad, sumando cantidades si el mismo producto apareciera en más de un
     * renglón. Se usa SOLO para calcular el stock a mover; no reemplaza la resolución
     * de nombre a IDProducto que ya hace {@link #insertarDetalle} al guardar cada
     * renglón (se mantienen separadas a propósito, para no alterar cómo se insertan los
     * renglones de {@code Detalles_Pedidos}, que no es parte de esta corrección).
     */
    private Map<Integer, Integer> resolverCantidadesPorProducto(Connection conexion, List<DetallePedido> detalles) throws SQLException {
        Map<Integer, Integer> cantidades = new LinkedHashMap<>();
        for (DetallePedido detalle : detalles) {
            int idProducto;
            try (PreparedStatement buscar = conexion.prepareStatement("SELECT IDProducto FROM Productos WHERE nombreProducto = ? AND estado = 'activo'")) {
                buscar.setString(1, detalle.getProducto());
                try (ResultSet rs = buscar.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Producto no encontrado: " + detalle.getProducto());
                    idProducto = rs.getInt(1);
                }
            }
            cantidades.merge(idProducto, detalle.getCantidad(), Integer::sum);
        }
        return cantidades;
    }

    /** Lee las cantidades ACTUALES (antes de modificar/eliminar) de {@code Detalles_Pedidos} para un pedido, como IDProducto -&gt; cantidad. */
    private Map<Integer, Integer> cantidadesActualesDelPedido(Connection conexion, int idPedido) throws SQLException {
        Map<Integer, Integer> cantidades = new LinkedHashMap<>();
        try (PreparedStatement ps = conexion.prepareStatement("SELECT IDProducto, cantidad FROM Detalles_Pedidos WHERE IDPedido = ?")) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) cantidades.merge(rs.getInt(1), rs.getInt(2), Integer::sum);
            }
        }
        return cantidades;
    }
}
