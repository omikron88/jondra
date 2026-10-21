package machine;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Tiskarna Tesla BT-100 (jednojehlova, bod po bodu) na portu 9 Ondry.
 *
 * Vystupy (port 9, uroven viz OUT_POL): bit 0 jehla, bit 2 vozik vpravo,
 * bit 4 posun papiru, bit 7 vozik vlevo.
 * Snimace (radek 0E009h v I/O okne, bity 0-3, uroven viz SENS_POL), zapojeni
 * podle skutecne tiskarny (ZX ovladac: D6 -> 0, D7 -> 1, D5 -> 2, D4 -> 3):
 *   bit 0 synchronizace tisku (ZX D6, aktivni v H) - aktivni krome uzkeho
 *         okenka na zacatku kazdych COORD_P bodu (-Dbt100.d0=ctverec:
 *         obdelnik s pulperiodou D0_HALF). Ovladac v okenku zacina tah vpravo,
 *         na konci dat pocita body do dalsiho okenka a zpetny tah zastavi
 *         v okenku pred zacatkem radku.
 *   bit 1 souradnice bodu (ZX D7, aktivni v L) - obdelnik, jedna perioda na
 *         jeden bod vozíku (aktivni v prvni polovine bodu)
 *   bit 2 doraz vozíku (ZX D5, aktivni v H) - vozik vlevo na dorazu
 * Draha vozíku X_MAX = 526 bodu (mereni), udery pri jizde vlevo dopadaji
 * o VULE = 2 body vpravo (ovladac to vyrovnava vychozi korekci).
 *   bit 3 posun papiru (ZX D4) - jeden impulz na jeden radek bodu
 * Snimace jsou na vstupech joysticku, ktere Ondra propojuje s datovymi vodici
 * matice klavesnice: snimac v L se objevi pri cteni kazdeho radku (Memory).
 * Model odpovida tomu, jak snimace pouziva ovladac (bt100.asm v SSM Word,
 * port ovladace "TISK BT" ze Spectra). Casy jsou v taktech pri 2 MHz,
 * pri TURBO se prepocitavaji (mechanika bezi v realnem case).
 */
public class BT100 {

    /** doba prejezdu vozíku o jeden bod */
    public static final double DOT_T = 4000;       // 2 ms (radek 64 znaku asi 12 s)
    /** rozjezd motoru vozíku (setrvacnost): po zapnuti stoji jeste tuto dobu */
    public static final double START_T = 3000;     // 1,5 ms
    /** doba posunu papiru o jeden radek bodu */
    public static final double FEED_T = 10000;     // 5 ms
    /** perioda snimace souradnic (bodu) */
    public static final int COORD_P = 8;
    /** okenko souradnic kolem kazdeho nasobku COORD_P: od -0,5 (ovladac ho na
     *  konci radku testuje v pulce periody, kdy je synchronizace v nule) do
     *  +0,45 (rozjezd z dorazu, pred prvni sestupnou hranou synchronizace);
     *  ovladac cte snimace asi po 0,2 bodu */
    public static final double COORD_PRED = 0.5, COORD_ZA = 0.45;
    /** dorazy vozíku */
    static final double X_MIN = -0.75, X_MAX = Double.parseDouble(System.getProperty("bt100.xmax", "526"));
    /** synchronizace tisku: obdelnik s pulperiodou D0_HALF bodu (mereni na skutecne
     *  tiskarne 2026-10-21: 52 zmen na 526 bodech); -Dbt100.d0=okna = uzka okenka
     *  po COORD_P bodech (puvodni model) */
    static final double D0_HALF = Double.parseDouble(System.getProperty("bt100.d0half", "10"));
    /** posun hran synchronizace tisku proti bodum (v bodech) */
    static final double D0_FAZE = Double.parseDouble(System.getProperty("bt100.d0faze", "0.3"));
    static final boolean D0_OKNA = "okna".equals(System.getProperty("bt100.d0", "okna"));
    /** sirka papiru v bodech */
    public static final int WIDTH = 600;

