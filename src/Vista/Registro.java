package Vista;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JPasswordField; 
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.SwingConstants;

// LIBRERÍA EXTERNA: Proporciona el componente visual de selección de fecha (Calendario)
import com.toedter.calendar.JDateChooser;

import controladores.ControladorUsuario;
import controladores.Validador; 
import idioma.ActualizableIdioma;
import idioma.Idioma;

public class Registro extends JFrame implements ActualizableIdioma {

	private static final long serialVersionUID = 1L;
	private JPanel VentanaRegistro;
	
	// DECLARACIÓN DE CAMPOS DE ENTRADA
	private JTextField txtNombre;
	private JTextField txtApellido;
	private JTextField txtDireccion;
	private JTextField txtTelefono;
	private JDateChooser dateChooserFechaNacimiento; // Componente de calendario para la fecha
	private JTextField txtUsuario;         
	private JTextField txtCorreo;
	private JPasswordField txtContrasenia;   
	private JPasswordField txtConfirmar;     
	
	// CONEXIÓN AL CONTROLADOR (LÓGICA DEL NEGOCIO)
	private ControladorUsuario controlador;

	// ETIQUETAS Y BOTONES (Atributos de clase para permitir traducción dinámica)
	private JLabel lblNombre, lblApellido, lblTelefono, lblDireccion, lblFechaNacimiento;
	private JLabel lblUsuario, lblCorreo, lblFoto, lblContrasenia, lblConfirmar;
	private JButton btnFoto, btnRegistrar, btnCancelar;

	/**
	 * Muestra la ventana de manera independiente para pruebas aisladas.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Registro frame = new Registro();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * CONSTRUCTOR 1: Útil para pruebas individuales en fase de desarrollo.
	 */
	public Registro() {
		controlador = new ControladorUsuario(this);
		inicializarInterfaz();
	}

	/**
	 * CONSTRUCTOR 2 (Uso en Producción): 
	 * Recibe la ventana 'usuarios' de origen para poder refrescar la tabla de empleados al registrar.
	 */
	public Registro(usuarios origenUsuarios) {
		controlador = new ControladorUsuario(this, origenUsuarios);
		inicializarInterfaz();
	}

	/**
	 * CONSTRUCCIÓN DE LA INTERFAZ GRÁFICA (GUI)
	 */
	private void inicializarInterfaz() {
		setTitle(Idioma.get("registro.tituloVentana"));
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 550, 560);
		
		// PALETA DE COLORES PERSONALIZADA (Temática de Cafetería)
		Color colorFondo = EstiloUI.FONDO;      // Crema suave
		Color colorCafeOscuro = EstiloUI.TEXTO;    // Café espresso profundo
		Color colorCafeCalido = EstiloUI.ACENTO;   // Café dorado para el botón principal
		Color colorBordeSuave = EstiloUI.BORDE;  // Tono arena sutil
		Color colorGrisTexto = EstiloUI.SECUNDARIO;  // Gris cálido elegante

		VentanaRegistro = new JPanel();
		VentanaRegistro.setBorder(new EmptyBorder(5, 5, 5, 5));
		VentanaRegistro.setBackground(colorFondo);
		setContentPane(VentanaRegistro);
		VentanaRegistro.setLayout(null);
		
		// Panel Contenedor Blanco
		JPanel panel = new EstiloUI.Tarjeta();
		panel.setBackground(Color.WHITE);
		panel.setBorder(new LineBorder(colorBordeSuave, 1));
		panel.setBounds(15, 115, 505, 340);
		VentanaRegistro.add(panel);
		panel.setLayout(null);
		
		// =================================================================
		// BLOQUE IZQUIERDO: DATOS PERSONALES
		// =================================================================
		lblNombre = new JLabel(Idioma.get("registro.nombre"));
		lblNombre.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblNombre.setForeground(colorCafeOscuro);
		lblNombre.setBounds(20, 15, 100, 15);
		panel.add(lblNombre);
		
		txtNombre = new JTextField();
		txtNombre.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		txtNombre.setBorder(new LineBorder(colorBordeSuave, 1));
		txtNombre.setBounds(20, 35, 190, 24);
		panel.add(txtNombre);
		// FILTRO: Solo permite escribir letras en tiempo real
		txtNombre.addKeyListener(Validador.soloLetras());
		
		lblApellido = new JLabel(Idioma.get("registro.apellido"));
		lblApellido.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblApellido.setForeground(colorCafeOscuro);
		lblApellido.setBounds(20, 69, 100, 15);
		panel.add(lblApellido);
		
