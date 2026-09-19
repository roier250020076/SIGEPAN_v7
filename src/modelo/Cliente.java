package modelo;

/**
 * Representa un cliente de la panadería (fila de la tabla {@code Clientes}): la persona
 * a la que se le vende, NO una cuenta de acceso al sistema (eso es {@link Usuario}).
 * <p>
 * Se usa en tres lugares: la ventana "Clientes" (listado completo), el combo de
 * "Cliente registrado" de Pedidos (para elegir uno ya existente sin volver a escribir
 * sus datos) y {@code ControladorPedido}, que lo usa para saber si el pedido debe
 * actualizar a un cliente ya existente o crear uno nuevo.
 */
public class Cliente {

    private final int idCliente;
    private final String nombre;
    private final String apellido;
    private final String telefono;
    private final String direccion;
    private final String correo;

    /**
     * @param idCliente folio del cliente (columna IDCliente)
     * @param nombre    nombre(s) del cliente
     * @param apellido  apellido(s) del cliente
     * @param telefono  teléfono a 10 dígitos
     * @param direccion dirección del cliente
     * @param correo    correo electrónico del cliente
     */
    public Cliente(int idCliente, String nombre, String apellido, String telefono, String direccion, String correo) {
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.apellido = apellido;
        this.telefono = telefono;
        this.direccion = direccion;
        this.correo = correo;
    }

    public int getIdCliente() { return idCliente; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getTelefono() { return telefono; }
    public String getDireccion() { return direccion; }
    public String getCorreo() { return correo; }

    /**
     * Texto que se muestra en el combo "Cliente registrado" de Pedidos (por ejemplo
     * {@code "Juan Pérez - 6671234567"}). JComboBox llama a toString() de cada elemento
     * para pintarlo, así que con esto no hace falta un renderer aparte solo para el
     * texto (el renderer de pedidos.java solo lo usa para el caso especial de "-- Nuevo
     * cliente --", que no es un Cliente real sino null).
     */
    @Override
    public String toString() {
        return nombre + " " + apellido + " - " + telefono;
    }
}
