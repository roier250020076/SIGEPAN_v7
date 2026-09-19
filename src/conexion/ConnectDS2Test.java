package conexion;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

public class ConnectDS2Test {

	// El DataSource se inicializa una sola vez cuando la clase se carga en memoria
	private static final SQLServerDataSource ds = new SQLServerDataSource();

	static {
		Properties propiedades = new Properties();

		// Busca el archivo en el classpath (ej. src/main/resources o src/properties)
		try (InputStream input = ConnectDS2Test.class.getClassLoader().getResourceAsStream("properties/config.properties")) {

			if (input == null) {
				throw new RuntimeException("No se pudo encontrar el archivo config.properties en el classpath.");
			}

			propiedades.load(input);

			// Configuración del DataSource (sin espacios en las claves)
			ds.setUser(propiedades.getProperty("user"));
			ds.setPassword(propiedades.getProperty("password"));
			ds.setServerName(propiedades.getProperty("serverName"));
			ds.setPortNumber(Integer.parseInt(propiedades.getProperty("portNumber")));
			ds.setDatabaseName(propiedades.getProperty("databaseName"));
			String instancia = propiedades.getProperty("instanceName", "").trim();
			if (!instancia.isEmpty()) ds.setInstanceName(instancia);

			// Nota: En producción, considera configurar un TrustStore real en lugar de esto
			ds.setTrustServerCertificate(true);

		} catch (IOException e) {
			throw new RuntimeException("Error al cargar la configuración de la base de datos.", e);
		}
	}

	/**
	 * Obtiene una conexión a la base de datos.
	 * 
	 * @return Connection activa.
	 * @throws SQLException si hay un error de conexión.
	 */
	public static Connection getConex() throws SQLException {
		// Como el DataSource ya está configurado (gracias al bloque static),
		// solo pedimos la conexión directamente.
		return ds.getConnection();
	}

	public static void main(String[] args) {

		// La consulta SQL solicitada (se corrigió el espacio en "SE LECT")
		String sql = "SELECT IDCliente, Nombre, apellido, telefono, direccion, correo, fechaNacimiento, fechaRegistro, estado " + "FROM SIGEPAN.dbo.Clientes;";

		// Usamos try-with-resources para cerrar automáticamente
		// la conexión, el statement y el resultSet
		try (Connection conexion = ConnectDS2Test.getConex();
				Statement stmt = conexion.createStatement();
				ResultSet rs = stmt.executeQuery(sql)) {

			System.out.println("=== Conexión exitosa a la base de datos POO ===\n");
			System.out.println("Ejecutando consulta: " + sql);
			System.out.println("---------------------------------------------------");
			System.out.printf("%-10s | %-20s | %-20s", "IDCliente", "Nombre", "apellido", "telefono", "direccion", "correo", "fechaNacimiento", "fechaRegistro", "estado ");
			System.out.println("---------------------------------------------------");

			int contador = 0;
			// Recorremos el ResultSet fila por fila
			while (rs.next()) {
				int idCliente = rs.getInt("IDCliente");
				String Nombre = rs.getString("Nombre");
				String apellido = rs.getString("apellido");
				String telefono = rs.getString("telefono");
				String direccion = rs.getString("direccion");
				String correo = rs.getString("correo");
				java.sql.Date fechaNacimiento = rs.getDate("fechaNacimiento");
				java.sql.Date fechaRegistro = rs.getDate("fechaRegistro");
				String estado = rs.getString("estado");

				System.out.printf("%-10d | %-20s | %-20s%n", idCliente, Nombre, apellido, telefono, direccion, correo, fechaNacimiento, fechaRegistro, estado );
				contador++;
			}

			System.out.println("---------------------------------------------------");
			System.out.println("Total de registros encontrados: " + contador);

		} catch (SQLException e) {
			System.err.println("❌ Error al ejecutar la consulta SQL:");
			System.err.println("Mensaje: " + e.getMessage());
			System.err.println("Código SQLState: " + e.getSQLState());
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("❌ Error inesperado:");
			e.printStackTrace();
		}
	}

}
