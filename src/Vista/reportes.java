package Vista;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

import controladores.ControladorReporte;
import idioma.ActualizableIdioma;
import idioma.Idioma;

/** 
 * Vista de reportes separada en las pestañas Ventas y Pedidos.
 * Implementa ActualizableIdioma para soporte multi-idioma en tiempo real.
 */
public class reportes extends JFrame implements ActualizableIdioma {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    
    // COMPONENTES DE TABLA Y MODELOS
    private JTable tablaVentas;
    private JTable tablaPedidos;
    private DefaultTableModel modeloVentas;
    private DefaultTableModel modeloPedidos;
    private JTabbedPane pestanas;
    
    // ETIQUETAS DE TARJETAS KPI (INDICADORES CLAVE DE DESEMPEÑO)
    private JLabel lblMontoIngresos;
    private JLabel lblCantVentas;
    private JLabel lblCantPedidos;
    
    // BOTONES DE ACCIÓN
    private JButton btnActualizar;
    private JButton btnEliminarFila;
    private JButton btnVaciarTabla;
    private JButton btnBorrarTodo;

    // REFERENCIAS DE MENÚ Y TEXTOS PARA TRADUCCIÓN DINÁMICA (i18n)
    private JMenu mnInicio, mnPedidos, mnUsuarios, mnClientes, mnReportes, mnConfiguracion, mnInventario, mnCerrarSesion;
    private JLabel lblTitulo, lblDescripcion, lblEtiquetaIngresos, lblEtiquetaVentas, lblEtiquetaPedidos;
    
    // VARIABLES DE ESTADO Y CONTROL DE ERRORES
    private int cantidadVentasMostrada = 0;
    private int cantidadPedidosMostrada = 0;
    private boolean huboErrorVentas = false;
    private boolean huboErrorPedidos = false;

