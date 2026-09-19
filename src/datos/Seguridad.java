package datos;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilería para "hashear" (cifrar de forma irreversible) contraseñas antes de
 * guardarlas en la base de datos.
 * <p>
 * SIGEPAN nunca guarda contraseñas en texto plano: la columna {@code Usuarios.passwordHash}
 * almacena el resultado de {@link #hashContrasena(String)}, no la contraseña original.
 * Al iniciar sesión, {@link UsuarioDAO#autenticar(String, String)} vuelve a calcular el
 * hash de la contraseña que el usuario escribió y compara ambos textos; nunca se
 * "descifra" nada, porque SHA-256 es de un solo sentido (no se puede revertir).
 * <p>
 * Es una clase de utilería (no se instancia): todos sus miembros son estáticos.
 */
public final class Seguridad {

    /** Constructor privado: esta clase solo expone métodos estáticos. */
    private Seguridad() { }

    /**
     * Calcula el hash SHA-256 de un texto y lo entrega en hexadecimal mayúsculas
     * (64 caracteres), exactamente en el mismo formato que produce SQL Server con
     * {@code HASHBYTES('SHA2_256', ...)} en el script SIGEPAN_v2.sql. Esto es importante:
     * si el formato no coincidiera, un usuario creado desde SQL directamente no podría
     * iniciar sesión desde la aplicación (o viceversa).
     *
     * @param texto la contraseña en texto plano que el usuario escribió
     * @return el hash SHA-256 en hexadecimal (siempre 64 caracteres, A-F y 0-9)
     * @throws IllegalStateException si el algoritmo SHA-256 no está disponible en la
     *                                máquina virtual de Java (no debería ocurrir nunca en
     *                                una instalación estándar de Java, por eso se convierte
     *                                en una excepción no verificada en vez de obligar a
     *                                todo el proyecto a manejarla con try/catch)
     */
    public static String hashContrasena(String texto) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder resultado = new StringBuilder(64);
            for (byte valor : bytes) resultado.append(String.format("%02X", valor));
            return resultado.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no esta disponible.", e);
        }
    }
}
