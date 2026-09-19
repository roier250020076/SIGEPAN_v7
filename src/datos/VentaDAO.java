package datos;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import modelo.Venta;

/**
 * DAO (Data Access Object) que registra las "Ventas rápidas" de mostrador (tabla
 * {@code Ventas}/{@code Detalles_Ventas}), descuenta el inventario correspondiente, y
 * arma el Reporte de Ventas que se muestra en la pestaña "Ventas cobradas" de Reportes.
 * <p>
 * <b>Dato clave para entender esta clase:</b> el Reporte de Ventas NO es solo la tabla
 * {@code Ventas}. Es la UNIÓN de dos orígenes de ingreso distintos:
 * <ol>
 *   <li><b>Ventas rápidas</b> (mostrador, ventana Inicio): fila en {@code Ventas}, folio
 *       con prefijo {@code "V-"}.</li>
 *   <li><b>Pedidos especiales ya pagados</b>: fila en {@code Pagos} con
 *       {@code estadoPago = 'Pagado'}, folio con prefijo {@code "P-"} (el mismo folio
 *       del pedido en {@link PedidoDAO}).</li>
 * </ol>
 * {@link #listarIngresos()} junta ambos con {@code UNION ALL}, y {@link #eliminar(String)}
 * usa el prefijo del folio para decidir a cuál de las dos tablas debe ir a borrar.
 */
public class VentaDAO {

    /**
     * Registra una venta rápida de mostrador (varios productos en un solo ticket) y
     * descuenta el stock correspondiente, todo dentro de una única transacción: si algo
     * falla a la mitad (por ejemplo, no hay stock suficiente de un producto), se revierte
     * TODO (no se guarda una venta a medias ni se descuenta stock de solo algunos productos).
     * <p>
     * Antes de guardar, agrupa las filas del carrito por producto (varias unidades del
     * mismo pan seleccionadas una por una en la tabla se consolidan en un solo renglón
     * de detalle con su cantidad sumada), para no generar un {@code Detalles_Ventas} con
     * un renglón repetido por cada clic.
     *
     * @param filas     filas del carrito de Inicio; cada una es {@code {nombreProducto (String),
     *                  precioUnitario (Number)}} (una fila por cada clic en el producto)
     * @param idUsuario IDUsuario del cajero que hizo la venta (sesión activa)
     * @throws SQLException si no hay un usuario autenticado (idUsuario &lt;= 0), si algún
     *                       producto no existe, si no hay stock suficiente, o si falla la
     *                       conexión; en cualquiera de estos casos la transacción se revierte
     */
    public void registrarVentaRapida(List<Object[]> filas, int idUsuario) throws SQLException {
        if (idUsuario <= 0) throw new SQLException("No hay un usuario autenticado.");
        Map<String, Linea> lineas = new LinkedHashMap<>();
        //ve lo que se agrego dentro del carrito
        for (Object[] fila : filas) {
            String producto = fila[0].toString();
            BigDecimal precio = BigDecimal.valueOf(((Number) fila[1]).doubleValue());
            //Luego revisa si ese producto ya estaba agregado:
            Linea linea = lineas.get(producto);
            if (linea == null) lineas.put(producto, new Linea(1, precio));
            else linea.cantidad++;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Linea linea : lineas.values()) total = total.add(linea.precio.multiply(BigDecimal.valueOf(linea.cantidad)));

        try (Connection conexion = BaseDeDatos.abrirConexion()) {
            conexion.setAutoCommit(false);
            try (PreparedStatement encabezado = conexion.prepareStatement("INSERT INTO Ventas (IDUsuario, fechaVenta, total, metodoPago) VALUES (?, SYSDATETIME(), ?, 'Efectivo')", Statement.RETURN_GENERATED_KEYS)) {
                encabezado.setInt(1, idUsuario); encabezado.setBigDecimal(2, total); encabezado.executeUpdate();
                try (ResultSet claves = encabezado.getGeneratedKeys()) {
                    if (!claves.next()) throw new SQLException("No se genero la venta.");
                    int idVenta = claves.getInt(1);
                    for (Map.Entry<String, Linea> entrada : lineas.entrySet()) insertarDetalle(conexion, idVenta, entrada.getKey(), entrada.getValue());
                }
                conexion.commit();
            } catch (SQLException ex) { conexion.rollback(); throw ex; }
            finally { conexion.setAutoCommit(true); }
        }
    }

