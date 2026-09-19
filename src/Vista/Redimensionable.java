package Vista;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

public final class Redimensionable {

    // ALMACENAMIENTO DE MEMORIA
    // Guarda la posición y tamaño original (X, Y, Ancho, Alto) de cada botón, etiqueta, caja de texto, etc.
    private final Map<Component, Rectangle> boundsOriginales = new HashMap<>();
    
    // Guarda las dimensiones originales del panel raíz o tarjeta (ej. 1050x730 px)
    private final Map<Container, Dimension> tamanoOriginalContenedor = new HashMap<>();

    // Constructor privado para evitar que la clase se instancie directamente con "new"
    private Redimensionable() { }

    /**
     * MÉTODO PRINCIPAL: Activa el rediseño automático para una ventana.
     */
    public static void activar(Window ventana, Container contentPane, int anchoDiseno, int altoDiseno) {
        Redimensionable r = new Redimensionable();
        
        // 1. Guardar tamaño original del panel principal
        r.tamanoOriginalContenedor.put(contentPane, new Dimension(anchoDiseno, altoDiseno));
        
        // 2. Escanear y memorizar todos los componentes dentro de la ventana
        r.registrar(contentPane);

        // 3. Establece que la ventana NUNCA se pueda hacer más pequeña que el diseño original
        ventana.setMinimumSize(new Dimension(anchoDiseno, altoDiseno));

        // 4. Queda atento a cuando el usuario maximice o estire la ventana con el ratón
        contentPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                r.reajustar(contentPane); // Recalcula posiciones cuando cambia el tamaño
            }
        });
    }

    /**
     * MÉTODO RECURSIVO: Guarda las medidas originales de cada elemento de la interfaz.
     */
    private void registrar(Container padre) {
        for (Component hijo : padre.getComponents()) {
            // Registra la ubicación inicial que le diste en WindowBuilder
            boundsOriginales.put(hijo, hijo.getBounds());
            
            // Si el componente es un panel secundario que contiene más componentes dentro, también lo escanea
            if (esPanelDeLayoutLibre(hijo)) {
                Container hijoContenedor = (Container) hijo;
                tamanoOriginalContenedor.put(hijoContenedor, hijoContenedor.getSize());
                registrar(hijoContenedor); // Llamada recursiva hacia adentro
            }
        }
    }

    /**
     * MÉTODO MATEMÁTICO: Recalcula en tiempo real las coordenadas de cada componente.
     */
    private void reajustar(Container padre) {
        Dimension original = tamanoOriginalContenedor.get(padre);
        if (original == null || original.width <= 0 || original.height <= 0) return;

        // Calcula los factores multiplicadores (Proporción de crecimiento)
        double escalaX = padre.getWidth() / (double) original.width;
        double escalaY = padre.getHeight() / (double) original.height;

        // Recorre cada elemento y multiplica sus medidas originales por la escala
        for (Component hijo : padre.getComponents()) {
            Rectangle b = boundsOriginales.get(hijo);
            if (b == null) continue;
            
            // Asigna la nueva ubicación y tamaño recalculados
            hijo.setBounds(
                (int) Math.round(b.x * escalaX),
                (int) Math.round(b.y * escalaY),
                (int) Math.round(b.width * escalaX),
                (int) Math.round(b.height * escalaY)
            );
            
            // Si el hijo es un panel con más cosas dentro, también reajusta sus elementos
            if (esPanelDeLayoutLibre(hijo)) {
                reajustar((Container) hijo);
            }
        }
        
        // Refresca visualmente el panel
        padre.revalidate();
        padre.repaint();
    }

    /**
     * FILTRO DE SEGURIDAD:
     * Retorna 'true' solo si es un JPanel con layout nulo creado en WindowBuilder.
     * Ignora tablas (JTable), barras de desplazamiento (JScrollPane) o desplegables (JComboBox)
     * para no romper su funcionamiento interno.
     */
    private static boolean esPanelDeLayoutLibre(Component componente) {
        return componente instanceof JPanel panel && panel.getLayout() == null && panel.getComponentCount() > 0;
    }
}