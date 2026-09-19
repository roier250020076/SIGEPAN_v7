package modelo;

import java.util.ArrayList;
import datos.ProductoDAO;

public class DatosGlobales {

    public static int idUsuarioActual;
    public static String nombreUsuarioActual = "";
    public static String rolUsuarioActual = "";
    
    // =========================================================================
    // COLECCIÓN ESTÁTICA COMPARTIDA (catálogo en memoria para Inicio y Pedidos)
    // =========================================================================
    
    /**
     * Catálogo maestro de productos disponibles en la panadería.
     * EXPLICACIÓN UNIVERSITARIA: Usas un ArrayList de 'Object[]' (Arreglo de Objetos).
     * Esto se hace porque cada elemento de la lista es una fila que contiene datos mixtos:
     * una cadena de texto (String para el nombre) y un número decimal (Double para el precio).
     * <p>
     * NOTA (revisión general): esta lista es solo una copia en memoria para pintar
     * rápido el catálogo en Inicio y el combo de Pedidos; la fuente real de datos es
     * SQL Server, vía {@link #recargarProductos()} y {@link datos.ProductoDAO}. Antes
     * esta clase también tenía {@code historialVentas}/{@code historialPedidos} como
     * "base de datos" en RAM, pero ya no se usaban en ningún lado (todo el historial de
     * ventas y pedidos se consulta directamente de SQL Server a través de VentaDAO y
     * PedidoDAO), así que se retiraron por ser código muerto.
     */
    public static ArrayList<Object[]> listaProductos = new ArrayList<>();

    // =========================================================================
    // BLOQUE INICIALIZADOR ESTÁTICO (Static Initialization Block)
    // =========================================================================
    /**
     * ALERTA EXAMEN: ¿Qué es este bloque 'static {}'?
     * Es un bloque de código que se ejecuta UNA SOLA VEZ en todo el ciclo de vida 
     * del programa, específicamente cuando la máquina virtual de Java (JVM) carga 
     * esta clase en memoria. No necesita que nadie lo llame; se dispara solito antes 
     * de que se abra cualquier ventana, asegurando que la panadería ya tenga mercancía disponible.
     * Es solo un respaldo temporal por si SQL Server no está disponible al arrancar;
     * en cuanto se puede conectar, {@link #recargarProductos()} lo reemplaza con el
     * catálogo real de la base de datos.
     */
    static {
        // Control de seguridad: Solo llena el menú inicial si la lista está completamente vacía
        if (listaProductos.isEmpty()) {
            listaProductos.add(new Object[]{"Concha", 15.0});
            listaProductos.add(new Object[]{"Ojo de buey", 12.0});
            listaProductos.add(new Object[]{"Coyota", 18.0});
            listaProductos.add(new Object[]{"Piña", 15.0});
            listaProductos.add(new Object[]{"Cortadillo", 15.0});
        }
    }

    // =========================================================================
    // MÉTODOS GLOBALES DE MANIPULACIÓN (API Interna)
    // =========================================================================

    /**
     * Reemplaza el catálogo en memoria con los productos activos que hay ahora mismo
     * en SQL Server. Se llama al abrir Inicio, Pedidos e Inventario para que siempre
     * se vea el catálogo más reciente (por ejemplo, después de dar de alta un producto
     * nuevo desde Inventario).
     *
     * @throws java.sql.SQLException si falla la conexión o la consulta a SQL Server
     */
    public static void recargarProductos() throws java.sql.SQLException {
        ArrayList<Object[]> productos = new ArrayList<>(new ProductoDAO().listarActivos());
        listaProductos.clear();
        listaProductos.addAll(productos);
    }
}
