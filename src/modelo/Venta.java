package modelo;

public class Venta {
    private String idVenta;
    private String fecha;
    private String usuario;
    private String productos;
    private double total;

    // Constructor completo
    public Venta(String idVenta, String fecha, String usuario, String productos, double total) {
        this.idVenta = idVenta;
        this.fecha = fecha;
        this.usuario = usuario;
        this.productos = productos;
        this.total = total;
    }

    // Getters necesarios para pintar la tabla
    public String getIdVenta() { return idVenta; }
    public String getFecha() { return fecha; }
    public String getUsuario() { return usuario; }
    public String getProductos() { return productos; }
    public double getTotal() { return total; }
}