package Vista;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

// IMPORTACIÓN DE CONTROLADORES Y MODELOS
import controladores.ControladorPedido;
import controladores.Validador;
import idioma.ActualizableIdioma;
import idioma.Idioma;
import modelo.Cliente;
import modelo.DatosGlobales;
import modelo.Pedido;

public class pedidos extends JFrame implements ActualizableIdioma {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	
	// COMPONENTES DE CAPTURA DE DATOS
	private JSpinner spCantidad;
	private JTextField txtCliente, txtApellido, txtTelefono, txtDireccion, txtCorreo, txtPrecio, txtTotal;
	private JComboBox<String> cboProducto;
	private JComboBox<Cliente> cboCliente;
	
	// TABLA Y CONTROLADOR
	private JTable tablaPedidos;
	private DefaultTableModel modelo;
	private ControladorPedido controlador; // Enlace con la lógica de negocio
	private JLabel lblTitulo;
	private JButton btnGuardarPedido;

	// ELEMENTOS MULTI-IDIOMA (i18n) Y MENÚ
	private JMenu mnInicio, mnPedidos, mnUsuarios, mnClientes, mnReportes, mnConfiguracion, mnInventario, mnCerrarSesion;
	private JLabel lblSeccionCliente, lblClienteRegistrado, lblCliente, lblApellido, lblTelefono, lblCorreo, lblDireccion;
	private JLabel lblSeccionPedido, lblProducto, lblCantidadTxt, lblPrecio, lblTotalTexto;
	private JButton btnAgregar, btnLimpiar, btnEliminar;

	// BANDERAS PARA SABER SI ESTAMOS CREANDO O MODIFICANDO UN PEDIDO
	private boolean modoEdicionActivo = false;
	private String idPedidoEnEdicionVista = null;
	
	// MÉTODO PRINCIPAL (Punto de entrada independiente para pruebas)
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					pedidos frame = new pedidos();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	// CONSTRUCTOR 1: Modo normal (Crear pedido nuevo)
	public pedidos() {
		setTitle(Idioma.get("pedidos.tituloVentana"));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1050, 730);

		// PALETA DE COLORES
		Color colorFondo = EstiloUI.FONDO;      // Crema
		Color colorCafeOscuro = EstiloUI.TEXTO;    // Café obscuro
		Color colorCafeCalido = EstiloUI.ACENTO;   // Café dorado
		Color colorBordeSuave = EstiloUI.BORDE;  // Tono arena
		Color colorGrisTexto = EstiloUI.SECUNDARIO;  // Gris cálido

		// BARRA DE MENÚ SUPERIOR Y SISTEMA DE IDIOMA
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

