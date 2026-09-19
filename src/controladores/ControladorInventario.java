package controladores;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import Vista.Inventario;
import datos.ProductoDAO;
import idioma.Idioma;
import modelo.DatosGlobales;

/** Alta, consulta, modificacion y baja de productos directamente en SQL Server. */
public class ControladorInventario {
    private final Inventario vista;
    // Guarda en paralelo las categorias mostradas en el combo, para poder resolver
    // el nombre seleccionado ("Panaderia") de vuelta a su IDCategoria numerico al guardar.
    private List<Object[]> categoriasDisponibles;
    // Guarda en paralelo, fila por fila, los datos COMPLETOS que se ven en la tabla de
    // catalogo (incluyendo el IDProducto real), para que Eliminar y Modificar sepan
    // exactamente sobre que registro de SQL Server deben actuar.
    private List<Object[]> productosMostrados = new ArrayList<>();
    // NUEVO: si no es null, el formulario esta en "modo edicion" sobre ese IDProducto;
    // si es null, el formulario esta en modo normal de "alta de producto nuevo".
    private Integer idProductoEnEdicion = null;

    public ControladorInventario(Inventario vista) {
        this.vista = vista;
        cargarCategorias();
        recargarDesdeBase();
        vista.getBtnAnadir().addActionListener(e -> guardarProducto());
        vista.getBtnEliminarProducto().addActionListener(e -> eliminarProducto());
        vista.getBtnModificarProducto().addActionListener(e -> cargarProductoParaEditar());
        vista.getBtnCancelarEdicion().addActionListener(e -> cancelarEdicion());
    }

    /** Llena el combo de categorias de la vista consultando la tabla Categorias. */
    private void cargarCategorias() {
        try {
            categoriasDisponibles = new ProductoDAO().listarCategorias();
            DefaultComboBoxModel<String> modeloCombo = new DefaultComboBoxModel<>();
            for (Object[] categoria : categoriasDisponibles) {
                modeloCombo.addElement(categoria[1].toString());
            }
            vista.getCboCategoria().setModel(modeloCombo);
        } catch (SQLException ex) {
            mostrarError(ex, Idioma.get("inventario.msg.errorCargarCategorias"));
        }
    }

    /** Actualiza la tabla "Productos en Catalogo" con nombre, categoria, precio y stock. */
    public void actualizarTablaProductos() {
        DefaultTableModel modelo = vista.getModeloExistentes();
        modelo.setRowCount(0);
        try {
            productosMostrados = new ProductoDAO().listarDetallado();
            for (Object[] producto : productosMostrados) {
                double precio = ((java.math.BigDecimal) producto[3]).doubleValue();
                modelo.addRow(new Object[] {
                    producto[1],                       // nombreProducto
                    producto[2],                        // nombreCategoria
                    String.format("$%.2f", precio),     // precioVenta formateado
                    producto[4]                          // stockActual
                });
            }
        } catch (SQLException ex) {
            mostrarError(ex, Idioma.get("inventario.msg.errorCargarCatalogo"));
        }
    }

