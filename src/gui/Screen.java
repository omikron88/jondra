package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;


/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author admin
 */
public class Screen extends javax.swing.JPanel {

    private boolean fullscreenMode = false; // Výchozí režim: normální
    private int ratio = 2; // Výchozí pomer zvetseni 2x
    private boolean scaleNx = false; // ScaleNx
    private boolean scanlines = false; // Výchozí režim: no scanlines
    private BufferedImage image;

    private AffineTransform tr;
    private AffineTransformOp trOp;
    private RenderingHints rHints;

    public void setScanlines(boolean bScan) {
        scanlines = bScan;
    }

    public void setFullScreen(boolean bFullScr) {
        fullscreenMode = bFullScr;
    }

    public void setScaleNx(boolean bScaleNx) {
        scaleNx = bScaleNx;
    }

    public boolean getScaleNx() {
        return scaleNx;
    }

    public boolean getFullscreenStatus() {
        return fullscreenMode;
    }

    public void setRatio(int nRat) {
        ratio = nRat;
    }

    public int getRatio() {
        return ratio;
    }

    public BufferedImage getImage() {
        return image;
    }

    /**
     * Creates new form JIQScreen
     */
    public Screen() {
        initComponents();

        image = null;

        tr = AffineTransform.getScaleInstance(2.0f, 2.0f);

        rHints = new RenderingHints(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        rHints.put(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);
        rHints.put(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        rHints.put(RenderingHints.KEY_COLOR_RENDERING,
                RenderingHints.VALUE_COLOR_RENDER_SPEED);

        trOp = new AffineTransformOp(tr, rHints);

        setMinimumSize(new Dimension(640, 512));
        setMaximumSize(new Dimension(640, 512));
        setPreferredSize(new Dimension(640, 512));
    } // constructor

    public void setImage(BufferedImage img) {
        image = img;
    } // setImage

    @Override
    public void paintComponent(Graphics gc) {        
        Graphics2D g2d = (Graphics2D) gc;

        // Vyplnění pozadí černou barvou
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        if (image != null) {
            // Rozměry panelu
            int panelWidth = getWidth();
            int panelHeight = getHeight();

            // Rozměry obrazu
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();

            BufferedImage scaledImage = null;
            int renderWidth, renderHeight;
            int offsetX, offsetY;

            if (fullscreenMode && scaleNx) {
                // Použij ScaleNx algoritmus
                scaledImage = applyScaleNx(image, panelWidth, panelHeight);                
                renderWidth = scaledImage.getWidth();
                renderHeight = scaledImage.getHeight();
            } else if (fullscreenMode) {
                // Fullscreen režim s klasickým škálováním
                double scaleX = (double) panelWidth / imageWidth;
                double scaleY = (double) panelHeight / imageHeight;
                double scale = Math.min(scaleX, scaleY); // Zachovat poměr stran

                renderWidth = (int) (imageWidth * scale);
                renderHeight = (int) (imageHeight * scale);
                scaledImage = image;
            } else {
                // Normální režim s pevně nastaveným ratio
                renderWidth = imageWidth * ratio;
                renderHeight = imageHeight * ratio;
                scaledImage = image;
            }

            // Vypočítání odsazení pro centrování obrazu
            offsetX = (panelWidth - renderWidth) / 2;
            offsetY = (panelHeight - renderHeight) / 2;

            // Vykreslení výsledného obrazu
            g2d.drawImage(scaledImage, offsetX, offsetY, renderWidth, renderHeight, null);

            if (fullscreenMode && scanlines) {
                addScanlines(g2d, offsetX, offsetY, renderWidth, renderHeight);
            }
        }
    }

    // Algoritmus ScaleNx
    private BufferedImage applyScaleNx(BufferedImage input, int screenWidth, int screenHeight) {
        int width = input.getWidth();
        int height = input.getHeight();

        // Výpočet scale faktoru
        int scale = Math.min(screenWidth / width, screenHeight / height);

        // Ujistíme se, že scale není menší než 1 (pro případ, že by byl obraz větší než okno)
        scale = Math.max(scale, 1);

        int newWidth = width * scale;
        int newHeight = height * scale;

        BufferedImage output = new BufferedImage(newWidth, newHeight, input.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int E = input.getRGB(x, y); // Aktuální pixel
                int A = (y > 0) ? input.getRGB(x, y - 1) : E; // Horní pixel
                int B = (x < width - 1) ? input.getRGB(x + 1, y) : E; // Pravý pixel
                int C = (x > 0) ? input.getRGB(x - 1, y) : E; // Levý pixel
                int D = (y < height - 1) ? input.getRGB(x, y + 1) : E; // Dolní pixel

                // Každý pixel se zvětší na (scale x scale) čtverec
                for (int dy = 0; dy < scale; dy++) {
                    for (int dx = 0; dx < scale; dx++) {
                        if (dy == 0 && dx == 0) {
                            output.setRGB(x * scale + dx, y * scale + dy, (A == C && A != D && C != B) ? A : E);
                        } else if (dy == 0 && dx == scale - 1) {
                            output.setRGB(x * scale + dx, y * scale + dy, (A == B && A != D && B != C) ? B : E);
                        } else if (dy == scale - 1 && dx == 0) {
                            output.setRGB(x * scale + dx, y * scale + dy, (D == C && D != A && C != B) ? C : E);
                        } else if (dy == scale - 1 && dx == scale - 1) {
                            output.setRGB(x * scale + dx, y * scale + dy, (D == B && D != A && B != C) ? D : E);
                        } else {
                            output.setRGB(x * scale + dx, y * scale + dy, E); // Střední část
                        }
                    }
                }
            }
        }

        return output;
    }
  

// Metoda pro přidání prokládání (scanlines)
    private void addScanlines(Graphics2D g2d, int offsetX, int offsetY, int width, int height) {
        g2d.setColor(new Color(0, 0, 0, 50)); // Tmavá barva s průhledností
        for (int y = offsetY; y < offsetY + height; y += 4) { // Čáry každé 4 pixely
            g2d.fillRect(offsetX, y, width, 2); // Čára o výšce 2 pixelů
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 396, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 296, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
