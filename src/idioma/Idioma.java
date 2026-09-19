package idioma;

import java.awt.Window;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Punto único de acceso a la internacionalización (i18n) de SIGEPAN.
 * <p>
 * Usa las clases estándar de Java para i18n: {@link Locale} y {@link ResourceBundle},
 * apoyadas en los archivos de propiedades {@code idioma/Mensajes_es.properties},
 * {@code idioma/Mensajes_en.properties} y {@code idioma/Mensajes_pt.properties}
 * (Español, English y Português). No se usan textos "quemados" en el código: toda
 * clase de Vista o Controlador que necesite mostrar texto al usuario debe pedirlo
 * aquí con {@link #get(String)}.
 * <p>
 * <b>Codificación UTF-8:</b> por compatibilidad histórica, {@link java.util.Properties}
 * lee archivos .properties asumiendo ISO-8859-1, lo que obligaría a escribir acentos
 * y eñes como escapes \\uXXXX (poco legible y fácil de equivocar a mano). Para poder
 * escribir los acentos tal cual en los archivos de recursos, aquí se usa un
 * {@link ResourceBundle.Control} personalizado que fuerza la lectura en UTF-8.
 * <p>
 * <b>Cómo funciona el cambio de idioma en caliente:</b> {@link #cambiarIdioma(Locale)}
 * reemplaza el {@link ResourceBundle} activo y luego recorre TODAS las ventanas que
 * el JVM tenga abiertas en ese momento ({@link Window#getWindows()}). A las que
 * implementen {@link ActualizableIdioma} se les llama {@code actualizarTextos()},
 * y ellas mismas vuelven a pintar sus propios textos. Así no hace falta reiniciar
 * la aplicación ni volver a abrir cada ventana para ver el nuevo idioma.
 */
public final class Idioma {

    /** Nombre base de los archivos de recursos: idioma/Mensajes_xx.properties */
    private static final String PAQUETE_BASE = "idioma.Mensajes";

    /**
     * Control personalizado que lee los .properties en UTF-8 en vez del ISO-8859-1
     * que usa Java por omisión. Es el enfoque recomendado en la documentación de
     * Oracle para evitar native2ascii y poder escribir acentos directamente.
     */
    private static final ResourceBundle.Control CONTROL_UTF8 = new ResourceBundle.Control() {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                ClassLoader loader, boolean reload) throws IOException {
            String nombreRecurso = toResourceName(toBundleName(baseName, locale), "properties");
            InputStream flujo = null;
            if (reload) {
                URL url = loader.getResource(nombreRecurso);
                if (url != null) {
                    URLConnection conexion = url.openConnection();
                    conexion.setUseCaches(false);
                    flujo = conexion.getInputStream();
                }
            } else {
                flujo = loader.getResourceAsStream(nombreRecurso);
            }
            if (flujo == null) return null;
            try (Reader lector = new InputStreamReader(flujo, StandardCharsets.UTF_8)) {
                return new PropertyResourceBundle(lector);
            }
        }
    };

    public static final Locale ESPANOL = Locale.of("es");
    public static final Locale INGLES = Locale.of("en");
    public static final Locale PORTUGUES = Locale.of("pt");

    /** Español es el idioma con el que arranca la aplicación por omisión. */
    private static Locale idiomaActual = ESPANOL;
    private static ResourceBundle bundle = ResourceBundle.getBundle(PAQUETE_BASE, idiomaActual, CONTROL_UTF8);

    private Idioma() {
        // Clase de utilería: no se instancia.
    }

    /** @return el {@link Locale} que la aplicación está usando actualmente. */
    public static Locale getIdiomaActual() {
        return idiomaActual;
    }

    /**
     * Obtiene el texto traducido asociado a una clave.
     *
     * @param clave identificador del texto dentro de los .properties (p. ej. "login.usuario")
     * @return el texto en el idioma actual; si la clave no existe en el archivo de
     *         propiedades, regresa la clave misma entre signos de interrogación
     *         (p. ej. "??login.usuario??") para que un texto faltante sea fácil de
     *         detectar durante el desarrollo en vez de lanzar una excepción.
     */
    public static String get(String clave) {
        try {
            return bundle.getString(clave);
        } catch (MissingResourceException ex) {
            return "??" + clave + "??";
        }
    }

    /**
     * Igual que {@link #get(String)}, pero para textos con datos variables dentro,
     * usando el formato estándar de {@link MessageFormat} (marcadores {0}, {1}, ...).
     * Ejemplo en el archivo de propiedades:
     * {@code pedidos.msg.guardadoExito=Pedido {0} guardado correctamente.}
     *
     * @param clave      identificador del texto dentro de los .properties
     * @param parametros valores que reemplazan a {0}, {1}, etc. dentro del texto
     * @return el texto ya formateado en el idioma actual
     */
    public static String get(String clave, Object... parametros) {
        return MessageFormat.format(get(clave), parametros);
    }

    /**
     * Cambia el idioma activo de toda la aplicación y actualiza en el acto cada
     * ventana que esté abierta en este momento.
     *
     * @param nuevoIdioma uno de {@link #ESPANOL}, {@link #INGLES} o {@link #PORTUGUES}
     */
    public static void cambiarIdioma(Locale nuevoIdioma) {
        if (nuevoIdioma == null || nuevoIdioma.equals(idiomaActual)) {
            return; // Nada que hacer si es el mismo idioma o un valor inválido
        }
        idiomaActual = nuevoIdioma;
        bundle = ResourceBundle.getBundle(PAQUETE_BASE, idiomaActual, CONTROL_UTF8);

        // Actualiza en caliente todas las ventanas actualmente abiertas de la aplicación.
        for (Window ventana : Window.getWindows()) {
            if (ventana.isDisplayable() && ventana instanceof ActualizableIdioma) {
                ((ActualizableIdioma) ventana).actualizarTextos();
            }
        }
    }

    /**
     * Convierte el menú "Configuración" en un submenú desplegable con las 3 opciones de
     * idioma (Español / English / Português), ya con sus eventos conectados a
     * {@link #cambiarIdioma(Locale)}.
     * <p>
     * Se centraliza aquí porque las 5 ventanas con barra de menú (Inicio, Pedidos,
     * Inventario, Usuarios y Reportes) necesitan exactamente el mismo submenú; así se
     * evita repetir la misma construcción de {@link JMenuItem} cinco veces.
     *
     * @param menuConfiguracion el JMenu "Configuración" de la ventana, ya creado y
     *                          agregado a su JMenuBar
     */
    public static void construirSubmenuIdiomas(JMenu menuConfiguracion) {
        JMenuItem itemEspanol = new JMenuItem(get("menu.configuracion.espanol"));
        JMenuItem itemIngles = new JMenuItem(get("menu.configuracion.ingles"));
        JMenuItem itemPortugues = new JMenuItem(get("menu.configuracion.portugues"));

        itemEspanol.addActionListener(e -> cambiarIdioma(ESPANOL));
        itemIngles.addActionListener(e -> cambiarIdioma(INGLES));
        itemPortugues.addActionListener(e -> cambiarIdioma(PORTUGUES));

        menuConfiguracion.removeAll(); // por si actualizarTextos() lo vuelve a construir
        menuConfiguracion.add(itemEspanol);
        menuConfiguracion.add(itemIngles);
        menuConfiguracion.add(itemPortugues);
    }
}
