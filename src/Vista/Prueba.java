package Vista;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.border.LineBorder;
import javax.swing.SwingConstants;

import controladores.ControladorUsuario;
import idioma.ActualizableIdioma;
import idioma.Idioma;

/**
 * Ventana de inicio de sesión. Implementa {@link ActualizableIdioma} para que,
 * si el usuario cambia de idioma estando esta ventana abierta (poco común, ya
 * que el menú de idiomas vive en las ventanas internas, pero puede pasar si se
 * abre más de una ventana), sus textos también se actualicen en caliente.
 */
public class Prueba extends JFrame implements ActualizableIdioma {

	private static final long serialVersionUID = 1L;
	private JPanel Ventana1;

	private JTextField textField_Usuario;
	private JPasswordField textField_Contraseña;

	private JButton iniciar_sesion;

	// NUEVO: se vuelven campos de la clase para que actualizarTextos() pueda
	// refrescar su contenido cuando el usuario cambia de idioma.
	private JLabel lblIngresaSesionPara;
	private JLabel lblSubtitulo;
	private JLabel lblUsuario;
	private JLabel lblContrasenia;
	private JLabel Descripcion;
	private JLabel lblParaPanaderias;

	// [MVC - VISTA]: Declaración del Controlador de Usuario (C del MVC).
	private ControladorUsuario controlador;

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Prueba frame = new Prueba();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public Prueba() {
		// [MVC - VISTA]: Configuración inicial de la interfaz gráfica y textos
		// traducibles.
		setTitle(Idioma.get("login.titulo"));
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 620, 460);

		// Paleta de colores cálida y fresca de panadería
		Color colorFondo = EstiloUI.FONDO; // Crema suave
		Color colorCafeOscuro = EstiloUI.TEXTO; // Café espresso profundo
		Color colorCafeCalido = EstiloUI.ACENTO; // Café dorado para botones principales
		Color colorBordeSuave = EstiloUI.BORDE; // Tono arena sutil para la tarjeta
		Color colorGrisTexto = EstiloUI.SECUNDARIO; // Gris cálido elegante

		Ventana1 = new JPanel();
		Ventana1.setBackground(colorFondo);
		Ventana1.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(Ventana1);
		Ventana1.setLayout(null);

		// ===================================
		// PANEL LOGIN (Diseño de tarjeta moderna)
		// ===================================
		JPanel Login = new EstiloUI.Tarjeta();
		Login.setBackground(Color.WHITE);
		Login.setBorder(new LineBorder(colorBordeSuave, 1));
		Login.setBounds(275, 25, 305, 340);
		Ventana1.add(Login);
		Login.setLayout(null);

		lblIngresaSesionPara = new JLabel(Idioma.get("login.bienvenida"));
		lblIngresaSesionPara.setHorizontalAlignment(SwingConstants.CENTER);
		lblIngresaSesionPara.setFont(new Font("Segoe UI Semibold", Font.BOLD, 16));
		lblIngresaSesionPara.setForeground(colorCafeOscuro);
		lblIngresaSesionPara.setBounds(10, 25, 285, 25);
		Login.add(lblIngresaSesionPara);

		lblSubtitulo = new JLabel(Idioma.get("login.subtitulo"));
		lblSubtitulo.setHorizontalAlignment(SwingConstants.CENTER);
		lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		lblSubtitulo.setForeground(colorGrisTexto);
		lblSubtitulo.setBounds(10, 48, 285, 20);
		Login.add(lblSubtitulo);

		// CAMPOS: USUARIO
		lblUsuario = new JLabel(Idioma.get("login.usuario"));
		lblUsuario.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblUsuario.setForeground(colorCafeOscuro);
		lblUsuario.setBounds(45, 88, 112, 20);
		Login.add(lblUsuario);

		textField_Usuario = new JTextField();
		textField_Usuario.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		textField_Usuario.setBorder(new LineBorder(colorBordeSuave, 1));
		textField_Usuario.setBounds(45, 110, 215, 32);
		Login.add(textField_Usuario);
		textField_Usuario.setColumns(10);

		// CAMPOS: CONTRASEÑA
		lblContrasenia = new JLabel(Idioma.get("login.contrasena"));
		lblContrasenia.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
		lblContrasenia.setForeground(colorCafeOscuro);
		lblContrasenia.setBounds(45, 148, 112, 20);
		Login.add(lblContrasenia);

		textField_Contraseña = new JPasswordField();
		textField_Contraseña.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		textField_Contraseña.setBorder(new LineBorder(colorBordeSuave, 1));
		textField_Contraseña.setBounds(45, 170, 215, 32);
		Login.add(textField_Contraseña);