    private final Ondra m;
    private boolean enabled;
    /** vypinac tiskarny (okno papiru): vypnuta ma snimace v klidu (H), takze
     *  neruší klavesnici, a nereaguje na motory ani jehlu */
    private boolean power;
    private int out = 0xFF;
    private double x;           // poloha vozíku v bodech (0 = doraz)
    private double feed;        // faze posunu papiru (cele cislo = radek)
    private int row;            // radek papiru pod jehlou
    private long lastT;
    private long moveFrom;      // vozik se rozjede az v tomto case (setrvacnost)
    private final List<BitSet> paper = new ArrayList<>();
    private int maxRow;
    private volatile long changes;  // pocitadlo zmen (pro prekresleni okna)

    public BT100(Ondra machine) {
        m = machine;
    }

    public void configure(boolean en) {
        enabled = en;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public synchronized boolean isPowered() {
        return power;
    }

    /** zapnout / vypnout tiskarnu (vypinac v okne papiru) */
    public synchronized void setPower(boolean on, long t) {
        if (on == power) {
            return;
        }
        advance(t);
        power = on;
        if (on && (out & 0x84) != 0x84) {   // motor uz zapnuty: rozjede se ted
            moveFrom = t + (long) (START_T * m.getCpuMul());
        }
        changes++;
    }

    public synchronized void reset(long t) {
        lastT = t;
        out = 0xFF;
    }

    /** posun mechaniky do casu t podle zapnutych motoru */
    private void advance(long t) {
        if (t < lastT) {
            lastT = t;
        }
        if (!power) {
            lastT = t;
            return;
        }
        double dt = (double) (t - lastT) / m.getCpuMul();
        double dm = (double) (t - Math.max(lastT, moveFrom)) / m.getCpuMul();
        lastT = t;
        if (dm > 0 && (out & 0x04) == 0) {
            x = Math.min(X_MAX, x + dm / DOT_T);
        }
        if (dm > 0 && (out & 0x80) == 0) {
            x = Math.max(X_MIN, x - dm / DOT_T);
        }
        if ((out & 0x10) == 0) {
            double f = feed + dt / FEED_T;
            int rows = (int) Math.floor(f) - (int) Math.floor(feed);
            feed = f;
            if (rows > 0) {
                row += rows;
                changes++;
            }
        }
    }

    /**
     * Uroven aktivnich signalu podle dokumentace BT 100 (bit = 1: aktivni v H).
     * Vystupy: jehla L (sestupna hrana spousti monostabil 74121), vozik
     * vpravo L, posun papiru L, vozik vlevo L.
     * Snimace: aktivni v L, doraz vozíku v H (overeno na skutecne tiskarne
     * 2026-10-20) a synchronizace tisku v H (jako ji vidi ovladac na Spectru:
     * skoro stale aktivni, neaktivni v uzkych okenkach; 2026-10-21).
     * Uvnitr modelu jsou vystupy aktivni v nule a snimace v jednicce.
     */
    public static final int OUT_POL = 0x00, SENS_POL = 0x05;

    /** urovne bitu 0-3 radku 0E009h podle snimacu (bez tiskarny se nemeni) */
    public int sensorLines(long t) {
        if (!isPowered()) {
            return 0x0F;                    // vypnuta: vstupy v klidu (H)
        }
        return (sensors(t) ^ ~SENS_POL) & 0x0F;
    }

    /** zapis na port 9 */
    public synchronized void write(int v, long t) {
        if (!enabled) {
            return;
        }
        advance(t);
        int old = out;
        out = (v ^ OUT_POL) & 0xff;
        boolean wasRun = (old & 0x84) != 0x84, run = (out & 0x84) != 0x84;
        if (run && !wasRun) {
            moveFrom = t + (long) (START_T * m.getCpuMul());
        }
        if (power && (old & 0x01) != 0 && (out & 0x01) == 0) {
            strike();
        }
        if (old != out) {
            changes++;
        }
    }

    /** uder jehly: bod pod jehlou (nejblizsi cely bod) */
    /** ladici vypis uderu jehly (-Dbt100.debug) */
    static final boolean DEBUG = System.getProperty("bt100.debug") != null;

    /** vule / zpozdeni pri jizde vlevo: uder dopadne o VULE bodu vpravo (skutecna
     *  tiskarna potrebuje korekci zpetneho pruchodu 2, 2026-10-21) */
    static final double VULE = Double.parseDouble(System.getProperty("bt100.vule", "2"));

    private void strike() {
        int col = (int) Math.round((out & 0x80) == 0 ? x + VULE : x);
        if (DEBUG) {
            System.out.printf("[BT100 uder radek %d x=%.2f -> sloupec %d]%n", row, x, col);
        }
        if (col < 0 || col >= WIDTH) {
            return;
        }
        while (paper.size() <= row) {
            paper.add(new BitSet(WIDTH));
        }
        paper.get(row).set(col);
        maxRow = Math.max(maxRow, row);
        changes++;
    }

    /**
     * Snimace v case t: maska aktivnich bitu 0-3 (urovne na radku 0E009h
     * dava sensorLines).
     */
    public synchronized int sensors(long t) {
        if (!enabled) {
            return 0;
        }
        advance(t);
        int s = 0;
        double fr = x - Math.floor(x);
        if (fr < 0.5) {
            s |= 0x02;                      // souradnice bodu (impuls na bod)
        }
        if (D0_OKNA) {
            double cx = x - Math.round(x / COORD_P) * (double) COORD_P;
            if (cx < -COORD_PRED || cx >= COORD_ZA) {
                s |= 0x01;                  // synchronizace tisku (mimo okenko)
            }
        } else if (((long) Math.floor((x - D0_FAZE) / D0_HALF) & 1) == 0) {
            s |= 0x01;                      // synchronizace tisku (obdelnik)
        }
        if (x <= 0) {
            s |= 0x04;                      // doraz
        }
        double ff = feed - Math.floor(feed);
        if ((out & 0x10) == 0 && ff >= 0.3 && ff < 0.7) {
            s |= 0x08;                      // impulz posunu papiru
        }
        return s;
    }

    // ---- papir pro okno -------------------------------------------------

    public long getChanges() {
        return changes;
    }

    public synchronized int getRow() {
        return row;
    }

    public synchronized double getCarriage() {
        return x;
    }

    public synchronized boolean isNeedleDown() {
        return (out & 0x01) == 0;
    }

    /** pocet radku papiru, ktere je treba zobrazit */
    public synchronized int getPaperRows() {
        return Math.max(maxRow, row) + 1;
    }

    /** novy papir: prazdny, jehla na prvnim radku */
    public synchronized void newPaper() {
        paper.clear();
        maxRow = 0;
        row = 0;
        feed = Math.floor(feed);
        changes++;
    }

    public synchronized boolean dot(int r, int c) {
        return r < paper.size() && paper.get(r).get(c);
    }

    /**
     * Obrazek papiru: 1 bod = scale x scale pixelu (kulaty bod), bila
     * plocha, minimalne minRows radku.
     */
    public synchronized BufferedImage render(int scale, int minRows) {
        int rows = Math.max(getPaperRows(), minRows);
        BufferedImage im = new BufferedImage(WIDTH * scale, rows * scale, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, im.getWidth(), im.getHeight());
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(40, 40, 60));
        int d = Math.max(1, scale + scale / 3);
        for (int r = 0; r < paper.size(); r++) {
            BitSet b = paper.get(r);
            for (int c = b.nextSetBit(0); c >= 0; c = b.nextSetBit(c + 1)) {
                g.fillOval(c * scale, r * scale, d, d);
            }
        }
        g.dispose();
        return im;
    }
}
