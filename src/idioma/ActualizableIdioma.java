package idioma;

/**
 * Contrato que debe implementar cualquier ventana (JFrame) de SIGEPAN que muestre
 * texto traducible (títulos, menús, botones, etiquetas, mensajes, etc.).
 * <p>
 * Cuando el usuario cambia de idioma desde el submenú "Configuración", el método
 * {@link Idioma#cambiarIdioma(java.util.Locale)} recorre TODAS las ventanas que
 * estén abiertas en ese momento (usando {@code java.awt.Window.getWindows()}, que es
 * parte estándar de AWT) y le llama {@link #actualizarTextos()} a cada una que
 * implemente esta interfaz. De esta forma el cambio de idioma se refleja de inmediato
 * en pantalla, sin necesidad de cerrar y volver a abrir la ventana ni reiniciar la app.
 * <p>
 * Cada clase que implemente esta interfaz debe, dentro de {@code actualizarTextos()},
 * volver a asignar el texto de cada componente visible leyéndolo con
 * {@link Idioma#get(String)} (o {@link Idioma#get(String, Object...)} si el texto
 * lleva datos variables, como un nombre o un folio).
 */
public interface ActualizableIdioma {

    /**
     * Vuelve a leer del {@link java.util.ResourceBundle} activo todos los textos
     * visibles de la ventana (título, menús, etiquetas, botones, etc.) y los
     * vuelve a asignar a sus componentes correspondientes.
     */
    void actualizarTextos();
}
