package datos;

import java.sql.Connection;
import java.sql.SQLException;

import conexion.ConnectDS;

/**
 * Punto único de acceso a la conexión de SQL Server para toda la capa DAO.
 * <p>
 * Todas las clases DAO (ClienteDAO, PedidoDAO, ProductoDAO, UsuarioDAO, VentaDAO)
 * abren su conexión a través de {@link #abrirConexion()} en vez de usar
 * {@link conexion.ConnectDS} directamente. Esto centraliza en un solo lugar cómo se
 * obtiene la conexión, de modo que si en el futuro cambia la forma de conectarse
 * (por ejemplo, para usar un <i>connection pool</i>), solo hay que modificar esta clase.
 * <p>
 * Es una clase de utilería (no se instancia): todos sus miembros son estáticos.
 */
public final class BaseDeDatos {

    /** Constructor privado: esta clase solo expone métodos estáticos. */
    private BaseDeDatos() { }

    /**
     * Abre una nueva conexión a la base de datos SIGEPAN en SQL Server, usando los
     * datos configurados en {@code properties/config.properties}.
     * <p>
     * Quien llama a este método es responsable de cerrar la conexión que recibe
     * (normalmente con un bloque {@code try-with-resources}, como hacen todos los DAO
     * de este proyecto) para no dejar conexiones abiertas innecesariamente.
     *
     * @return una conexión JDBC lista para usarse
     * @throws SQLException si el servidor no responde, las credenciales son incorrectas
     *                       o la base de datos SIGEPAN no existe
     */
    public static Connection abrirConexion() throws SQLException {
        return ConnectDS.getConex();
    }
}
