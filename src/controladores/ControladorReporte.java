package controladores;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import Vista.pedidos;
import Vista.reportes;
import datos.PedidoDAO;
import datos.VentaDAO;
import idioma.Idioma;
import modelo.Pedido;
import modelo.Venta;

/** Mantiene sincronizadas las pestanas de ventas y pedidos. */
public class ControladorReporte {

    private final reportes vista;
    private final PedidoDAO pedidoDAO = new PedidoDAO();
    private final VentaDAO ventaDAO = new VentaDAO();

    public ControladorReporte(reportes vista) {
        this.vista = vista;
        cargarInformacion();
        // ANTES: este botón solo volvía a llamar cargarInformacion() (o sea, "refrescar
        // la vista"). AHORA es un botón real de "Modificar Pedido": cambiar la vista ya
        // no hace falta manualmente, porque al regresar de editar, la ventana de Reportes
        // se vuelve a abrir y consulta SQL Server de nuevo (ver ControladorPedido).
        vista.getBtnActualizar().addActionListener(e -> modificarPedidoSeleccionado());
        vista.getBtnEliminarFila().addActionListener(e -> eliminarFilaSeleccionada());
        vista.getBtnVaciarTabla().addActionListener(e -> vaciarTablaActual());
        vista.getBtnBorrarTodo().addActionListener(e -> borrarTodo());
    }

    public void cargarInformacion() {
        cargarVentas();
        cargarPedidos();
    }

    private void cargarVentas() {
        DefaultTableModel modelo = vista.getModeloVentas();
        modelo.setRowCount(0);
        double ingresos = 0;
        try {
            java.util.List<Venta> ventas = ventaDAO.listarIngresos();
            for (Venta venta : ventas) {
                modelo.addRow(new Object[] { venta.getIdVenta(), venta.getFecha(), venta.getUsuario(), venta.getProductos(), String.format("$%.2f", venta.getTotal()) });
                ingresos += venta.getTotal();
            }
            vista.setMontoIngresos(String.format("$%.2f", ingresos));
            vista.setCantidadVentas(ventas.size());
        } catch (java.sql.SQLException ex) {
            vista.setErrorVentas();
            JOptionPane.showMessageDialog(vista, Idioma.get("reportes.msg.errorCargarVentas", ex.getMessage()), Idioma.get("comun.baseDeDatos"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarPedidos() {
        DefaultTableModel modelo = vista.getModeloPedidos();
        modelo.setRowCount(0);
        try {
            java.util.List<Pedido> pedidos = pedidoDAO.listar();
            for (Pedido pedido : pedidos) {
                modelo.addRow(new Object[] {
                    pedido.getIdPedido(), pedido.getFecha(), pedido.getCliente(), pedido.getTelefono(),
                    pedido.getProductosResumen(), String.format("$%.2f", pedido.getTotal()), pedido.getEstado()
                });
            }
            vista.setCantidadPedidos(pedidos.size());
        } catch (java.sql.SQLException ex) {
            vista.setErrorPedidos();
            JOptionPane.showMessageDialog(vista, Idioma.get("reportes.msg.errorCargarPedidos", ex.getMessage()), Idioma.get("comun.baseDeDatos"), JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * NUEVO: reemplaza lo que antes hacía "Actualizar pedidos". Verifica que el usuario
     * esté en la pestaña de Pedidos y tenga un pedido seleccionado, trae ese pedido
     * COMPLETO desde SQL Server (incluyendo IDCliente y sus datos de contacto) y abre la
     * ventana de Pedidos ya en modo edición con toda su información precargada.
     */
    private void modificarPedidoSeleccionado() {
        if (vista.mostrandoVentas()) {
            JOptionPane.showMessageDialog(vista, Idioma.get("reportes.msg.seleccionaPestanaPedidos"), Idioma.get("comun.aplicacion"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        int fila = vista.getFilaSeleccionada();
        if (fila < 0) {
            JOptionPane.showMessageDialog(vista, Idioma.get("reportes.msg.seleccionaPedidoModificar"), Idioma.get("comun.aplicacion"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        String idPedido = vista.getModeloPedidos().getValueAt(fila, 0).toString();
        try {
            Pedido pedidoCompleto = pedidoDAO.obtenerCompleto(idPedido);
            // Se cierra Reportes y se abre Pedidos ya cargado con la información del
            // pedido seleccionado; ControladorPedido.guardarPedido() sabrá que debe
            // actualizar este pedido en lugar de crear uno nuevo.
            pedidos ventanaEdicion = new pedidos(pedidoCompleto);
            ventanaEdicion.setVisible(true);
            vista.dispose();
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(vista, Idioma.get("reportes.msg.errorCargarPedido", ex.getMessage()), Idioma.get("comun.baseDeDatos"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarFilaSeleccionada() {
        int fila = vista.getFilaSeleccionada();
        if (fila < 0) {
            JOptionPane.showMessageDialog(vista, Idioma.get("reportes.msg.seleccionaFilaEliminar"), Idioma.get("comun.aplicacion"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (vista.mostrandoVentas()) {
            String idVenta = vista.getModeloVentas().getValueAt(fila, 0).toString();
            if (confirmar(Idioma.get("reportes.msg.confirmarEliminarVenta", idVenta))) eliminarVenta(idVenta);
        } else {
            String idPedido = vista.getModeloPedidos().getValueAt(fila, 0).toString();
            if (confirmar(Idioma.get("reportes.msg.confirmarEliminarPedido", idPedido))) eliminarPedido(idPedido);
        }
        cargarInformacion();
    }

    private void eliminarVenta(String idVenta) {
        try {
            ventaDAO.eliminar(idVenta);
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(vista, Idioma.get("reportes.msg.errorEliminarVenta", ex.getMessage()), Idioma.get("comun.baseDeDatos"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarPedido(String idPedido) {
        try {
            pedidoDAO.eliminar(idPedido);
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(vista, Idioma.get("reportes.msg.errorEliminarPedido", ex.getMessage()), Idioma.get("comun.baseDeDatos"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void vaciarTablaActual() {
        String mensaje = vista.mostrandoVentas() ? Idioma.get("reportes.msg.confirmarVaciarVentas") : Idioma.get("reportes.msg.confirmarVaciarPedidos");
        if (!confirmar(mensaje)) return;
        if (vista.mostrandoVentas()) {
            try {
                ventaDAO.vaciar();
            } catch (java.sql.SQLException ex) {
                JOptionPane.showMessageDialog(vista, Idioma.get("reportes.msg.errorVaciarVentas", ex.getMessage()), Idioma.get("comun.baseDeDatos"), JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            try {
                pedidoDAO.vaciar();
            } catch (java.sql.SQLException ex) {
                JOptionPane.showMessageDialog(vista, Idioma.get("reportes.msg.errorVaciarPedidos", ex.getMessage()), Idioma.get("comun.baseDeDatos"), JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        cargarInformacion();
    }

    private void borrarTodo() {
        if (!confirmar(Idioma.get("reportes.msg.confirmarBorrarTodo"))) return;
        try {
            ventaDAO.vaciar();
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(vista, Idioma.get("reportes.msg.errorBorrarVentas", ex.getMessage()), Idioma.get("comun.baseDeDatos"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            pedidoDAO.vaciar();
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(vista, Idioma.get("reportes.msg.errorBorrarPedidos", ex.getMessage()), Idioma.get("comun.baseDeDatos"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        cargarInformacion();
    }

    private boolean confirmar(String mensaje) {
        return JOptionPane.showConfirmDialog(vista, mensaje, Idioma.get("usuarios.msg.confirmarTitulo"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }
}
