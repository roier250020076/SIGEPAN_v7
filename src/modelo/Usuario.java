package modelo;

/**
 * Representa un empleado con acceso al sistema (fila de la tabla {@code Usuarios}).
 * <p>
 * NOTA (revisión general): antes existían un segundo constructor de 7 parámetros, un
 * campo {@code contrasenia} y su getter {@code getContrasenia()}, y un método
 * {@code getDirection()} duplicado de {@link #getDireccion()}. Ninguno de los tres se
 * usaba en ningún lado (la contraseña siempre se maneja como texto plano temporal en
 * el controlador y se hashea con {@link datos.Seguridad} antes de guardarse, nunca se
 * guarda dentro de este objeto), así que se retiraron por ser código muerto/repetido.
 */
public class Usuario {
	private int idUsuario;
	private String nombre;
	private String apellido;
	private String direccion;
	private String telefono;
	private String fechaNacimiento;
	private String nombreUsuario;
	private String correo;
	private String rol;

	/**
	 * @param idUsuario       folio (0 si aún no se ha insertado en la base de datos)
	 * @param nombre          nombre(s) del empleado
	 * @param apellido        apellido(s) del empleado
	 * @param direccion       dirección del empleado
	 * @param telefono        teléfono a 10 dígitos
	 * @param correo          correo electrónico
	 * @param fechaNacimiento fecha de nacimiento en formato ISO ("aaaa-mm-dd"), como la entrega SQL Server
	 * @param nombreUsuario   nombre de usuario para iniciar sesión
	 * @param rol             "ADMIN" o "EMPLEADO"
	 */
	public Usuario(int idUsuario, String nombre, String apellido, String direccion, String telefono, String correo,
			String fechaNacimiento, String nombreUsuario, String rol) {
		this.idUsuario = idUsuario;
		this.nombre = nombre;
		this.apellido = apellido;
		this.direccion = direccion;
		this.telefono = telefono;
		this.correo = correo;
		this.fechaNacimiento = fechaNacimiento;
		this.nombreUsuario = nombreUsuario;
		this.rol = rol;
	}

	// Métodos GET (El controlador los necesita para leer la información y ponerla
	// en la JTable)
	public String getNombre() {
		return nombre;
	}

	public int getIdUsuario() { return idUsuario; }
	public String getCorreo() { return correo; }
	public String getRol() { return rol; }

	public String getApellido() {
		return apellido;
	}

	public String getDireccion() {
		return direccion;
	}

	public String getTelefono() {
		return telefono;
	}

	public String getFechaNacimiento() {
		return fechaNacimiento;
	}

	public String getNombreUsuario() {
		return nombreUsuario;
	}
}
