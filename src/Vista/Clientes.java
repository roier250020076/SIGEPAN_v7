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

import controladores.ControladorCliente;
import idioma.ActualizableIdioma;
import idioma.Idioma;

/**
 * Ventana "Clientes": lista, busca y da de baja a los CLIENTES de la panadería (los
 * datos de la tabla {@code Clientes} de la base de datos).
 * <p>
 * <b>IMPORTANTE - no confundir con la ventana {@link usuarios}:</b> esa otra ventana
 * (menú "Usuarios") administra las CUENTAS DE ACCESO al sistema (empleados que
 * inician sesión, tabla {@code Usuarios}). Esta ventana es distinta a propósito: los
 * clientes se registran solos la primera vez que se les hace un pedido (ver
 * {@code ControladorPedido}), y aquí solo se consultan, se buscan y, si hace falta, se
 * dan de baja. Por eso el diseño es casi idéntico al de {@code usuarios.java} (mismo
 * patrón de barra de menú, buscador y tabla de solo lectura) pero sin el botón de
 * "Registrar", ya que los clientes nuevos se capturan desde Pedidos, no desde aquí.
 */
public class Clientes extends JFrame implements ActualizableIdioma {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField txtBuscar;

	private JTable tablaClientes;
	private DefaultTableModel modelo;
	private JLabel lblTotal;
	private JButton btnBuscar;
	private JButton btnEliminar;

	private TableRowSorter<DefaultTableModel> sorter;
	// [MVC - VISTA]: Declaración del Controlador de Clientes (C del MVC).
	private ControladorCliente controlador;
	private int totalClientesMostrado = 0;

	// NUEVO (i18n): campos de menú y etiquetas que actualizarTextos() debe refrescar.
	private JMenu mnInicio, mnPedidos, mnUsuarios, mnClientes, mnReportes, mnConfiguracion, mnInventario, mnCerrarSesion;
	private JLabel lblTitulo, lblSubtitulo, lblBuscar;

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Clientes frame = new Clientes();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public Clientes() {

		// [MVC - VISTA]: Configuración visual de la ventana de gestión de clientes.
		setTitle(Idioma.get("clientes.tituloVentana"));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 740, 560);

		// Misma paleta de colores que el resto de la aplicación.
		Color colorFondo = EstiloUI.FONDO;      // Crema suave
		Color colorCafeOscuro = EstiloUI.TEXTO;    // Café espresso profundo
		Color colorCafeCalido = EstiloUI.ACENTO;   // Café dorado para botones secundarios
		Color colorBordeSuave = EstiloUI.BORDE;  // Tono arena sutil
		Color colorGrisTexto = EstiloUI.SECUNDARIO;  // Gris cálido elegante

		// BARRA DE MENÚ SUPERIOR UNIFICADA
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

		// [MVC - VISTA]: Manejadores de eventos de la barra de menú para la navegación rápida entre ventanas.
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
		mnUsuarios.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				new usuarios().setVisible(true);
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

		// PANEL PRINCIPAL
		contentPane = new JPanel();
		contentPane.setBackground(colorFondo);
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		// ENCABEZADO
		lblTitulo = new JLabel(Idioma.get("clientes.titulo"));
		lblTitulo.setFont(new Font("Segoe UI Semibold", Font.BOLD, 24));
		lblTitulo.setForeground(colorCafeOscuro);
		lblTitulo.setBounds(25, 20, 350, 30);
		contentPane.add(lblTitulo);

		lblSubtitulo = new JLabel(Idioma.get("clientes.subtitulo"));
		lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		lblSubtitulo.setForeground(colorGrisTexto);
		lblSubtitulo.setBounds(25, 50, 400, 20);
		contentPane.add(lblSubtitulo);

		// SECCIÓN BUSCADOR (Elegante y en línea)
		lblBuscar = new JLabel(Idioma.get("clientes.filtrarPor"));
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

		btnBuscar = new JButton(Idioma.get("clientes.btnBuscar"));
		btnBuscar.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		btnBuscar.setBackground(Color.WHITE);
		btnBuscar.setForeground(colorCafeCalido);
		btnBuscar.setBorder(new LineBorder(colorBordeSuave, 1));
		btnBuscar.setFocusable(false);
		btnBuscar.setBounds(615, 22, 80, 26);
		contentPane.add(btnBuscar);

		// CONTENEDOR BLANCO PARA LA TABLA (Tarjeta flotante)
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

		// Tabla de SOLO LECTURA, igual que usuarios/Inventario/Reportes: los cambios se
		// hacen con el botón "Eliminar", nunca editando una celda directamente.
		modelo = new DefaultTableModel() {
			private static final long serialVersionUID = 1L;
			@Override
			public boolean isCellEditable(int row, int column) { return false; }
		};
		modelo.addColumn(Idioma.get("clientes.columnaId"));
		modelo.addColumn(Idioma.get("clientes.columnaNombre"));
		modelo.addColumn(Idioma.get("clientes.columnaApellido"));
		modelo.addColumn(Idioma.get("clientes.columnaTelefono"));
		modelo.addColumn(Idioma.get("clientes.columnaDireccion"));
		modelo.addColumn(Idioma.get("clientes.columnaCorreo"));