    /** Inserta un renglón de {@code Detalles_Ventas} y descuenta el stock de ese producto (parte de la transacción de {@link #registrarVentaRapida}). */
    private void insertarDetalle(Connection conexion, int idVenta, String nombre, Linea linea) throws SQLException {
        int idProducto;
        try (PreparedStatement buscar = conexion.prepareStatement("SELECT IDProducto FROM Productos WHERE nombreProducto = ? AND estado = 'activo'")) {
            buscar.setString(1, nombre);
            try (ResultSet rs = buscar.executeQuery()) { if (!rs.next()) throw new SQLException("Producto no encontrado: " + nombre); idProducto = rs.getInt(1); }
        }
        // La condicion "stockActual >= ?" evita vender de mas: si no hay suficiente
        // stock, el UPDATE afecta 0 filas y se lanza la excepcion (que revierte todo).
        try (PreparedStatement stock = conexion.prepareStatement("UPDATE Productos SET stockActual = stockActual - ? WHERE IDProducto = ? AND stockActual >= ?")) {
            stock.setInt(1, linea.cantidad); stock.setInt(2, idProducto); stock.setInt(3, linea.cantidad);
            if (stock.executeUpdate() == 0) throw new SQLException("Stock insuficiente para " + nombre + ".");
        }
        try (PreparedStatement detalle = conexion.prepareStatement("INSERT INTO Detalles_Ventas (IDVenta, IDProducto, cantidad, precioUnitario, subtotal) VALUES (?, ?, ?, ?, ?)")) {
            detalle.setInt(1, idVenta); detalle.setInt(2, idProducto); detalle.setInt(3, linea.cantidad); detalle.setBigDecimal(4, linea.precio); detalle.setBigDecimal(5, linea.precio.multiply(BigDecimal.valueOf(linea.cantidad))); detalle.executeUpdate();
        }
    }

    /**
     * Arma el Reporte de Ventas completo: junta las ventas rápidas de mostrador con los
     * pedidos especiales que ya fueron pagados, ordenados del más reciente al más antiguo.
     * Ver la documentación de la clase para el detalle de los dos orígenes y el
     * significado de los prefijos "V-"/"P-" en el folio.
     *
     * @return lista combinada de {@link Venta}, ordenada por fecha descendente
     * @throws SQLException si falla la conexión o la consulta
     */
    public List<Venta> listarIngresos() throws SQLException {
        List<Venta> ventas = new ArrayList<>();
        String rapidas = "SELECT 'V-' + CONVERT(VARCHAR(20), IDVenta), CONVERT(VARCHAR(16), fechaVenta, 103) + ' ' + CONVERT(VARCHAR(5), fechaVenta, 108), 'Venta rapida', 'Venta de mostrador', total FROM Ventas";
        String pedidos = "SELECT 'P-' + CONVERT(VARCHAR(20), p.IDPedido), CONVERT(VARCHAR(16), pg.fechaPago, 103) + ' ' + CONVERT(VARCHAR(5), pg.fechaPago, 108), 'Pedido pagado', c.nombre, pg.monto FROM Pagos pg INNER JOIN Pedidos p ON p.IDPedido = pg.IDPedido INNER JOIN Clientes c ON c.IDCliente = p.IDCliente WHERE pg.estadoPago = 'Pagado'";
        try (Connection conexion = BaseDeDatos.abrirConexion(); Statement st = conexion.createStatement(); ResultSet rs = st.executeQuery(rapidas + " UNION ALL " + pedidos + " ORDER BY 2 DESC")) {
            while (rs.next()) ventas.add(new Venta(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getBigDecimal(5).doubleValue()));
        }
        return ventas;
    }

