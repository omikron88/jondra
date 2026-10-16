package machine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Emulace modulu OndraSD (firmware "OndraSD interface (2.2) + RTC", procesor 8051).
 *
 * Modul je pripojeny na seriovou linku Ondry: vysilani Ondry je bit 2 portu 0AH,
 * prijem je bit 6 pri cteni I/O okna (0E000H-0FFFFH). Emulace pracuje na urovni
 * jednotlivych bitu s casovanim podle T-stavu, takze funguji puvodni rutiny
 * (zavadec v ROM, __LOADER.BIN, OndraFM i programy, ktere si soubory nacitaji samy).
 *
 * SD kartu predstavuje adresar na disku. Jmena souboru se zobrazuji jako FAT 8.3
 * (velka pismena, dlouha jmena se zkrati na JMENO~1.EXT).
 *
 * Chovani odpovida disassembly firmware (ATM.HEX):
 *  - po zapnuti/resetu posle 0DH a pak kazdych ~1,7 s vysila __LOADER.BIN (9600 Bd),
 *    dokud neprijde prikaz N;
 *  - prikazy: A rychlost, I LED, K verze, D adresar, C zmena adresare, F soubor,
 *    T/S hodiny, E echo, M text, G/J/P/Z stav, L znovu vysilat zavadec,
 *    N/U/V stop zavadece, X/H a bajt 00H reset modulu;
 *  - F na neexistujici soubor neodpovi nicim (tak jako skutecny modul).
 * Sektorove prikazy B/R/W (CP/M) nejsou podporovany, odpovi chybou (00H).
 */
public class OndraSD {

    /** takt Ondry (312 radku x 128 T x 50 Hz) */
    private static final double CPU_HZ = 312 * 128 * 50;
    /** krystal 8051: 9600 Bd pri TH1=F4h a 57600 Bd pri TH1=FEh se SMOD=1 */
    private static final double XTAL_HZ = 22118400.0;
    /** jeden strojovy cyklus 8051 v T-stavech Ondry */
    private static final double CYCLE_T = 12.0 * CPU_HZ / XTAL_HZ;
    private static final long FRAME_T = 312 * 128;
    /** odhad prace 8051 s kartou (SPI): nacteni sektoru a jedne polozky adresare */
    private static final int SECTOR_CYCLES = 3000;
    private static final int DIRENTRY_CYCLES = 1500;

    private static final int VERSION = 0x12;
    private static final String COPYRIGHT =
            "\r\nOndraSD interface (2.2) + RTC\r\nM1 (c) 2015, 2016\r\nhttps://sites.google.com/site/ondraspo186\r\n";

    private boolean enabled = false;
    private File rootDir = null;
    private File curDir = null;

    // --- prevod T-stavu procesoru na skutecny cas (pri zapnutem videu procesor stoji)
    private long frameClk = 0, frameReal = 0;
    private boolean frameStarted = false;

    // --- stav modulu
    private int baud = 9600;
    private int delay7C = 0xC0;           // mezera mezi vysilanymi bajty (R1 ve smycce djnz)
    private boolean broadcast = true;     // vysilani zavadece
    private boolean waitLineHigh = false; // po prikazu 00H/X ceka na klid linky
    private long nextBroadcast = 0;
    private long modFree = 0;             // kdy modul dokonci praci (vysilani)
    private boolean led = false;
    private int errorCode = 0;            // blikani modre LED: 7 karta, 8 chybi zavadec
    private long errorSince = 0;
    private long lastT = 0;               // posledni znamy cas modulu
    private long lastTxActive = -1000000000L; // konec posledniho odvysilaneho bajtu
    private long rtcOffsetSec = 0;

    private int cmd = -1;
    private final ArrayList<Integer> params = new ArrayList<>();
    private int pendingRx = -1;           // bajt v SBUF, kdyz modul jeste vysila
    private long pendingRxTime = 0;

    // --- linka Ondra -> modul (dekodovani UART)
    private int txLine = 1;
    private boolean rxActive = false;
    private long rxStart;
    private double rxBitT;
    private int rxBit;                    // 0 = start, 1..8 data, 9 stop
    private int rxShift;

