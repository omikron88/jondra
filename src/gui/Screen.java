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

            if (scanlines) {
                // Merkur CRT effect (v okně i na celé obrazovce): mikrořádky se stopou paprsku
                double scale = Math.min((double) panelWidth / imageWidth, (double) panelHeight / imageHeight);
                renderWidth = (int) (imageWidth * scale);
                renderHeight = (int) (imageHeight * scale);
                g2d.drawImage(renderCrt(image, renderWidth, renderHeight),
                        (panelWidth - renderWidth) / 2, (panelHeight - renderHeight) / 2, null);
                return;
            }

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
                // (celočíselný posun, aby mikrořádky lícovaly s maskou scanlines)
                AffineTransform transform = AffineTransform.getTranslateInstance(
                        (panelWidth - renderWidth) / 2,
                        (panelHeight - renderHeight) / 2
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

// Scanlines: Ondra kreslí snímky neprokládaně, každý mikrořádek je na černobílé
// CRT obrazovce svislý profil 15 % černá / 70 % světle šedá / 15 % černá. Souvislý úsek
// rozsvícených bodů je stopa paprsku: pás s půlkruhovými konci (paprsek se rozsvítí
// a zhasne kulatou stopou). Kreslí se softwarově, vyhlazené podle pokrytí pixelu.
    private static final double SCAN_DARK = 0.15;     // černý okraj nahoře i dole (podíl mikrořádku)
    private static final int SCAN_GRAY = 225;         // rozsvícený bod: světle šedá místo bílé
    private static final int SCAN_SS = 8;             // převzorkování pro okraje stopy
    private BufferedImage crtImage;
    private int[] crtPixels;
    private int[] crtRowStart, crtRowLen, crtColStart; // bloky pixelů obrazovky pro mikrořádky a sloupce
    private float[][][] crtCap;                        // [výška bloku][řádek bloku][vzdálenost od konce úseku]
    private float[][] crtBand;                         // [výška bloku][řádek bloku] pokrytí uprostřed úseku
    private int crtW = -1, crtH = -1, crtSrcW = -1, crtSrcH = -1;
    private final int[] crtGray = new int[256];

    // rozdělení na bloky jako při zvětšení nejbližším sousedem (střed pixelu)
    private static int[] crtBlocks(int src, int dst, int[] len) {
        int[] start = new int[src + 1];
        double scale = (double) dst / src;
        int p = 0;
        for (int i = 0; i < src; i++) {
            start[i] = p;
            while (p < dst && (int) Math.floor((p + 0.5) / scale) == i) {
                p++;
            }
            if (len != null) {
                len[i] = p - start[i];
            }
        }
        start[src] = p;
        return start;
    }

    private void crtPrepare(int w, int h, int srcW, int srcH) {
        crtW = w; crtH = h; crtSrcW = srcW; crtSrcH = srcH;
        crtImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        crtPixels = ((java.awt.image.DataBufferInt) crtImage.getRaster().getDataBuffer()).getData();
        crtRowLen = new int[srcH];
        crtRowStart = crtBlocks(srcH, h, crtRowLen);
        crtColStart = crtBlocks(srcW, w, null);
        int maxN = 1;
        for (int n : crtRowLen) maxN = Math.max(maxN, n);
        crtCap = new float[maxN + 1][][];
        crtBand = new float[maxN + 1][];
        for (int n = 1; n <= maxN; n++) {
            double top = SCAN_DARK * n, bot = (1 - SCAN_DARK) * n;   // svítící pás v pixelech bloku
            double r = (bot - top) / 2, yc = n / 2.0;               // poloměr stopy
            int capW = (int) Math.ceil(r) + 1;
            crtBand[n] = new float[n];
            crtCap[n] = new float[n][capW];
            for (int t = 0; t < n; t++) {
                int in = 0;
                for (int sy = 0; sy < SCAN_SS; sy++) {
                    double y = t + (sy + 0.5) / SCAN_SS;
                    if (y >= top && y < bot) in++;
                }
                crtBand[n][t] = (float) in / SCAN_SS;
                for (int k = 0; k < capW; k++) {
                    int c = 0;
                    for (int sy = 0; sy < SCAN_SS; sy++) {
                        double y = t + (sy + 0.5) / SCAN_SS;
                        for (int sx = 0; sx < SCAN_SS; sx++) {
                            double x = k + (sx + 0.5) / SCAN_SS;        // vzdálenost od konce úseku
                            double dx = x < r ? r - x : 0, dy = y - yc;
                            if (dx * dx + dy * dy <= r * r) c++;
                        }
                    }
                    crtCap[n][t][k] = (float) c / (SCAN_SS * SCAN_SS);
                }
            }
        }
        for (int i = 0; i < 256; i++) {
            int v = Math.round(i / 255f * SCAN_GRAY);
            crtGray[i] = (v << 16) | (v << 8) | v;
        }
    }

    private BufferedImage renderCrt(BufferedImage src, int w, int h) {
        int sw = src.getWidth(), sh = src.getHeight();
        if (crtImage == null || w != crtW || h != crtH || sw != crtSrcW || sh != crtSrcH) {
            crtPrepare(w, h, sw, sh);
        }
        java.util.Arrays.fill(crtPixels, 0);
        boolean[] lit = new boolean[sw + 1];
        // obraz Ondry je 1bitový (TYPE_BYTE_BINARY): čti přímo bajty, MSB = levý bod
        byte[] bits = src.getType() == BufferedImage.TYPE_BYTE_BINARY
                ? ((java.awt.image.DataBufferByte) src.getRaster().getDataBuffer()).getData() : null;
        int stride = (sw + 7) / 8;
        for (int j = 0; j < sh; j++) {
            int n = crtRowLen[j];
            if (n == 0) continue;
            for (int x = 0; x < sw; x++) {
                lit[x] = bits != null ? ((bits[j * stride + (x >> 3)] >> (7 - (x & 7))) & 1) != 0
                        : (src.getRGB(x, j) & 0xFFFFFF) != 0;
            }
            lit[sw] = false;
            int x = 0;
            while (x < sw) {
                if (!lit[x]) { x++; continue; }
                int x0 = x;
                while (lit[x]) x++;
                int a = crtColStart[x0], b = crtColStart[x];         // úsek v pixelech obrazovky
                int width = b - a;
                for (int t = 0; t < n; t++) {
                    float[] cap = crtCap[n][t];
                    float band = crtBand[n][t];
                    int capW = cap.length;
                    int base = (crtRowStart[j] + t) * w;
                    for (int k = 0; k < width; k++) {
                        int dl = k, dr = width - 1 - k;
                        float al = dl < capW ? cap[dl] : band;
                        float ar = dr < capW ? cap[dr] : band;
                        float alpha = Math.min(al, ar);
                        if (alpha > 0) {
                            crtPixels[base + a + k] = crtGray[Math.round(alpha * 255)];
                        }
                    }
                }
            }
        }
        return crtImage;
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
