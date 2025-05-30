package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;


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

    //private AffineTransform tr;
    //private AffineTransformOp trOp;
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

        // Nastavení počátečních hodnot
        image = null;

        // AffineTransform je potřeba dynamicky nastavovat, proto jej nemusíme inicializovat zde
//    tr = null;
        // Nastavení RenderingHints s důrazem na rychlost
        rHints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        rHints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        rHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        rHints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);

        // Nastavení výchozích rozměrů panelu
        setMinimumSize(new Dimension(640, 512));
        setMaximumSize(new Dimension(640, 512));
        setPreferredSize(new Dimension(640, 512));
    }// constructor

    public void setImage(BufferedImage img) {
        image = img;
    } // setImage

    @Override
    public void paintComponent(Graphics gc) {
        super.paintComponent(gc);
        Graphics2D g2d = (Graphics2D) gc;
        // Aplikace RenderingHints pro zajištění optimalizace
        g2d.setRenderingHints(rHints);
        // Vyplnění pozadí černou barvou
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        if (image != null) {
            int panelWidth = getWidth();
            int panelHeight = getHeight();
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();

            BufferedImage scaledImage = image; // Výchozí obraz, pokud není ScaleNx aktivní
            int renderWidth, renderHeight;

            if (fullscreenMode && scaleNx) {
                // Použití ScaleNx pro zvětšení
                scaledImage = applyScaleNx(image, panelWidth, panelHeight);
                renderWidth = scaledImage.getWidth();
                renderHeight = scaledImage.getHeight();
            } else {
                // Standardní škálování s využitím AffineTransform
                double scaleX = (double) panelWidth / imageWidth;
                double scaleY = (double) panelHeight / imageHeight;
                double scale = Math.min(scaleX, scaleY);

                renderWidth = (int) (imageWidth * scale);
                renderHeight = (int) (imageHeight * scale);

                // Použití AffineTransform pro akcelerované škálování
                AffineTransform transform = AffineTransform.getTranslateInstance(
                        (panelWidth - renderWidth) / 2.0,
                        (panelHeight - renderHeight) / 2.0
                );
                transform.scale(scale, scale);

                // Vykreslení obrazu s transformací
                g2d.drawImage(image, transform, null);
            }

            if (fullscreenMode && scaleNx) {
                // Pokud byl použit ScaleNx, vykreslíme obraz přímo
                int offsetX = (panelWidth - renderWidth) / 2;
                int offsetY = (panelHeight - renderHeight) / 2;
                g2d.drawImage(scaledImage, offsetX, offsetY, null);
            }

            if (fullscreenMode && scanlines) {
                addScanlines(g2d, (panelWidth - renderWidth) / 2, (panelHeight - renderHeight) / 2, renderWidth, renderHeight);
            }
        }
    }

    private BufferedImage applyScaleNx(BufferedImage input, int screenWidth, int screenHeight) {
        int width = input.getWidth();
        int height = input.getHeight();

        // Výpočet scale faktoru
        int scale = screenHeight / height;
        if (screenWidth / width < scale) {
            scale = screenWidth / width;
        }
        if (scale < 1) {
            scale = 1;
        }

        int newWidth = width * scale;
        int newHeight = height * scale;

        BufferedImage output = new BufferedImage(newWidth, newHeight, input.getType());

        WritableRaster inputRaster = input.getRaster();
        WritableRaster outputRaster = output.getRaster();

        int[] pixelE = new int[3]; // Střední pixel (RGB)
        int[] pixelA = new int[3]; // Horní pixel
        int[] pixelB = new int[3]; // Pravý pixel
        int[] pixelC = new int[3]; // Levý pixel
        int[] pixelD = new int[3]; // Dolní pixel

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Načtení aktuálního pixelu
                inputRaster.getPixel(x, y, pixelE);

                // Načtení okolních pixelů (nebo aktuálního, pokud není soused)
                inputRaster.getPixel(x, Math.max(0, y - 1), pixelA); // Horní pixel
                inputRaster.getPixel(Math.min(width - 1, x + 1), y, pixelB); // Pravý pixel
                inputRaster.getPixel(Math.max(0, x - 1), y, pixelC); // Levý pixel
                inputRaster.getPixel(x, Math.min(height - 1, y + 1), pixelD); // Dolní pixel

                // Každý pixel se zvětší na (scale x scale) čtverec
                for (int dy = 0; dy < scale; dy++) {
                    for (int dx = 0; dx < scale; dx++) {
                        if (dy == 0 && dx == 0) {
                            setPixel(outputRaster, x * scale + dx, y * scale + dy, (equals(pixelA, pixelC) && !equals(pixelA, pixelD) && !equals(pixelC, pixelB)) ? pixelA : pixelE);
                        } else if (dy == 0 && dx == scale - 1) {
                            setPixel(outputRaster, x * scale + dx, y * scale + dy, (equals(pixelA, pixelB) && !equals(pixelA, pixelD) && !equals(pixelB, pixelC)) ? pixelB : pixelE);
                        } else if (dy == scale - 1 && dx == 0) {
                            setPixel(outputRaster, x * scale + dx, y * scale + dy, (equals(pixelD, pixelC) && !equals(pixelD, pixelA) && !equals(pixelC, pixelB)) ? pixelC : pixelE);
                        } else if (dy == scale - 1 && dx == scale - 1) {
                            setPixel(outputRaster, x * scale + dx, y * scale + dy, (equals(pixelD, pixelB) && !equals(pixelD, pixelA) && !equals(pixelB, pixelC)) ? pixelD : pixelE);
                        } else {
                            setPixel(outputRaster, x * scale + dx, y * scale + dy, pixelE); // Střední část
                        }
                    }
                }
            }
        }

        return output;
    }

// Nastaví pixel do rastru
    private void setPixel(WritableRaster raster, int x, int y, int[] pixel) {
        raster.setPixel(x, y, pixel);
    }

// Porovná, zda dva pixely jsou stejné
    private boolean equals(int[] pixel1, int[] pixel2) {
        return pixel1[0] == pixel2[0] && pixel1[1] == pixel2[1] && pixel1[2] == pixel2[2];
    }

// Metoda pro přidání prokládání (scanlines)
    private BufferedImage scanlinePattern;

    private void generateScanlinePattern(int width, int height) {
        scanlinePattern = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scanlinePattern.createGraphics();

        g2d.setColor(new Color(0, 0, 0, 50)); // Tmavá barva s průhledností
        for (int y = 0; y < height; y += 4) { // Čáry každé 4 pixely
            g2d.fillRect(0, y, width, 2); // Čára o výšce 2 pixelů
        }

        g2d.dispose();
    }

    private void addScanlines(Graphics2D g2d, int offsetX, int offsetY, int width, int height) {
        if (scanlinePattern == null || scanlinePattern.getWidth() != width || scanlinePattern.getHeight() < height) {
            generateScanlinePattern(width, height);
        }
        g2d.drawImage(scanlinePattern, offsetX, offsetY, null);
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