		// BOTÓN INICIAR SESIÓN (Estilo destacado)
		iniciar_sesion = new JButton(Idioma.get("login.btnIniciarSesion"));
		iniciar_sesion.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
		iniciar_sesion.setBackground(colorCafeCalido);
		iniciar_sesion.setForeground(Color.WHITE);
		iniciar_sesion.setBorder(null);
		iniciar_sesion.setFocusable(false);
		iniciar_sesion.setBounds(45, 225, 215, 38);
		Login.add(iniciar_sesion);

		// ICONOS DEL LOGIN (Ajustados con respecto a las cajas de texto grandes)
		JLabel lblIconUser = new JLabel("");
		lblIconUser.setIcon(new ImageIcon(Prueba.class.getResource("/imagenes/user_2.png")));
		lblIconUser.setBounds(16, 112, 20, 23);
		Login.add(lblIconUser);

		JLabel lblIconPass = new JLabel("");
		lblIconPass.setIcon(new ImageIcon(Prueba.class.getResource("/imagenes/segur_1.png")));
		lblIconPass.setBounds(16, 171, 20, 23);
		Login.add(lblIconPass);

		// ===================================
		// PANEL IZQUIERDO (Identidad Visual)
		// ===================================
		JLabel lblLogo =

				new JLabel("");
		lblLogo.setHorizontalAlignment(SwingConstants.CENTER);
		lblLogo.setIcon(new ImageIcon(Prueba.class.getResource("/imagenes/logo.png")));
		lblLogo.setBounds(30, 25, 211, 130);
		Ventana1.add(lblLogo);

		Descripcion = new JLabel(Idioma.get("login.descripcion"));
		Descripcion.setHorizontalAlignment(SwingConstants.CENTER);
		Descripcion.setFont(new Font("Segoe UI Semibold", Font.BOLD, 14));
		Descripcion.setForeground(colorCafeOscuro);
		Descripcion.setBounds(10, 140, 250, 25);
		Ventana1.add(Descripcion);

		lblParaPanaderias = new JLabel(Idioma.get("login.eslogan"));
		lblParaPanaderias.setHorizontalAlignment(SwingConstants.CENTER);
		lblParaPanaderias.setFont(new Font("Segoe UI", Font.ITALIC, 12));
		lblParaPanaderias.setForeground(colorGrisTexto);
		lblParaPanaderias.setBounds(10, 165, 250, 20);
		Ventana1.add(lblParaPanaderias);

		// Ilustración de Pan de Fondo
		JLabel lblPanFondo = new JLabel("");
		lblPanFondo.setHorizontalAlignment(SwingConstants.CENTER);
		lblPanFondo.setIcon(new ImageIcon(Prueba.class.getResource("/imagenes/pan_fonfo.png")));
		lblPanFondo.setBounds(10, 200, 250, 200);
		Ventana1.add(lblPanFondo);

		// =====================================
		// CONTROLADOR Y EVENTOS
		// =====================================
		// [MVC - CONTROLADOR]: Instanciación del controlador inyectando la vista actual
		// ('this').
		controlador = new ControladorUsuario(this);

		// [MVC - VISTA A CONTROLADOR]: Vinculación del evento del botón con el método
		// del controlador.
		iniciar_sesion.addActionListener(e -> controlador.iniciarSesion());
		EstiloUI.aplicar(this, null);
	}

	// [MVC - VISTA]: Métodos de acceso (Getters) para que el Controlador pueda
	// consultar los inputs.
	public String getUsuario() {
		return textField_Usuario.getText();
	}

	public String getContraseña() {
		return String.valueOf(textField_Contraseña.getPassword());
	}

	// [MVC - VISTA]: Método para manipular la interfaz ordenado por el controlador
	// tras una acción.
	public void limpiarCampos() {
		textField_Usuario.setText("");
		textField_Contraseña.setText("");
	}

	/**
	 * Vuelve a asignar todos los textos visibles de esta ventana en el idioma
	 * actualmente seleccionado. Se llama automáticamente desde
	 * {@link Idioma#cambiarIdioma}.
	 */
	@Override
	public void actualizarTextos() {
		setTitle(Idioma.get("login.titulo"));
		lblIngresaSesionPara.setText(Idioma.get("login.bienvenida"));
		lblSubtitulo.setText(Idioma.get("login.subtitulo"));
		lblUsuario.setText(Idioma.get("login.usuario"));
		lblContrasenia.setText(Idioma.get("login.contrasena"));
		iniciar_sesion.setText(Idioma.get("login.btnIniciarSesion"));
		Descripcion.setText(Idioma.get("login.descripcion"));
		lblParaPanaderias.setText(Idioma.get("login.eslogan"));
	}
}