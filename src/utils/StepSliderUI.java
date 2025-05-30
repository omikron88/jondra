//implementace skokoveho slideru pro jemne nastavovani kroku v timeline
package utils;


import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.plaf.basic.BasicSliderUI;


public class StepSliderUI extends BasicSliderUI  {

    private boolean dragging = false;

    public StepSliderUI(JSlider slider) {
        super(slider);
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);

        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragging = true;
                snapToClosest(e.getX());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragging = false;
                snapToClosest(e.getX());
                slider.repaint();
            }
        });

        c.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragging) {
                    snapToClosest(e.getX());
                    slider.repaint();
                }
            }
        });
    }

    private void snapToClosest(int mouseX) {
        int value = valueForXPosition(mouseX);

        // Zaokrouhlení na nejbližší tick
        int majorTickSpacing = slider.getMajorTickSpacing();
        if (majorTickSpacing > 0) {
            value = Math.round((float) value / majorTickSpacing) * majorTickSpacing;
        }

        slider.setValue(value);
        updateThumbLocation();
    }

    private void updateThumbLocation() {
        int value = slider.getValue();
        int trackLength = trackRect.width;
        int thumbPos = trackRect.x
                + (int) ((double) (value - slider.getMinimum())
                / (slider.getMaximum() - slider.getMinimum()) * trackLength);
        setThumbLocation(thumbPos - thumbRect.width / 2, trackRect.y + (trackRect.height - thumbRect.height) / 2);
    }

    @Override
    public void setThumbLocation(int x, int y) {
        super.setThumbLocation(x, y);
        slider.repaint(); // Zajistí překreslení slideru
    }

    @Override
    public void paintThumb(Graphics g) {
        // Zavoláme snapping logiku, abychom se ujistili, že thumb je na správném místě
        updateThumbLocation();

        // Pokud je thumb na správné pozici, vykreslíme standardní knob
        super.paintThumb(g);
    }

    @Override
    public void calculateThumbLocation() {
        updateThumbLocation();
    }

}