package Vista;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

import controladores.ControladorPedido;
import idioma.ActualizableIdioma;
import idioma.Idioma;
import modelo.DatosGlobales;

public class Inicio extends JFrame implements ActualizableIdioma {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	
	// Componentes gráficos globales accesibles para el controlador
	private JTable tablaProductos;
	private JTable tablaCarrito;
	private JLabel lblCantidad;
	private JButton btnLimpiar;
	private JButton btnProcesar;
	private JButton btnPagarPedido;

	// NUEVO: se vuelven campos de la clase para poder refrescar sus textos cuando
	// el usuario cambia de idioma desde el menú Configuración (ver actualizarTextos()).
	private JMenu mnInicio, mnPedidos, mnUsuarios, mnClientes, mnReportes, mnConfiguracion, mnInventario, mnCerrarSesion;
	private JLabel lblTitulo, lblDescripcion, lblProductos, lblCarrito, lblTotal;
	private DefaultTableModel modeloProductos;
	
	// [MVC - VISTA]: Enlace directo al Controlador de Pedidos (C del MVC).
	private ControladorPedido controlador;

	/**
	 * Método Main: Hilo de ejecución que arranca de forma asíncrona la interfaz
	 * mediante EventQueue.invokeLater para asegurar la estabilidad visual de Swing.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Inicio frame = new Inicio();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Constructor de la ventana: Inicializa y da formato a todos los componentes visuales.
	 */
	public Inicio() {
		try {
			DatosGlobales.recargarProductos();
		} catch (java.sql.SQLException e) {
			// Se conserva el catalogo temporal si SQL Server no esta disponible.
		}
		// [MVC - VISTA]: Configuración visual de la ventana principal y textos traducibles.
		setTitle(Idioma.get("inicio.titulo"));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 720, 590); // Dimensiones de la ventana principal

		// PALETA DE COLORES ARTESANAL (Identidad Corporativa de la Panadería)
		Color colorFondo = new Color(253, 251, 247);      // Crema suave de fondo
		Color colorCafeOscuro = new Color(74, 53, 37);    // Café espresso para textos y títulos
		Color colorCafeCalido = new Color(139, 94, 60);   // Café cálido para botones principales
		Color colorBordeSuave = new Color(222, 212, 198);  // Tono arena sutil para contenedores
		Color colorGrisTexto = new Color(110, 100, 90);   // Gris cálido para subtítulos

		// =================================================================
		// MENÚ SUPERIOR ESTILIZADO Y NAVEGACIÓN
		// =================================================================
		JMenuBar menuBar = new JMenuBar();
		menuBar.setBackground(Color.WHITE);
		menuBar.setBorder(new LineBorder(colorBordeSuave, 1));
		setJMenuBar(menuBar);

		mnInicio = new JMenu(Idioma.get("menu.inicio"));
		mnPedidos = new JMenu(Idioma.get("menu.pedidos"));
		mnUsuarios = new JMenu(Idioma.get("menu.usuarios"));
		mnClientes = new JMenu(Idioma.get("menu.clientes"));
		mnReportes = new JMenu(Idioma.get("menu.reportes"));
		// NUEVO: "Configuración" ahora es un submenú real con las 3 opciones de idioma
		// (antes era un JMenu decorativo sin ningún evento conectado).
		mnConfiguracion = new JMenu(Idioma.get("menu.configuracion"));
		Idioma.construirSubmenuIdiomas(mnConfiguracion);
		mnInventario = new JMenu(Idioma.get("menu.inventario"));
		mnCerrarSesion = new JMenu(Idioma.get("menu.cerrarSesion"));
		
		mnCerrarSesion.setForeground(new Color(180, 50, 50)); // Resalte en rojo para salida
		Font fontMenu = new Font("Segoe UI", Font.PLAIN, 12);
		
