package conexion;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

public class ConnectDS {

	// El DataSource se inicializa una sola vez cuando la clase se carga en memoria
	private static final SQLServerDataSource ds = new SQLServerDataSource();

	static {
		Properties propiedades = new Properties();

		// Busca el archivo en el classpath (ej. src/main/resources o src/properties)
		try (InputStream input = ConnectDS.class.getClassLoader().getResourceAsStream("properties/config.properties")) {

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

}
