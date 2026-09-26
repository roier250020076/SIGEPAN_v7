package Vista;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

import controladores.ControladorInventario;
import controladores.Validador;
import idioma.ActualizableIdioma;
import idioma.Idioma;

public class Inventario extends JFrame implements ActualizableIdioma {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	
	// [MVC - VISTA]: Declaración del Controlador de Inventario (C del MVC).
	private ControladorInventario controlador;
	
	private JTable tablaExistentes;
	private DefaultTableModel modeloExistentes;
	
	private JTextField txtNombrePan;
	private JTextField txtPrecioPan;
	private JComboBox<String> cboCategoria;
	private JTextField txtDescripcion;
	private JTextField txtCodigo;
	private JSpinner spStockActual;
	private JSpinner spStockMinimo;
	private JButton btnAnadir;
	private JButton btnCancelarEdicion;
	private JButton btnEliminarProducto;
	private JButton btnModificarProducto;
	private JLabel lblNuevo;

	// NUEVO (i18n): campos de menú y etiquetas que actualizarTextos() debe refrescar.
	private JMenu mnInicio, mnPedidos, mnUsuarios, mnClientes, mnReportes, mnConfiguracion, mnInventario, mnCerrarSesion;
	private JLabel lblTitulo, lblExistentes, lblNombre, lblCategoria, lblDescripcion, lblCodigo, lblPrecio;
	private JLabel lblStockActual, lblStockMinimo, lblNota;
	// Igual que en pedidos.java: recuerda si se está editando, para reconstruir el
	// texto correcto de lblNuevo/btnAnadir tras un cambio de idioma.
	private boolean modoEdicionActivo = false;
	private String nombreProductoEnEdicion = null;

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Inventario frame = new Inventario();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public Inventario() {
		// [MVC - VISTA]: Configuración visual de la ventana de gestión de inventario.
		setTitle(Idioma.get("inventario.tituloVentana"));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1050, 680);

		// Paleta de colores unificada (Identidad Visual de la Panadería)
		Color colorFondo = EstiloUI.FONDO;      // Crema suave
		Color colorCafeOscuro = EstiloUI.TEXTO;    // Café espresso profundo
		Color colorCafeCalido = EstiloUI.ACENTO;   // Café dorado
		Color colorBordeSuave = EstiloUI.BORDE;  // Tono arena sutil
		Color colorGrisTexto = EstiloUI.SECUNDARIO;  // Gris cálido elegante

		// BARRA DE MENÚ SUPERIOR
		JMenuBar menuBar = new JMenuBar();
		menuBar.setBackground(Color.WHITE);
		menuBar.setBorder(new LineBorder(colorBordeSuave, 1));
		setJMenuBar(menuBar);

