package datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object) para las tablas {@code Productos} y {@code Categorias} de
 * SQL Server. Todas las consultas usan {@link PreparedStatement} con parámetros ("?"),
 * nunca concatenación de texto, para no exponer la aplicación a inyección SQL.
 * <p>
 * Ofrece dos formas distintas de listar productos porque cada pantalla necesita un
 * nivel de detalle distinto: {@link #listarActivos()} (nombre + precio, para Inicio y
 * Pedidos) y {@link #listarDetallado()} (todos los campos, para Inventario). Ambas
 * comparten el mismo filtro {@code estado = 'activo'}, así que un producto "eliminado"
 * (ver {@link #eliminar(int)}) desaparece automáticamente de las dos.
 */
public class ProductoDAO {

    /**
     * Lista los productos activos con solo su nombre y precio de venta.
     * <p>
     * Este es el listado que alimenta el catálogo de la ventana Inicio y el combo de
     * productos de Pedidos; ambas pantallas solo necesitan esos dos datos para vender.
     * Es independiente de {@link #listarDetallado()}, que trae más columnas para
     * Inventario: cambiar una no debe afectar a la otra.
     *
     * @return lista de filas {@code {nombreProducto (String), precioVenta (BigDecimal)}},
     *         ordenada alfabéticamente por nombre
     * @throws SQLException si falla la conexión o la consulta
     */
    public List<Object[]> listarActivos() throws SQLException {
        List<Object[]> productos = new ArrayList<>();
        String sql = "SELECT nombreProducto, precioVenta FROM Productos WHERE estado = 'activo' ORDER BY nombreProducto";
        try (var conexion = BaseDeDatos.abrirConexion(); PreparedStatement ps = conexion.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) productos.add(new Object[] { rs.getString(1), rs.getBigDecimal(2) });
        }
        return productos;
    }

    /**
     * Listado ENRIQUECIDO para la pantalla de Inventario: agrega categoría y stock,
     * que es justo la información nueva que ahora se captura al dar de alta un producto.
     * OJO: esto es independiente de {@link #listarActivos()} (que usan Inicio y Pedidos
     * y debe seguir teniendo solo nombre+precio); aquí solo se alimenta la tabla de
     * Inventario.
     * <p>
     * También regresa {@code IDProducto} (columna 0): {@code nombreProducto} NO tiene
     * restricción UNIQUE en la base de datos, así que no es seguro identificar una fila
     * por su nombre para eliminarla o modificarla; el ID real es la única referencia
     * confiable, y el controlador la guarda en paralelo a cada fila de la tabla.
     *
     * @return lista de filas {@code {IDProducto (Integer), nombreProducto (String),
     *         nombreCategoria (String), precioVenta (BigDecimal), stockActual (Integer),
     *         stockMinimo (Integer), codigo (String), descripcion (String, puede ser null)}}
     * @throws SQLException si falla la conexión o la consulta
     */
    public List<Object[]> listarDetallado() throws SQLException {
        List<Object[]> productos = new ArrayList<>();
        String sql = "SELECT p.IDProducto, p.nombreProducto, c.nombreCategoria, p.precioVenta, p.stockActual, p.stockMinimo, p.codigo, p.descripcion " +
                     "FROM Productos p INNER JOIN Categorias c ON c.IDCategoria = p.IDCategoria " +
                     "WHERE p.estado = 'activo' ORDER BY p.nombreProducto";
        try (var conexion = BaseDeDatos.abrirConexion(); PreparedStatement ps = conexion.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                productos.add(new Object[] {
                    rs.getInt(1), rs.getString(2), rs.getString(3), rs.getBigDecimal(4),
                    rs.getInt(5), rs.getInt(6), rs.getString(7), rs.getString(8)
                });
            }
        }
        return productos;
    }

    /**
     * Categorías activas disponibles para el combo de Inventario.
     *
     * @return lista de filas {@code {IDCategoria (Integer), nombreCategoria (String)}},
     *         ordenada alfabéticamente
     * @throws SQLException si falla la conexión o la consulta
     */
    public List<Object[]> listarCategorias() throws SQLException {
        List<Object[]> categorias = new ArrayList<>();
        String sql = "SELECT IDCategoria, nombreCategoria FROM Categorias WHERE estado = 'activo' ORDER BY nombreCategoria";
        try (var conexion = BaseDeDatos.abrirConexion(); PreparedStatement ps = conexion.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) categorias.add(new Object[] { rs.getInt(1), rs.getString(2) });
        }
        return categorias;
    }

    /**
     * Busca el IDProducto de un producto activo a partir de su nombre exacto.
     *
     * @param nombre nombre exacto del producto (tal como está en {@code nombreProducto})
     * @return el IDProducto correspondiente
     * @throws SQLException si falla la conexión, o si no existe ningún producto activo
     *                       con ese nombre
     */
    public int obtenerIdPorNombre(String nombre) throws SQLException {
        String sql = "SELECT IDProducto FROM Productos WHERE nombreProducto = ? AND estado = 'activo'";
        try (var conexion = BaseDeDatos.abrirConexion(); PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("El producto no existe o esta inactivo: " + nombre);
                return rs.getInt(1);
            }
        }
    }

    /**
     * Da de alta un producto nuevo en el catálogo (ALTA COMPLETA DE PRODUCTO).
     * <p>
     * Antes solo recibía nombre y precio (y usaba la primera categoría activa y un
     * código generado al vuelo). Ahora Inventario ya captura categoría, descripción,
     * código y stock; si el código llega vacío se sigue generando automáticamente
     * (con la marca de tiempo actual) para no romper la restricción UNIQUE NOT NULL de
     * la columna {@code codigo}.
     * <p>
     * La validación de que el nombre no esté duplicado, y de que precio/stock sean
     * válidos, la hace {@code ControladorInventario} ANTES de llamar a este método;
     * este DAO asume que los datos que recibe ya están validados.
     *
     * @param nombre       nombre del producto (obligatorio)
     * @param idCategoria  categoría a la que pertenece (debe existir en Categorias)
     * @param descripcion  descripción opcional; si es null o está en blanco se guarda
     *                     NULL en la base de datos
     * @param codigo       código/SKU opcional; si es null o está en blanco se genera
     *                     uno automáticamente
     * @param precio       precio de venta (debe ser mayor que cero; no se valida aquí)
     * @param stockActual  cantidad inicial en existencia
     * @param stockMinimo  cantidad mínima antes de considerarse "por agotarse"
     * @throws SQLException si falla la conexión o la inserción (por ejemplo, si el
     *                       código ya existe y viola la restricción UNIQUE)
     */
    public void insertar(String nombre, int idCategoria, String descripcion, String codigo,
                          double precio, int stockActual, int stockMinimo) throws SQLException {
        String codigoFinal = (codigo == null || codigo.isBlank())
                ? ("P" + (System.currentTimeMillis() % 10_000_000_000_000L))
                : codigo.trim();
        String sql = "INSERT INTO Productos (IDCategoria, nombreProducto, descripcion, codigo, precioVenta, stockActual, stockMinimo) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (var conexion = BaseDeDatos.abrirConexion(); PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idCategoria);
            ps.setString(2, nombre.trim());
            ps.setString(3, (descripcion == null || descripcion.isBlank()) ? null : descripcion.trim());
            ps.setString(4, codigoFinal);
            ps.setBigDecimal(5, java.math.BigDecimal.valueOf(precio));
            ps.setInt(6, stockActual);
            ps.setInt(7, stockMinimo);
            ps.executeUpdate();
        }
    }

    /**
     * MODIFICAR PRODUCTO: actualiza TODOS los campos editables de un producto que YA
     * EXISTE (se identifica por IDProducto, nunca por nombre, porque el nombre no es
     * único). No inserta una fila nueva.
     *
     * @param idProducto   folio del producto a modificar
     * @param nombre       nuevo nombre
     * @param idCategoria  nueva categoría
     * @param descripcion  nueva descripción (o null/blanco para guardar NULL)
     * @param codigo       nuevo código (o blanco para generar uno nuevo automáticamente)
     * @param precio       nuevo precio de venta
     * @param stockActual  nuevo stock actual
     * @param stockMinimo  nuevo stock mínimo de alerta
     * @throws SQLException si falla la conexión, o si el producto ya no existe o fue
     *                       eliminado por otro usuario (0 filas afectadas)
     */
    public void actualizar(int idProducto, String nombre, int idCategoria, String descripcion, String codigo,
                            double precio, int stockActual, int stockMinimo) throws SQLException {
        String codigoFinal = (codigo == null || codigo.isBlank())
                ? ("P" + (System.currentTimeMillis() % 10_000_000_000_000L))
                : codigo.trim();
        String sql = "UPDATE Productos SET IDCategoria = ?, nombreProducto = ?, descripcion = ?, codigo = ?, "
                   + "precioVenta = ?, stockActual = ?, stockMinimo = ? WHERE IDProducto = ? AND estado = 'activo'";
        try (var conexion = BaseDeDatos.abrirConexion(); PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idCategoria);
            ps.setString(2, nombre.trim());
            ps.setString(3, (descripcion == null || descripcion.isBlank()) ? null : descripcion.trim());
            ps.setString(4, codigoFinal);
            ps.setBigDecimal(5, java.math.BigDecimal.valueOf(precio));
            ps.setInt(6, stockActual);
            ps.setInt(7, stockMinimo);
            ps.setInt(8, idProducto);
            int filasAfectadas = ps.executeUpdate();
            if (filasAfectadas == 0) throw new SQLException("El producto ya no existe o fue eliminado por otro usuario.");
        }
    }

    /**
     * ELIMINAR PRODUCTO: se hace con "borrado lógico" (estado = 'inactivo'), igual que
     * {@link UsuarioDAO}. Un DELETE físico fallaría con un error de llave foránea en
     * cuanto el producto ya tuviera algún pedido registrado en Detalles_Pedidos, además
     * de que destruiría el historial de pedidos pasados que lo mencionan. Al marcarlo
     * inactivo:
     * <ul>
     *   <li>Desaparece de inmediato del catálogo de Inventario y del combo de Pedidos
     *       ({@link #listarDetallado()} y {@link #listarActivos()} ya filtran por
     *       {@code estado = 'activo'}).</li>
     *   <li>Los pedidos históricos que ya lo incluían siguen intactos y consultables.</li>
     * </ul>
     *
     * @param idProducto folio del producto a eliminar (lógicamente)
     * @throws SQLException si falla la conexión, o si el producto ya no existe o ya
     *                       había sido eliminado (0 filas afectadas)
     */
    public void eliminar(int idProducto) throws SQLException {
        String sql = "UPDATE Productos SET estado = 'inactivo' WHERE IDProducto = ? AND estado = 'activo'";
        try (var conexion = BaseDeDatos.abrirConexion(); PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            int filasAfectadas = ps.executeUpdate();
            if (filasAfectadas == 0) throw new SQLException("El producto ya no existe o ya habia sido eliminado.");
        }
    }

    // =========================================================================
    // LOGICA DE STOCK (SIGEPAN v6)
    // -------------------------------------------------------------------------
    // Antes, guardar/modificar/eliminar un Pedido NO tocaba Productos.stockActual
    // en ningun lado (ver PedidoDAO): un pedido se podia guardar sin importar cuanto
    // stock hubiera. Estos dos metodos son la UNICA logica de todo el proyecto que
    // suma o resta stock, y ambos reciben la Connection de quien los llama (en vez de
    // abrir la suya propia) para poder participar en la MISMA transaccion que ya esta
    // usando PedidoDAO: si algo mas falla despues (por ejemplo, insertar un renglon del
    // pedido), el rollback de esa transaccion tambien deshace el cambio de stock.
    // Ni PedidoDAO ni ningun otro DAO deben restar/sumar stockActual por su cuenta;
    // siempre deben pasar por aqui para que la logica quede en un solo lugar.
    // =========================================================================

    /**
     * Descuenta stock de un producto de forma ATOMICA (una sola sentencia UPDATE). La
     * condicion {@code stockActual >= ?} es lo que evita que el stock quede negativo: si
     * no hay suficiente, el UPDATE afecta 0 filas y se lanza {@link SQLException} con un
     * mensaje listo para mostrarle al usuario; quien llama debe dejar que esa excepcion
     * revierta su transaccion (no se descuenta "una parte" de la cantidad pedida).
     *
     * @param conexion   conexion/transacción ya abierta por quien llama (normalmente
     *                   {@code PedidoDAO}), NO se cierra aquí
     * @param idProducto producto al que se le va a descontar stock
     * @param cantidad   unidades a descontar; si es 0 o negativo, no hace nada
     * @throws SQLException si no hay stock suficiente, o si falla la consulta
     */
    public void descontarStock(Connection conexion, int idProducto, int cantidad) throws SQLException {
        if (cantidad <= 0) return;
        try (PreparedStatement ps = conexion.prepareStatement(
                "UPDATE Productos SET stockActual = stockActual - ? WHERE IDProducto = ? AND stockActual >= ?")) {
            ps.setInt(1, cantidad);
            ps.setInt(2, idProducto);
            ps.setInt(3, cantidad);
            if (ps.executeUpdate() == 0) {
                throw new SQLException(mensajeStockInsuficiente(conexion, idProducto, cantidad));
            }
        }
    }

    /**
     * Devuelve stock a un producto (operación inversa de {@link #descontarStock}),
     * también de forma atómica y dentro de la transacción ya abierta por quien llama. Se
     * usa cuando un pedido se cancela/elimina, o cuando se reduce (o se quita por
     * completo) la cantidad de un producto al modificar un pedido existente.
     *
     * @param conexion   conexión/transacción ya abierta por quien llama, NO se cierra aquí
     * @param idProducto producto al que se le va a regresar stock
     * @param cantidad   unidades a regresar; si es 0 o negativo, no hace nada
     * @throws SQLException si falla la consulta
     */
    public void restaurarStock(Connection conexion, int idProducto, int cantidad) throws SQLException {
        if (cantidad <= 0) return;
        try (PreparedStatement ps = conexion.prepareStatement(
                "UPDATE Productos SET stockActual = stockActual + ? WHERE IDProducto = ?")) {
            ps.setInt(1, cantidad);
            ps.setInt(2, idProducto);
            ps.executeUpdate();
        }
    }

    /** Arma el mensaje de error de {@link #descontarStock} con el nombre y el stock disponible reales del producto, para que el usuario sepa exactamente qué pasó. */
    private String mensajeStockInsuficiente(Connection conexion, int idProducto, int cantidadSolicitada) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement("SELECT nombreProducto, stockActual FROM Productos WHERE IDProducto = ?")) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return "Stock insuficiente de \"" + rs.getString(1) + "\". Disponible: " + rs.getInt(2) + ", solicitado: " + cantidadSolicitada + ".";
                }
            }
        }
        return "Stock insuficiente para el producto solicitado.";
    }

    /**
     * Stock disponible de un producto para agregarlo a un pedido especial, calculado
     * ANTES de guardar nada (se usa desde {@code ControladorPedido.agregarProducto()}
     * para avisarle al usuario de inmediato si no alcanza, en vez de que se entere hasta
     * que presiona "Guardar Pedido"). El control real y definitivo contra ventas
     * simultáneas lo sigue haciendo {@link #descontarStock} dentro de la transacción de
     * {@code PedidoDAO}; esta consulta es solo informativa/preventiva.
     * <p>
     * Si {@code idPedidoEnEdicion} no es null, al stock actual se le SUMA lo que ESE
     * pedido ya tenía reservado de este producto, para no bloquear al usuario cuando
     * solo está aumentando la cantidad de un producto que su propio pedido, en edición,
     * ya tenía apartado.
     *
     * @param nombreProducto    nombre exacto del producto (tal como aparece en el combo de Pedidos)
     * @param idPedidoEnEdicion folio del pedido que se está editando (con o sin "P-"), o
     *                          {@code null} si se está capturando un pedido nuevo
     * @return unidades disponibles para agregar
     * @throws SQLException si el producto no existe/está inactivo, o si falla la conexión
     */
    public int obtenerStockDisponible(String nombreProducto, String idPedidoEnEdicion) throws SQLException {
        String sql = "SELECT p.stockActual + ISNULL((SELECT SUM(dp.cantidad) FROM Detalles_Pedidos dp " +
                     "WHERE dp.IDPedido = ? AND dp.IDProducto = p.IDProducto), 0) " +
                     "FROM Productos p WHERE p.nombreProducto = ? AND p.estado = 'activo'";
        try (var conexion = BaseDeDatos.abrirConexion(); PreparedStatement ps = conexion.prepareStatement(sql)) {
            if (idPedidoEnEdicion == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, Integer.parseInt(idPedidoEnEdicion.replace("P-", "")));
            }
            ps.setString(2, nombreProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("El producto no existe o esta inactivo: " + nombreProducto);
                return rs.getInt(1);
            }
        }
    }
}
