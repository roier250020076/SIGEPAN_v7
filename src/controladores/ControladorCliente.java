package controladores;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import Vista.Clientes;
import datos.ClienteDAO;
import idioma.Idioma;
import modelo.Cliente;

/**
 * Controlador de la ventana {@link Clientes} (listado, búsqueda y baja de los
 * CLIENTES de la panadería).
 * <p>
 * No confundir con {@link ControladorUsuario}, que administra las cuentas de acceso al
 * sistema (empleados). Este controlador es nuevo y separado a propósito (ver punto 8
 * de la solicitud: "separar los controladores"), con una sola responsabilidad: todo lo
 * relacionado con la tabla {@code Clientes}.
 * <p>
 * El registro de clientes NUEVOS y la detección de clientes YA EXISTENTES no vive
 * aquí: eso lo sigue haciendo {@code ClienteDAO.obtenerOCrear(...)}, usado
 * directamente por {@code ControladorPedido} al guardar un pedido (que es, en la
 * práctica, la única puerta de entrada para registrar un cliente nuevo). Este
 * controlador solo consulta y da de baja lo que ya existe.
 */
public class ControladorCliente {

    private final Clientes vista;
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private List<Cliente> clientesMostrados = new ArrayList<>();

    public ControladorCliente(Clientes vista) {
        this.vista = vista;
        actualizarTablaClientes();
        vista.getBtnEliminar().addActionListener(e -> eliminarCliente());
    }

    /** Vuelve a cargar la tabla desde la base de datos (clientes activos, ordenados por nombre y apellido). */
    public void actualizarTablaClientes() {
        try {
            clientesMostrados = clienteDAO.listarActivos();
            DefaultTableModel modelo = vista.getModelo();
            modelo.setRowCount(0);
            for (Cliente cliente : clientesMostrados) {
                modelo.addRow(new Object[] {
                        cliente.getIdCliente(), cliente.getNombre(), cliente.getApellido(),
                        cliente.getTelefono(), cliente.getDireccion(), cliente.getCorreo()
                });
            }
            vista.setTotalClientesMostrado(clientesMostrados.size());
        } catch (SQLException ex) {
            mostrarError(ex, Idioma.get("clientes.msg.errorCargarLista"));
        }
    }

    /** Da de baja (lógicamente, sin borrar su historial de pedidos) al cliente seleccionado en la tabla, con confirmación previa. */
    private void eliminarCliente() {
        int filaVista = vista.getTablaClientes().getSelectedRow();
        if (filaVista < 0) {
            JOptionPane.showMessageDialog(vista, Idioma.get("clientes.msg.seleccionaEliminar"), Idioma.get("comun.advertencia"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        int filaModelo = vista.getTablaClientes().convertRowIndexToModel(filaVista);
        Cliente cliente = clientesMostrados.get(filaModelo);
        String nombreCompleto = cliente.getNombre() + " " + cliente.getApellido();
        if (JOptionPane.showConfirmDialog(vista, Idioma.get("clientes.msg.confirmarEliminar", nombreCompleto),
                Idioma.get("clientes.msg.confirmarTitulo"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try {
                clienteDAO.desactivar(cliente.getIdCliente());
                actualizarTablaClientes();
            } catch (SQLException ex) {
                mostrarError(ex, Idioma.get("clientes.msg.errorEliminar"));
            }
        }
    }

    private void mostrarError(SQLException ex, String mensaje) {
        JOptionPane.showMessageDialog(vista, Idioma.get("comun.msg.errorBaseDatos", mensaje, ex.getMessage()), Idioma.get("comun.baseDeDatos"), JOptionPane.ERROR_MESSAGE);
    }
}
