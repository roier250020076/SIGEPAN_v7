package Vista;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter; 
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter; 

import controladores.ControladorUsuario;
import idioma.ActualizableIdioma;
import idioma.Idioma;

public class usuarios extends JFrame implements ActualizableIdioma {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField txtBuscar;
	
	// COMPONENTES DE TABLA Y CONTROLES
	private JTable tablaUsuarios;
	private DefaultTableModel modelo;
	private JLabel lblTotal;
	private JButton btnBuscar;
	private JButton btnEliminar;
	private JButton btnRegistrar; // Botón para abrir el formulario de alta de empleados
	
	// FILTRADO Y CONTROLADOR
	private TableRowSorter<DefaultTableModel> sorter;
	private ControladorUsuario controlador;
	private int totalUsuariosMostrado = 0;

	// ELEMENTOS DEL MENÚ Y ETIQUETAS (Atributos para soporte i18n)
	private JMenu mnInicio, mnPedidos, mnUsuarios, mnClientes, mnReportes, mnConfiguracion, mnInventario, mnCerrarSesion;
	private JLabel lblTitulo, lblSubtitulo, lblBuscar;

	/**
	 * Método main para probar la vista de forma independiente.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					usuarios frame = new usuarios();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * CONSTRUCTOR: Configura la interfaz gráfica, menús, eventos y controladores.
	 */
	public usuarios() {

		setTitle(Idioma.get("usuarios.tituloVentana"));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 740, 560); 

		// PALETA DE COLORES PERSONALIZADA (Temática Panadería)
		Color colorFondo = EstiloUI.FONDO;      // Crema suave
		Color colorCafeOscuro = EstiloUI.TEXTO;    // Café espresso profundo
		Color colorCafeCalido = EstiloUI.ACENTO;   // Café dorado para botones
		Color colorBordeSuave = EstiloUI.BORDE;  // Tono arena sutil
		Color colorGrisTexto = EstiloUI.SECUNDARIO;  // Gris cálido elegante

		// =================================================================
		// BARRA DE MENÚ SUPERIOR UNIFICADA
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
		mnConfiguracion = new JMenu(Idioma.get("menu.configuracion"));
		Idioma.construirSubmenuIdiomas(mnConfiguracion);
		mnInventario = new JMenu(Idioma.get("menu.inventario")); 
		mnCerrarSesion = new JMenu(Idioma.get("menu.cerrarSesion"));
		
		mnCerrarSesion.setForeground(new Color(180, 50, 50));
		Font fontMenu = new Font("Segoe UI", Font.PLAIN, 12);
		
		JMenu[] menus = {mnInicio, mnPedidos, mnUsuarios, mnClientes, mnReportes, mnConfiguracion, mnInventario, mnCerrarSesion};
		for (JMenu m : menus) { m.setFont(fontMenu); }

		menuBar.add(mnInicio);
		menuBar.add(mnPedidos);
		menuBar.add(mnUsuarios);
		menuBar.add(mnClientes);
		menuBar.add(mnReportes);
		menuBar.add(mnConfiguracion);
		menuBar.add(mnInventario);    
		menuBar.add(mnCerrarSesion); 