    /**
     * Botón "Modificar Producto": toma la fila seleccionada de la tabla y vuelca todos
     * sus datos en el formulario para que el usuario los edite. Aquí SOLO se cargan los
     * datos; el guardado real ocurre cuando se presiona el botón principal (que en modo
     * edición pasa a llamarse "Guardar Cambios") y dispara guardarProducto().
     */
    private void cargarProductoParaEditar() {
        int fila = vista.getFilaSeleccionadaExistente();
        if (fila < 0) {
            JOptionPane.showMessageDialog(vista, Idioma.get("inventario.msg.seleccionaModificar"), Idioma.get("comun.advertencia"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        Object[] producto = productosMostrados.get(fila);
        idProductoEnEdicion = (Integer) producto[0];
        String nombre = producto[1].toString();
        String categoria = producto[2].toString();
        double precio = ((java.math.BigDecimal) producto[3]).doubleValue();
        int stockActual = (Integer) producto[4];
        int stockMinimo = (Integer) producto[5];
        String codigo = producto[6] == null ? "" : producto[6].toString();
        String descripcion = producto[7] == null ? "" : producto[7].toString();

        vista.cargarProductoEnFormulario(nombre, categoria, descripcion, codigo, precio, stockActual, stockMinimo);
        vista.setModoEdicion(true, nombre);
    }

    /** Cancela el modo edición y regresa el formulario a "alta de producto nuevo". */
    private void cancelarEdicion() {
        idProductoEnEdicion = null;
        vista.limpiarCampos();
        vista.setModoEdicion(false, null);
    }

    /**
     * Botón "Eliminar Producto": pide confirmación, elimina (de forma lógica) el producto
     * seleccionado y refresca la tabla. Si el producto eliminado era el que estaba siendo
     * editado en el formulario, también se cancela esa edición para no dejarla "colgada".
     */
    private void eliminarProducto() {
        int fila = vista.getFilaSeleccionadaExistente();
        if (fila < 0) {
            JOptionPane.showMessageDialog(vista, Idioma.get("inventario.msg.seleccionaEliminar"), Idioma.get("comun.advertencia"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        Object[] producto = productosMostrados.get(fila);
        int idProducto = (Integer) producto[0];
        String nombre = producto[1].toString();

        boolean confirmado = JOptionPane.showConfirmDialog(vista,
                Idioma.get("inventario.msg.confirmarEliminarTexto", nombre),
                Idioma.get("inventario.msg.confirmarEliminarTitulo"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
        if (!confirmado) return;

        try {
            new ProductoDAO().eliminar(idProducto);
            if (idProductoEnEdicion != null && idProductoEnEdicion == idProducto) {
                cancelarEdicion();
            }
            recargarDesdeBase();
            JOptionPane.showMessageDialog(vista, Idioma.get("inventario.msg.eliminadoExito"), Idioma.get("comun.exito"), JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            mostrarError(ex, Idioma.get("inventario.msg.errorEliminar"));
        }
    }

    /**
     * Botón principal del formulario. Si NO se está editando nada (idProductoEnEdicion es
     * null) se comporta como antes: da de alta un producto nuevo. Si SÍ se está editando,
     * actualiza el producto existente en SQL Server en lugar de crear uno nuevo.
     */
    private void guardarProducto() {
        String nombre = vista.getNombrePan();
        String precioTexto = vista.getPrecioPanTexto();
        String descripcion = vista.getDescripcion();
        String codigo = vista.getCodigo();
        String categoriaSeleccionada = vista.getCategoriaSeleccionada();
        String stockActualTexto = vista.getStockActualTexto();
        String stockMinimoTexto = vista.getStockMinimoTexto();

        if (nombre.isEmpty() || precioTexto.isEmpty()) {
            JOptionPane.showMessageDialog(vista, Idioma.get("inventario.msg.nombrePrecioObligatorio"), Idioma.get("comun.camposObligatorios"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (categoriaSeleccionada == null || categoriaSeleccionada.isEmpty()) {
            JOptionPane.showMessageDialog(vista, Idioma.get("inventario.msg.categoriaObligatoria"), Idioma.get("comun.camposObligatorios"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        // NUEVA VALIDACIÓN: evita registrar dos productos activos con el mismo nombre.
        // Se compara contra el catálogo ya cargado (sin distinguir mayúsculas/espacios), y
        // se ignora el propio producto cuando se está justo editándolo.
        if (existeNombreDuplicado(nombre)) {
            JOptionPane.showMessageDialog(vista, Idioma.get("inventario.msg.nombreDuplicado"), Idioma.get("comun.datosInvalidos"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            double precio = Double.parseDouble(precioTexto);
            if (precio <= 0 || Double.isNaN(precio) || Double.isInfinite(precio)) {
                JOptionPane.showMessageDialog(vista, Idioma.get("inventario.msg.precioMayorCero"), Idioma.get("comun.datosInvalidos"), JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Stock actual y minimo son opcionales: si se dejan vacios, se registran en cero.
            int stockActual = stockActualTexto.isEmpty() ? 0 : Integer.parseInt(stockActualTexto);
            int stockMinimo = stockMinimoTexto.isEmpty() ? 0 : Integer.parseInt(stockMinimoTexto);
            if (stockActual < 0 || stockMinimo < 0) {
                JOptionPane.showMessageDialog(vista, Idioma.get("inventario.msg.stockNegativo"), Idioma.get("comun.datosInvalidos"), JOptionPane.WARNING_MESSAGE);
                return;
            }

            int idCategoria = resolverIdCategoria(categoriaSeleccionada);
            if (idCategoria < 0) {
                JOptionPane.showMessageDialog(vista, Idioma.get("inventario.msg.categoriaNoEncontrada"), Idioma.get("comun.datosInvalidos"), JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (idProductoEnEdicion == null) {
                // MODO ALTA: exactamente el mismo comportamiento que ya existía.
                new ProductoDAO().insertar(nombre, idCategoria, descripcion, codigo, precio, stockActual, stockMinimo);
                recargarDesdeBase();
                vista.limpiarCampos();
                JOptionPane.showMessageDialog(vista, Idioma.get("inventario.msg.registradoExito"), Idioma.get("comun.exito"), JOptionPane.INFORMATION_MESSAGE);
            } else {
                // MODO EDICIÓN: actualiza el registro existente; no crea uno nuevo.
                new ProductoDAO().actualizar(idProductoEnEdicion, nombre, idCategoria, descripcion, codigo, precio, stockActual, stockMinimo);
                idProductoEnEdicion = null;
                vista.setModoEdicion(false, null);
                recargarDesdeBase();
                vista.limpiarCampos();
                JOptionPane.showMessageDialog(vista, Idioma.get("inventario.msg.actualizadoExito"), Idioma.get("comun.exito"), JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(vista, Idioma.get("inventario.msg.numerosInvalidos"), Idioma.get("comun.datosInvalidos"), JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            mostrarError(ex, Idioma.get("inventario.msg.errorGuardar"));
        }
    }

    /**
     * NUEVO (validación agregada): true si ya existe, entre los productos activos
     * mostrados en la tabla, otro con el mismo nombre (sin distinguir mayúsculas ni
     * espacios extra). Al estar en modo edición, el propio producto que se está
     * editando queda excluido de la comparación para no marcarse a sí mismo como duplicado.
     */
    private boolean existeNombreDuplicado(String nombre) {
        String nombreNormalizado = nombre.trim();
        for (Object[] producto : productosMostrados) {
            boolean mismoNombre = producto[1].toString().equalsIgnoreCase(nombreNormalizado);
            boolean esOtroProducto = idProductoEnEdicion == null || !idProductoEnEdicion.equals(producto[0]);
            if (mismoNombre && esOtroProducto) return true;
        }
        return false;
    }

    /** Busca, dentro de la lista cargada del combo, el IDCategoria que corresponde al nombre elegido. */
    private int resolverIdCategoria(String nombreCategoria) {
        if (categoriasDisponibles == null) return -1;
        for (Object[] categoria : categoriasDisponibles) {
            if (categoria[1].toString().equals(nombreCategoria)) {
                return (Integer) categoria[0];
            }
        }
        return -1;
    }

    private void recargarDesdeBase() {
        try {
            DatosGlobales.recargarProductos();
            actualizarTablaProductos();
        } catch (SQLException ex) {
            actualizarTablaProductos();
            mostrarError(ex, Idioma.get("inventario.msg.errorCargarCatalogo"));
        }
    }

    private void mostrarError(SQLException ex, String accion) {
        JOptionPane.showMessageDialog(vista, Idioma.get("comun.msg.errorBaseDatos", accion, ex.getMessage()), Idioma.get("comun.baseDeDatos"), JOptionPane.ERROR_MESSAGE);
    }
}
