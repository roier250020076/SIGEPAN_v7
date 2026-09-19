package datos;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import modelo.Usuario;

/**
 * DAO (Data Access Object) para la tabla {@code Usuarios} de SQL Server: autenticación
 * (login), alta de nuevos empleados y baja lógica.
 * <p>
 * Las contraseñas NUNCA se guardan ni se comparan en texto plano: siempre se pasan por
 * {@link Seguridad#hashContrasena(String)} antes de tocar la base de datos, tanto al
 * registrar un usuario como al autenticarlo.
 */
public class UsuarioDAO {

    /** Columnas que se leen en todas las consultas SELECT, para no repetirlas 3 veces. */
    private static final String COLUMNAS = "IDUsuario, nombre, apellido, direccion, telefono, correo, usuario, fechaNacimiento, rol";

    /**
     * Verifica las credenciales de acceso (usuario y contraseña) contra la base de datos.
     *
     * @param nombreUsuario nombre de usuario tal como se escribió en el login
     * @param contrasena    contraseña en texto plano (aquí se hashea antes de comparar)
     * @return el {@link Usuario} autenticado si las credenciales son correctas y la
     *         cuenta está activa; {@code null} si no coinciden (usuario incorrecto,
     *         contraseña incorrecta, o cuenta desactivada)
     * @throws SQLException si falla la conexión o la consulta
     */
    public Usuario autenticar(String nombreUsuario, String contrasena) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Usuarios WHERE usuario = ? AND passwordHash = ? AND estado = 'activo'";
        try (var conexion = BaseDeDatos.abrirConexion(); PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario.trim());
            ps.setString(2, Seguridad.hashContrasena(contrasena));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leerUsuario(rs) : null;
            }
        }
    }

    /**
     * Lista todos los usuarios activos (empleados con acceso al sistema), para la
     * tabla de la ventana Usuarios.
     *
     * @return lista de usuarios activos, ordenada por nombre y apellido
     * @throws SQLException si falla la conexión o la consulta
     */
    public List<Usuario> listarActivos() throws SQLException {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT " + COLUMNAS + " FROM Usuarios WHERE estado = 'activo' ORDER BY nombre, apellido";
        try (var conexion = BaseDeDatos.abrirConexion(); PreparedStatement ps = conexion.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) usuarios.add(leerUsuario(rs));
        }
        return usuarios;
    }

    /**
     * Da de alta un nuevo empleado con acceso al sistema.
     *
     * @param usuario    datos del nuevo usuario (el IDUsuario del objeto se ignora; lo
     *                   asigna SQL Server automáticamente con IDENTITY)
     * @param contrasena contraseña en texto plano capturada en el formulario de
     *                   Registro; se hashea aquí antes de guardarse, nunca se guarda
     *                   el texto original
     * @throws SQLException si falla la conexión o la inserción (por ejemplo, si el
     *                       nombre de usuario o el correo ya existen, que tienen
     *                       restricción UNIQUE en la base de datos)
     */
    public void insertar(Usuario usuario, String contrasena) throws SQLException {
        String sql = "INSERT INTO Usuarios (nombre, apellido, direccion, telefono, correo, usuario, passwordHash, fechaNacimiento, rol) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (var conexion = BaseDeDatos.abrirConexion(); PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getApellido());
            ps.setString(3, usuario.getDireccion());
            ps.setString(4, usuario.getTelefono());
            ps.setString(5, usuario.getCorreo());
            ps.setString(6, usuario.getNombreUsuario());
            ps.setString(7, Seguridad.hashContrasena(contrasena));
            if (usuario.getFechaNacimiento() == null || usuario.getFechaNacimiento().isBlank()) ps.setNull(8, java.sql.Types.DATE);
            else ps.setDate(8, Date.valueOf(usuario.getFechaNacimiento()));
            ps.setString(9, usuario.getRol());
            ps.executeUpdate();
        }
    }

    /**
     * Da de baja (lógicamente) a un usuario: lo marca {@code estado = 'inactivo'} en
     * vez de borrarlo físicamente, para no perder el historial de pedidos y ventas que
     * ese empleado haya registrado (que quedan ligados a su IDUsuario por llave foránea).
     * Un usuario inactivo ya no puede iniciar sesión ({@link #autenticar} lo excluye) ni
     * aparece en {@link #listarActivos()}.
     *
     * @param idUsuario folio del usuario a desactivar
     * @throws SQLException si falla la conexión o la actualización
     */
    public void desactivar(int idUsuario) throws SQLException {
        String sql = "UPDATE Usuarios SET estado = 'inactivo' WHERE IDUsuario = ?";
        try (var conexion = BaseDeDatos.abrirConexion(); PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        }
    }

    /** Construye un {@link Usuario} a partir de la fila actual de un ResultSet posicionado con {@link #COLUMNAS}. */
    private Usuario leerUsuario(ResultSet rs) throws SQLException {
        Date fecha = rs.getDate("fechaNacimiento");
        return new Usuario(rs.getInt("IDUsuario"), rs.getString("nombre"), rs.getString("apellido"),
            rs.getString("direccion"), rs.getString("telefono"), rs.getString("correo"),
            fecha == null ? "" : fecha.toString(), rs.getString("usuario"), rs.getString("rol"));
    }
}