		// EVENTOS DE NAVEGACIÓN ENTRE VENTANAS
		mnInicio.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				new Inicio().setVisible(true);
				dispose();
			}
		});
		mnPedidos.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				new pedidos().setVisible(true);
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
				new Prueba().setVisible(true);
				dispose();
			}
		});

		// =================================================================
		// PANEL PRINCIPAL Y ENCABEZADOS
		// =================================================================
		contentPane = new JPanel();
		contentPane.setBackground(colorFondo);
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		lblTitulo = new JLabel(Idioma.get("usuarios.titulo"));
		lblTitulo.setFont(new Font("Segoe UI Semibold", Font.BOLD, 24));
		lblTitulo.setForeground(colorCafeOscuro);
		lblTitulo.setBounds(25, 20, 350, 30);
		contentPane.add(lblTitulo);

		lblSubtitulo = new JLabel(Idioma.get("usuarios.subtitulo"));
		lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		lblSubtitulo.setForeground(colorGrisTexto);
		lblSubtitulo.setBounds(25, 50, 350, 20);
		contentPane.add(lblSubtitulo);

		// SECCIÓN BUSCADOR
		lblBuscar = new JLabel(Idioma.get("usuarios.filtrarPor"));
		lblBuscar.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblBuscar.setForeground(colorCafeOscuro);
		lblBuscar.setBounds(345, 25, 120, 20);
		contentPane.add(lblBuscar);

		txtBuscar = new JTextField();
		txtBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		txtBuscar.setBorder(new LineBorder(colorBordeSuave, 1));
		txtBuscar.setBounds(465, 22, 140, 26);
		contentPane.add(txtBuscar);
		txtBuscar.setColumns(10);

		btnBuscar = new JButton(Idioma.get("usuarios.btnBuscar"));
		btnBuscar.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		btnBuscar.setBackground(Color.WHITE);
		btnBuscar.setForeground(colorCafeCalido);
		btnBuscar.setBorder(new LineBorder(colorBordeSuave, 1));
		btnBuscar.setFocusable(false);
		btnBuscar.setBounds(615, 22, 80, 26);
		contentPane.add(btnBuscar);

		// =================================================================
		// PANEL DE LA TABLA Y CONFIGURACIÓN DE SOLO LECTURA
		// =================================================================
		JPanel panelTabla = new EstiloUI.Tarjeta();
		panelTabla.setBackground(Color.WHITE);
		panelTabla.setBorder(new LineBorder(colorBordeSuave, 1));
		panelTabla.setBounds(25, 95, 670, 375);
		panelTabla.setLayout(null);
		contentPane.add(panelTabla);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(15, 15, 640, 345);
		scrollPane.getViewport().setBackground(Color.WHITE);
		scrollPane.setBorder(new LineBorder(new Color(240, 240, 240), 1));
		panelTabla.add(scrollPane);

		// Sobrescribe isCellEditable para evitar la edición directa de celdas
		modelo = new DefaultTableModel() {
			private static final long serialVersionUID = 1L;
			@Override
			public boolean isCellEditable(int row, int column) { return false; }
		};
		// Definición de columnas
		modelo.addColumn(Idioma.get("usuarios.columnaNombre"));
		modelo.addColumn(Idioma.get("usuarios.columnaApellido"));
		modelo.addColumn(Idioma.get("usuarios.columnaDireccion"));
		modelo.addColumn(Idioma.get("usuarios.columnaTelefono"));
		modelo.addColumn(Idioma.get("usuarios.columnaCorreo"));
		modelo.addColumn(Idioma.get("usuarios.columnaFechaNacimiento"));
		modelo.addColumn(Idioma.get("usuarios.columnaUsuario"));
		modelo.addColumn(Idioma.get("usuarios.columnaRol"));

		tablaUsuarios = new EstiloUI.Tabla(modelo);
		tablaUsuarios.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		tablaUsuarios.setRowHeight(24);
		tablaUsuarios.setGridColor(new Color(245, 245, 245));
		scrollPane.setViewportView(tablaUsuarios);

		// CONFIGURACIÓN DEL FILTRADO DINÁMICO
		sorter = new TableRowSorter<>(modelo);
		tablaUsuarios.setRowSorter(sorter);

		// Evento de búsqueda por coincidencias
		btnBuscar.addActionListener(e -> {
			String textoBusqueda = txtBuscar.getText().trim();
			if (textoBusqueda.isEmpty()) {
				sorter.setRowFilter(null);
			} else {
				// Aplica expresión regular insensible a mayúsculas/minúsculas
				sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(textoBusqueda), 0));
			}
		});

		// =================================================================
		// SECCIÓN INFERIOR: CONTADOR Y BOTONES DE ACCIÓN
		// =================================================================
		lblTotal = new JLabel(Idioma.get("usuarios.contador", 0));
		lblTotal.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
		lblTotal.setForeground(colorCafeOscuro);
		lblTotal.setBounds(25, 482, 250, 25);
		contentPane.add(lblTotal);
		
		btnRegistrar = new JButton(Idioma.get("usuarios.btnRegistrar"));
		btnRegistrar.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
		btnRegistrar.setBackground(colorCafeCalido);
		btnRegistrar.setForeground(Color.WHITE);
		btnRegistrar.setBorder(null);
		btnRegistrar.setFocusable(false);
		btnRegistrar.setBounds(285, 482, 190, 32);
		contentPane.add(btnRegistrar);

		btnEliminar = new JButton(Idioma.get("usuarios.btnEliminar"));
		btnEliminar.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
		btnEliminar.setBackground(Color.WHITE);
		btnEliminar.setForeground(new Color(180, 50, 50));
		btnEliminar.setBorder(new LineBorder(new Color(230, 200, 200), 1));
		btnEliminar.setFocusable(false);
		btnEliminar.setBounds(485, 482, 210, 32);
		contentPane.add(btnEliminar);
		
		// Inicializa el controlador encargado de llenar los datos en la tabla
		controlador = new ControladorUsuario(this);

		// Conecta el botón para abrir el formulario de registro de empleado
		btnRegistrar.addActionListener(e -> controlador.abrirRegistroDesdeUsuarios());

		// Activa el redimensionamiento dinámico responsive de la interfaz
		EstiloUI.aplicar(this, mnUsuarios);
		Redimensionable.activar(this, contentPane, 740, 560);
	}

	// =================================================================
	// MÉTODOS GETTERS Y SETTERS
	// =================================================================
	public DefaultTableModel getModelo() { return modelo; }
	public JTable getTablaUsuarios() { return tablaUsuarios; }
	public JLabel getLblTotal() { return lblTotal; }
	public JTextField getTxtBuscar() { return txtBuscar; }
	public JButton getBtnBuscar() { return btnBuscar; }
	public JButton getBtnEliminar() { return btnEliminar; }
	public JButton getBtnRegistrar() { return btnRegistrar; }

	/**
	 * Actualiza el total guardado y refresca el texto del contador en pantalla.
	 */
	public void setTotalUsuariosMostrado(int total) {
		this.totalUsuariosMostrado = total;
		lblTotal.setText(Idioma.get("usuarios.contador", total));
	}

	/**
	 * MÉTODOS DE LA INTERFAZ 'ActualizableIdioma'
	 * Traduce todos los textos del menú, tabla y botones si se cambia de idioma.
	 */
	@Override
	public void actualizarTextos() {
		setTitle(Idioma.get("usuarios.tituloVentana"));
		mnInicio.setText(Idioma.get("menu.inicio"));
		mnPedidos.setText(Idioma.get("menu.pedidos"));
		mnUsuarios.setText(Idioma.get("menu.usuarios"));
		mnClientes.setText(Idioma.get("menu.clientes"));
		mnReportes.setText(Idioma.get("menu.reportes"));
		mnConfiguracion.setText(Idioma.get("menu.configuracion"));
		Idioma.construirSubmenuIdiomas(mnConfiguracion);
		mnInventario.setText(Idioma.get("menu.inventario"));
		mnCerrarSesion.setText(Idioma.get("menu.cerrarSesion"));

		lblTitulo.setText(Idioma.get("usuarios.titulo"));
		lblSubtitulo.setText(Idioma.get("usuarios.subtitulo"));
		lblBuscar.setText(Idioma.get("usuarios.filtrarPor"));
		btnBuscar.setText(Idioma.get("usuarios.btnBuscar"));
		lblTotal.setText(Idioma.get("usuarios.contador", totalUsuariosMostrado));
		btnRegistrar.setText(Idioma.get("usuarios.btnRegistrar"));
		btnEliminar.setText(Idioma.get("usuarios.btnEliminar"));

		// Refresca los encabezados traducidos de la tabla
		modelo.setColumnIdentifiers(new Object[] {
			Idioma.get("usuarios.columnaNombre"), Idioma.get("usuarios.columnaApellido"),
			Idioma.get("usuarios.columnaDireccion"), Idioma.get("usuarios.columnaTelefono"),
			Idioma.get("usuarios.columnaCorreo"), Idioma.get("usuarios.columnaFechaNacimiento"),
			Idioma.get("usuarios.columnaUsuario"), Idioma.get("usuarios.columnaRol")
		});
	}
}