    /**
     * Método main para ejecuciones de prueba independientes.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try { new reportes().setVisible(true); }
            catch (Exception e) { e.printStackTrace(); }
        });
    }

    /**
     * CONSTRUCTOR: Configura la interfaz gráfica, tarjetas KPI, pestañas y menú.
     */
    public reportes() {
        setTitle(Idioma.get("reportes.tituloVentana"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 980, 650);

        // PALETA DE COLORES PERSONALIZADA (Temática Panadería)
        Color fondo = new Color(253, 251, 247);  // Crema suave
        Color cafe = new Color(74, 53, 37);      // Café espresso profundo
        Color dorado = new Color(139, 94, 60);   // Café dorado para resaltados
        Color borde = new Color(222, 212, 198);  // Tono arena sutil
        Color gris = new Color(120, 110, 100);   // Gris cálido elegante

        // Construcción de la barra de navegación superior
        crearMenu(borde);
        
        contentPane = new JPanel();
        contentPane.setBackground(fondo);
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(null);
        setContentPane(contentPane);

        // ENCABEZADOS PRINCIPALES
        lblTitulo = new JLabel(Idioma.get("reportes.titulo"));
        lblTitulo.setFont(new Font("Segoe UI Semibold", Font.BOLD, 24));
        lblTitulo.setForeground(cafe);
        lblTitulo.setBounds(25, 20, 250, 30);
        contentPane.add(lblTitulo);

        lblDescripcion = new JLabel(Idioma.get("reportes.subtitulo"));
        lblDescripcion.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDescripcion.setForeground(gris);
        lblDescripcion.setBounds(25, 50, 450, 20);
        contentPane.add(lblDescripcion);

        // =================================================================
        // TARJETAS KPI (RESUMEN FINANCIERO Y OPERATIVO)
        // =================================================================
        // Tarjeta 1: Total Ingresos acumulados
        JPanel tarjetaIngresos = tarjeta(500, 18, 135, 58, borde);
        contentPane.add(tarjetaIngresos);
        lblEtiquetaIngresos = etiqueta(tarjetaIngresos, Idioma.get("reportes.tarjetaIngresos"), gris, 10, 8, 5, 110, 15);
        lblMontoIngresos = etiqueta(tarjetaIngresos, Idioma.get("reportes.montoInicial"), dorado, 17, Font.BOLD, 8, 23, 120, 27);

        // Tarjeta 2: Conteo Total de Ventas
        JPanel tarjetaVentas = tarjeta(650, 18, 135, 58, borde);
        contentPane.add(tarjetaVentas);
        lblEtiquetaVentas = etiqueta(tarjetaVentas, Idioma.get("reportes.tarjetaVentas"), gris, 10, 8, 5, 110, 15);
        lblCantVentas = etiqueta(tarjetaVentas, Idioma.get("reportes.textoCantidadVentas", 0), cafe, 15, Font.BOLD, 8, 23, 120, 27);

        // Tarjeta 3: Conteo Total de Pedidos
        JPanel tarjetaPedidos = tarjeta(800, 18, 135, 58, borde);
        contentPane.add(tarjetaPedidos);
        lblEtiquetaPedidos = etiqueta(tarjetaPedidos, Idioma.get("reportes.tarjetaPedidos"), gris, 10, 8, 5, 110, 15);
        lblCantPedidos = etiqueta(tarjetaPedidos, Idioma.get("reportes.textoCantidadPedidos", 0), cafe, 15, Font.BOLD, 8, 23, 120, 27);

        // =================================================================
        // PANEL DE PESTAÑAS Y TABLAS
        // =================================================================
        pestanas = new JTabbedPane();
        pestanas.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
        pestanas.setBounds(25, 95, 910, 390);
        contentPane.add(pestanas);

        // Pestaña 1: Ventas en Mostrador
        modeloVentas = crearModelo(new String[] { 
            Idioma.get("reportes.columnaIdVenta"), Idioma.get("reportes.columnaFecha"), 
            Idioma.get("reportes.columnaOrigen"), Idioma.get("reportes.columnaProductos"), 
            Idioma.get("reportes.columnaTotal") 
        });
        tablaVentas = crearTabla(modeloVentas);
        pestanas.addTab(Idioma.get("reportes.pestanaVentas"), new JScrollPane(tablaVentas));

        // Pestaña 2: Pedidos por Encargo
        modeloPedidos = crearModelo(new String[] { 
            Idioma.get("reportes.columnaIdPedido"), Idioma.get("reportes.columnaFecha"), 
            Idioma.get("reportes.columnaCliente"), Idioma.get("reportes.columnaTelefono"), 
            Idioma.get("reportes.columnaProductos"), Idioma.get("reportes.columnaTotal"), 
            Idioma.get("reportes.columnaEstado") 
        });
        tablaPedidos = crearTabla(modeloPedidos);
        pestanas.addTab(Idioma.get("reportes.pestanaPedidos"), new JScrollPane(tablaPedidos));

        // =================================================================
        // BOTONES DE ACCIÓN
        // =================================================================
        btnActualizar = boton(Idioma.get("reportes.btnModificarPedido"), dorado, Color.WHITE, 25, 510, 165, 34, null);
        btnEliminarFila = boton(Idioma.get("reportes.btnEliminarFila"), Color.WHITE, new Color(180, 50, 50), 205, 510, 135, 34, new LineBorder(new Color(230, 200, 200), 1));
        btnVaciarTabla = boton(Idioma.get("reportes.btnVaciarTabla"), Color.WHITE, cafe, 355, 510, 165, 34, new LineBorder(borde, 1));
        btnBorrarTodo = boton(Idioma.get("reportes.btnBorrarTodo"), new Color(180, 50, 50), Color.WHITE, 700, 510, 190, 34, null);
        
        contentPane.add(btnActualizar);
        contentPane.add(btnEliminarFila);
        contentPane.add(btnVaciarTabla);
        contentPane.add(btnBorrarTodo);

        // Inicialización del controlador vinculado a esta vista
        new ControladorReporte(this);

        // Habilita el redimensionamiento responsivo dinámico
        Redimensionable.activar(this, contentPane, 980, 650);
    }

    // =================================================================
    // MÉTODOS AUXILIARES DE CONSTRUCCIÓN DE INTERFAZ
    // =================================================================
    private void crearMenu(Color borde) {
        JMenuBar barra = new JMenuBar();
        barra.setBackground(Color.WHITE);
        barra.setBorder(new LineBorder(borde, 1));
        setJMenuBar(barra);
        
        mnInicio = menu(Idioma.get("menu.inicio"));
        mnPedidos = menu(Idioma.get("menu.pedidos"));
        mnUsuarios = menu(Idioma.get("menu.usuarios"));
        mnClientes = menu(Idioma.get("menu.clientes"));
        mnReportes = menu(Idioma.get("menu.reportes"));
        mnConfiguracion = menu(Idioma.get("menu.configuracion"));
        Idioma.construirSubmenuIdiomas(mnConfiguracion);
        mnInventario = menu(Idioma.get("menu.inventario"));
        mnCerrarSesion = menu(Idioma.get("menu.cerrarSesion"));
        mnCerrarSesion.setForeground(new Color(180, 50, 50));
        
        barra.add(mnInicio); barra.add(mnPedidos); barra.add(mnUsuarios); 
        barra.add(mnClientes); barra.add(mnReportes); barra.add(mnConfiguracion); 
        barra.add(mnInventario); barra.add(mnCerrarSesion);
        
        // Asignación de navegaciones entre ventanas
        mnInicio.addMouseListener(navegar(() -> new Inicio()));
        mnPedidos.addMouseListener(navegar(() -> new pedidos()));
        mnUsuarios.addMouseListener(navegar(() -> new usuarios()));
        mnClientes.addMouseListener(navegar(() -> new Clientes()));
        mnInventario.addMouseListener(navegar(() -> new Inventario()));
        mnCerrarSesion.addMouseListener(navegar(() -> new Prueba()));
    }

    private JMenu menu(String texto) { 
        JMenu menu = new JMenu(texto); 
        menu.setFont(new Font("Segoe UI", Font.PLAIN, 12)); 
        return menu; 
    }
    
    private MouseAdapter navegar(java.util.function.Supplier<JFrame> destino) {
        return new MouseAdapter() { 
            @Override public void mouseClicked(MouseEvent e) { 
                destino.get().setVisible(true); 
                dispose(); 
            } 
        };
    }
    
    private JPanel tarjeta(int x, int y, int ancho, int alto, Color borde) { 
        JPanel panel = new JPanel(null); 
        panel.setBackground(Color.WHITE); 
        panel.setBorder(new LineBorder(borde, 1)); 
        panel.setBounds(x, y, ancho, alto); 
        return panel; 
    }
    
    private JLabel etiqueta(JPanel panel, String texto, Color color, int tamanio, int estilo, int x, int y, int ancho, int alto) { 
        JLabel etiqueta = new JLabel(texto); 
        etiqueta.setFont(new Font("Segoe UI", estilo, tamanio)); 
        etiqueta.setForeground(color); 
        etiqueta.setBounds(x, y, ancho, alto); 
        panel.add(etiqueta); 
        return etiqueta; 
    }
    
    private JLabel etiqueta(JPanel panel, String texto, Color color, int tamanio, int x, int y, int ancho, int alto) { 
        return etiqueta(panel, texto, color, tamanio, Font.BOLD, x, y, ancho, alto); 
    }
    
    private DefaultTableModel crearModelo(String[] columnas) { 
        return new DefaultTableModel(columnas, 0) { 
            private static final long serialVersionUID = 1L; 
            @Override public boolean isCellEditable(int fila, int columna) { return false; } 
        }; 
    }
    
    private JTable crearTabla(DefaultTableModel modelo) { 
        JTable tabla = new JTable(modelo); 
        tabla.setRowHeight(24); 
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 12)); 
        tabla.setGridColor(new Color(245, 245, 245)); 
        return tabla; 
    }
    
    private JButton boton(String texto, Color fondo, Color frente, int x, int y, int ancho, int alto, javax.swing.border.Border borde) { 
        JButton boton = new JButton(texto); 
        boton.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12)); 
        boton.setBackground(fondo); 
        boton.setForeground(frente); 
        boton.setBorder(borde); 
        boton.setFocusable(false); 
        boton.setBounds(x, y, ancho, alto); 
        return boton; 
    }

    // =================================================================
    // MÉTODOS GETTERS, SETTERS Y CONSULTA DE ESTADO
    // =================================================================
    public DefaultTableModel getModeloVentas() { return modeloVentas; }
    public DefaultTableModel getModeloPedidos() { return modeloPedidos; }
    public JTable getTablaVentas() { return tablaVentas; }
    public JTable getTablaPedidos() { return tablaPedidos; }
    public JButton getBtnActualizar() { return btnActualizar; }
    public JButton getBtnEliminarFila() { return btnEliminarFila; }
    public JButton getBtnVaciarTabla() { return btnVaciarTabla; }
    public JButton getBtnBorrarTodo() { return btnBorrarTodo; }

    /** Retorna true si la pestaña activa actualmente es la de Ventas (índice 0) */
    public boolean mostrandoVentas() { return pestanas.getSelectedIndex() == 0; }

    /** Retorna la fila seleccionada de la tabla que está visible en el momento */
    public int getFilaSeleccionada() { 
        return mostrandoVentas() ? tablaVentas.getSelectedRow() : tablaPedidos.getSelectedRow(); 
    }

    public void setMontoIngresos(String texto) { lblMontoIngresos.setText(texto); }

    public void setCantidadVentas(int cantidad) {
        huboErrorVentas = false;
        cantidadVentasMostrada = cantidad;
        lblCantVentas.setText(Idioma.get("reportes.textoCantidadVentas", cantidad));
    }

    public void setCantidadPedidos(int cantidad) {
        huboErrorPedidos = false;
        cantidadPedidosMostrada = cantidad;
        lblCantPedidos.setText(Idioma.get("reportes.textoCantidadPedidos", cantidad));
    }

    public void setErrorVentas() { 
        huboErrorVentas = true; 
        lblMontoIngresos.setText(Idioma.get("reportes.errorSql")); 
        lblCantVentas.setText(Idioma.get("reportes.errorSql")); 
    }

    public void setErrorPedidos() { 
        huboErrorPedidos = true; 
        lblCantPedidos.setText(Idioma.get("reportes.errorSql")); 
    }

    // =================================================================
    // MÉTODOS DE LA INTERFAZ 'ActualizableIdioma'
    // =================================================================
    @Override
    public void actualizarTextos() {
        setTitle(Idioma.get("reportes.tituloVentana"));
        mnInicio.setText(Idioma.get("menu.inicio"));
        mnPedidos.setText(Idioma.get("menu.pedidos"));
        mnUsuarios.setText(Idioma.get("menu.usuarios"));
        mnReportes.setText(Idioma.get("menu.reportes"));
        mnConfiguracion.setText(Idioma.get("menu.configuracion"));
        Idioma.construirSubmenuIdiomas(mnConfiguracion);
        mnInventario.setText(Idioma.get("menu.inventario"));
        mnCerrarSesion.setText(Idioma.get("menu.cerrarSesion"));

        lblTitulo.setText(Idioma.get("reportes.titulo"));
        lblDescripcion.setText(Idioma.get("reportes.subtitulo"));
        lblEtiquetaIngresos.setText(Idioma.get("reportes.tarjetaIngresos"));
        lblEtiquetaVentas.setText(Idioma.get("reportes.tarjetaVentas"));
        lblEtiquetaPedidos.setText(Idioma.get("reportes.tarjetaPedidos"));

        // Reconstruye las etiquetas KPI respetando si hubo un error o mostrando el valor actual
        if (huboErrorVentas) { 
            lblCantVentas.setText(Idioma.get("reportes.errorSql")); 
            lblMontoIngresos.setText(Idioma.get("reportes.errorSql")); 
        } else {
            lblCantVentas.setText(Idioma.get("reportes.textoCantidadVentas", cantidadVentasMostrada));
        }

        if (huboErrorPedidos) {
            lblCantPedidos.setText(Idioma.get("reportes.errorSql"));
        } else {
            lblCantPedidos.setText(Idioma.get("reportes.textoCantidadPedidos", cantidadPedidosMostrada));
        }

        // Actualiza los títulos de las pestañas
        pestanas.setTitleAt(0, Idioma.get("reportes.pestanaVentas"));
        pestanas.setTitleAt(1, Idioma.get("reportes.pestanaPedidos"));

        // Actualiza los textos de los botones
        btnActualizar.setText(Idioma.get("reportes.btnModificarPedido"));
        btnEliminarFila.setText(Idioma.get("reportes.btnEliminarFila"));
        btnVaciarTabla.setText(Idioma.get("reportes.btnVaciarTabla"));
        btnBorrarTodo.setText(Idioma.get("reportes.btnBorrarTodo"));

        // Actualiza las cabeceras de ambas tablas
        modeloVentas.setColumnIdentifiers(new Object[] { 
            Idioma.get("reportes.columnaIdVenta"), Idioma.get("reportes.columnaFecha"), 
            Idioma.get("reportes.columnaOrigen"), Idioma.get("reportes.columnaProductos"), 
            Idioma.get("reportes.columnaTotal") 
        });
        
        modeloPedidos.setColumnIdentifiers(new Object[] { 
            Idioma.get("reportes.columnaIdPedido"), Idioma.get("reportes.columnaFecha"), 
            Idioma.get("reportes.columnaCliente"), Idioma.get("reportes.columnaTelefono"), 
            Idioma.get("reportes.columnaProductos"), Idioma.get("reportes.columnaTotal"), 
            Idioma.get("reportes.columnaEstado") 
        });
    }
}