    /**
     * Elimina UNA fila del Reporte de Ventas, sin importar de qué origen sea (el prefijo
     * del folio lo indica):
     * <ul>
     *   <li><b>{@code "P-"} (pedido pagado):</b> se borra el registro de {@code Pagos} y
     *       el pedido regresa a estado "No pagado" (el pedido en sí NO se borra, solo
     *       deja de contar como ingreso).</li>
     *   <li><b>{@code "V-"} (venta rápida):</b> se restaura el stock que se había
     *       descontado y se borran los renglones de {@code Detalles_Ventas} y la venta.</li>
     * </ul>
     * Ambos casos corren dentro de una transacción: si algo falla a la mitad, se revierte todo.
     *
     * @param id folio tal como aparece en la tabla de Reportes (con su prefijo "V-" o "P-")
     * @throws SQLException si falla la conexión o cualquiera de los pasos de la eliminación
     */
    public void eliminar(String id) throws SQLException {
        if (id.startsWith("P-")) {
            int idPedido = Integer.parseInt(id.substring(2));
            try (Connection conexion = BaseDeDatos.abrirConexion()) {
                conexion.setAutoCommit(false);
                try { ejecutar(conexion, "DELETE FROM Pagos WHERE IDPedido = ?", idPedido); ejecutar(conexion, "UPDATE Pedidos SET estado = 'No pagado' WHERE IDPedido = ?", idPedido); conexion.commit(); }
                catch (SQLException ex) { conexion.rollback(); throw ex; } finally { conexion.setAutoCommit(true); }
            }
        } else {
            int idVenta = Integer.parseInt(id.substring(2));
            try (Connection conexion = BaseDeDatos.abrirConexion()) {
                conexion.setAutoCommit(false);
                try {
                    restaurarStock(conexion, idVenta);
                    ejecutar(conexion, "DELETE FROM Detalles_Ventas WHERE IDVenta = ?", idVenta);
                    ejecutar(conexion, "DELETE FROM Ventas WHERE IDVenta = ?", idVenta);
                    conexion.commit();
                }
                catch (SQLException ex) { conexion.rollback(); throw ex; } finally { conexion.setAutoCommit(true); }
            }
        }
    }

    /**
     * Vacía POR COMPLETO el Reporte de Ventas: borra todas las ventas rápidas (y sus
     * detalles) y todos los pagos registrados, y regresa a "No pagado" cualquier pedido
     * que estuviera marcado como "Pagado" (para que la base de datos quede consistente:
     * ya no hay ningún pago que respalde ese estado). Los pedidos en sí NO se borran,
     * solo dejan de aparecer como ingreso.
     * <p>
     * OJO: a diferencia de {@link #eliminar(String)}, este método NO restaura el stock
     * de las ventas rápidas que borra (vaciar el reporte se usa para "empezar de cero"
     * el histórico de ingresos, no para deshacer ventas una por una).
     *
     * @throws SQLException si falla la conexión o cualquiera de los DELETE/UPDATE;
     *                       la transacción se revierte completa si algo falla
     */
    public void vaciar() throws SQLException {
        try (Connection conexion = BaseDeDatos.abrirConexion(); Statement st = conexion.createStatement()) {
            conexion.setAutoCommit(false);
            try { st.executeUpdate("DELETE FROM Detalles_Ventas"); st.executeUpdate("DELETE FROM Ventas"); st.executeUpdate("DELETE FROM Pagos"); st.executeUpdate("UPDATE Pedidos SET estado = 'No pagado' WHERE estado = 'Pagado'"); conexion.commit(); }
            catch (SQLException ex) { conexion.rollback(); throw ex; } finally { conexion.setAutoCommit(true); }
        }
    }

    /** Ejecuta un UPDATE/DELETE parametrizado con un único entero (ID), como parte de una transacción ya abierta. */
    private void ejecutar(Connection conexion, String sql, int id) throws SQLException { try (PreparedStatement ps = conexion.prepareStatement(sql)) { ps.setInt(1, id); ps.executeUpdate(); } }

    /** Devuelve al inventario las cantidades vendidas en una venta rápida que se está eliminando (parte de la transacción de {@link #eliminar(String)}). */
    private void restaurarStock(Connection conexion, int idVenta) throws SQLException {
        try (PreparedStatement detalles = conexion.prepareStatement("SELECT IDProducto, cantidad FROM Detalles_Ventas WHERE IDVenta = ?")) {
            detalles.setInt(1, idVenta);
            try (ResultSet rs = detalles.executeQuery(); PreparedStatement actualizar = conexion.prepareStatement("UPDATE Productos SET stockActual = stockActual + ? WHERE IDProducto = ?")) {
                while (rs.next()) { actualizar.setInt(1, rs.getInt(2)); actualizar.setInt(2, rs.getInt(1)); actualizar.addBatch(); }
                actualizar.executeBatch();
            }
        }
    }

    /** Renglón de venta acumulado en memoria mientras se agrupan los productos repetidos del carrito antes de guardarlos. */
    private static final class Linea { private int cantidad; private final BigDecimal precio; private Linea(int cantidad, BigDecimal precio) { this.cantidad = cantidad; this.precio = precio; } }
}
