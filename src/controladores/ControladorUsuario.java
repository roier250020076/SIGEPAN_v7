package controladores;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import Vista.Inicio;
import Vista.Prueba;
import Vista.Registro;
import Vista.usuarios;
import datos.UsuarioDAO;
import idioma.Idioma;
import modelo.DatosGlobales;
import modelo.Usuario;

/**
 * Controlador para la autenticación y administración de usuarios respaldadas por SQL Server.
 */
public class ControladorUsuario {

    // REFERENCIAS A LAS DISTINTAS VISTAS CON LAS QUE INTERACTÚA
    private Prueba ventanaLogin;
    private Registro ventanaRegistro;
    private usuarios vistaUsuarios;
    
    // CAPA DE DATOS Y LISTA DE ALMACENAMIENTO TEMPORAL
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private List<Usuario> usuariosMostrados = new ArrayList<>();

    // =========================================================================
    // CONSTRUCTORES SOBRECARGADOS (Según la pantalla desde donde se invoque)
    // =========================================================================
    
    /** Constructor para el inicio de sesión (Login) */
    public ControladorUsuario(Prueba ventanaLogin) { 
        this.ventanaLogin = ventanaLogin; 
    }

    /** Constructor para registro simple */
    public ControladorUsuario(Registro ventanaRegistro) { 
        this.ventanaRegistro = ventanaRegistro; 
    }

    /** Constructor para registro abierto desde la vista principal de Usuarios */
    public ControladorUsuario(Registro ventanaRegistro, usuarios origenUsuarios) { 
        this.ventanaRegistro = ventanaRegistro; 
        this.vistaUsuarios = origenUsuarios; 
    }

    /** Constructor para la pantalla principal de administración de Usuarios */
    public ControladorUsuario(usuarios vistaUsuarios) {
        this.vistaUsuarios = vistaUsuarios;
        actualizarTablaUsuarios(); // Carga la tabla inmediatamente al abrir
        
        // Asigna el evento al botón Eliminar/Desactivar
        vistaUsuarios.getBtnEliminar().addActionListener(e -> eliminarUsuario());
    }

    // =========================================================================
    // LÓGICA DE NEGOCIO Y OPERACIONES
    // =========================================================================

    /**
     * Consulta la base de datos y llena el modelo de la tabla en la vista 'usuarios'.
     */
    public void actualizarTablaUsuarios() {
        if (vistaUsuarios == null) return;
        try {
            usuariosMostrados = usuarioDAO.listarActivos();
            DefaultTableModel modelo = vistaUsuarios.getModelo();
            modelo.setRowCount(0); // Limpia las filas previas
            
            // Recorre los usuarios y los agrega fila por fila a la tabla visual
            for (Usuario usuario : usuariosMostrados) {
                modelo.addRow(new Object[] { 
                    usuario.getNombre(), 
                    usuario.getApellido(), 
                    usuario.getDireccion(), 
                    usuario.getTelefono(), 
                    usuario.getCorreo(), 
                    usuario.getFechaNacimiento(), 
                    usuario.getNombreUsuario(), 
                    usuario.getRol() 
                });
            }
            // Actualiza el contador dinámico en la vista
            vistaUsuarios.setTotalUsuariosMostrado(usuariosMostrados.size());
        } catch (SQLException ex) {
            mostrarError(vistaUsuarios, ex, Idioma.get("usuarios.msg.errorCargarLista"));
        }
    }

