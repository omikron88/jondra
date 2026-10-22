package machine;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Tiskarna na Centronics portu Ondry.
 *
 * Data jsou na portu 9 (portA1), tiskarna je prevezme pri sestupne hrane /STB
 * (bit 3 portu 0Ah) a zapise do souboru. BUSY (bit 5 na 0E00Fh v I/O okne) drzi
 * po kazdem bajtu kratky handshake; kdyz se zaplni buffer, ceka se, nez tiskarna
 * rychlosti CPS vytiskne, co se do bufferu nevejde. Cas se meri v taktech CPU,
 * pri TURBO se prepocita (tiskarna bezi v realnem case).
 */
public class Printer {

    /** velikost bufferu tiskarny v bajtech */
    public static final int BUFFER = 2048;
    /** rychlost tisku (znaky za sekundu) */
    public static final double CPS = 200.0;
    /** BUSY po kazdem bajtu: asi 100 us pri 2 MHz */
    static final long ACK_T = 200;

    private final Ondra m;
    private boolean enabled;
    private String path = "";
    private OutputStream out;
    private double queued;      // bajty v bufferu
    private long lastT;
    private long busyUntil;
    private long total;

    public Printer(Ondra machine) {
        m = machine;
    }

    /** zapnuti a vystupni soubor (pripisuje se na konec) */
    public void configure(boolean en, String file) {
        close();
        enabled = en && file != null && !file.isEmpty();
        path = file == null ? "" : file;
        reset(0);
    }

    public void reset(long t) {
        queued = 0;
        lastT = t;
        busyUntil = 0;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getTotal() {
        return total;
    }

    private void drain(long t) {
        if (t < lastT) {
            lastT = t;
        }
        double sec = (t - lastT) / (2e6 * m.getCpuMul());
        queued = Math.max(0, queued - sec * CPS);
        lastT = t;
    }

    /** sestupna hrana /STB: prevzit bajt */
    public void strobe(int data, long t) {
        if (!enabled) {
            return;
        }
        drain(t);
        try {
            if (out == null) {
                out = new FileOutputStream(path, true);
            }
            out.write(data & 0xff);
            out.flush();
            total++;
        } catch (IOException ex) {
            System.err.println("Tiskarna: nelze zapsat do " + path + ": " + ex.getMessage());
        }
        queued += 1;
        long mul = m.getCpuMul();
        busyUntil = t + ACK_T * mul;
        if (queued > BUFFER) {
            busyUntil = Math.max(busyUntil, t + (long) ((queued - BUFFER) / CPS * 2e6 * mul));
        }
    }

    public boolean isBusy(long t) {
        return t < busyUntil;
    }

    public void close() {
        if (out != null) {
            try {
                out.close();
            } catch (IOException ex) {
            }
            out = null;
        }
    }
}