		txtApellido = new JTextField();
		txtApellido.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		txtApellido.setBorder(new LineBorder(colorBordeSuave, 1));
		txtApellido.setBounds(20, 89, 190, 24);
		panel.add(txtApellido);
		// FILTRO: Solo letras
		txtApellido.addKeyListener(Validador.soloLetras());
		
		lblTelefono = new JLabel(Idioma.get("registro.telefono"));
		lblTelefono.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblTelefono.setForeground(colorCafeOscuro);
		lblTelefono.setBounds(20, 123, 150, 15);
		panel.add(lblTelefono);
		
		txtTelefono = new JTextField();
		txtTelefono.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		txtTelefono.setBorder(new LineBorder(colorBordeSuave, 1));
		txtTelefono.setBounds(20, 143, 190, 24);
		panel.add(txtTelefono);
		// FILTRO: Solo números y con un máximo estricto de 10 caracteres
		txtTelefono.addKeyListener(Validador.soloNumeros(txtTelefono, 10));
		
		lblDireccion = new JLabel(Idioma.get("registro.direccion"));
		lblDireccion.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblDireccion.setForeground(colorCafeOscuro);
		lblDireccion.setBounds(20, 177, 100, 15);
		panel.add(lblDireccion);
		
		txtDireccion = new JTextField();
		txtDireccion.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		txtDireccion.setBorder(new LineBorder(colorBordeSuave, 1));
		txtDireccion.setBounds(20, 197, 190, 24);
		panel.add(txtDireccion);
		// FILTRO: Permite caracteres válidos para direcciones (#, ., ,, -)
		txtDireccion.addKeyListener(Validador.formatoDireccion());

		// Selector de Fecha de Nacimiento con JDateChooser
		lblFechaNacimiento = new JLabel(Idioma.get("registro.fechaNacimiento"));
		lblFechaNacimiento.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblFechaNacimiento.setForeground(colorCafeOscuro);
		lblFechaNacimiento.setBounds(20, 231, 220, 15);
		panel.add(lblFechaNacimiento);

		dateChooserFechaNacimiento = new JDateChooser();
		dateChooserFechaNacimiento.setDateFormatString("dd/MM/yyyy");
		dateChooserFechaNacimiento.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		dateChooserFechaNacimiento.setBorder(new LineBorder(colorBordeSuave, 1));
		dateChooserFechaNacimiento.setBounds(20, 251, 210, 24);
		panel.add(dateChooserFechaNacimiento);

		// =================================================================
		// BLOQUE DERECHO: USUARIO Y FOTOGRAFÍA
		// =================================================================
		lblUsuario = new JLabel(Idioma.get("registro.usuario"));
		lblUsuario.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblUsuario.setForeground(colorCafeOscuro);
		lblUsuario.setBounds(255, 15, 150, 15);
		panel.add(lblUsuario);
		
		txtUsuario = new JTextField();
		txtUsuario.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		txtUsuario.setBorder(new LineBorder(colorBordeSuave, 1));
		txtUsuario.setBounds(255, 35, 225, 24);
		panel.add(txtUsuario);
		// FILTRO: Bloquea el uso de barra espaciadora
		txtUsuario.addKeyListener(Validador.sinEspacios());

		lblCorreo = new JLabel(Idioma.get("registro.correo"));
		lblCorreo.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblCorreo.setForeground(colorCafeOscuro);
		lblCorreo.setBounds(255, 69, 150, 15);
		panel.add(lblCorreo);

		txtCorreo = new JTextField();
		txtCorreo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		txtCorreo.setBorder(new LineBorder(colorBordeSuave, 1));
		txtCorreo.setBounds(255, 89, 225, 24);
		panel.add(txtCorreo);
		
		// Vista previa de la fotografía del usuario
		lblFoto = new JLabel(Idioma.get("registro.sinFoto"));
		lblFoto.setHorizontalAlignment(SwingConstants.CENTER);
		lblFoto.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		lblFoto.setForeground(colorGrisTexto);
		lblFoto.setBorder(new LineBorder(colorBordeSuave, 1));
		lblFoto.setBackground(colorFondo);
		lblFoto.setOpaque(true);
		lblFoto.setBounds(298, 125, 141, 68);
		panel.add(lblFoto);
		