    /**
     * Valida permisos de Administrador antes de abrir el formulario de Alta de Empleado.
     */
    public void abrirRegistroDesdeUsuarios() {
        if (!"ADMIN".equals(DatosGlobales.rolUsuarioActual)) {
            JOptionPane.showMessageDialog(
                vistaUsuarios, 
                Idioma.get("usuarios.msg.soloAdmin"), 
                Idioma.get("comun.accesoDenegado"), 
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        Registro ventana = new Registro(vistaUsuarios);
        vistaUsuarios.setVisible(false);
        ventana.setVisible(true);
    }

    /**
     * Valida y procesa la creación de un nuevo empleado en el sistema.
     */
    public void registrarNuevoUsuario(String nombre, String apellido, String direccion, String telefono, String correo,
            String fechaTexto, String nombreUsuario, String contrasena, String confirmacion) {
        
        // 1. Validación de campos obligatorios y longitud del teléfono
        if (nombre.isBlank() || apellido.isBlank() || direccion.isBlank() || telefono.length() != 10 || correo.isBlank() || nombreUsuario.isBlank() || contrasena.isBlank()) {
            JOptionPane.showMessageDialog(ventanaRegistro, Idioma.get("usuarios.msg.datosIncompletos"), Idioma.get("comun.datosInvalidos"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 2. Validación de sintaxis del correo electrónico
        if (!Validador.correoValido(correo)) {
            JOptionPane.showMessageDialog(ventanaRegistro, Idioma.get("usuarios.msg.correoInvalido"), Idioma.get("comun.datosInvalidos"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 3. Validación de longitud y confirmación de la contraseña
        if (contrasena.length() < 8 || !contrasena.equals(confirmacion)) {
            JOptionPane.showMessageDialog(ventanaRegistro, Idioma.get("usuarios.msg.contrasenaInvalida"), Idioma.get("comun.datosInvalidos"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // Convierte la fecha del formato dd/MM/yyyy al formato estándar de BD (yyyy-MM-dd)
            String fecha = LocalDate.parse(fechaTexto, DateTimeFormatter.ofPattern("dd/MM/uuuu")).toString();
            
            // Instancia el modelo Usuario con el rol por defecto EMPLEADO
            Usuario usuario = new Usuario(0, nombre.trim(), apellido.trim(), direccion.trim(), telefono, correo.trim(), fecha, nombreUsuario.trim(), "EMPLEADO");
            
            // Guarda en la BD mediante el DAO
            usuarioDAO.insertar(usuario, contrasena);
            
            JOptionPane.showMessageDialog(ventanaRegistro, Idioma.get("usuarios.msg.registradoExito"), Idioma.get("comun.exito"), JOptionPane.INFORMATION_MESSAGE);
            finalizarRegistro();
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(ventanaRegistro, Idioma.get("usuarios.msg.fechaInvalida"), Idioma.get("comun.datosInvalidos"), JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            mostrarError(ventanaRegistro, ex, Idioma.get("registro.msg.errorRegistrarEmpleado"));
        }
    }

    /** Cancela el proceso de registro y cierra la ventana */
    public void cancelarRegistro() { 
        finalizarRegistro(); 
    }

    /**
     * Valida las credenciales de inicio de sesión ingresadas en la ventana Login ('Prueba').
     */
    public void iniciarSesion() {
        try {
            Usuario usuario = usuarioDAO.autenticar(ventanaLogin.getUsuario(), ventanaLogin.getContraseña());
            
            if (usuario == null) {
                JOptionPane.showMessageDialog(ventanaLogin, Idioma.get("login.msg.credencialesInvalidas"), Idioma.get("comun.accesoDenegado"), JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Asigna las variables de sesión global
            DatosGlobales.idUsuarioActual = usuario.getIdUsuario();
            DatosGlobales.nombreUsuarioActual = usuario.getNombreUsuario();
            DatosGlobales.rolUsuarioActual = usuario.getRol();
            
            JOptionPane.showMessageDialog(ventanaLogin, Idioma.get("login.msg.bienvenida", usuario.getNombre()), Idioma.get("comun.exito"), JOptionPane.INFORMATION_MESSAGE);
            
            // Abre la pantalla de Inicio y destruye la pantalla de login
            new Inicio().setVisible(true);
            ventanaLogin.dispose();
        } catch (SQLException ex) {
            mostrarError(ventanaLogin, ex, Idioma.get("login.msg.errorIniciarSesion"));
        }
    }

    /**
     * Procesa la desactivación (baja lógica) de un empleado seleccionado en la JTable.
     */
    private void eliminarUsuario() {
        int filaVista = vistaUsuarios.getTablaUsuarios().getSelectedRow();
        if (filaVista < 0) {
            JOptionPane.showMessageDialog(vistaUsuarios, Idioma.get("usuarios.msg.seleccionaDesactivar"), Idioma.get("comun.advertencia"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Mapea el índice de la vista filtrada con el índice real del modelo
        int filaModelo = vistaUsuarios.getTablaUsuarios().convertRowIndexToModel(filaVista);
        Usuario usuario = usuariosMostrados.get(filaModelo);
        
        // REGLA DE SEGURIDAD: Evita que el usuario con sesión activa se desactive a sí mismo
        if (usuario.getIdUsuario() == DatosGlobales.idUsuarioActual) {
            JOptionPane.showMessageDialog(vistaUsuarios, Idioma.get("usuarios.msg.noAutoDesactivar"), Idioma.get("comun.advertencia"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Confirmación antes de aplicar cambios en BD
        if (JOptionPane.showConfirmDialog(
                vistaUsuarios, 
                Idioma.get("usuarios.msg.confirmarDesactivar", usuario.getNombreUsuario()), 
                Idioma.get("usuarios.msg.confirmarTitulo"), 
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try {
                usuarioDAO.desactivar(usuario.getIdUsuario());
                actualizarTablaUsuarios(); // Refresca la lista
            } catch (SQLException ex) {
                mostrarError(vistaUsuarios, ex, Idioma.get("usuarios.msg.errorDesactivar"));
            }
        }
    }

    /**
     * Cierra la ventana de registro y regresa a la vista origen adecuada.
     */
    private void finalizarRegistro() {
        if (ventanaRegistro != null) ventanaRegistro.dispose();
        if (vistaUsuarios != null) {
            vistaUsuarios.setVisible(true);
            actualizarTablaUsuarios();
        } else {
            new Prueba().setVisible(true);
        }
    }

    /**
     * Método auxiliar utilitario para desplegar cuadros de diálogo de error de BD estandarizados.
     */
    private void mostrarError(java.awt.Component componente, SQLException ex, String mensaje) {
        JOptionPane.showMessageDialog(
            componente, 
            Idioma.get("comun.msg.errorBaseDatos", mensaje, ex.getMessage()), 
            Idioma.get("comun.baseDeDatos"), 
            JOptionPane.ERROR_MESSAGE
        );
    }
}