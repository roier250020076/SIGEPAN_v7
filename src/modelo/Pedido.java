package modelo;

import java.util.ArrayList;

public class Pedido {

    private String idPedido;
    private String cliente;
    private String telefono;
    private String fecha;
    private String estado;

    // =========================================================================
    // NUEVOS CAMPOS (para poder cargar el pedido COMPLETO en modo "Modificar Pedido")
    // -------------------------------------------------------------------------
    // Antes Pedido solo cargaba nombre+telefono (suficiente para listarlo en el
    // reporte). Para poder editarlo hace falta tambien su IDCliente real (para
    // saber a que fila de Clientes actualizar) y sus demas datos de contacto.
    // =========================================================================
    private int idCliente = -1;
    private String apellido = "";
    private String direccion = "";
    private String correo = "";

    private ArrayList<DetallePedido> detalles =
            new ArrayList<>();

    public Pedido(String idPedido, String cliente, String telefono, String fecha) {
        this.idPedido = idPedido;
        this.cliente = cliente;
        this.telefono = telefono;
        this.fecha = fecha;
        this.estado = "En proceso";
    }

    public String getIdPedido() { return idPedido; }
    public void setIdPedido(String idPedido) { this.idPedido = idPedido; }

    public String getCliente() {
        return cliente;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getFecha() { return fecha; }

    public String getEstado() { return estado; }

    public void setEstado(String estado) { this.estado = estado; }

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public ArrayList<DetallePedido> getDetalles() {
        return detalles;
    }

    public double getTotal() {
        double total = 0.0;
        for (DetallePedido detalle : detalles) {
            total += detalle.getSubtotal();
        }
        return total;
    }

    public String getProductosResumen() {
        StringBuilder resumen = new StringBuilder();
        for (int i = 0; i < detalles.size(); i++) {
            DetallePedido detalle = detalles.get(i);
            resumen.append(detalle.getProducto()).append(" (x").append(detalle.getCantidad()).append(")");
            if (i < detalles.size() - 1) {
                resumen.append(", ");
            }
        }
        return resumen.toString();
    }

}
