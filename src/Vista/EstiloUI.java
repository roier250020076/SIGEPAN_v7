package Vista;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicMenuUI;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;

/** Presentacion compartida. No modifica modelos, acciones ni valores de los campos. */
public final class EstiloUI {
    public static final Color FONDO = new Color(245, 242, 236);
    public static final Color TEXTO = new Color(49, 43, 38);
    public static final Color ACENTO = new Color(139, 72, 39);
    public static final Color BORDE = new Color(221, 215, 204);
    public static final Color SECUNDARIO = new Color(109, 101, 92);
    private static final Color SUAVE = new Color(246, 237, 225);
    private static final Color PELIGRO = new Color(168, 46, 46);
    private static final Color SELECCION = new Color(230, 211, 187);

    private EstiloUI() { }

    /** Se invoca al terminar de construir la vista, antes de registrar sus medidas. */
    public static void aplicar(JFrame ventana, JMenu activo) {
        ventana.getContentPane().setBackground(FONDO);
        recorrer(ventana.getContentPane());
        JMenuBar barra = ventana.getJMenuBar();
        if (barra != null) {
            // Las coordenadas de WindowBuilder describen el contenido, no el marco.
            // Reserva tambien el alto del menu para no recortar las acciones inferiores.
            ventana.getContentPane().setPreferredSize(new Dimension(ventana.getWidth(), ventana.getHeight()));
            barra.setBackground(Color.WHITE);
            barra.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDE),
                    new EmptyBorder(6, 4, 6, 4)));
            for (int i = 0; i < barra.getMenuCount(); i++) {
                JMenu menu = barra.getMenu(i);
                if (menu == null) continue;
                menu.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                menu.setBorder(new EmptyBorder(7, 7, 7, 7));
                menu.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                menu.setUI(new BasicMenuUI() {
                    @Override protected void paintBackground(Graphics g, JMenuItem item, Color bg) {
                        Color anterior = g.getColor();
                        boolean resaltado = item == activo || item.getModel().isSelected()
                                || item.getModel().isRollover();
                        g.setColor(resaltado ? SUAVE : Color.WHITE);
                        g.fillRoundRect(0, 0, item.getWidth(), item.getHeight(), 10, 10);
                        if (item == activo) {
                            g.setColor(ACENTO);
                            g.fillRoundRect(7, item.getHeight() - 3, item.getWidth() - 14, 3, 3, 3);
                        }
                        g.setColor(anterior);
                    }
                    @Override protected void paintText(Graphics g, JMenuItem item, Rectangle r, String texto) {
                        g.setColor(item.isEnabled() ? item.getForeground() : SECUNDARIO);
                        javax.swing.plaf.basic.BasicGraphicsUtils.drawStringUnderlineCharAt(
                                g, texto, item.getDisplayedMnemonicIndex(), r.x, r.y + g.getFontMetrics().getAscent());
                    }
                });
                menu.setRolloverEnabled(true);
                menu.addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { menu.getModel().setRollover(true); }
                    @Override public void mouseExited(MouseEvent e) { menu.getModel().setRollover(false); }
                });
            }
            ventana.pack();
        }
    }

    private static void recorrer(Container contenedor) {
        for (Component c : contenedor.getComponents()) {
            if (c.getFont() != null) {
                Font f = c.getFont();
                c.setFont(new Font("Segoe UI", f.getStyle(), f.getSize()));
            }
            if (c instanceof JButton) {
                boton((JButton) c);
            } else if (c instanceof JTextComponent) {
                JTextComponent campo = (JTextComponent) c;
                campo.setForeground(TEXTO);
                campo.setCaretColor(ACENTO);
                campo.setSelectionColor(SELECCION);
                campo.setSelectedTextColor(TEXTO);
                campo.setBackground(campo.isEditable() ? Color.WHITE : SUAVE);
                campo.setBorder(new BordeRedondo(8, 7));
                campo.addFocusListener(new FocusAdapter() {
                    @Override public void focusGained(FocusEvent e) { campo.repaint(); }
                    @Override public void focusLost(FocusEvent e) { campo.repaint(); }
                });
            } else if (c instanceof JTable) {
                JTable tabla = (JTable) c;
                tabla.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                tabla.setRowHeight(32);
                tabla.setShowVerticalLines(false);
                tabla.setShowHorizontalLines(true);
                tabla.setGridColor(FONDO);
                tabla.setIntercellSpacing(new Dimension(0, 1));
                tabla.setSelectionBackground(SELECCION);
                tabla.setSelectionForeground(TEXTO);
                tabla.setFillsViewportHeight(true);
                tabla.getTableHeader().setPreferredSize(new Dimension(0, 36));
                TableCellRenderer anterior = tabla.getTableHeader().getDefaultRenderer();
                tabla.getTableHeader().setDefaultRenderer((t, valor, seleccionado, foco, fila, columna) -> {
                    Component cabecera = anterior.getTableCellRendererComponent(t, valor, seleccionado, foco, fila, columna);
                    cabecera.setBackground(SUAVE);
                    cabecera.setForeground(TEXTO);
                    cabecera.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    if (cabecera instanceof JComponent) {
                        ((JComponent) cabecera).setBorder(new EmptyBorder(5, 6, 5, 6));
                        ((JComponent) cabecera).setOpaque(true);
                    }
                    return cabecera;
                });
            } else if (c instanceof JScrollPane) {
                JScrollPane scroll = (JScrollPane) c;
                scroll.setBorder(BorderFactory.createLineBorder(BORDE));
                scroll.getViewport().setBackground(Color.WHITE);
                recorrer(scroll.getViewport());
            } else if (c instanceof JPanel && ((JPanel) c).getLayout() == null) {
                recorrer((Container) c);
            } else if (c instanceof JTabbedPane) {
                c.setBackground(SUAVE);
                c.setForeground(TEXTO);
                recorrer((Container) c);
            } else if (c instanceof JComboBox || c instanceof JSpinner) {
                c.setBackground(Color.WHITE);
                c.setForeground(TEXTO);
                // Conserva renderers, editores y botones internos del componente.
            } else if (c instanceof JSeparator) {
                c.setForeground(BORDE);
                c.setBackground(Color.WHITE);
            }
        }
    }

    private static boolean esRojo(Color color) {
        return color != null && color.getRed() > color.getGreen() * 2
                && color.getRed() > color.getBlue() * 2;
    }

    private static void boton(JButton boton) {
        boolean principal = Color.WHITE.equals(boton.getForeground());
        boolean peligro = esRojo(boton.getForeground()) || esRojo(boton.getBackground());
        Color tinta = peligro ? PELIGRO : ACENTO;
        boton.setBackground(principal ? tinta : Color.WHITE);
        boton.setForeground(principal ? Color.WHITE : tinta);
        boton.setOpaque(false);
        boton.setContentAreaFilled(false);
        boton.setRolloverEnabled(true);
        boton.setFocusable(true);
        boton.setFocusPainted(true);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setBorder(new EmptyBorder(3, 6, 3, 6));
        boton.setUI(new BasicButtonUI() {
            @Override public void paint(Graphics g, JComponent c) {
                AbstractButton b = (AbstractButton) c;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fondo = b.getBackground();
                if (!b.isEnabled()) fondo = BORDE;
                else if (b.getModel().isPressed()) fondo = principal ? tinta.darker() : SELECCION;
                else if (b.getModel().isRollover()) fondo = principal ? tinta.darker() : SUAVE;
                g2.setColor(fondo);
                g2.fillRoundRect(1, 1, c.getWidth() - 3, c.getHeight() - 3, 12, 12);
                g2.setColor(b.hasFocus() ? ACENTO : principal ? fondo : BORDE);
                g2.setStroke(new BasicStroke(b.hasFocus() ? 2f : 1f));
                g2.drawRoundRect(1, 1, c.getWidth() - 3, c.getHeight() - 3, 12, 12);
                g2.dispose();
                super.paint(g, c);
            }
        });
    }

    /** Tarjeta compatible con los paneles de posicionamiento libre de WindowBuilder. */
    public static class Tarjeta extends JPanel {
        private static final long serialVersionUID = 1L;
        public Tarjeta() { this(new FlowLayout()); }
        public Tarjeta(LayoutManager layout) { super(layout); setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(232, 225, 214));
            g2.fillRoundRect(1, 3, getWidth() - 2, getHeight() - 3, 18, 18);
            g2.setColor(getBackground());
            g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 4, 18, 18);
            g2.setColor(BORDE);
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 4, 18, 18);
            g2.dispose();
        }
        @Override protected void paintBorder(Graphics g) { /* Borde pintado con la tarjeta. */ }
    }

    /** Mantiene los renderers existentes (incluidas fotos y tipos de columna). */
    public static class Tabla extends JTable {
        private static final long serialVersionUID = 1L;
        public Tabla() { super(); }
        public Tabla(TableModel modelo) { super(modelo); }
        @Override public Component prepareRenderer(TableCellRenderer renderer, int fila, int columna) {
            Component c = super.prepareRenderer(renderer, fila, columna);
            c.setBackground(isCellSelected(fila, columna) ? getSelectionBackground()
                    : fila % 2 == 0 ? Color.WHITE : new Color(250, 248, 244));
            c.setForeground(isCellSelected(fila, columna) ? getSelectionForeground() : TEXTO);
            return c;
        }
    }

    private static class BordeRedondo extends AbstractBorder {
        private static final long serialVersionUID = 1L;
        private final int radio, margen;
        BordeRedondo(int radio, int margen) { this.radio = radio; this.margen = margen; }
        @Override public Insets getBorderInsets(Component c) { return new Insets(2, margen, 2, margen); }
        @Override public Insets getBorderInsets(Component c, Insets i) {
            i.set(2, margen, 2, margen); return i;
        }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.hasFocus() ? ACENTO : BORDE);
            g2.setStroke(new BasicStroke(c.hasFocus() ? 2f : 1f));
            g2.drawRoundRect(x + 1, y + 1, w - 3, h - 3, radio, radio);
            g2.dispose();
        }
    }
}