		// Botón para seleccionar imagen con JFileChooser
		btnFoto = new JButton(Idioma.get("registro.btnCargarFoto"));
		btnFoto.setFont(new Font("Segoe UI Semibold", Font.BOLD, 11));
		btnFoto.setBackground(Color.WHITE);
		btnFoto.setForeground(colorCafeOscuro);
		btnFoto.setBorder(new LineBorder(colorBordeSuave, 1));
		btnFoto.setFocusable(false);
		btnFoto.setBounds(321, 198, 100, 25);
		btnFoto.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser selector = new JFileChooser();
				int opcion = selector.showOpenDialog(null);
				if (opcion == JFileChooser.APPROVE_OPTION) {
					String ruta = selector.getSelectedFile().getAbsolutePath();
					ImageIcon imagen = new ImageIcon(ruta);
					// Escala suavemente la imagen elegida al tamaño exacto del JLabel
					Image img = imagen.getImage().getScaledInstance(
							lblFoto.getWidth(),
							lblFoto.getHeight(),
							Image.SCALE_SMOOTH);
					lblFoto.setText(""); 
					lblFoto.setIcon(new ImageIcon(img));
				}
			}
		});
		panel.add(btnFoto);
		
		JSeparator separator = new JSeparator();
		separator.setForeground(new Color(245, 245, 245));
		separator.setBounds(15, 285, 475, 5);
		panel.add(separator);
		
		// =================================================================
		// SECCIÓN INFERIOR: CREDENCIALES (CONTRASEÑAS)
		// =================================================================
		lblContrasenia = new JLabel(Idioma.get("registro.contrasena"));
		lblContrasenia.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblContrasenia.setForeground(colorCafeOscuro);
		lblContrasenia.setBounds(45, 293, 100, 15);
		panel.add(lblContrasenia);
		
		txtContrasenia = new JPasswordField();
		txtContrasenia.setBorder(new LineBorder(colorBordeSuave, 1));
		txtContrasenia.setBounds(45, 310, 165, 24);
		panel.add(txtContrasenia);
		txtContrasenia.addKeyListener(Validador.limitarLongitudPassword(txtContrasenia, 16));
		
		lblConfirmar = new JLabel(Idioma.get("registro.confirmarContrasena"));
		lblConfirmar.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblConfirmar.setForeground(colorCafeOscuro);
		lblConfirmar.setBounds(255, 293, 150, 15);
		panel.add(lblConfirmar);
		
		txtConfirmar = new JPasswordField();
		txtConfirmar.setBorder(new LineBorder(colorBordeSuave, 1));
		txtConfirmar.setBounds(255, 310, 165, 24);
		panel.add(txtConfirmar);
		txtConfirmar.addKeyListener(Validador.limitarLongitudPassword(txtConfirmar, 16));
		
		JLabel lblIconPass = new JLabel("");
		lblIconPass.setIcon(new ImageIcon(Registro.class.getResource("/imagenes/segur_1.png")));
		lblIconPass.setBounds(20, 310, 20, 24);
		panel.add(lblIconPass);
		
		// =================================================================
		// ACCIONES DE BOTONES (PROCESO DE REGISTRO)
		// =================================================================
		btnRegistrar = new JButton(Idioma.get("registro.btnRegistrar"));
		btnRegistrar.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
		btnRegistrar.setBackground(colorCafeCalido);
		btnRegistrar.setForeground(Color.WHITE);
		btnRegistrar.setBorder(null);
		btnRegistrar.setFocusable(false);
		btnRegistrar.setBounds(100, 470, 150, 32);
		btnRegistrar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Captura todos los valores ingresados por el usuario
				String nom = getTxtNombre();
				String ape = getTxtApellido();
				String dir = getTxtDireccion();
				String tel = getTxtTelefono();
				String fechaNac = getTxtFechaNacimiento();
				String user = getTxtUsuario();
				String correo = getTxtCorreo();
				String pass = getTxtContrasenia();
				String confirmPass = getTxtConfirmar();

				// VALIDACIONES FORMALES DE FORMULARIO (Post-Submit)
				if (nom.isEmpty() || ape.isEmpty() || dir.isEmpty() || tel.isEmpty() || fechaNac.isEmpty() || user.isEmpty() || correo.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
					JOptionPane.showMessageDialog(null, Idioma.get("registro.msg.camposObligatorios"), Idioma.get("comun.errorRegistro"), JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (tel.length() != 10) {
					JOptionPane.showMessageDialog(null, Idioma.get("registro.msg.telefonoInvalido"), Idioma.get("comun.errorRegistro"), JOptionPane.ERROR_MESSAGE);
					return;
				}
				// Validación del correo usando la clase Validador
				if (!Validador.correoValido(correo)) {
					JOptionPane.showMessageDialog(null, Idioma.get("usuarios.msg.correoInvalido"), Idioma.get("comun.errorRegistro"), JOptionPane.ERROR_MESSAGE);
					return;
				}
				// Validación del formato de la fecha dd/MM/yyyy
				if (!fechaNac.matches("\\d{2}/\\d{2}/\\d{4}")) {
					JOptionPane.showMessageDialog(null, Idioma.get("registro.msg.fechaInvalida"), Idioma.get("comun.errorRegistro"), JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (pass.length() < 8) {
					JOptionPane.showMessageDialog(null, Idioma.get("registro.msg.contrasenaCorta"), Idioma.get("comun.errorRegistro"), JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (!pass.equals(confirmPass)) {
					JOptionPane.showMessageDialog(null, Idioma.get("registro.msg.contrasenasNoCoinciden"), Idioma.get("comun.errorRegistro"), JOptionPane.ERROR_MESSAGE);
					return;
				}

				// SI TODO ES CORRECTO: Transfiere los datos al controlador para ser guardados
				controlador.registrarNuevoUsuario(nom, ape, dir, tel, correo, fechaNac, user, pass, confirmPass);
			}
		});
		VentanaRegistro.add(btnRegistrar);
		
		btnCancelar = new JButton(Idioma.get("registro.btnCancelar"));
		btnCancelar.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
		btnCancelar.setBackground(Color.WHITE);
		btnCancelar.setForeground(colorCafeOscuro);
		btnCancelar.setBorder(new LineBorder(colorBordeSuave, 1));
		btnCancelar.setFocusable(false);
		btnCancelar.setBounds(270, 470, 150, 32);
		btnCancelar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controlador.cancelarRegistro();
			}
		});
		VentanaRegistro.add(btnCancelar);
		
		// LOGOTIPO DE LA CAFETERÍA E IMÁGENES DECORATIVAS
		JLabel lblLogo = new JLabel("");
		lblLogo.setIcon(new ImageIcon(Registro.class.getResource("/imagenes/logo.png")));
		lblLogo.setBounds(15, -5, 260, 120);
		VentanaRegistro.add(lblLogo);
		
		JLabel lblDecoracion = new JLabel("");
		lblDecoracion.setHorizontalAlignment(SwingConstants.RIGHT);
		lblDecoracion.setIcon(new ImageIcon(Registro.class.getResource("/imagenes/images-Photoroom23.png")));
		lblDecoracion.setBounds(330, 15, 190, 95);
		VentanaRegistro.add(lblDecoracion);
		EstiloUI.aplicar(this, null);
	}
	
	// =================================================================
	// MÉTODOS GETTERS: Exponen los valores a otras clases/controladores
	// =================================================================
	public String getTxtNombre() { return txtNombre.getText().trim(); }
	public String getTxtApellido() { return txtApellido.getText().trim(); }
	public String getTxtDireccion() { return txtDireccion.getText().trim(); }
	public String getTxtTelefono() { return txtTelefono.getText().trim(); }

	/**
	 * Formatea la fecha obtenida del JDateChooser al patrón "dd/MM/yyyy".
	 * Retorna una cadena vacía "" si el usuario no ha elegido ninguna fecha.
	 */
	public String getTxtFechaNacimiento() {
		java.util.Date fecha = dateChooserFechaNacimiento.getDate();
		if (fecha == null) return "";
		return new java.text.SimpleDateFormat("dd/MM/yyyy").format(fecha);
	}
	public String getTxtUsuario() { return txtUsuario.getText().trim(); }
	public String getTxtCorreo() { return txtCorreo.getText().trim(); }
	public String getTxtContrasenia() { return String.valueOf(txtContrasenia.getPassword()); }
	public String getTxtConfirmar() { return String.valueOf(txtConfirmar.getPassword()); }

	/**
	 * MÉTODOS DE LA INTERFAZ 'ActualizableIdioma'
	 * Actualiza el texto de los componentes si el sistema cambia de idioma dinámicamente.
	 */
	@Override
	public void actualizarTextos() {
		setTitle(Idioma.get("registro.tituloVentana"));
		lblNombre.setText(Idioma.get("registro.nombre"));
		lblApellido.setText(Idioma.get("registro.apellido"));
		lblTelefono.setText(Idioma.get("registro.telefono"));
		lblDireccion.setText(Idioma.get("registro.direccion"));
		lblFechaNacimiento.setText(Idioma.get("registro.fechaNacimiento"));
		lblUsuario.setText(Idioma.get("registro.usuario"));
		lblCorreo.setText(Idioma.get("registro.correo"));
		if (lblFoto.getIcon() == null) {
			lblFoto.setText(Idioma.get("registro.sinFoto"));
		}
		btnFoto.setText(Idioma.get("registro.btnCargarFoto"));
		lblContrasenia.setText(Idioma.get("registro.contrasena"));
		lblConfirmar.setText(Idioma.get("registro.confirmarContrasena"));
		btnRegistrar.setText(Idioma.get("registro.btnRegistrar"));
		btnCancelar.setText(Idioma.get("registro.btnCancelar"));
	}
}