		// NAVEGACIÓN ENTRE VENTANAS AL HACER CLIC EN EL MENÚ
		mnInicio.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) { new Inicio().setVisible(true); dispose(); }
		});
		mnUsuarios.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) { new usuarios().setVisible(true); dispose(); }
		});
		mnClientes.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) { new Clientes().setVisible(true); dispose(); }
		});
		mnReportes.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) { new reportes().setVisible(true); dispose(); }
		});
		mnInventario.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) { new Inventario().setVisible(true); dispose(); }
		});
		mnCerrarSesion.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) { new Prueba().setVisible(true); dispose(); }
		});

		// PANEL PRINCIPAL
		contentPane = new JPanel();
		contentPane.setBackground(colorFondo);
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		// ENCABEZADO
		lblTitulo = new JLabel(Idioma.get("pedidos.titulo"));
		lblTitulo.setFont(new Font("Segoe UI Semibold", Font.BOLD, 24));
		lblTitulo.setForeground(colorCafeOscuro);
		lblTitulo.setBounds(25, 25, 400, 35);
		contentPane.add(lblTitulo);

		// TABLA DE PEDIDOS (Lado Izquierdo)
		JPanel panelTabla = new EstiloUI.Tarjeta();
		panelTabla.setBackground(Color.WHITE);
		panelTabla.setBorder(new LineBorder(colorBordeSuave, 1));
		panelTabla.setBounds(25, 85, 480, 500);
		panelTabla.setLayout(null);
		contentPane.add(panelTabla);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(15, 15, 450, 470);
		scrollPane.getViewport().setBackground(Color.WHITE);
		scrollPane.setBorder(new LineBorder(new Color(240, 240, 240), 1));
		panelTabla.add(scrollPane);

		// Configuración de la tabla como SOLO LECTURA (isCellEditable = false)
		modelo = new DefaultTableModel() {
			private static final long serialVersionUID = 1L;
			@Override
			public boolean isCellEditable(int row, int column) { return false; }
		};
		modelo.addColumn(Idioma.get("pedidos.columnaProducto"));
		modelo.addColumn(Idioma.get("pedidos.columnaCantidad"));
		modelo.addColumn(Idioma.get("pedidos.columnaPrecio"));
		modelo.addColumn(Idioma.get("pedidos.columnaSubtotal"));
		modelo.addColumn(Idioma.get("pedidos.columnaEstado"));
		
		tablaPedidos = new EstiloUI.Tabla(modelo);
		tablaPedidos.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		tablaPedidos.setRowHeight(22);
		tablaPedidos.setGridColor(new Color(245, 245, 245));
		scrollPane.setViewportView(tablaPedidos);

		// PANEL FORMULARIO (Lado Derecho)
		JPanel panelFormulario = new EstiloUI.Tarjeta();
		panelFormulario.setBackground(Color.WHITE);
		panelFormulario.setBorder(new LineBorder(colorBordeSuave, 1));
		panelFormulario.setBounds(535, 85, 490, 500);
		contentPane.add(panelFormulario);
		panelFormulario.setLayout(null);

		// --- SECCIÓN 1: DATOS DEL CLIENTE ---
		lblSeccionCliente = new JLabel(Idioma.get("pedidos.seccionCliente"));
		lblSeccionCliente.setFont(new Font("Segoe UI Semibold", Font.BOLD, 11));
		lblSeccionCliente.setForeground(colorGrisTexto);
		lblSeccionCliente.setBounds(20, 12, 300, 14);
		panelFormulario.add(lblSeccionCliente);

		lblClienteRegistrado = new JLabel(Idioma.get("pedidos.clienteRegistrado"));
		lblClienteRegistrado.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblClienteRegistrado.setForeground(colorCafeOscuro);
		lblClienteRegistrado.setBounds(20, 30, 300, 15);
		panelFormulario.add(lblClienteRegistrado);

		cboCliente = new JComboBox<>();
		cboCliente.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		cboCliente.setBackground(Color.WHITE);
		cboCliente.setBorder(new LineBorder(colorBordeSuave, 1));
		cboCliente.setBounds(20, 48, 450, 26);
		// Personaliza el primer elemento si es nulo ("-- Nuevo cliente --")
		cboCliente.setRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;
			@Override
			public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice, boolean seleccionado, boolean conFoco) {
				String texto = (valor == null) ? Idioma.get("pedidos.cboNuevoCliente") : valor.toString();
				return super.getListCellRendererComponent(lista, texto, indice, seleccionado, conFoco);
			}
		});
		panelFormulario.add(cboCliente);

		// Campos de texto del cliente + Validaciones en tiempo real
		lblCliente = new JLabel(Idioma.get("pedidos.nombre"));
		lblCliente.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblCliente.setForeground(colorCafeOscuro);
		lblCliente.setBounds(20, 86, 200, 15);
		panelFormulario.add(lblCliente);

		txtCliente = new JTextField();
		txtCliente.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		txtCliente.setBorder(new LineBorder(colorBordeSuave, 1));
		txtCliente.setBounds(20, 104, 215, 26);
		panelFormulario.add(txtCliente);
		txtCliente.addKeyListener(Validador.soloLetras()); // Valida solo letras

		lblApellido = new JLabel(Idioma.get("pedidos.apellido"));
		lblApellido.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblApellido.setForeground(colorCafeOscuro);
		lblApellido.setBounds(255, 86, 200, 15);
		panelFormulario.add(lblApellido);

		txtApellido = new JTextField();
		txtApellido.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		txtApellido.setBorder(new LineBorder(colorBordeSuave, 1));
		txtApellido.setBounds(255, 104, 215, 26);
		panelFormulario.add(txtApellido);
		txtApellido.addKeyListener(Validador.soloLetras()); // Valida solo letras

		lblTelefono = new JLabel(Idioma.get("pedidos.telefono"));
		lblTelefono.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblTelefono.setForeground(colorCafeOscuro);
		lblTelefono.setBounds(20, 142, 200, 15);
		panelFormulario.add(lblTelefono);

		txtTelefono = new JTextField();
		txtTelefono.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		txtTelefono.setBorder(new LineBorder(colorBordeSuave, 1));
		txtTelefono.setBounds(20, 160, 215, 26);
		panelFormulario.add(txtTelefono);
		txtTelefono.addKeyListener(Validador.soloNumeros(txtTelefono, 10)); // Valida 10 números máx.

		lblCorreo = new JLabel(Idioma.get("pedidos.correo"));
		lblCorreo.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblCorreo.setForeground(colorCafeOscuro);
		lblCorreo.setBounds(255, 142, 200, 15);
		panelFormulario.add(lblCorreo);

		txtCorreo = new JTextField();
		txtCorreo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		txtCorreo.setBorder(new LineBorder(colorBordeSuave, 1));
		txtCorreo.setBounds(255, 160, 215, 26);
		panelFormulario.add(txtCorreo);

		lblDireccion = new JLabel(Idioma.get("pedidos.direccion"));
		lblDireccion.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblDireccion.setForeground(colorCafeOscuro);
		lblDireccion.setBounds(20, 198, 300, 15);
		panelFormulario.add(lblDireccion);

		txtDireccion = new JTextField();
		txtDireccion.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		txtDireccion.setBorder(new LineBorder(colorBordeSuave, 1));
		txtDireccion.setBounds(20, 216, 450, 26);
		panelFormulario.add(txtDireccion);
		txtDireccion.addKeyListener(Validador.formatoDireccion()); // Valida dirección

		JSeparator separadorCliente = new JSeparator();
		separadorCliente.setBounds(20, 252, 450, 2);
		panelFormulario.add(separadorCliente);

		// --- SECCIÓN 2: DATOS DEL PRODUCTO Y PRECIO ---
		lblSeccionPedido = new JLabel(Idioma.get("pedidos.seccionPedido"));
		lblSeccionPedido.setFont(new Font("Segoe UI Semibold", Font.BOLD, 11));
		lblSeccionPedido.setForeground(colorGrisTexto);
		lblSeccionPedido.setBounds(20, 260, 300, 14);
		panelFormulario.add(lblSeccionPedido);

		lblProducto = new JLabel(Idioma.get("pedidos.producto"));
		lblProducto.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblProducto.setForeground(colorCafeOscuro);
		lblProducto.setBounds(20, 278, 150, 15);
		panelFormulario.add(lblProducto);

		cboProducto = new JComboBox<>();
		cboProducto.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		cboProducto.setBackground(Color.WHITE);
		cboProducto.setBorder(new LineBorder(colorBordeSuave, 1));
		cboProducto.setBounds(20, 296, 450, 26);

		// Carga los productos desde la clase DatosGlobales
		DefaultComboBoxModel<String> comboModelo = new DefaultComboBoxModel<>();
		for (Object[] prod : DatosGlobales.listaProductos) {
			comboModelo.addElement(prod[0].toString());
		}
		cboProducto.setModel(comboModelo);
		panelFormulario.add(cboProducto);

		lblCantidadTxt = new JLabel(Idioma.get("pedidos.cantidad"));
		lblCantidadTxt.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblCantidadTxt.setForeground(colorCafeOscuro);
		lblCantidadTxt.setBounds(20, 334, 80, 15);
		panelFormulario.add(lblCantidadTxt);

		spCantidad = new JSpinner(new SpinnerNumberModel(0, 0, 999, 1)); // Mínimo 0, Máximo 999
		spCantidad.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		spCantidad.setBorder(new LineBorder(colorBordeSuave, 1));
		spCantidad.setBounds(20, 352, 100, 26);
		panelFormulario.add(spCantidad);

		lblPrecio = new JLabel(Idioma.get("pedidos.precioUnitario"));
		lblPrecio.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblPrecio.setForeground(colorCafeOscuro);
		lblPrecio.setBounds(140, 334, 150, 15);
		panelFormulario.add(lblPrecio);

		txtPrecio = new JTextField();
		txtPrecio.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		txtPrecio.setBorder(new LineBorder(colorBordeSuave, 1));
		txtPrecio.setBounds(140, 352, 330, 26);
		txtPrecio.setEditable(true);
		panelFormulario.add(txtPrecio);

		// Al cambiar de producto en el combo, cambia automáticamente el precio mostrado
		cboProducto.addActionListener(e -> {
			String productoSeleccionado = cboProducto.getSelectedItem().toString();
			for (Object[] prod : DatosGlobales.listaProductos) {
				if (prod[0].toString().equals(productoSeleccionado)) {
					txtPrecio.setText(prod[1].toString());
					break;
				}
			}
			spCantidad.setValue(((SpinnerNumberModel) spCantidad.getModel()).getMinimum());
		});

		// Seleccionar el primer producto por defecto
		if (cboProducto.getItemCount() > 0) {
			cboProducto.setSelectedIndex(0);
			String primero = cboProducto.getSelectedItem().toString();
			for (Object[] prod : DatosGlobales.listaProductos) {
				if (prod[0].toString().equals(primero)) {
					txtPrecio.setText(prod[1].toString());
					break;
				}
			}
		}

		// BOTÓN AGREGAR PRODUCTO A LA TABLA
		btnAgregar = new JButton(Idioma.get("pedidos.btnAgregar"));
		btnAgregar.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		btnAgregar.setBackground(colorCafeCalido);
		btnAgregar.setForeground(Color.WHITE);
		btnAgregar.setBorder(null);
		btnAgregar.setFocusable(false);
		btnAgregar.setBounds(20, 392, 450, 30);
		panelFormulario.add(btnAgregar);

		JSeparator separadorPedido = new JSeparator();
		separadorPedido.setBounds(20, 436, 450, 2);
		panelFormulario.add(separadorPedido);

		// SECCIÓN TOTAL
		lblTotalTexto = new JLabel(Idioma.get("pedidos.totalTexto"));
		lblTotalTexto.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
		lblTotalTexto.setForeground(colorGrisTexto);
		lblTotalTexto.setBounds(20, 452, 100, 20);
		panelFormulario.add(lblTotalTexto);

		txtTotal = new JTextField(Idioma.get("pedidos.totalInicial"));
		txtTotal.setFont(new Font("Segoe UI Black", Font.BOLD, 20));
		txtTotal.setForeground(colorCafeOscuro);
		txtTotal.setHorizontalAlignment(SwingConstants.RIGHT);
		txtTotal.setBorder(null);
		txtTotal.setBackground(Color.WHITE);
		txtTotal.setEditable(false);
		txtTotal.setBounds(140, 446, 330, 30);
		panelFormulario.add(txtTotal);

		// BOTONES DE ACCIÓN (Inferiores)
		btnLimpiar = new JButton(Idioma.get("pedidos.btnLimpiar"));
		btnLimpiar.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
		btnLimpiar.setBackground(Color.WHITE);
		btnLimpiar.setForeground(colorCafeOscuro);
		btnLimpiar.setBorder(new LineBorder(colorBordeSuave, 1));
		btnLimpiar.setFocusable(false);
		btnLimpiar.setBounds(25, 610, 130, 35);
		contentPane.add(btnLimpiar);

		btnEliminar = new JButton(Idioma.get("pedidos.btnEliminarFila"));
		btnEliminar.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
		btnEliminar.setBackground(Color.WHITE);
		btnEliminar.setForeground(new Color(180, 50, 50));
		btnEliminar.setBorder(new LineBorder(new Color(230, 200, 200), 1));
		btnEliminar.setFocusable(false);
		btnEliminar.setBounds(170, 610, 130, 35);
		contentPane.add(btnEliminar);

		btnGuardarPedido = new JButton(Idioma.get("pedidos.btnGuardar"));
		btnGuardarPedido.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
		btnGuardarPedido.setBackground(colorCafeOscuro);
		btnGuardarPedido.setForeground(Color.WHITE);
		btnGuardarPedido.setBorder(null);
		btnGuardarPedido.setFocusable(false);
		btnGuardarPedido.setBounds(830, 610, 195, 35);
		contentPane.add(btnGuardarPedido);
		
		// EVENTOS DE LOS BOTONES: Delegan la acción al ControladorPedido
		controlador = new ControladorPedido(this);

		btnAgregar.addActionListener(e -> controlador.agregarProducto());
		btnEliminar.addActionListener(e -> controlador.eliminarProducto());
		btnLimpiar.addActionListener(e -> { controlador.limpiar();
			cboProducto.setSelectedIndex(cboProducto.getSelectedIndex());
		});
		btnGuardarPedido.addActionListener(e -> controlador.guardarPedido());

		// Activa el diseño responsive/autoajustable al maximizar
		EstiloUI.aplicar(this, mnPedidos);
		Redimensionable.activar(this, contentPane, 1050, 730);
	}

	// CONSTRUCTOR 2: Modo Modificar (Abre la ventana con datos de un pedido existente)
	public pedidos(Pedido pedidoAEditar) {
		this(); // Llama al constructor base para armar la interfaz
		controlador.cargarPedidoParaEditar(pedidoAEditar); // Carga los datos existentes
	}

	// MÉTODOS GETTER Y SETTER (Usados por el Controlador para leer/escribir datos)
	public String getCliente() { return txtCliente.getText(); }
	public String getApellido() { return txtApellido.getText(); }
	public String getTelefono() { return txtTelefono.getText(); }
	public String getDireccion() { return txtDireccion.getText(); }
	public String getCorreo() { return txtCorreo.getText(); }
	public String getProducto() { return cboProducto.getSelectedItem().toString(); }
	
	public double getPrecio() {
		if (txtPrecio.getText().trim().isEmpty()) return 0.0;
		return Double.parseDouble(txtPrecio.getText().trim());
	}
	
	public DefaultTableModel getModelo() { return modelo; }
	public void setTotal(double total) { txtTotal.setText(String.format("$%.2f", total)); }
	public int getFilaSeleccionada() { return tablaPedidos.getSelectedRow(); }
	public JComboBox<Cliente> getCboCliente() { return cboCliente; }
	public int getCantidad() { return (Integer) spCantidad.getValue(); }

	// Borra los datos digitados en los formularios
	public void limpiarCampos() {
		txtCliente.setText("");
		txtApellido.setText("");
		txtTelefono.setText("");
		txtDireccion.setText("");
		txtCorreo.setText("");
		txtPrecio.setText("");
		txtTotal.setText(Idioma.get("pedidos.totalInicial"));
	}

	// Carga los 5 datos del cliente en los campos de texto
	public void cargarDatosCliente(String nombre, String apellido, String telefono, String direccion, String correo) {
		txtCliente.setText(nombre);
		txtApellido.setText(apellido);
		txtTelefono.setText(telefono);
		txtDireccion.setText(direccion);
		txtCorreo.setText(correo);
	}

	// Busca un cliente por su ID en el combo box y lo selecciona
	public void seleccionarClienteEnCombo(int idCliente) {
		for (int i = 0; i < cboCliente.getItemCount(); i++) {
			Cliente c = cboCliente.getItemAt(i);
			if (c != null && c.getIdCliente() == idCliente) {
				cboCliente.setSelectedIndex(i);
				return;
			}
		}
		cboCliente.setSelectedIndex(0);
	}

	// Modifica la interfaz entre "Nuevo Pedido" y "Modificando pedido P-X"
	public void setModoEdicion(boolean activo, String idPedido) {
		modoEdicionActivo = activo;
		idPedidoEnEdicionVista = idPedido;
		if (activo) {
			lblTitulo.setText(Idioma.get("pedidos.tituloModificar", idPedido));
			btnGuardarPedido.setText(Idioma.get("pedidos.btnGuardarCambios"));
		} else {
			lblTitulo.setText(Idioma.get("pedidos.titulo"));
			btnGuardarPedido.setText(Idioma.get("pedidos.btnGuardar"));
		}
	}

	// MÉTODO OBLIGATORIO DE LA INTERFAZ ActualizableIdioma
	// Traduce toda la interfaz en tiempo de ejecución al cambiar de idioma
	@Override
	public void actualizarTextos() {
		setTitle(Idioma.get("pedidos.tituloVentana"));
		mnInicio.setText(Idioma.get("menu.inicio"));
		mnPedidos.setText(Idioma.get("menu.pedidos"));
		mnUsuarios.setText(Idioma.get("menu.usuarios"));
		mnClientes.setText(Idioma.get("menu.clientes"));
		mnReportes.setText(Idioma.get("menu.reportes"));
		mnConfiguracion.setText(Idioma.get("menu.configuracion"));
		Idioma.construirSubmenuIdiomas(mnConfiguracion);
		mnInventario.setText(Idioma.get("menu.inventario"));
		mnCerrarSesion.setText(Idioma.get("menu.cerrarSesion"));

		setModoEdicion(modoEdicionActivo, idPedidoEnEdicionVista);

		lblSeccionCliente.setText(Idioma.get("pedidos.seccionCliente"));
		lblClienteRegistrado.setText(Idioma.get("pedidos.clienteRegistrado"));
		lblCliente.setText(Idioma.get("pedidos.nombre"));
		lblApellido.setText(Idioma.get("pedidos.apellido"));
		lblTelefono.setText(Idioma.get("pedidos.telefono"));
		lblCorreo.setText(Idioma.get("pedidos.correo"));
		lblDireccion.setText(Idioma.get("pedidos.direccion"));
		
		cboCliente.repaint();
		lblSeccionPedido.setText(Idioma.get("pedidos.seccionPedido"));
		lblProducto.setText(Idioma.get("pedidos.producto"));
		lblCantidadTxt.setText(Idioma.get("pedidos.cantidad"));
		lblPrecio.setText(Idioma.get("pedidos.precioUnitario"));
		lblTotalTexto.setText(Idioma.get("pedidos.totalTexto"));
		btnAgregar.setText(Idioma.get("pedidos.btnAgregar"));
		btnLimpiar.setText(Idioma.get("pedidos.btnLimpiar"));
		btnEliminar.setText(Idioma.get("pedidos.btnEliminarFila"));

		// Actualiza los encabezados de la tabla
		modelo.setColumnIdentifiers(new Object[] {
			Idioma.get("pedidos.columnaProducto"), Idioma.get("pedidos.columnaCantidad"),
			Idioma.get("pedidos.columnaPrecio"), Idioma.get("pedidos.columnaSubtotal"),
			Idioma.get("pedidos.columnaEstado")
		});
	}
}