    // --- linka modul -> Ondra: naplanovane bajty
    private static final class TxByte {
        final long start;
        final double bitT;
        final int value;

        TxByte(long s, double b, int v) {
            start = s;
            bitT = b;
            value = v;
        }

        long end() {
            return start + (long) Math.ceil(10 * bitT);
        }
    }
    private final ArrayDeque<TxByte> txQueue = new ArrayDeque<>();
    private long lastTxEnd = 0;

    /** ladici vypis prikazu na stdout */
    public static boolean debug = false;

    // =====================================================================
    // konfigurace

    public synchronized void configure(boolean on, String dir) {
        enabled = on;
        rootDir = (dir == null || dir.isEmpty()) ? null : new File(dir);
        curDir = rootDir;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public synchronized String getCardDir() {
        return rootDir == null ? "" : rootDir.getPath();
    }

    /**
     * Modra LED (P3.2): sviti po zapnuti, ovlada ji prikaz I, pri chybe blika kod
     * (7x = karta, 8x = chybi __LOADER.BIN; L0098 ve firmware).
     */
    public synchronized boolean isBlueLedOn() {
        if (errorCode == 0) {
            return led;
        }
        long period = (long) (0.5 * CPU_HZ);                  // jedno bliknuti
        long cycle = (errorCode + 3) * period;                // kod + pauza
        long p = Math.floorMod(lastT - errorSince, cycle);
        return p < errorCode * period && (p % period) < period / 2;
    }

    /** zluta LED: modul vysila (zavadec, odpoved) */
    public synchronized boolean isYellowLedOn() {
        TxByte b = txQueue.peekFirst();
        if (b != null && b.start <= lastT) {
            return true;
        }
        return lastT - lastTxActive < (long) (0.05 * CPU_HZ);
    }

    /** popis pro bublinu ve stavovem radku (HTML) */
    public synchronized String getStatusText() {
        StringBuilder sb = new StringBuilder("<html><b>OndraSD</b><br>SD card: ");
        sb.append(rootDir == null ? "<i>not set (Settings)</i>" : escape(rootDir.getAbsolutePath()));
        if (rootDir == null || !rootDir.isDirectory()) {
            sb.append("<br><font color=red>directory not found - blue LED blinks 7x</font>");
        } else {
            if (findFile(rootDir, "__LOADERBIN", false) == null) {
                sb.append("<br><font color=red>__LOADER.BIN missing in root - blue LED blinks 8x</font>");
            }
            if (curDir != null && !sameFile(curDir, rootDir)) {
                sb.append("<br>current directory: ").append(escape(curDir.getAbsolutePath()));
            }
            sb.append("<br>").append(baud).append(" Bd").append(broadcast ? ", broadcasting loader" : "");
        }
        return sb.append("</html>").toString();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // =====================================================================
    // cas

    private long real(long clk) {
        return frameReal + (clk - frameClk);
    }

    /** volat na zacatku kazdeho snimku (Ondra.ms20) */
    public synchronized void frameStart(long clk) {
        if (!frameStarted) {
            frameStarted = true;
            frameReal = clk;
        } else {
            // Pri zapnutem videu probehne v JOndre jen cast snimku a procesor pak stoji.
            // Je-li linka v klidu, ubehne cely snimek (casovace modulu jdou podle hodin).
            // Behem prenosu se cas modulu zastavi s procesorem, jinak by se bajty ztratily
            // (JOndra zdrzeni sousti na konec snimku, i kdyz program video mezitim vypne).
            long ran = clk - frameClk;
            boolean busy = rxActive || !txQueue.isEmpty() || pendingRx >= 0 || cmd >= 0;
            frameReal += busy ? ran : Math.max(FRAME_T, ran);
        }
        frameClk = clk;
        if (enabled) {
            advance(real(clk));
        }
    }

    private double bitT() {
        return CPU_HZ / baud;
    }

    private double gapT() {
        // jnb TI / acall L040D (djnz R1 = 7Ch, 2 cykly) / mov SBUF + cteni z karty
        return (2 * (delay7C == 0 ? 256 : delay7C) + 40) * CYCLE_T;
    }

    // =====================================================================
    // reset

    /** zapnuti napajeni / reset Ondry (TXD v nule -> modul se restartuje) */
    public synchronized void reset(long clk) {
        frameStarted = false;
        frameClk = clk;
        frameReal = clk;
        txQueue.clear();
        lastTxEnd = 0;
        rxActive = false;
        txLine = 0;                       // latch portu 0AH je po resetu vynulovany; bez hrany modul nic neprijme
        cmd = -1;
        params.clear();
        pendingRx = -1;
        waitLineHigh = false;
        led = false;
        curDir = rootDir;
        restart(real(clk) + (long) (1.1 * CPU_HZ)); // L0030: uvodni cekani ~1,1 s
    }

    private void restart(long t) {
        baud = 9600;
        delay7C = 0xC0;
        curDir = rootDir;
        cmd = -1;
        params.clear();
        modFree = t;
        led = true;                       // L0030: P3 = FFh
        errorSince = t;
        if (rootDir == null || !rootDir.isDirectory()) {
            errorCode = 7;
        } else if (findFile(rootDir, "__LOADERBIN", false) == null) {
            errorCode = 8;
        } else {
            errorCode = 0;
        }
        if (debug) {
            System.out.println("OndraSD: restart, karta " + rootDir + (rootDir != null && rootDir.isDirectory() ? "" : " (neni adresar)"));
        }
        if (enabled && rootDir != null && rootDir.isDirectory()) {
            send(0x0D, t);                // L0059
            broadcast = true;
            nextBroadcast = modFree;
        } else {
            broadcast = false;            // bez karty jen blika chybu
        }
    }

    // =====================================================================
    // rozhrani k Ondrovi

    /** zapis do portu 0AH: bit 2 = TXD */
    public synchronized void txWrite(int level, long clk) {
        if (!enabled) {
            return;
        }
        long t = real(clk);
        advance(t);
        level &= 1;
        if (level != txLine) {
            if (txLine == 1 && level == 0 && !rxActive) {
                rxActive = true;
                rxStart = t;
                rxBitT = bitT();
                rxBit = 0;
                rxShift = 0;
            }
            txLine = level;
            if (level == 1 && waitLineHigh) {
                waitLineHigh = false;
                restart(Math.max(t, modFree));
            }
        }
    }

    /** stav linky RXD (bit 6 I/O okna) */
    public synchronized boolean rxLevel(long clk) {
        if (!enabled) {
            return true;
        }
        long t = real(clk);
        advance(t);
        TxByte b = txQueue.peekFirst();
        if (b == null || t < b.start) {
            return true;
        }
        int idx = (int) ((t - b.start) / b.bitT);
        if (idx == 0) {
            return false;                 // start-bit
        }
        if (idx >= 9) {
            return true;                  // stop-bit
        }
        return ((b.value >> (idx - 1)) & 1) != 0;
    }

    /** ladici popis stavu linky modul -> Ondra */
    public synchronized String status(long clk) {
        long t = real(clk);
        TxByte b = txQueue.peekFirst();
        return "t=" + t + (b == null ? " fronta prazdna" : String.format(" bajt %02X start %d (%+d T), ve fronte %d",
                b.value, b.start, t - b.start, txQueue.size()));
    }

    // =====================================================================
    // vnitrni prubeh

    private void advance(long t) {
        lastT = Math.max(lastT, t);
        while (!txQueue.isEmpty() && txQueue.peekFirst().end() <= t) {
            lastTxActive = txQueue.pollFirst().end();
        }
        // dekodovani bajtu od Ondry
        while (rxActive) {
            long ts = rxStart + (long) ((rxBit + 0.5) * rxBitT);
            if (ts >= t) {
                break;
            }
            int v = txLine;
            if (rxBit == 0) {
                if (v != 0) {
                    rxActive = false;     // falesny start-bit
                    break;
                }
            } else if (rxBit <= 8) {
                rxShift |= v << (rxBit - 1);
            } else {
                rxActive = false;         // stop-bit (8051 nastavi RI i pri chybe ramce)
                byteReceived(rxShift, ts);
                if (txLine == 0) {        // linka zustala v nule: hned dalsi start
                    rxActive = true;
                    rxStart = ts;
                    rxBitT = bitT();
                    rxBit = 0;
                    rxShift = 0;
                    continue;
                }
                break;
            }
            rxBit++;
        }
        // bajt cekajici v SBUF, nez modul dovysila
        if (pendingRx >= 0 && t >= modFree) {
            int b = pendingRx;
            pendingRx = -1;
            process(b, Math.max(modFree, pendingRxTime));
        }
        // vysilani zavadece
        while (broadcast && t >= nextBroadcast && nextBroadcast >= modFree) {
            File f = findFile(rootDir, "__LOADERBIN", false);
            if (f == null) {
                nextBroadcast = Long.MAX_VALUE;
                break;
            }
            if (debug) {
                System.out.println("OndraSD: vysilam zavadec v T=" + nextBroadcast);
            }
            sendFile(f, Math.max(nextBroadcast, t), 0);
            nextBroadcast = modFree + (long) (255 * 6 * 2048 * CYCLE_T); // cekani ~1,7 s
        }
    }

    private void byteReceived(int b, long t) {
        if (t < modFree) {
            if (pendingRx < 0) {          // dalsi bajt by 8051 ztratil (RI je jeste nastaveny)
                pendingRx = b;
                pendingRxTime = t;
            }
            return;
        }
        process(b, t);
    }

    private void process(int b, long t) {
        if (waitLineHigh) {
            return;
        }
        if (cmd < 0) {
            cmd = b;
            params.clear();
        } else {
            params.add(b);
        }
        modFree = Math.max(modFree, t);
        int n = params.size();
        if (debug && n == 0) {
            System.out.println(String.format("OndraSD: prikaz %02X '%c' (%d Bd)", cmd,
                    (cmd >= 32 && cmd < 127) ? (char) cmd : '?', baud));
        }
        switch (cmd) {
            case 0x00:                       // L014B: ceka na klid linky a restartuje
            case 'X':
            case 'H':
                cmd = -1;
                if (txLine == 1) {
                    restart(t);
                } else {
                    waitLineHigh = true;
                }
                break;
            case 'N':
            case 'U':
            case 'V':
                cmd = -1;
                broadcast = false;
                break;
            case 'L':
                cmd = -1;
                broadcast = true;
                nextBroadcast = modFree;
                break;
            case 'O':
            case 'Q':
                cmd = -1;
                break;
            case 'A':
                if (n == 1) {
                    cmd = -1;
                    if ((params.get(0) & 1) != 0) {
                        baud = 57600;
                        delay7C = 0x20;
                    } else {
                        baud = 9600;
                        delay7C = 0xC0;
                    }
                }
                break;
            case 'I':
                if (n == 1) {
                    cmd = -1;
                    errorCode = 0;
                    led = (params.get(0) & 1) != 0;
                }
                break;
            case 'E':
                if (n == 1) {
                    cmd = -1;
                    send(params.get(0), t);
                }
                break;
            case 'K':
                cmd = -1;
                send(VERSION, t);
                break;
            case 'M':
                cmd = -1;
                for (char c : COPYRIGHT.toCharArray()) {
                    send(c, t);
                }
                break;
            case 'G':
            case 'J':
            case 'P':
            case 'Z':
                cmd = -1;
                send('+', t + (long) (5 * 2 * delay7C * CYCLE_T));  // '+' = hodiny pritomny
                break;
            case 'T':
                cmd = -1;
                sendRtc(t);
                break;
            case 'S':
                if (n == 7) {
                    cmd = -1;
                    setRtc();
                }
                break;
            case 'B':
            case 'R':
            case 'W':
                if (n == 4) {
                    cmd = -1;
                    send(0x00, t + 2000);   // sektory nejsou podporovany -> chyba
                }
                break;
            case 'D':
                cmd = -1;
                sendDir(t + 2000);
                break;
            case 'C':
                if (n == 11) {
                    cmd = -1;
                    changeDir(name11());
                }
                break;
            case 'F':
                if (n == 11) {
                    cmd = -1;
                    String nm = name11();
                    File f = findFile(curDir, nm, false);
                    if (debug) {
                        System.out.println("OndraSD: F " + nm + (f == null ? " - nenalezeno" : " -> " + f));
                    }
                    if (f != null) {
                        sendFile(f, t + 4000, nm.charAt(8) == 'T' ? 1 : 0);
                    }
                }
                break;
            default:                         // @ [ \ ] ^ _ : firmware nic neudela
                cmd = -1;
                break;
        }
    }

    private String name11() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 11; i++) {
            sb.append((char) (params.get(i) & 0xff));
        }
        return sb.toString();
    }

