package datos;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import modelo.Cliente;

/**
 * DAO (Data Access Object) para la tabla {@code Clientes} de SQL Server.
 * <p>
 * Se encarga de: encontrar el folio (IDCliente) de un cliente ya registrado, o crearlo
 * si es la primera vez que compra ({@link #obtenerOCrear}); actualizar sus datos cuando
 * se edita un pedido existente ({@link #actualizar}); y listar/dar de baja clientes para
 * la ventana Clientes ({@link #listarActivos}, {@link #desactivar}).
 * <p>
 * CAMBIO IMPORTANTE (historial): Antes este DAO pedía apellido/dirección/correo con
 * ventanas emergentes (JOptionPane) DESPUÉS de que el cajero ya había guardado el
 * pedido. Ahora esos datos ya vienen completos desde la propia ventana de Pedidos (el
 * ControladorPedido los valida antes de siquiera llamar a este método), así que
 * aquí únicamente los recibimos como parámetros y los insertamos. Ya no se abre
 * ninguna ventana emergente ni se le pide nada extra al usuario.
 */
public class ClienteDAO {

    /**
     * Busca un cliente activo por teléfono O correo; si no existe ninguno con esos
     * datos, lo crea con todos sus datos de una sola vez.
     * <p>
     * El teléfono y el correo funcionan como la clave "de negocio" para no duplicar
     * clientes: si dos pedidos distintos comparten el mismo teléfono O el mismo correo,
     * se asume que es el mismo cliente y se reutiliza su folio en vez de crear un
     * registro nuevo (los datos nuevos que se hayan escrito en el formulario NO
     * sobreescriben al cliente ya guardado; para eso existe {@link #actualizar}).
     * <p>
     * NOTA (revisión de duplicados): antes esta búsqueda solo comparaba el teléfono. Se
     * agregó también el correo porque {@code Clientes.correo} tiene una restricción
     * UNIQUE en la base de datos: si alguien compraba antes con un teléfono distinto
     * pero el mismo correo, el INSERT de abajo fallaba con un error de SQL Server en vez
     * de reutilizar al cliente. Buscar por "teléfono O correo" evita ese choque y evita
     * duplicados en cualquiera de los dos casos.
     *
     * @param nombre    nombre(s) del cliente
     * @param apellido  apellido(s) del cliente
     * @param telefono  teléfono a 10 dígitos; se usa para buscar si el cliente ya existe
     * @param direccion dirección del cliente
     * @param correo    correo electrónico del cliente; también se usa para buscar
     * @return el IDCliente (ya existente, o recién generado) que debe usarse como
     *         llave foránea al guardar el Pedido
     * @throws SQLException si falla la conexión, o si el INSERT no devuelve un ID
     *                       generado (caso extremo que en la práctica no debería pasar)
     */
    public int obtenerOCrear(String nombre, String apellido, String telefono, String direccion, String correo) throws SQLException {
        String consulta = "SELECT IDCliente FROM Clientes WHERE estado = 'activo' AND (telefono = ? OR correo = ?)";
        try (var conexion = BaseDeDatos.abrirConexion(); PreparedStatement ps = conexion.prepareStatement(consulta)) {
            ps.setString(1, telefono);
            ps.setString(2, correo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                // Cliente ya registrado con ese telefono o correo: se reutiliza su folio, no se duplica.
                if (rs.next()) return rs.getInt(1);
            }
        }

        String insertar = "INSERT INTO Clientes (nombre, apellido, telefono, direccion, correo) VALUES (?, ?, ?, ?, ?)";
        try (var conexion = BaseDeDatos.abrirConexion();
             PreparedStatement ps = conexion.prepareStatement(insertar, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre.trim());
            ps.setString(2, apellido.trim());
            ps.setString(3, telefono);
            ps.setString(4, direccion.trim());
            ps.setString(5, correo.trim());
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) return claves.getInt(1);
            }
        }
        throw new SQLException("No se pudo obtener el identificador del cliente.");
    }

    /**
     * Actualiza los datos de un cliente que YA EXISTE (identificado por su IDCliente).
     * Se usa desde "Modificar Pedido", cuando el usuario corrige el nombre, teléfono,
     * dirección o correo de un pedido ya guardado.
     *
     * @param idCliente folio del cliente a actualizar (columna IDCliente)
     * @param nombre    nuevo nombre(s)
     * @param apellido  nuevo apellido(s)
     * @param telefono  nuevo teléfono
     * @param direccion nueva dirección
     * @param correo    nuevo correo electrónico
     * @throws SQLException si falla la conexión, o si el cliente ya no existe o está
     *                       inactivo (0 filas afectadas), para que quien llama sepa que
     *                       la actualización no se aplicó
     */
    public void actualizar(int idCliente, String nombre, String apellido, String telefono, String direccion, String correo) throws SQLException {
        String sql = "UPDATE Clientes SET nombre = ?, apellido = ?, telefono = ?, direccion = ?, correo = ? WHERE IDCliente = ? AND estado = 'activo'";
        try (var conexion = BaseDeDatos.abrirConexion(); PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            ps.setString(2, apellido.trim());
            ps.setString(3, telefono);
            ps.setString(4, direccion.trim());
            ps.setString(5, correo.trim());
            ps.setInt(6, idCliente);
            int filas = ps.executeUpdate();
            if (filas == 0) throw new SQLException("El cliente ya no existe o esta inactivo.");
        }
    }

    /**
     * Lista todos los clientes activos, para la ventana Clientes y para el combo
     * "Cliente registrado" de Pedidos.
     *
     * @return lista de clientes activos, ordenada por nombre y apellido
     * @throws SQLException si falla la conexión o la consulta
     */
    public List<Cliente> listarActivos() throws SQLException {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT IDCliente, nombre, apellido, telefono, direccion, correo FROM Clientes WHERE estado = 'activo' ORDER BY nombre, apellido";
        try (var conexion = BaseDeDatos.abrirConexion(); PreparedStatement ps = conexion.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clientes.add(new Cliente(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6)));
            }
        }
        return clientes;
    }

    /**
     * Da de baja (lógicamente) a un cliente: lo marca {@code estado = 'inactivo'} en vez
     * de borrarlo físicamente, igual que {@link ProductoDAO#eliminar} y
     * {@link UsuarioDAO#desactivar}, para no perder el historial de pedidos que ya
     * tenía asociados (quedan intactos, ligados a su IDCliente por llave foránea). Un
     * cliente inactivo ya no aparece en {@link #listarActivos()} ni en el combo de
     * Pedidos, y si alguien vuelve a comprar con su mismo teléfono/correo,
     * {@link #obtenerOCrear} no lo encuentra y registra un cliente nuevo.
     *
     * @param idCliente folio del cliente a desactivar
     * @throws SQLException si falla la conexión, o si el cliente ya no existe o ya
     *                       estaba inactivo (0 filas afectadas)
     */
    public void desactivar(int idCliente) throws SQLException {
        String sql = "UPDATE Clientes SET estado = 'inactivo' WHERE IDCliente = ? AND estado = 'activo'";
        try (var conexion = BaseDeDatos.abrirConexion(); PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            int filas = ps.executeUpdate();
            if (filas == 0) throw new SQLException("El cliente ya no existe o ya estaba inactivo.");
        }
    }
}
