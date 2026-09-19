package controladores;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.Pattern;
import javax.swing.JTextField;

/**
 * Utilería de validación reutilizada por todos los formularios de captura de la
 * aplicación (Registro, Pedidos, Inventario, Usuarios). Se divide en dos tipos de
 * ayuda:
 * <ul>
 *   <li><b>Filtros de teclado</b> ({@link KeyAdapter}): bloquean caracteres inválidos
 *       MIENTRAS el usuario escribe (por ejemplo, {@link #soloNumeros}), para que nunca
 *       llegue a existir texto inválido en el campo.</li>
 *   <li><b>Validaciones de "al enviar"</b> (como {@link #correoValido(String)}): revisan
 *       el valor completo cuando el usuario presiona el botón de guardar/registrar,
 *       para reglas que no se pueden aplicar tecla por tecla (formato de correo, rangos
 *       numéricos, etc.).</li>
 * </ul>
 * Es una clase de utilería (no se instancia): todos sus miembros son estáticos.
 */
public class Validador {

    /** Mismo patrón de correo usado en toda la aplicación (Pedidos y Usuarios), centralizado aquí para no repetirlo. */
    private static final Pattern PATRON_CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /**
     * Valida que solo se ingresen letras y espacios. Ideal para nombres y apellidos.
     */
    public static KeyAdapter soloLetras() {
        return new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isLetter(c) && c != KeyEvent.VK_SPACE) {
                    e.consume(); // Destruye el caracter
                }
            }
        };
    }

    /**
     * Valida que solo se ingresen números y limita la longitud máxima. Ideal para teléfonos.
     */
    public static KeyAdapter soloNumeros(JTextField campo, int limiteMaximo) {
        return new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c)) {
                    e.consume();
                    return;
                }
                if (campo.getText().trim().length() >= limiteMaximo) {
                    e.consume();
                }
            }
        };
    }

    /**
     * Bloquea el uso de la barra espaciadora. Ideal para nombres de usuario.
     */
    public static KeyAdapter sinEspacios() {
        return new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (c == KeyEvent.VK_SPACE) {
                    e.consume();
                }
            }
        };
    }

    /**
     * Filtra caracteres para direcciones mexicanas (letras, números, espacios, #, comunes).
     */
    public static KeyAdapter formatoDireccion() {
        return new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isLetterOrDigit(c) && c != KeyEvent.VK_SPACE && c != '#' && c != ',' && c != '.' && c != '-') {
                    e.consume();
                }
            }
        };
    }
    public static KeyAdapter limitarLongitudPassword(javax.swing.JPasswordField campo, int limiteMaximo) {
        return new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (String.valueOf(campo.getPassword()).length() >= limiteMaximo) {
                    e.consume();
                }
            }
        };
        
      
     }

    /**
     * Filtra caracteres para fechas con formato dd/mm/aaaa.
     * Solo permite dígitos y el separador '/', y limita el largo total a 10
     * caracteres (2+1+2+1+4 = 10), que es justo lo que mide "dd/mm/aaaa".
     */
    public static KeyAdapter soloFecha(JTextField campo) {
        return new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) && c != '/') {
                    e.consume();
                    return;
                }
                if (campo.getText().trim().length() >= 10) {
                    e.consume();
                }
            }
        };
    }

    /**
     * Valida que un correo tenga un formato razonable ("algo@algo.algo"), sin espacios.
     * NUEVO (revisión de validaciones): antes ControladorPedido y ControladorUsuario
     * repetían por su cuenta la misma expresión regular; se centraliza aquí para que un
     * futuro ajuste al criterio de validación solo se tenga que hacer en un lugar.
     * <p>
     * No es una validación exhaustiva de RFC 5322 (eso requeriría una expresión mucho
     * más compleja y en la práctica no evita que alguien escriba un correo que no le
     * pertenece); solo detecta los errores de formato más comunes al capturar a mano.
     *
     * @param correo el texto capturado por el usuario
     * @return {@code true} si el formato es válido; {@code false} si es null o no cumple el patrón
     */
    public static boolean correoValido(String correo) {
        return correo != null && PATRON_CORREO.matcher(correo.trim()).matches();
    }

}