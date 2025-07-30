/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package a_lexico_robot;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Set;

public class PanelNumerosLinea extends JPanel {
    private JTextArea textArea;
    private Set<Integer> lineasConError = new HashSet<>();
    private Color colorNumeros = Color.GRAY;
    private Color colorError = new Color(200, 0, 0); // Rojo oscuro

    public PanelNumerosLinea() {
        setPreferredSize(new Dimension(40, 0));
        setBackground(new Color(240, 240, 240)); // Fondo gris claro
    }

    public void setTextArea(JTextArea textArea) {
        this.textArea = textArea;

        // Cuando el documento cambie (escritura, borrado, etc.), repinta los números
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                repaint();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                repaint();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                repaint();
            }
        });

        // Cuando se haga scroll vertical, repinta los números
        JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, textArea);
        if (scroll != null) {
            scroll.getVerticalScrollBar().addAdjustmentListener(e -> repaint());
        }
    }

    public void setLineasConError(Set<Integer> lineasConError) {
        this.lineasConError = new HashSet<>(lineasConError); // Copia defensiva
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (textArea == null) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        FontMetrics fm = g2d.getFontMetrics(textArea.getFont());
        int fontHeight = fm.getHeight();
        int baseLine = fm.getAscent();

        Element root = textArea.getDocument().getDefaultRootElement();

        // Calcular el rango visible de líneas
        int startOffset = textArea.viewToModel(new Point(0, 0));
        int endOffset = textArea.viewToModel(new Point(0, textArea.getHeight()));

        int startLine = root.getElementIndex(startOffset);
        int endLine = root.getElementIndex(endOffset);

        for (int i = startLine; i <= endLine && i < root.getElementCount(); i++) {
            try {
                Rectangle lineRect = textArea.modelToView(root.getElement(i).getStartOffset());
                if (lineRect == null) continue;

                String lineNumber = String.valueOf(i + 1);
                int x = getWidth() - fm.stringWidth(lineNumber) - 5;
                int y = lineRect.y + baseLine;

                // Resaltar líneas con error
                if (lineasConError.contains(i + 1)) {
                    g2d.setColor(colorError);
                    g2d.setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    g2d.setColor(colorNumeros);
                    g2d.setFont(getFont().deriveFont(Font.PLAIN));
                }

                g2d.drawString(lineNumber, x, y);
            } catch (Exception e) {
                // Ignorar errores de renderizado
            }
        }
    }
}