    /** naplanuje odvysilani bajtu nejdriv v case t */
    private void send(int value, long t) {
        long start = Math.max(t, lastTxEnd + (long) gapT());
        start = Math.max(start, modFree);
        TxByte b = new TxByte(start, bitT(), value & 0xff);
        txQueue.addLast(b);
        lastTxEnd = b.end();
        modFree = lastTxEnd;
    }

    /** prace modulu bez vysilani (cteni z karty): posune dalsi bajt o dany pocet cyklu 8051 */
    private void pause(int cycles) {
        lastTxEnd += (long) (cycles * CYCLE_T);
        modFree = Math.max(modFree, lastTxEnd);
    }

    private void sendFile(File f, long t, int tap) {
        byte[] data;
        try {
            data = Files.readAllBytes(f.toPath());
        } catch (IOException ex) {
            return;
        }
        if (tap == 0) {
            for (int i = 0; i < data.length; i++) {
                if (i > 0 && (i & 511) == 0) {
                    pause(SECTOR_CYCLES);
                }
                send(data[i], t);
            }
            return;
        }
        // TAP (L0347): preskoc 26 B, posli 1024 B, preskoc 27 B, posli 1024 B ...
        boolean skip = true;
        int cnt = 26;
        for (int i = 0; i < data.length; i++) {
            if (i > 0 && (i & 511) == 0) {
                pause(SECTOR_CYCLES);
            }
            if (!skip) {
                send(data[i], t);
            }
            if (--cnt == 0) {
                if (skip) {
                    skip = false;
                    cnt = 1024;
                } else {
                    skip = true;
                    cnt = 27;
                }
            }
        }
    }