		// Bucle iterativo para homogeneizar la tipografía de todos los menús
		JMenu[] menus = {mnInicio, mnPedidos, mnUsuarios, mnClientes, mnReportes, mnConfiguracion, mnInventario, mnCerrarSesion};
		for (JMenu m : menus) {
			m.setFont(fontMenu);
		}

		menuBar.add(mnInicio);
		menuBar.add(mnPedidos);
		menuBar.add(mnUsuarios);
		menuBar.add(mnClientes);
		menuBar.add(mnReportes);
		menuBar.add(mnConfiguracion);
		menuBar.add(mnInventario);
		menuBar.add(mnCerrarSesion); 
		
		// [MVC - VISTA]: Manejadores de eventos de la barra de menú para la navegación entre distintas ventanas.
		mnPedidos.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				new pedidos().setVisible(true);
				dispose(); // Libera los recursos de la ventana actual de la memoria RAM
			}
		});

		mnUsuarios.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				new usuarios().setVisible(true);
				dispose();
			}
		});

		mnClientes.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				new Clientes().setVisible(true);
				dispose();
			}
		});

		mnReportes.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				new reportes().setVisible(true);
				dispose();
			}
		});
		
		mnInventario.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				new Inventario().setVisible(true);
				dispose();
			}
		});
		
		mnCerrarSesion.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				new Prueba().setVisible(true); // Redirige al Login (Prueba)
				dispose();
			}
		});

		// =================================================================
		// CONTENEDOR PRINCIPAL Y ENCABEZADOS
		// =================================================================
		contentPane = new JPanel();
		contentPane.setBackground(colorFondo);
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null); // Desactivación del Layout Manager para posicionamiento absoluto (setBounds)

		lblTitulo = new JLabel(Idioma.get("inicio.encabezado"));
		lblTitulo.setFont(new Font("Segoe UI Semibold", Font.BOLD, 26));
		lblTitulo.setForeground(colorCafeOscuro);
		lblTitulo.setBounds(25, 30, 250, 35);
		contentPane.add(lblTitulo);

		lblDescripcion = new JLabel(Idioma.get("inicio.subtitulo"));
		lblDescripcion.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		lblDescripcion.setForeground(colorGrisTexto);
		lblDescripcion.setBounds(25, 65, 250, 20);
		contentPane.add(lblDescripcion);

		// =================================================================
		// PANEL IZQUIERDO: PRODUCTOS DISPONIBLES (CATÁLOGO MAESTRO)
		// =================================================================
		JPanel panelProductos = new JPanel();
		panelProductos.setBackground(Color.WHITE);
		panelProductos.setBorder(new LineBorder(colorBordeSuave, 1));
		panelProductos.setBounds(25, 120, 310, 290);
		panelProductos.setLayout(null);
		contentPane.add(panelProductos);

		lblProductos = new JLabel(Idioma.get("inicio.catalogo"));
		lblProductos.setFont(new Font("Segoe UI Semibold", Font.BOLD, 15));
		lblProductos.setForeground(colorCafeOscuro);
		lblProductos.setBounds(15, 12, 200, 22);
		panelProductos.add(lblProductos);

		JScrollPane scrollPaneProductos = new JScrollPane();
		scrollPaneProductos.setBounds(15, 45, 280, 230);
		scrollPaneProductos.getViewport().setBackground(Color.WHITE);
		scrollPaneProductos.setBorder(new LineBorder(new Color(240, 240, 240), 1));
		panelProductos.add(scrollPaneProductos);
		
		//tabla productos
		tablaProductos = new JTable();
		tablaProductos.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		tablaProductos.setRowHeight(22);
		tablaProductos.setGridColor(new Color(245, 245, 245));
		scrollPaneProductos.setViewportView(tablaProductos);
		
		modeloProductos = new DefaultTableModel(new Object[][] {}, new String[] {Idioma.get("inicio.columnaProducto"), Idioma.get("inicio.columnaPrecio")}) {
			private static final long serialVersionUID = 1L;
			@Override
			public boolean isCellEditable(int row, int column) { return false; }
		};
		tablaProductos.setModel(modeloProductos);

		// ALGORITMO DE CARGA AUTOMÁTICA: Vuelca el catálogo maestro de DatosGlobales en la JTable
		for (Object[] prod : DatosGlobales.listaProductos) {
			modeloProductos.addRow(prod);
		}

		// =================================================================
		// PANEL DERECHO: CARRITO DE COMPRA (VENTA ACTUAL)
		// =================================================================
		JPanel panelCarrito = new JPanel();
		panelCarrito.setBackground(Color.WHITE);
		panelCarrito.setBorder(new LineBorder(colorBordeSuave, 1));
		panelCarrito.setBounds(360, 120, 320, 290);
		panelCarrito.setLayout(null);
		contentPane.add(panelCarrito);

		lblCarrito = new JLabel(Idioma.get("inicio.carrito"));
		lblCarrito.setFont(new Font("Segoe UI Semibold", Font.BOLD, 15));
		lblCarrito.setForeground(colorCafeOscuro);
		lblCarrito.setBounds(15, 12, 180, 22);
		panelCarrito.add(lblCarrito);

		JScrollPane scrollPaneCarrito = new JScrollPane();
		scrollPaneCarrito.setBounds(15, 45, 290, 230);
		scrollPaneCarrito.getViewport().setBackground(Color.WHITE);
		scrollPaneCarrito.setBorder(new LineBorder(new Color(240, 240, 240), 1));
		panelCarrito.add(scrollPaneCarrito);

		// NOTA: Esta JTable no tiene modelo asignado aquí, su estructura se la inyecta el controlador dinámicamente
		tablaCarrito = new JTable();
		tablaCarrito.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		tablaCarrito.setRowHeight(22);
		tablaCarrito.setGridColor(new Color(245, 245, 245));
		scrollPaneCarrito.setViewportView(tablaCarrito);

		// =================================================================
		// PANEL INFERIOR: TOTALIZADOR Y ACCIONES DE COBRO
		// =================================================================
		JPanel panelTotal = new JPanel();
		panelTotal.setBackground(Color.WHITE);
		panelTotal.setBorder(new LineBorder(colorBordeSuave, 1));
		panelTotal.setBounds(25, 430, 655, 75);
		panelTotal.setLayout(null);
		contentPane.add(panelTotal);

		lblTotal = new JLabel(Idioma.get("inicio.total"));
		lblTotal.setFont(new Font("Segoe UI Semibold", Font.BOLD, 15));
		lblTotal.setForeground(colorGrisTexto);
		lblTotal.setBounds(20, 22, 150, 30);
		panelTotal.add(lblTotal);

		lblCantidad = new JLabel(Idioma.get("inicio.totalInicial"));
		lblCantidad.setFont(new Font("Segoe UI Black", Font.BOLD, 26));
		lblCantidad.setForeground(colorCafeOscuro);
		lblCantidad.setBounds(150, 18, 160, 35);
		panelTotal.add(lblCantidad);

		// BOTÓN LIMPIAR (Diseño secundario minimalista)
		btnLimpiar = new JButton(Idioma.get("inicio.btnLimpiar"));
		btnLimpiar.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
		btnLimpiar.setBackground(Color.WHITE);
		btnLimpiar.setForeground(colorCafeOscuro);
		btnLimpiar.setBorder(new LineBorder(colorBordeSuave, 1));
		btnLimpiar.setFocusable(false);
		btnLimpiar.setBounds(310, 22, 90, 32);
		panelTotal.add(btnLimpiar);

		// Permite cobrar un pedido especial previamente registrado.
		btnPagarPedido = new JButton(Idioma.get("inicio.btnPagarPedido"));
		btnPagarPedido.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
		btnPagarPedido.setBackground(Color.WHITE);
		btnPagarPedido.setForeground(colorCafeOscuro);
		btnPagarPedido.setBorder(new LineBorder(colorBordeSuave, 1));
		btnPagarPedido.setFocusable(false);
		btnPagarPedido.setBounds(410, 22, 110, 32);
		panelTotal.add(btnPagarPedido);

		// BOTÓN PROCESAR VENTA (Diseño primario destacado de alto contraste)
		btnProcesar = new JButton(Idioma.get("inicio.btnProcesar"));
		btnProcesar.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
		btnProcesar.setBackground(colorCafeCalido);
		btnProcesar.setForeground(Color.WHITE);
		btnProcesar.setBorder(null);
		btnProcesar.setFocusable(false);
		btnProcesar.setBounds(530, 22, 110, 32);
		panelTotal.add(btnProcesar);
		
		// CARGA DE ELEMENTOS GRÁFICOS COMPLEMENTARIOS (Logotipo)
		JLabel lblLogo = new JLabel("");
		lblLogo.setIcon(new ImageIcon(Inicio.class.getResource("/imagenes/logo.png")));
		lblLogo.setBounds(430, 0, 260, 120);
		contentPane.add(lblLogo);

		// =====================================
		// CONTROLADOR Y EVENTOS
		// =====================================
		// [MVC - CONTROLADOR]: Instanciación del Controlador de Pedidos inyectando la vista actual ('this').
		controlador = new ControladorPedido(this);

		// NUEVO: ventana autoajustable (SIGEPAN v6), ver Vista.Redimensionable.
		Redimensionable.activar(this, contentPane, 720, 590);
	}

	// [MVC - VISTA]: Métodos Getter públicos para que el ControladorPedido pueda manipular los componentes y gestionar los eventos de la tabla y botones.
	public JTable getTablaProductos() { return tablaProductos; }
	public JTable getTablaCarrito() { return tablaCarrito; }
	public JLabel getLblCantidad() { return lblCantidad; }
	public JButton getBtnLimpiar() { return btnLimpiar; }
	public JButton getBtnProcesar() { return btnProcesar; }
	public JButton getBtnPagarPedido() { return btnPagarPedido; }

	/**
	 * Refresca todos los textos visibles de esta ventana (menú, títulos, botones y
	 * encabezados de la tabla de catálogo) en el idioma actualmente seleccionado.
	 * El carrito (tabla derecha) lo maneja el controlador porque su modelo vive ahí.
	 */
	@Override
	public void actualizarTextos() {
		setTitle(Idioma.get("inicio.titulo"));
		mnInicio.setText(Idioma.get("menu.inicio"));
		mnPedidos.setText(Idioma.get("menu.pedidos"));
		mnUsuarios.setText(Idioma.get("menu.usuarios"));
		mnClientes.setText(Idioma.get("menu.clientes"));
		mnReportes.setText(Idioma.get("menu.reportes"));
		mnConfiguracion.setText(Idioma.get("menu.configuracion"));
		Idioma.construirSubmenuIdiomas(mnConfiguracion);
		mnInventario.setText(Idioma.get("menu.inventario"));
		mnCerrarSesion.setText(Idioma.get("menu.cerrarSesion"));

		lblTitulo.setText(Idioma.get("inicio.encabezado"));
		lblDescripcion.setText(Idioma.get("inicio.subtitulo"));
		lblProductos.setText(Idioma.get("inicio.catalogo"));
		lblCarrito.setText(Idioma.get("inicio.carrito"));
		lblTotal.setText(Idioma.get("inicio.total"));
		btnLimpiar.setText(Idioma.get("inicio.btnLimpiar"));
		btnPagarPedido.setText(Idioma.get("inicio.btnPagarPedido"));
		btnProcesar.setText(Idioma.get("inicio.btnProcesar"));

		modeloProductos.setColumnIdentifiers(new Object[] { Idioma.get("inicio.columnaProducto"), Idioma.get("inicio.columnaPrecio") });
		if (controlador != null) {
			controlador.actualizarTextosCarrito();
		}
	}
}