		tablaClientes = new EstiloUI.Tabla(modelo);
		tablaClientes.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		tablaClientes.setRowHeight(24);
		tablaClientes.setGridColor(new Color(245, 245, 245));
		scrollPane.setViewportView(tablaClientes);

		// CONFIGURACIÓN DEL FILTRADO (busca en todas las columnas, igual que en Usuarios)
		sorter = new TableRowSorter<>(modelo);
		tablaClientes.setRowSorter(sorter);

		btnBuscar.addActionListener(e -> {
			String textoBusqueda = txtBuscar.getText().trim();
			if (textoBusqueda.isEmpty()) {
				sorter.setRowFilter(null);
			} else {
				sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(textoBusqueda)));
			}
		});

		// CONTADOR Y BOTÓN DE ACCIÓN (Zona Inferior)
		lblTotal = new JLabel(Idioma.get("clientes.contador", 0));
		lblTotal.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
		lblTotal.setForeground(colorCafeOscuro);
		lblTotal.setBounds(25, 482, 350, 25);
		contentPane.add(lblTotal);

		// NOTA: a propósito NO hay botón "Registrar" aquí (a diferencia de usuarios.java):
		// los clientes se registran solos la primera vez que se les hace un pedido (ver
		// ControladorPedido); esta ventana es para consultarlos, buscarlos y darlos de baja.
		btnEliminar = new JButton(Idioma.get("clientes.btnEliminar"));
		btnEliminar.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
		btnEliminar.setBackground(Color.WHITE);
		btnEliminar.setForeground(new Color(180, 50, 50));
		btnEliminar.setBorder(new LineBorder(new Color(230, 200, 200), 1));
		btnEliminar.setFocusable(false);
		btnEliminar.setBounds(485, 482, 210, 32);
		contentPane.add(btnEliminar);

		// =====================================
		// CONTROLADOR Y EVENTOS
		// =====================================
		// [MVC - CONTROLADOR]: Instanciación del Controlador de Clientes inyectando la vista actual ('this').
		controlador = new ControladorCliente(this);

		// NUEVO: ventana autoajustable (SIGEPAN v6), ver Vista.Redimensionable.
		EstiloUI.aplicar(this, mnClientes);
		Redimensionable.activar(this, contentPane, 740, 560);
	}

	// [MVC - VISTA]: Métodos Getter públicos para que el ControladorCliente pueda manipular los componentes, la tabla y los eventos.
	public DefaultTableModel getModelo() { return modelo; }
	public JTable getTablaClientes() { return tablaClientes; }
	public JLabel getLblTotal() { return lblTotal; }
	public JTextField getTxtBuscar() { return txtBuscar; }
	public JButton getBtnBuscar() { return btnBuscar; }
	public JButton getBtnEliminar() { return btnEliminar; }
	/** Guarda el total mostrado para poder re-traducir "Clientes registrados: N" si cambia el idioma. */
	public void setTotalClientesMostrado(int total) {
		this.totalClientesMostrado = total;
		lblTotal.setText(Idioma.get("clientes.contador", total));
	}

	/** Refresca todos los textos visibles de esta ventana en el idioma actual. */
	@Override
	public void actualizarTextos() {
		setTitle(Idioma.get("clientes.tituloVentana"));
		mnInicio.setText(Idioma.get("menu.inicio"));
		mnPedidos.setText(Idioma.get("menu.pedidos"));
		mnUsuarios.setText(Idioma.get("menu.usuarios"));
		mnClientes.setText(Idioma.get("menu.clientes"));
		mnReportes.setText(Idioma.get("menu.reportes"));
		mnConfiguracion.setText(Idioma.get("menu.configuracion"));
		Idioma.construirSubmenuIdiomas(mnConfiguracion);
		mnInventario.setText(Idioma.get("menu.inventario"));
		mnCerrarSesion.setText(Idioma.get("menu.cerrarSesion"));

		lblTitulo.setText(Idioma.get("clientes.titulo"));
		lblSubtitulo.setText(Idioma.get("clientes.subtitulo"));
		lblBuscar.setText(Idioma.get("clientes.filtrarPor"));
		btnBuscar.setText(Idioma.get("clientes.btnBuscar"));
		lblTotal.setText(Idioma.get("clientes.contador", totalClientesMostrado));
		btnEliminar.setText(Idioma.get("clientes.btnEliminar"));

		modelo.setColumnIdentifiers(new Object[] {
			Idioma.get("clientes.columnaId"), Idioma.get("clientes.columnaNombre"), Idioma.get("clientes.columnaApellido"),
			Idioma.get("clientes.columnaTelefono"), Idioma.get("clientes.columnaDireccion"), Idioma.get("clientes.columnaCorreo")
		});
	}
}