    // =====================================================================
    // hodiny (DS1307: s, min, h, den v tydnu, den, mesic, rok v BCD)

    private static int bcd(int v) {
        return ((v / 10) << 4) | (v % 10);
    }

    private static int unbcd(int v) {
        return (v >> 4) * 10 + (v & 15);
    }

    private void sendRtc(long t) {
        LocalDateTime d = LocalDateTime.now().plusSeconds(rtcOffsetSec);
        int[] r = {bcd(d.getSecond()), bcd(d.getMinute()), bcd(d.getHour()), d.getDayOfWeek().getValue(),
            bcd(d.getDayOfMonth()), bcd(d.getMonthValue()), bcd(d.getYear() % 100)};
        for (int v : r) {
            send(v, t + 1000);
        }
    }

    private void setRtc() {
        try {
            LocalDateTime d = LocalDateTime.of(2000 + unbcd(params.get(6)), unbcd(params.get(5)),
                    unbcd(params.get(4)), unbcd(params.get(2) & 0x3f), unbcd(params.get(1)), unbcd(params.get(0) & 0x7f));
            rtcOffsetSec = java.time.Duration.between(LocalDateTime.now(), d).getSeconds();
        } catch (Exception e) {
            // neplatne datum: skutecny obvod by ho zapsal, tady se ignoruje
        }
    }

