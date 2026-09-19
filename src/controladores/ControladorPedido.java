package controladores;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import Vista.Inicio;
import Vista.pedidos;
import datos.ClienteDAO;
import datos.PedidoDAO;
import datos.ProductoDAO;
import datos.VentaDAO;
import idioma.Idioma;
import modelo.Cliente;
import modelo.DatosGlobales;
import modelo.DetallePedido;
import modelo.Pedido;

/** Controla la venta de mostrador, los pedidos especiales y su cobro. */
public class ControladorPedido {

    private pedidos vista;
    private Inicio vistaInicio;
    private final ArrayList<DetallePedido> detalles = new ArrayList<>();
    private DefaultTableModel modeloCarrito;
    private double totalAcumuladoInicio;
    private final PedidoDAO pedidoDAO = new PedidoDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final VentaDAO ventaDAO = new VentaDAO();

    // =========================================================================
    // MODO EDICIÓN ("Modificar Pedido")
    // -------------------------------------------------------------------------
    // Cuando estos dos campos tienen valor, guardarPedido() actualiza el pedido
    // existente (idPedidoEnEdicion / idClienteEnEdicion) en vez de crear uno nuevo.
    // Se activan desde cargarPedidoParaEditar(...), llamado por ControladorReporte
    // cuando el usuario presiona "Modificar Pedido" en la ventana de Reportes.
    // =========================================================================
    private String idPedidoEnEdicion = null;
    private int idClienteEnEdicion = -1;

    // =========================================================================
    // SELECCIÓN DE CLIENTE EXISTENTE (combo "Cliente registrado" de Pedidos)
    // -------------------------------------------------------------------------
    // Si el usuario elige un cliente real del combo (no la opción "-- Nuevo
    // cliente --", que se representa como null), aquí se guarda cuál para que,
    // al guardar, se actualice ESE cliente en vez de buscarlo de nuevo por
    // teléfono/correo. Si se deja en "-- Nuevo cliente --", esto queda null y
    // guardarPedidoNuevo() sigue el flujo de siempre (obtenerOCrear).
    // =========================================================================
    private Cliente clienteSeleccionado = null;

    public ControladorPedido(pedidos vista) {
        this.vista = vista;
        cargarClientesEnCombo();
        vista.getCboCliente().addActionListener(e -> seleccionarClienteDelCombo());
    }

    public ControladorPedido(Inicio vistaInicio) {
        this.vistaInicio = vistaInicio;
        inicializarComponentesInicio();
    }