		mnInicio = new JMenu(Idioma.get("menu.inicio"));
		mnPedidos = new JMenu(Idioma.get("menu.pedidos"));
		mnUsuarios = new JMenu(Idioma.get("menu.usuarios"));
		mnClientes = new JMenu(Idioma.get("menu.clientes"));
		mnReportes = new JMenu(Idioma.get("menu.reportes"));
		// NUEVO: esta ventana no tenía el menú "Configuración" (inconsistencia con las
		// otras 4 ventanas); se agrega aquí también para que el idioma se pueda cambiar
		// desde cualquier pantalla de la aplicación.
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
		mnCerrarSesion.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				new Prueba().setVisible(true);
				dispose();
			}
		});

		// PANEL CONTENEDOR PRINCIPAL
		contentPane = new JPanel();
		contentPane.setBackground(colorFondo);
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		// ENCABEZADO
		lblTitulo = new JLabel(Idioma.get("inventario.titulo"));
		lblTitulo.setFont(new Font("Segoe UI Semibold", Font.BOLD, 24));
		lblTitulo.setForeground(colorCafeOscuro);
		lblTitulo.setBounds(25, 25, 400, 35);
		contentPane.add(lblTitulo);

		// -----------------------------------------------------------------
		// BLOQUE IZQUIERDO: PRODUCTOS REGISTRADOS
		// -----------------------------------------------------------------
		lblExistentes = new JLabel(Idioma.get("inventario.catalogo"));
		lblExistentes.setFont(new Font("Segoe UI Semibold", Font.BOLD, 14));
		lblExistentes.setForeground(colorCafeOscuro);
		lblExistentes.setBounds(25, 75, 200, 20);
		contentPane.add(lblExistentes);

		JPanel panelTabla = new EstiloUI.Tarjeta();
		panelTabla.setBackground(Color.WHITE);
		panelTabla.setBorder(new LineBorder(colorBordeSuave, 1));
		panelTabla.setBounds(25, 105, 480, 470);
		panelTabla.setLayout(null);
		contentPane.add(panelTabla);

		JScrollPane scrollIzquierdo = new JScrollPane();
		scrollIzquierdo.setBounds(15, 15, 450, 440);
		scrollIzquierdo.getViewport().setBackground(Color.WHITE);
		scrollIzquierdo.setBorder(new LineBorder(new Color(240, 240, 240), 1));
		panelTabla.add(scrollIzquierdo);

		// NUEVAS COLUMNAS: Categoria y Stock, para que el catalogo refleje toda la
		// informacion que ahora captura el formulario (antes solo mostraba Producto y Precio).
		modeloExistentes = new DefaultTableModel(new Object[][] {}, new String[] {
				Idioma.get("inventario.columnaProducto"), Idioma.get("inventario.columnaCategoria"),
				Idioma.get("inventario.columnaPrecio"), Idioma.get("inventario.columnaStock")}) {
			private static final long serialVersionUID = 1L;
			@Override
			public boolean isCellEditable(int row, int column) { return false; }
		};
		
		tablaExistentes = new EstiloUI.Tabla(modeloExistentes);
		tablaExistentes.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		tablaExistentes.setRowHeight(24);
		tablaExistentes.setGridColor(new Color(245, 245, 245));
		scrollIzquierdo.setViewportView(tablaExistentes);

		// NUEVOS BOTONES: actúan sobre la fila seleccionada de la tabla de la izquierda
		btnEliminarProducto = new JButton(Idioma.get("inventario.btnEliminar"));
		btnEliminarProducto.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
		btnEliminarProducto.setBackground(Color.WHITE);
		btnEliminarProducto.setForeground(new Color(180, 50, 50));
		btnEliminarProducto.setBorder(new LineBorder(new Color(230, 200, 200), 1));
		btnEliminarProducto.setFocusable(false);
		btnEliminarProducto.setBounds(25, 585, 230, 35);
		contentPane.add(btnEliminarProducto);

		btnModificarProducto = new JButton(Idioma.get("inventario.btnModificar"));
		btnModificarProducto.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
		btnModificarProducto.setBackground(Color.WHITE);
		btnModificarProducto.setForeground(colorCafeOscuro);
		btnModificarProducto.setBorder(new LineBorder(colorBordeSuave, 1));
		btnModificarProducto.setFocusable(false);
		btnModificarProducto.setBounds(275, 585, 230, 35);
		contentPane.add(btnModificarProducto);

		// -----------------------------------------------------------------
		// BLOQUE DERECHO: FORMULARIO DE ALTA DE PRODUCTOS
		// -----------------------------------------------------------------
		lblNuevo = new JLabel(Idioma.get("inventario.tituloFormulario"));
		lblNuevo.setFont(new Font("Segoe UI Semibold", Font.BOLD, 14));
		lblNuevo.setForeground(colorCafeOscuro);
		lblNuevo.setBounds(535, 75, 400, 20);
		contentPane.add(lblNuevo);

		JPanel panelRegistro = new EstiloUI.Tarjeta();
		panelRegistro.setBackground(Color.WHITE);
		panelRegistro.setBorder(new LineBorder(colorBordeSuave, 1));
		panelRegistro.setBounds(535, 105, 490, 470);
		contentPane.add(panelRegistro);
		panelRegistro.setLayout(null);

		lblNombre = new JLabel(Idioma.get("inventario.nombre"));
		lblNombre.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblNombre.setForeground(colorCafeOscuro);
		lblNombre.setBounds(20, 20, 200, 15);
		panelRegistro.add(lblNombre);

		txtNombrePan = new JTextField();
		txtNombrePan.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		txtNombrePan.setBorder(new LineBorder(colorBordeSuave, 1));
		txtNombrePan.setBounds(20, 38, 450, 26);
		panelRegistro.add(txtNombrePan);

		// NUEVO CAMPO: Categoria (se llena consultando la tabla Categorias de SQL Server)
		lblCategoria = new JLabel(Idioma.get("inventario.categoria"));
		lblCategoria.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblCategoria.setForeground(colorCafeOscuro);
		lblCategoria.setBounds(20, 76, 200, 15);
		panelRegistro.add(lblCategoria);

		cboCategoria = new JComboBox<>();
		cboCategoria.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		cboCategoria.setBackground(Color.WHITE);
		cboCategoria.setBorder(new LineBorder(colorBordeSuave, 1));
		cboCategoria.setBounds(20, 94, 450, 26);
		panelRegistro.add(cboCategoria);

		// NUEVO CAMPO: Descripcion (columna 'descripcion' de Productos, opcional)
		lblDescripcion = new JLabel(Idioma.get("inventario.descripcion"));
		lblDescripcion.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblDescripcion.setForeground(colorCafeOscuro);
		lblDescripcion.setBounds(20, 132, 250, 15);
		panelRegistro.add(lblDescripcion);

		txtDescripcion = new JTextField();
		txtDescripcion.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		txtDescripcion.setBorder(new LineBorder(colorBordeSuave, 1));
		txtDescripcion.setBounds(20, 150, 450, 26);
		panelRegistro.add(txtDescripcion);

		// NUEVO CAMPO: Codigo (columna 'codigo' UNIQUE; si se deja vacio, se autogenera)
		lblCodigo = new JLabel(Idioma.get("inventario.codigo"));
		lblCodigo.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblCodigo.setForeground(colorCafeOscuro);
		lblCodigo.setBounds(20, 188, 210, 15);
		panelRegistro.add(lblCodigo);

		txtCodigo = new JTextField();
		txtCodigo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		txtCodigo.setBorder(new LineBorder(colorBordeSuave, 1));
		txtCodigo.setBounds(20, 206, 215, 26);
		panelRegistro.add(txtCodigo);

		// LLAMADA AL VALIDADOR MODULAR: un codigo/SKU no debe llevar espacios
		txtCodigo.addKeyListener(Validador.sinEspacios());

		lblPrecio = new JLabel(Idioma.get("inventario.precio"));
		lblPrecio.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblPrecio.setForeground(colorCafeOscuro);
		lblPrecio.setBounds(255, 188, 210, 15);
		panelRegistro.add(lblPrecio);

		txtPrecioPan = new JTextField();
		txtPrecioPan.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		txtPrecioPan.setBorder(new LineBorder(colorBordeSuave, 1));
		txtPrecioPan.setBounds(255, 206, 215, 26);
		panelRegistro.add(txtPrecioPan);

		// NUEVOS CAMPOS: Stock actual y stock minimo (columnas de Productos con default 0)
		lblStockActual = new JLabel(Idioma.get("inventario.stockActual"));
		lblStockActual.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblStockActual.setForeground(colorCafeOscuro);
		lblStockActual.setBounds(20, 244, 210, 15);
		panelRegistro.add(lblStockActual);

		spStockActual = new JSpinner(new SpinnerNumberModel(0, 0, 999999, 1));
		spStockActual.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		spStockActual.setBorder(new LineBorder(colorBordeSuave, 1));
		spStockActual.setBounds(20, 262, 215, 26);
		panelRegistro.add(spStockActual);

		lblStockMinimo = new JLabel(Idioma.get("inventario.stockMinimo"));
		lblStockMinimo.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblStockMinimo.setForeground(colorCafeOscuro);
		lblStockMinimo.setBounds(255, 244, 210, 15);
		panelRegistro.add(lblStockMinimo);

		spStockMinimo = new JSpinner(new SpinnerNumberModel(0, 0, 999999, 1));
		spStockMinimo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		spStockMinimo.setBorder(new LineBorder(colorBordeSuave, 1));
		spStockMinimo.setBounds(255, 262, 215, 26);
		panelRegistro.add(spStockMinimo);

		btnAnadir = new JButton(Idioma.get("inventario.btnRegistrar"));
		btnAnadir.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
		btnAnadir.setBackground(colorCafeCalido);
		btnAnadir.setForeground(Color.WHITE);
		btnAnadir.setBorder(null);
		btnAnadir.setFocusable(false);
		btnAnadir.setBounds(20, 310, 330, 38);
		panelRegistro.add(btnAnadir);

		// NUEVO BOTÓN: cancela un "Modificar Producto" en curso y limpia el formulario
		btnCancelarEdicion = new JButton(Idioma.get("inventario.btnCancelar"));
		btnCancelarEdicion.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
		btnCancelarEdicion.setBackground(Color.WHITE);
		btnCancelarEdicion.setForeground(colorGrisTexto);
		btnCancelarEdicion.setBorder(new LineBorder(colorBordeSuave, 1));
		btnCancelarEdicion.setFocusable(false);
		btnCancelarEdicion.setBounds(360, 310, 110, 38);
		panelRegistro.add(btnCancelarEdicion);
		
		// Nota informativa decorativa inferior (ahora dentro de la propia tarjeta del formulario)
		lblNota = new JLabel(Idioma.get("inventario.nota"));
		lblNota.setFont(new Font("Segoe UI", Font.ITALIC, 11));
		lblNota.setForeground(colorGrisTexto);
		lblNota.setBounds(20, 366, 450, 90);
		panelRegistro.add(lblNota);

		// =====================================
		// CONTROLADOR Y EVENTOS
		// =====================================
		// [MVC - CONTROLADOR]: Instanciación del Controlador de Inventario inyectando la vista actual ('this').
		controlador = new ControladorInventario(this);

		// NUEVO: ventana autoajustable (SIGEPAN v6), ver Vista.Redimensionable.
		EstiloUI.aplicar(this, mnInventario);
		Redimensionable.activar(this, contentPane, 1050, 680);
	}

	// =========================================================================
	// [MVC - VISTA]: Getters y Setters públicos para que el ControladorInventario pueda leer y manipular la vista
	// =========================================================================
	public JButton getBtnAnadir() { return btnAnadir; }
	public JButton getBtnCancelarEdicion() { return btnCancelarEdicion; }
	public JButton getBtnEliminarProducto() { return btnEliminarProducto; }
	public JButton getBtnModificarProducto() { return btnModificarProducto; }
	public String getNombrePan() { return txtNombrePan.getText().trim(); }
	public String getPrecioPanTexto() { return txtPrecioPan.getText().trim(); }
	public DefaultTableModel getModeloExistentes() { return modeloExistentes; }
	// NUEVO: fila seleccionada en la tabla de catálogo (para Eliminar/Modificar Producto)
	public int getFilaSeleccionadaExistente() { return tablaExistentes.getSelectedRow(); }

	// NUEVOS GETTERS: exponen los campos agregados para el catálogo completo
	public JComboBox<String> getCboCategoria() { return cboCategoria; }
	public String getCategoriaSeleccionada() {
		return cboCategoria.getSelectedItem() == null ? "" : cboCategoria.getSelectedItem().toString();
	}
	public String getDescripcion() { return txtDescripcion.getText().trim(); }
	public String getCodigo() { return txtCodigo.getText().trim(); }
	public String getStockActualTexto() { return spStockActual.getValue().toString(); }
	public String getStockMinimoTexto() { return spStockMinimo.getValue().toString(); }

	/**
	 * NUEVO: vuelca en el formulario los datos de un producto ya existente, para que
	 * el usuario los edite antes de presionar "Guardar Cambios" (botón "Modificar Producto").
	 */
	public void cargarProductoEnFormulario(String nombre, String categoria, String descripcion, String codigo,
			double precio, int stockActual, int stockMinimo) {
		txtNombrePan.setText(nombre);
		cboCategoria.setSelectedItem(categoria);
		txtDescripcion.setText(descripcion);
		txtCodigo.setText(codigo);
		txtPrecioPan.setText(String.valueOf(precio));
		spStockActual.setValue(stockActual);
		spStockMinimo.setValue(stockMinimo);
	}

	/**
	 * NUEVO: cambia la apariencia del formulario entre "Añadir producto nuevo" y
	 * "Modificando: <producto>", para que quede claro en qué modo está trabajando el usuario.
	 */
	public void setModoEdicion(boolean activo, String nombreProducto) {
		modoEdicionActivo = activo;
		nombreProductoEnEdicion = nombreProducto;
		if (activo) {
			lblNuevo.setText(Idioma.get("inventario.tituloModificar", nombreProducto));
			btnAnadir.setText(Idioma.get("inventario.btnGuardarCambios"));
		} else {
			lblNuevo.setText(Idioma.get("inventario.tituloFormulario"));
			btnAnadir.setText(Idioma.get("inventario.btnRegistrar"));
		}
	}

	public void limpiarCampos() {
		txtNombrePan.setText("");
		txtPrecioPan.setText("");
		txtDescripcion.setText("");
		txtCodigo.setText("");
		spStockActual.setValue(0);
		spStockMinimo.setValue(0);
	}

	/** Refresca todos los textos visibles de esta ventana en el idioma actual. */
	@Override
	public void actualizarTextos() {
		setTitle(Idioma.get("inventario.tituloVentana"));
		mnInicio.setText(Idioma.get("menu.inicio"));
		mnPedidos.setText(Idioma.get("menu.pedidos"));
		mnUsuarios.setText(Idioma.get("menu.usuarios"));
		mnClientes.setText(Idioma.get("menu.clientes"));
		mnReportes.setText(Idioma.get("menu.reportes"));
		mnConfiguracion.setText(Idioma.get("menu.configuracion"));
		Idioma.construirSubmenuIdiomas(mnConfiguracion);
		mnInventario.setText(Idioma.get("menu.inventario"));
		mnCerrarSesion.setText(Idioma.get("menu.cerrarSesion"));

		lblTitulo.setText(Idioma.get("inventario.titulo"));
		lblExistentes.setText(Idioma.get("inventario.catalogo"));
		btnEliminarProducto.setText(Idioma.get("inventario.btnEliminar"));
		btnModificarProducto.setText(Idioma.get("inventario.btnModificar"));

		setModoEdicion(modoEdicionActivo, nombreProductoEnEdicion);

		lblNombre.setText(Idioma.get("inventario.nombre"));
		lblCategoria.setText(Idioma.get("inventario.categoria"));
		lblDescripcion.setText(Idioma.get("inventario.descripcion"));
		lblCodigo.setText(Idioma.get("inventario.codigo"));
		lblPrecio.setText(Idioma.get("inventario.precio"));
		lblStockActual.setText(Idioma.get("inventario.stockActual"));
		lblStockMinimo.setText(Idioma.get("inventario.stockMinimo"));
		btnCancelarEdicion.setText(Idioma.get("inventario.btnCancelar"));
		lblNota.setText(Idioma.get("inventario.nota"));

		modeloExistentes.setColumnIdentifiers(new Object[] {
				Idioma.get("inventario.columnaProducto"), Idioma.get("inventario.columnaCategoria"),
				Idioma.get("inventario.columnaPrecio"), Idioma.get("inventario.columnaStock")});
	}
}