    // =====================================================================
    // "SD karta" = adresar na disku

    private static final class Entry {
        String fat;      // 11 znaku, jako v adresari FAT
        File file;
        boolean dir;
        long size;
    }

    private static String clean(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toUpperCase().toCharArray()) {
            if (c > 32 && c < 127 && "\"*+,./:;<=>?[\\]|".indexOf(c) < 0) {
                sb.append(c);
            } else if (c != ' ' && c != '.') {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    private List<Entry> listDir(File dir) {
        List<Entry> res = new ArrayList<>();
        if (dir == null || !dir.isDirectory()) {
            return res;
        }
        if (rootDir != null && !sameFile(dir, rootDir)) {
            for (String dots : new String[]{".", ".."}) {
                Entry e = new Entry();
                e.fat = String.format("%-11s", dots);
                e.dir = true;
                e.file = dots.equals("..") ? dir.getParentFile() : dir;
                res.add(e);
            }
        }
        File[] fs = dir.listFiles();
        if (fs == null) {
            return res;
        }
        Arrays.sort(fs, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        Set<String> used = new HashSet<>();
        for (File f : fs) {
            String nm = f.getName();
            if (nm.startsWith(".")) {
                continue;
            }
            int dot = f.isDirectory() ? -1 : nm.lastIndexOf('.');
            String base = clean(dot > 0 ? nm.substring(0, dot) : nm);
            String ext = dot > 0 ? clean(nm.substring(dot + 1)) : "";
            boolean exact = base.length() <= 8 && ext.length() <= 3 && !base.isEmpty()
                    && nm.toUpperCase().equals(dot > 0 ? base + "." + ext : base);
            if (ext.length() > 3) {
                ext = ext.substring(0, 3);
            }
            String fat = String.format("%-8s%-3s", base.length() > 8 ? base.substring(0, 8) : base, ext);
            if (!exact || used.contains(fat)) {
                for (int i = 1; i < 10; i++) {
                    String b = (base.length() > 6 ? base.substring(0, 6) : base) + "~" + i;
                    fat = String.format("%-8s%-3s", b, ext);
                    if (!used.contains(fat)) {
                        break;
                    }
                }
            }
            used.add(fat);
            Entry e = new Entry();
            e.fat = fat;
            e.file = f;
            e.dir = f.isDirectory();
            e.size = f.length();
            res.add(e);
        }
        return res;
    }

    private static boolean sameFile(File a, File b) {
        try {
            return a.getCanonicalPath().equals(b.getCanonicalPath());
        } catch (IOException ex) {
            return a.equals(b);
        }
    }

    private File findFile(File dir, String fat, boolean wantDir) {
        for (Entry e : listDir(dir)) {
            if (e.dir == wantDir && e.fat.equals(fat) && e.file != null) {
                return e.file;
            }
        }
        return null;
    }

    private void sendDir(long t) {
        for (Entry e : listDir(curDir)) {
            int attr;
            if (e.dir) {
                attr = 0x10;
            } else {
                String ext = e.fat.substring(8);
                if (e.size >= 0x10000 || !(ext.equals("BIN") || ext.equals("TAP"))) {
                    continue;
                }
                attr = 0x20;
            }
            for (char c : e.fat.toCharArray()) {
                send(c, t);
            }
            send(attr | 0x40, t);
            send(0x00, t);
            pause(DIRENTRY_CYCLES);       // L0A0D: cteni dalsi polozky adresare z karty
        }
        send(0xFF, t);
    }

    private void changeDir(String fat) {
        if (fat.charAt(0) == '/') {
            curDir = rootDir;
            return;
        }
        for (Entry e : listDir(curDir)) {
            if (e.dir && e.fat.equals(fat)) {
                File d = e.file;
                if (d == null || rootDir == null) {
                    return;
                }
                try {
                    // nad koren karty se nejde ("..")
                    if (!d.getCanonicalPath().startsWith(rootDir.getCanonicalPath())) {
                        d = rootDir;
                    }
                } catch (IOException ex) {
                    d = rootDir;
                }
                curDir = d;
                return;
            }
        }
    }
}
