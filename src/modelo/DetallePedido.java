package modelo;

public class DetallePedido {

    private String producto;
    private int cantidad;
    private double precio;
    private double subtotal;

    public DetallePedido(String producto, int cantidad, double precio) {

        this.producto = producto;
        this.cantidad = cantidad;
        this.precio = precio;
        this.subtotal = cantidad * precio;
    }

    public String getProducto() {
        return producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public double getPrecio() {
        return precio;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
        this.subtotal = cantidad * precio;
    }

}