    /**
     * Llena el combo "Cliente registrado" con los clientes activos de SQL Server. El
     * primer elemento es {@code null} (la vista lo pinta como "-- Nuevo cliente --"; ver
     * el renderer en {@code pedidos.java}), para que capturar un cliente nuevo siga
     * siendo tan fácil como antes: simplemente se deja el combo ahí y se escribe en los
     * campos de texto de siempre.
     */
    private void cargarClientesEnCombo() {
        try {
            java.util.List<Cliente> clientes = clienteDAO.listarActivos();
            javax.swing.DefaultComboBoxModel<Cliente> modelo = new javax.swing.DefaultComboBoxModel<>();
            modelo.addElement(null);
            for (Cliente cliente : clientes) modelo.addElement(cliente);
            vista.getCboCliente().setModel(modelo);
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(vista, Idioma.get("comun.msg.errorBaseDatos", Idioma.get("pedidos.msg.errorCargarClientes"), ex.getMessage()), Idioma.get("comun.baseDeDatos"), JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Se dispara cuando el usuario elige algo en el combo "Cliente registrado".
     * <p>
     * Si eligió un cliente real, carga sus 5 datos en los campos de texto de inmediato
     * (para "no tener que escribirlos nuevamente") y lo recuerda en
     * {@code clienteSeleccionado} para que {@link #guardarPedidoNuevo()} actualice a ese
     * cliente en vez de crear uno nuevo. Si eligió "-- Nuevo cliente --" (null), limpia
     * esos mismos campos para que el usuario capture desde cero.
     */
    private void seleccionarClienteDelCombo() {
        Object seleccion = vista.getCboCliente().getSelectedItem();
        clienteSeleccionado = (seleccion instanceof Cliente) ? (Cliente) seleccion : null;
        if (clienteSeleccionado != null) {
            vista.cargarDatosCliente(clienteSeleccionado.getNombre(), clienteSeleccionado.getApellido(),
                    clienteSeleccionado.getTelefono(), clienteSeleccionado.getDireccion(), clienteSeleccionado.getCorreo());
        } else {
            vista.cargarDatosCliente("", "", "", "", "");
        }
    }
    //carrito
    private void inicializarComponentesInicio() {
        modeloCarrito = new DefaultTableModel(new Object[][] {}, new String[] { Idioma.get("inicio.columnaProducto"), Idioma.get("inicio.columnaPrecio") }) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        vistaInicio.getTablaCarrito().setModel(modeloCarrito);

        vistaInicio.getTablaProductos().addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int fila = vistaInicio.getTablaProductos().getSelectedRow();
                if (fila >= 0) {
                    String producto = vistaInicio.getTablaProductos().getValueAt(fila, 0).toString();
                    double precio = Double.parseDouble(vistaInicio.getTablaProductos().getValueAt(fila, 1).toString());
                    modeloCarrito.addRow(new Object[] { producto, precio });
                    totalAcumuladoInicio += precio;
                    vistaInicio.getLblCantidad().setText(formatearMoneda(totalAcumuladoInicio));
                }
            }
        });

        vistaInicio.getBtnLimpiar().addActionListener(e -> limpiarVentaRapida());
        vistaInicio.getBtnProcesar().addActionListener(e -> procesarVentaRapida());
        vistaInicio.getBtnPagarPedido().addActionListener(e -> gestionarPagoPedido());
    }

    /**
     * NUEVO: vuelve a poner los encabezados de columna del carrito ("Producto"/"Precio")
     * en el idioma actual. Se llama desde {@code Inicio.actualizarTextos()} porque el
     * modelo de esa tabla vive en este controlador, no en la vista.
     */
    public void actualizarTextosCarrito() {
        if (modeloCarrito != null) {
            modeloCarrito.setColumnIdentifiers(new Object[] { Idioma.get("inicio.columnaProducto"), Idioma.get("inicio.columnaPrecio") });
        }
    }

    private void procesarVentaRapida() {
        if (modeloCarrito.getRowCount() == 0 || totalAcumuladoInicio <= 0) {
            JOptionPane.showMessageDialog(vistaInicio, Idioma.get("inicio.msg.carritoVacio"), Idioma.get("comun.advertencia"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.util.List<Object[]> filas = new java.util.ArrayList<>();
        for (int i = 0; i < modeloCarrito.getRowCount(); i++) {
            filas.add(new Object[] { modeloCarrito.getValueAt(i, 0), modeloCarrito.getValueAt(i, 1) });
        }
        try {
            ventaDAO.registrarVentaRapida(filas, DatosGlobales.idUsuarioActual);
            JOptionPane.showMessageDialog(vistaInicio,
                Idioma.get("inicio.msg.ventaRegistrada", formatearMoneda(totalAcumuladoInicio)),
                Idioma.get("comun.exito"), JOptionPane.INFORMATION_MESSAGE);
            limpiarVentaRapida();
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(vistaInicio, Idioma.get("inicio.msg.errorVenta", ex.getMessage()), Idioma.get("comun.baseDeDatos"), JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Permite localizar un pedido abierto y registrar su pago desde la pantalla Inicio. */
    private void gestionarPagoPedido() {
        ArrayList<Pedido> pendientes = new ArrayList<>();
        try {
            for (Pedido pedido : pedidoDAO.listar()) {
                if (!"Pagado".equals(pedido.getEstado())) pendientes.add(pedido);
            }
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(vistaInicio, Idioma.get("inicio.msg.errorCargarPedidos", ex.getMessage()), Idioma.get("comun.baseDeDatos"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (pendientes.isEmpty()) {
            JOptionPane.showMessageDialog(vistaInicio, Idioma.get("inicio.msg.sinPedidosPendientes"), Idioma.get("comun.exito"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // NOTA: el "estado" que se muestra aquí (pedido.getEstado()) es un dato que viene
        // tal cual de la base de datos (columna Pedidos.estado), no un texto fijo del
        // código; por eso no se traduce con Idioma.get(...) como el resto de la interfaz.
        String[] opciones = new String[pendientes.size()];
        for (int i = 0; i < pendientes.size(); i++) {
            Pedido pedido = pendientes.get(i);
            opciones[i] = pedido.getIdPedido() + " | " + pedido.getCliente() + " | "
                + pedido.getEstado() + " | " + formatearMoneda(pedido.getTotal());
        }
        String seleccionado = (String) JOptionPane.showInputDialog(vistaInicio,
            Idioma.get("inicio.msg.elegirPedido"), Idioma.get("inicio.msg.elegirPedidoTitulo"),
            JOptionPane.PLAIN_MESSAGE, null, opciones, opciones[0]);
        if (seleccionado == null) return;

        Pedido pedido = pendientes.get(java.util.Arrays.asList(opciones).indexOf(seleccionado));
        Object[] acciones = { Idioma.get("inicio.msg.opcionPagado"),
        						Idioma.get("inicio.msg.opcionNoPagado"),
        						Idioma.get("comun.cancelar") };
        int accion = JOptionPane.showOptionDialog(vistaInicio,
            Idioma.get("inicio.msg.confirmarPagoTexto", pedido.getIdPedido()) + "\n" + formatearMoneda(pedido.getTotal()),
            Idioma.get("inicio.msg.confirmarPagoTitulo"), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, acciones, acciones[0]);

        if (accion == 0) {
            actualizarEstadoPedido(pedido, true);
        } else if (accion == 1) {
            actualizarEstadoPedido(pedido, false);
        }
    }

    /**
     * NUEVO: activa el "Modo Modificar Pedido". Llamado por ControladorReporte cuando el
     * usuario presiona "Modificar Pedido" en la ventana de Reportes con un pedido
     * seleccionado. Carga cliente, teléfono, dirección, correo y TODOS los productos del
     * pedido dentro de esta misma ventana de Pedidos para que el usuario los edite.
     */
    public void cargarPedidoParaEditar(Pedido pedido) {
        idPedidoEnEdicion = pedido.getIdPedido();
        idClienteEnEdicion = pedido.getIdCliente();

        // Deja el combo "Cliente registrado" mostrando al cliente real del pedido (si
        // sigue activo y aparece en la lista), en vez de dejarlo en "-- Nuevo cliente --".
        // Se hace ANTES de cargarDatosCliente(...) a propósito: cambiar el combo dispara
        // su propio autollenado de campos, y si el cliente no apareciera en la lista (por
        // ejemplo, quedó inactivo) ese autollenado los dejaría vacíos. Al llamar
        // cargarDatosCliente(...) con los datos reales del pedido DESPUÉS, esos son
        // siempre los que quedan visibles, sin importar lo que haya hecho el combo.
        vista.seleccionarClienteEnCombo(idClienteEnEdicion);
        vista.cargarDatosCliente(pedido.getCliente(), pedido.getApellido(), pedido.getTelefono(),
                pedido.getDireccion(), pedido.getCorreo());
        clienteSeleccionado = null; // en modo edición, guardarModificacionPedido() usa idClienteEnEdicion directamente, no este campo

        detalles.clear();
        detalles.addAll(pedido.getDetalles());
        actualizarTabla();
        calcularTotal();

        vista.setModoEdicion(true, idPedidoEnEdicion);
    }

    public void agregarProducto() {
        try {
            String producto = vista.getProducto();
            int cantidad = vista.getCantidad();
            double precio = vista.getPrecio();
            if (cantidad <= 0 || precio <= 0 || Double.isNaN(precio) || Double.isInfinite(precio)) {
                JOptionPane.showMessageDialog(vista, Idioma.get("pedidos.msg.cantidadPrecioInvalido"), Idioma.get("comun.datosInvalidos"), JOptionPane.WARNING_MESSAGE);
                return;
            }

            // VALIDACIÓN DE STOCK: lo que ya está en el carrito de este producto, más lo
            // que se intenta agregar ahora, no debe superar el stock disponible. Si se
            // está modificando un pedido existente, obtenerStockDisponible ya le suma lo
            // que ESE pedido tenía reservado de este producto, para no bloquear al
            // usuario cuando solo está subiendo una cantidad que su propio pedido ya
            // tenía apartada. Esto es un aviso inmediato en pantalla; el control real y
            // definitivo (por si dos personas venden al mismo tiempo) sigue estando en
            // PedidoDAO al momento de guardar.
            int cantidadYaEnCarrito = 0;
            for (DetallePedido detalle : detalles) {
                if (detalle.getProducto().equals(producto)) { cantidadYaEnCarrito = detalle.getCantidad(); break; }
            }
            int disponible = productoDAO.obtenerStockDisponible(producto, idPedidoEnEdicion);
            if (cantidadYaEnCarrito + cantidad > disponible) {
                JOptionPane.showMessageDialog(vista, Idioma.get("pedidos.msg.stockInsuficiente", producto, disponible), Idioma.get("comun.datosInvalidos"), JOptionPane.WARNING_MESSAGE);
                return;
            }

            for (DetallePedido detalle : detalles) {
                if (detalle.getProducto().equals(producto) && Double.compare(detalle.getPrecio(), precio) == 0) {
                    detalle.setCantidad(detalle.getCantidad() + cantidad);
                    actualizarTabla();
                    calcularTotal();
                    return;
                }
            }
            detalles.add(new DetallePedido(producto, cantidad, precio));
            actualizarTabla();
            calcularTotal();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(vista, Idioma.get("pedidos.msg.precioInvalido"), Idioma.get("comun.datosInvalidos"), JOptionPane.WARNING_MESSAGE);
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(vista, Idioma.get("comun.msg.errorBaseDatos", Idioma.get("pedidos.msg.errorConsultarStock"), ex.getMessage()), Idioma.get("comun.baseDeDatos"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarTabla() {
        DefaultTableModel modelo = vista.getModelo();
        modelo.setRowCount(0);
        for (DetallePedido detalle : detalles) {
            modelo.addRow(new Object[] { detalle.getProducto(), detalle.getCantidad(), detalle.getPrecio(), detalle.getSubtotal(), Idioma.get("pedidos.estadoEnProceso") });
        }
    }
     
    //se calcula el total
    public void calcularTotal() {
        double total = 0;
        for (DetallePedido detalle : detalles) total += detalle.getSubtotal();
        vista.setTotal(total);
    }

    //eliminar un producto
    public void eliminarProducto() {
        int fila = vista.getFilaSeleccionada();
        if (fila < 0) {
            JOptionPane.showMessageDialog(vista, Idioma.get("pedidos.msg.seleccionaFilaEliminar"), Idioma.get("comun.exito"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        detalles.remove(fila);
        actualizarTabla();
        calcularTotal();
    }

    public void limpiar() {
        detalles.clear();
        vista.limpiarCampos();
        actualizarTabla();
        vista.setTotal(0);
        // Regresa el combo a "-- Nuevo cliente --" (null) para no dejar seleccionado a un
        // cliente de un pedido anterior/cancelado.
        clienteSeleccionado = null;
        vista.getCboCliente().setSelectedIndex(0);
        // Si "Limpiar Todo" se presiona a la mitad de una edición, se cancela esa edición
        // y la ventana regresa a su modo normal de "capturar un pedido nuevo".
        if (idPedidoEnEdicion != null) {
            idPedidoEnEdicion = null;
            idClienteEnEdicion = -1;
            vista.setModoEdicion(false, null);
        }
    }

    /** Guarda un pedido especial nuevo, o actualiza uno existente si está en modo edición. */
    public void guardarPedido() {
        if (vista.getCliente().trim().isEmpty() || detalles.isEmpty()) {
            JOptionPane.showMessageDialog(vista, Idioma.get("pedidos.msg.clienteYProductoObligatorio"), Idioma.get("comun.exito"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (vista.getTelefono().trim().length() != 10) {
            JOptionPane.showMessageDialog(vista, Idioma.get("pedidos.msg.telefonoInvalido"), Idioma.get("comun.datosInvalidos"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        // NUEVAS VALIDACIONES: apellido, direccion y correo ahora se capturan en esta misma
        // ventana (ya no se piden despues con ventanas emergentes), asi que se validan aqui
        // igual que el resto de los campos del cliente.
        if (vista.getApellido().trim().isEmpty()) {
            JOptionPane.showMessageDialog(vista, Idioma.get("pedidos.msg.apellidoObligatorio"), Idioma.get("comun.datosInvalidos"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (vista.getDireccion().trim().isEmpty()) {
            JOptionPane.showMessageDialog(vista, Idioma.get("pedidos.msg.direccionObligatoria"), Idioma.get("comun.datosInvalidos"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!Validador.correoValido(vista.getCorreo())) {
            JOptionPane.showMessageDialog(vista, Idioma.get("pedidos.msg.correoInvalido"), Idioma.get("comun.datosInvalidos"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (DatosGlobales.idUsuarioActual <= 0) {
            JOptionPane.showMessageDialog(vista, Idioma.get("pedidos.msg.iniciaSesion"), Idioma.get("comun.exito"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (idPedidoEnEdicion != null) {
            guardarModificacionPedido();
        } else {
            guardarPedidoNuevo();
        }
    }

    /**
     * Crea un cliente (o reutiliza uno) y un pedido nuevo.
     * <p>
     * NUEVO: si el usuario eligió un cliente del combo "Cliente registrado"
     * ({@code clienteSeleccionado != null}), ya se sabe exactamente su IDCliente, así
     * que se actualiza directamente con lo que haya en los campos de texto (por si
     * corrigió algo, como la dirección) en vez de buscarlo de nuevo por teléfono/correo.
     * Si se dejó en "-- Nuevo cliente --" ({@code clienteSeleccionado == null}), el
     * comportamiento es el ORIGINAL: {@link ClienteDAO#obtenerOCrear} busca por
     * teléfono/correo y solo crea un registro nuevo si de verdad no existe.
     */
    private void guardarPedidoNuevo() {
        try {
            int idCliente;
            if (clienteSeleccionado != null) {
                idCliente = clienteSeleccionado.getIdCliente();
                clienteDAO.actualizar(idCliente,
                        vista.getCliente().trim(), vista.getApellido().trim(), vista.getTelefono().trim(),
                        vista.getDireccion().trim(), vista.getCorreo().trim());
            } else {
                // Un solo viaje a la base de datos: el cliente completo se busca o se crea
                // con TODOS sus datos de una vez (nombre, apellido, telefono, direccion y correo).
                idCliente = clienteDAO.obtenerOCrear(
                        vista.getCliente().trim(),
                        vista.getApellido().trim(),
                        vista.getTelefono().trim(),
                        vista.getDireccion().trim(),
                        vista.getCorreo().trim());
            }
            if (idCliente < 0) return;
            Pedido pedido = new Pedido("", vista.getCliente().trim(), vista.getTelefono().trim(), fechaActual());
            pedido.getDetalles().addAll(detalles);
            pedidoDAO.guardar(pedido, idCliente, DatosGlobales.idUsuarioActual);
            JOptionPane.showMessageDialog(vista, Idioma.get("pedidos.msg.guardadoExito", pedido.getIdPedido()), Idioma.get("comun.exito"), JOptionPane.INFORMATION_MESSAGE);
            limpiar();
            cargarClientesEnCombo(); // por si el pedido registró un cliente nuevo, ya aparece disponible para el siguiente pedido
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(vista, Idioma.get("pedidos.msg.errorGuardar", ex.getMessage()), Idioma.get("comun.baseDeDatos"), JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * NUEVO: guarda los cambios de "Modificar Pedido" -- actualiza el cliente y el pedido
     * YA EXISTENTES (identificados por idClienteEnEdicion / idPedidoEnEdicion) en lugar de
     * crear registros nuevos. Al terminar, regresa a la ventana de Reportes para que el
     * usuario vea la tabla de pedidos ya actualizada.
     */
    private void guardarModificacionPedido() {
        try {
            clienteDAO.actualizar(idClienteEnEdicion,
                    vista.getCliente().trim(),
                    vista.getApellido().trim(),
                    vista.getTelefono().trim(),
                    vista.getDireccion().trim(),
                    vista.getCorreo().trim());

            Pedido pedido = new Pedido(idPedidoEnEdicion, vista.getCliente().trim(), vista.getTelefono().trim(), fechaActual());
            pedido.getDetalles().addAll(detalles);
            pedidoDAO.actualizar(pedido, idClienteEnEdicion);

            JOptionPane.showMessageDialog(vista, Idioma.get("pedidos.msg.actualizadoExito", idPedidoEnEdicion), Idioma.get("comun.exito"), JOptionPane.INFORMATION_MESSAGE);

            idPedidoEnEdicion = null;
            idClienteEnEdicion = -1;
            // Se regresa a Reportes en vez de quedarse aqui: es la ventana desde donde
            // se inicio la edicion, y al reabrirla vuelve a consultar SQL Server, por lo
            // que la tabla de pedidos ya se ve actualizada de forma automatica.
            vista.dispose();
            new Vista.reportes().setVisible(true);
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(vista, Idioma.get("pedidos.msg.errorActualizar", ex.getMessage()), Idioma.get("comun.baseDeDatos"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarVentaRapida() {
        modeloCarrito.setRowCount(0);
        totalAcumuladoInicio = 0.0;
        vistaInicio.getLblCantidad().setText(Idioma.get("inicio.totalInicial"));
    }

    private String fechaActual() { return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()); }
    private String formatearMoneda(double monto) { return String.format("$%.2f", monto); }

    private void actualizarEstadoPedido(Pedido pedido, boolean pagado) {
        try {
            pedidoDAO.registrarPago(pedido.getIdPedido(), pagado);
            JOptionPane.showMessageDialog(vistaInicio,
                pagado ? Idioma.get("inicio.msg.pagoRegistrado") : Idioma.get("inicio.msg.marcadoNoPagado"),
                Idioma.get("comun.exito"), JOptionPane.INFORMATION_MESSAGE);
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(vistaInicio, Idioma.get("inicio.msg.errorPago", ex.getMessage()), Idioma.get("comun.baseDeDatos"), JOptionPane.ERROR_MESSAGE);
        }
    }
}
