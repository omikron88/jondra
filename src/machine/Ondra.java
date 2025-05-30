/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package machine;

import gui.Debugger;
import gui.JOndra;
import gui.Screen;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.util.Arrays;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import z80core.Z80;
import z80core.Z80State;

/**
 *
 * @author Administrator
 */
public class Ondra extends Thread
        implements z80core.MemIoOps, z80core.NotifyOps {

    private final int T_DMAOFF = 312 * 128;
    private final int T_DMAON = (312 - 255) * 128;

    private final int OSN_VERSION = 0x02; //version 0.1

    public Screen scr;
    private BufferedImage img;
    private byte px[];
    private Config cfg;
    private Keyboard key;
    public Memory mem;
    private Timer tim;
    private int msSpeed;
    private MTimer task;
    public Clock clk;
    public Z80 cpu;
    public Tape tap;
    public JOndra frame;
    public static long nSpeedPercent = 0;
    public int nSpeedPercentUpdateMaxCycles = 50;
    public int nSpeedPercentUpdateDec = nSpeedPercentUpdateMaxCycles;
    public long nLastSeen = System.currentTimeMillis() - 1;
    public Sound snd;
    public Melodik melodik;
    public long nLastMilisec = 0;

    public JLabel GreenLed, YellowLed, TapeLed;
    public JToggleButton RecButton;

    private boolean paused;

    private int dispAdr[];

    public boolean dmaEnabled;
    public byte portA0, portA1, portA3;
    private byte iov[];

    private int t_resolution_correct = 0;
    private int t_frame = T_DMAOFF;
    private int nDMAStatus = 0;

    private boolean tapestart;

    private Debugger deb;

    public int nRozliseni = 255;
    boolean bFrst = true;

    public boolean bAutoRunAfterReset = false;

    private final byte[] snapshotBuffer;
    public int memorySize = 42 + 0x10000; // Velikost paměti (pro ulozeni registru a pameti)
    public int maxRecords = 500000; // Maximální počet záznamů ve frontě
    public int snapshotInterval = 100; // Interval pro pravidelné keyframy

    public Ondra() {
        snapshotBuffer = new byte[memorySize]; // Alokace bufferu při inicializaci
        img = new BufferedImage(320, 256, BufferedImage.TYPE_BYTE_BINARY);
        px = ((DataBufferByte) img.getRaster().getDataBuffer()).getBankData()[0];
        cfg = new Config();
        utils.Config.LoadConfig();
        cfg.setAudio(utils.Config.bAudio);
        cfg.setMelodik(utils.Config.bMelodik);
        cfg.setFullscreen(utils.Config.bFullscreen);
        cfg.setScanlines(utils.Config.bScanlines);
        cfg.setScaleNx(utils.Config.bScaleNx);
        mem = new Memory(this, cfg);
        msSpeed = 20;
        tim = new Timer("Timer");
        clk = new Clock();
        cpu = new Z80(clk, this, this);
        snd = new Sound();
        snd.setEnabled(cfg.getAudio());
        snd.init();

        iov = new byte[mem.PAGE_SIZE];
        mem.setIOVect(iov);
        key = new Keyboard(iov);

        melodik = new Melodik(iov);
        melodik.setEnabled(cfg.getMelodik());
        melodik.init(clk.getTstates());

        dmaEnabled = true;
        dispAdr = new int[10240];
        genDispTables();
        tap = new Tape(this);

        paused = true;

        Reset(true);
    }

    public void setDebugger(Debugger indeb) {
        deb = indeb;
    }

    public void setFrame(JOndra inJon) {
        frame = inJon;
    }

    public Debugger getDebugger() {
        return deb;
    }
    
    public Keyboard getKeyboard() {
        return key;
    }

    public void setConfig(Config c) {
        if (!cfg.equals(c)) {
            cfg = c;
            stopEmulation();
            Reset(false);
            startEmulation();
        }
    }

    public Config getConfig() {
        return cfg;
    }

    public void setScreen(Screen screen) {
        scr = screen;
    }

    public BufferedImage getImage() {
        return img;
    }    

    public void setGreenLed(JLabel led) {
        GreenLed = led;
    }

    public void setYellowLed(JLabel led) {
        YellowLed = led;
    }

    public void setTapeLed(JLabel led) {
        TapeLed = led;
    }

    public void setRecButton(JToggleButton b) {
        RecButton = b;
    }

    public void setClockSpeed(int inSpeed) {
        msSpeed = inSpeed;
    }

    public int getClockSpeed() {
        return msSpeed;
    }

    public final void Reset(boolean dirty) {
        portA3 = portA0 = portA1 = 0;
        nDMAStatus = 0;
        t_resolution_correct = 0;
        t_frame = T_DMAOFF;
        cpu.timeline.clearTimeline();
        mem.Reset(dirty);
        mem.mapRom(true);
        clk.reset();
        cpu.reset();
        key.Reset();

        nRozliseni = 255;
        genDispTables();
        tapestart = false;
        if (RecButton != null) {
            tap.tapeStop();
        }
        tap = new Tape(this);
        if ((snd.isEnabled()) && (!cfg.getAudio())) {
            //musim disablovat audio
            snd.setEnabled(cfg.getAudio());
            snd.deinit();
        } else {
            if ((!snd.isEnabled()) && (cfg.getAudio())) {
                //musim spustit audio
                snd.setEnabled(cfg.getAudio());
                snd.init();
            }
        }
        if (melodik.isEnabled()) {
            melodik.initChip();
        }
        if ((melodik.isEnabled()) && (!cfg.getMelodik())) {
            //musim disablovat melodik
            melodik.setEnabled(cfg.getMelodik());
            melodik.deinit();
        } else {
            if ((!melodik.isEnabled()) && (cfg.getMelodik())) {
                //musim spustit melodik
                melodik.setEnabled(cfg.getMelodik());
                melodik.init(clk.getTstates());
            }
        }
    }

    public final void Nmi() {
        cpu.setNMI(true);
        cpu.execute(clk.getTstates() + 8);
        cpu.setNMI(false);
    }

    public void startEmulation() {
        if (!paused) {
            return;
        }

        scr.repaint();
        paused = false;
        task = new MTimer(this);
        tim.scheduleAtFixedRate(task, 250, msSpeed);
    }

    public void stopEmulation() {
        if (paused) {
            return;
        }

        paused = true;
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public void genDispTables() {
        Arrays.fill(dispAdr, -1);
        Arrays.fill(px, (byte) 0);
        int nSkew = 255 - nRozliseni;
        int adr = 0;
        int vm;
        for (int y = 255 - nSkew; y != 0; y--) {
            for (int x = 0xff00; x != 0xd700; x -= 0x0100) {
                vm = (y >>> 1) | ((y & 1) << 7) | x;
                dispAdr[vm - 0xd800] = adr;
                px[adr++] = mem.readRam(vm);

            }

        }
    }

    public void ms20() {
        //aktualizuji rychlost emulace
        long nNowSeen = System.currentTimeMillis();
        if ((nNowSeen - nLastSeen) > 0) {
            nSpeedPercent = nSpeedPercent + (int) (200000 / (nNowSeen - nLastSeen));
        }
        nLastSeen = nNowSeen;
        nSpeedPercentUpdateDec--;
        //je treba zobrazit - prumer z 30 po sobe jdoucich hodnot + 20x predchozi zobrazena hodnota (aby byly zmeny plynulejsi)
        if (nSpeedPercentUpdateDec <= 0) {
            nSpeedPercentUpdateDec = nSpeedPercentUpdateMaxCycles;
            //zaokrouhleni nahoru
            nSpeedPercent = ((nSpeedPercent / (10 * nSpeedPercentUpdateMaxCycles)) + 7) / 10;
            frame.setTitle("Ondra SPO 186 - " + nSpeedPercent + "%");
            nSpeedPercent = 2000 * nSpeedPercent;
            nSpeedPercentUpdateDec -= 20;
        }
        if (!paused) {
            if (snd.isEnabled()) {
                snd.switchBuffers(clk.getTstates());
                snd.setDataReady();
            }
            if (melodik.isEnabled()) {
                melodik.setMelodikDetectOn();
                melodik.updateSound(clk.getTstates());
                melodik.switchBuffers();
                melodik.setDataReady();
            }
            scr.repaint();
            cpu.setINTLine(true);
            cpu.execute(clk.getTstates() + 16);
            cpu.setINTLine(false);
            if (!paused) {
                cpu.execute(clk.getTstates() + t_frame - 16);
            }
        }

        if ((bFrst) && (frame.strArgFile.length() > 0) && (frame.nStartAddress < 0)) {
            //pokud je v command line nazev souboru a neni tam zadna cilova adresa do ktere musi CPU dojit, tak ho nahraji do pameti ihned a spustim
            bFrst = false;
            StartArgumentImage(true);
        } else if ((frame.strArgFile.length() > 0) && (frame.nStartAddress >= 0)) {
            //nahrani binarky a spusteni az po dosazeni nejake adresy predane v argumentu cmd - napr. az po inicializaci obrazovky
            if (bAutoRunAfterReset) {
                //smazu predchozi BP, pokud ho nebylo dosazeno
                cpu.setBreakpoint(frame.nStartAddress, false);
            }
            cpu.setBreakpoint(frame.nStartAddress, true);
            bAutoRunAfterReset = true;
        }
    }

    public void StartArgumentImage(boolean bAutoStart) {
        snd.setEnabled(cfg.getAudio());
        melodik.setEnabled(cfg.getMelodik());
        frame.LoadBinSilently(bAutoStart);
    }

    @Override
    public void run() {
        //pokud je v command line nazev souboru, tak vypnu zvuk dokud nenahraji soubor do pameti
        if (frame.strArgFile.length() > 0) {
            snd.setEnabled(false);
            melodik.setEnabled(false);
        }
        startEmulation();
        try {
            sleep(500);
        } catch (InterruptedException ex) {

        }
        boolean forever = true;
        while (forever) {
            try {
                sleep(Long.MAX_VALUE);
            } catch (InterruptedException ex) {
                Logger.getLogger(Ondra.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void processVram(int address) {
        int x = dispAdr[address - 0xd800];
        if (x != -1 && dmaEnabled) {
            px[x] = mem.readRam(address);
            
        }
    }
    
    private void dmaEnable() {
        
        for (int address = 0xd800; address < 0x10000; address++) {
            int x = dispAdr[address - 0xd800];
            if (x != -1) {
                px[x] = mem.readRam(address);
            }
        }
        
        dmaEnabled = true;
    }

    private void dmaDisable() {
        
        dmaEnabled = false;
        for (int address = 0xd800; address < 0x10000; address++) {
            int x = dispAdr[address - 0xd800];
            if (x != -1) {
                px[x] = 0;
            }
        }
        
    }

    @Override
    public int fetchOpcode(int address) {
        clk.addTstates(4);
        int opcode = mem.readByte(address) & 0xff;
//        System.out.println(String.format("PC: %04X (%02X)", address,opcode));
        return opcode;
    }

    @Override
    public int peek8(int address) {
        int value = 0;
        boolean bExe = true;

        if (utils.Config.bBP7) {
            //memoryread BP
            if (utils.Config.nBP7Address == address) {
                cpu.bMemBPR = true; //ukonci provadeni instrukci
                bExe = false;
            }
        }
        if (bExe) {
            clk.addTstates(3);
            value = mem.readByte(address) & 0xff;            
        }
        return value;
    }

    @Override
    public void poke8(int address, int value) {
//        System.out.println(String.format("Poke: %04X,%02X (%04X)", address,value,cpu.getRegPC()));
        boolean bExe = true;

        if (utils.Config.bBP6) {
            //memorywrite BP
            if (utils.Config.nBP6Address == address) {
                cpu.bMemBP = true; //ukonci provadeni instrukci
                bExe = false;
            }
        }
        if (bExe) {
            clk.addTstates(3);
            mem.writeByte(address, (byte) value);
            cpu.timeline.addChange(address, (byte) value);
        }
    }

    @Override
    public int peek16(int address) {
        int lsb = peek8(address) & 0xff;
        address = (address + 1) & 0xffff;
        return ((peek8(address) << 8) & 0xff00 | lsb);
    }

    @Override
    public void poke16(int address, int word) {
        poke8(address, (byte) word);
        address = (address + 1) & 0xffff;
        poke8(address, (byte) (word >>> 8));
    }

    @Override
    public int inPort(int port) {
        clk.addTstates(4);
//detekce zmeny rozliseni
        if ((portA3 & 0x30) == 0x00) {
            int nC = port & 0xff;
            int nCarry = nC & 128;
            if (nCarry > 0) {
                nCarry = 1;
            }
            nRozliseni = nC << 1;
            nRozliseni &= 255;
            nRozliseni |= nCarry;
            changeResolution();
        }
//        System.out.println(String.format("In: %04X (PC=%04X, portA0=%02X, portA1=%02X, portA3=%02X)", port,cpu.getRegPC(), portA0, portA1, portA3));
        return 0xff;
    }

    public void changeResolution() {
        //zmena rychlosti Ondry na zaklade rozliseni
        t_resolution_correct = (255 - nRozliseni) * 128;
        if (nDMAStatus == 0) {
            t_frame = T_DMAOFF;
        } else {
            t_frame = T_DMAON + t_resolution_correct;
        }
        genDispTables();
    }

    @Override
    public void outPort(int port, int value) {        
        clk.addTstates(4);
        port &= 0xff;
        value &= 0xff;
  //      System.out.println(String.format("PC=%04X,out(%02X),%02X", cpu.getRegPC(),port,value));
        if ((port & 0x08) == 0) {
            portA3 = (byte) value;
             if ((portA3 & 0x02) == 0) {
                mem.mapRom(true);
            } else {
                mem.mapRom(false);
            }
            if ((portA3 & 0x04) == 0) {
                mem.mapIO(false);
            } else {
                mem.mapIO(true);
            }
            if ((portA3 & 0x01) == 0) {
                t_frame = T_DMAOFF;
                nDMAStatus = 0;
                dmaDisable();
                scr.repaint();
            } else {
                t_frame = T_DMAON + t_resolution_correct;
                nDMAStatus = 1;
                dmaEnable();
                scr.repaint();
            }           

        }
        if ((port & 0x01) == 0) {
            portA0 = (byte) value;
            if (snd.isEnabled()) {
                snd.fillBuffer.fillWithSample(((value & 224) >>> 5), clk.getTstates());
            }
            if ((portA0 & 0x01) == 0) {
                GreenLed.setEnabled(true);
            } else {
                GreenLed.setEnabled(false);
            }
            if ((portA0 & 0x02) == 0) {
                YellowLed.setEnabled(true);
            } else {
                YellowLed.setEnabled(false);
            }
            if ((portA0 & 0x10) != 0) {
                if (!tapestart) {
                    tapestart = true;
                    tap.tapeStart();
                }
            } else {
                if (tapestart) {
                    tapestart = false;
                    tap.tapeStop();
                }
            }
        }
        //printer port
        if ((port & 0x02) == 0) {
            portA1 = (byte) value;
            //pokud je strobe na high, tak zapis na Melodik
            if ((portA0 & 0x8) != 0) {
                if (melodik.isEnabled()) {
                    melodik.sndChip.write(value);
                }
            }
        }
    }

    @Override
    public void contendedStates(int address, long tstates) {
        clk.addTstates(tstates);
    }

    @Override
    public int atAddress(int address, int opcode) {
//        System.out.println(String.format("bp: %04X,%02X", address,opcode));
//        System.out.println(String.format("HL: %04X DE: %04X", cpu.getRegHL(),cpu.getRegDE()));

        return opcode;
    }

    @Override
    public void execDone() {

    }

    public void openLoadTape(String canonicalPath) {
        tap.openLoadTape(canonicalPath);
    }

    public void openSaveTape(String canonicalPath) {
        tap.openSaveTape(canonicalPath);
    }

    public void setTapeMode(boolean rec) {
        tap.setTapeMode(rec);
    }

    public void closeClenaup() {
        tap.closeCleanup();
    }

    public final void loadSnapshot(String filename) {
        BufferedInputStream fIn;
        boolean res = false;
        int i = 0, cnt = 0, tmp;
        int version;

        try {
            fIn = new BufferedInputStream(new FileInputStream(filename));
            //nastavim zakladni rozliseni a vymazu VRAM pamet Ondry          
            nRozliseni = 255;
            changeResolution();
            for (i = 0xd800; i < 0x10000; i++) {
                mem.writeByte(i, (byte) 0);
            }
            Arrays.fill(dispAdr, -1);

            Z80State state = new Z80State();
            try {
                tmp = fIn.read();	//O
                tmp = fIn.read();	//S
                tmp = fIn.read();	//N skip those (for now without checking)
                version = fIn.read();
                state.setRegI(fIn.read());
                state.setRegLx(fIn.read());
                state.setRegHx(fIn.read());
                state.setRegEx(fIn.read());
                state.setRegDx(fIn.read());
                state.setRegCx(fIn.read());
                state.setRegBx(fIn.read());
                state.setRegFx(fIn.read());
                state.setRegAx(fIn.read());
                state.setRegL(fIn.read());
                state.setRegH(fIn.read());
                state.setRegE(fIn.read());
                state.setRegD(fIn.read());
                state.setRegC(fIn.read());
                state.setRegB(fIn.read());

                tmp = fIn.read();
                tmp |= (fIn.read() << 8);
                state.setRegIY(tmp);
                tmp = fIn.read();
                tmp |= (fIn.read() << 8);
                state.setRegIX(tmp);

                tmp = fIn.read();
                state.setIFF1((tmp & 1) != 0);
                state.setIFF2((tmp & 2) != 0);
                state.setPendingEI((tmp & 4) != 0);
                state.setNMI((tmp & 8) != 0);
                state.setINTLine((tmp & 16) != 0);
                state.setHalted((tmp & 32) != 0);

                state.setRegR(fIn.read());
                state.setRegF(fIn.read());
                state.setRegA(fIn.read());

                tmp = fIn.read();
                tmp |= (fIn.read() << 8);
                state.setRegSP(tmp);
                tmp = fIn.read();
                tmp |= (fIn.read() << 8);
                state.setRegPC(tmp);

                switch (fIn.read()) {
                    case 0:
                        state.setIM(Z80.IntMode.IM0);
                        break;
                    case 1:
                        state.setIM(Z80.IntMode.IM1);
                        break;
                    case 2:
                        state.setIM(Z80.IntMode.IM2);
                        break;
                }

                tmp = fIn.read();
                tmp |= (fIn.read() << 8);
                state.setMemPtr(tmp);

                //cpu_debug("read");
                cpu.setZ80State(state);

                outPort(0x0e, fIn.read());
                outPort(0x0d, fIn.read());
                outPort(0x03, fIn.read());

                cfg.setRomType((byte) fIn.read());

                if (version == 2) {
                    nRozliseni = fIn.read();
                    changeResolution();
                }
                //cpu_debug("read");
            } catch (IOException ex) {
                Logger.getLogger(Ondra.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (cfg.getRomType() == cfg.CUSTOM) {
                res = mem.loadSnapshotCustomRom(fIn);
            }
            res = mem.loadSnapshotRam(fIn);

            for (i = 0xd800; i < 0x10000; i++) {
                processVram(i);
            }

            try {
                fIn.close();
            } catch (IOException ex) {
                Logger.getLogger(Ondra.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            //  String msg =
            //      java.util.ResourceBundle.getBundle("machine/Bundle").getString(
            //     "FILE_RAM_ERROR");
            //  System.out.println(String.format("%s: %s", msg, filename));

        }
    }

    public final void saveSnapshot(String filename) {
        BufferedOutputStream fOut;
        boolean res = false;
        int i = 0, cnt = 0;

        try {
            fOut = new BufferedOutputStream(new FileOutputStream(filename));

            Z80State state = cpu.getZ80State();
            try {
                fOut.write('O');
                fOut.write('S');
                fOut.write('N');
                fOut.write(OSN_VERSION);
                fOut.write(state.getRegI());
                fOut.write(state.getRegLx());
                fOut.write(state.getRegHx());
                fOut.write(state.getRegEx());
                fOut.write(state.getRegDx());
                fOut.write(state.getRegCx());
                fOut.write(state.getRegBx());
                fOut.write(state.getRegFx());
                fOut.write(state.getRegAx());
                fOut.write(state.getRegL());
                fOut.write(state.getRegH());
                fOut.write(state.getRegE());
                fOut.write(state.getRegD());
                fOut.write(state.getRegC());
                fOut.write(state.getRegB());

                fOut.write(state.getRegIY() & 0xff);
                fOut.write((state.getRegIY() >>> 8) & 0xff);
                fOut.write(state.getRegIX() & 0xff);
                fOut.write((state.getRegIX() >>> 8) & 0xff);

                int tmp = 0;
                if (state.isIFF1()) {
                    tmp |= 1;
                }
                if (state.isIFF2()) {
                    tmp |= 2;
                }
                if (state.isPendingEI()) {
                    tmp |= 4;
                }
                if (state.isNMI()) {
                    tmp |= 8;
                }
                if (state.isINTLine()) {
                    tmp |= 16;
                }
                if (state.isHalted()) {
                    tmp |= 32;
                }

                fOut.write(tmp);

                fOut.write(state.getRegR());
                fOut.write(state.getRegF());
                fOut.write(state.getRegA());

                fOut.write(state.getRegSP() & 0xff);
                fOut.write((state.getRegSP() >>> 8) & 0xff);

                fOut.write(state.getRegPC() & 0xff);
                fOut.write((state.getRegPC() >>> 8) & 0xff);

                switch (state.getIM()) {
                    case IM0:
                        fOut.write(0);
                        break;
                    case IM1:
                        fOut.write(1);
                        break;
                    case IM2:
                        fOut.write(2);
                        break;
                }

                fOut.write(state.getMemPtr() & 0xff);
                fOut.write((state.getMemPtr() >>> 8) & 0xff);

                fOut.write(portA0);
                fOut.write(portA1);
                fOut.write(portA3);

                fOut.write(cfg.getRomType());
                fOut.write(nRozliseni);

                //cpu_debug("write");
            } catch (IOException ex) {
                Logger.getLogger(Ondra.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (cfg.getRomType() == cfg.CUSTOM) {
                res = mem.saveSnapshotCustomRom(fOut);
            }
            res = mem.saveSnapshotRam(fOut);
            try {
                fOut.close();
            } catch (IOException ex) {
                Logger.getLogger(Ondra.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            String msg
                    = java.util.ResourceBundle.getBundle("machine/Bundle").getString(
                            "FILE_RAM_ERROR");
            System.out.println(String.format("%s: %s", msg, filename));
            //Logger.getLogger(Spectrum.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public final byte[] saveSnapshotToArray() {

        int index = 0;

        Z80State state = cpu.getZ80State();

        try {
            // Uložení stavu registrů
            snapshotBuffer[index++] = (byte) state.getRegI();
            snapshotBuffer[index++] = (byte) state.getRegLx();
            snapshotBuffer[index++] = (byte) state.getRegHx();
            snapshotBuffer[index++] = (byte) state.getRegEx();
            snapshotBuffer[index++] = (byte) state.getRegDx();
            snapshotBuffer[index++] = (byte) state.getRegCx();
            snapshotBuffer[index++] = (byte) state.getRegBx();
            snapshotBuffer[index++] = (byte) state.getRegFx();
            snapshotBuffer[index++] = (byte) state.getRegAx();
            snapshotBuffer[index++] = (byte) state.getRegL();
            snapshotBuffer[index++] = (byte) state.getRegH();
            snapshotBuffer[index++] = (byte) state.getRegE();
            snapshotBuffer[index++] = (byte) state.getRegD();
            snapshotBuffer[index++] = (byte) state.getRegC();
            snapshotBuffer[index++] = (byte) state.getRegB();

            snapshotBuffer[index++] = (byte) (state.getRegIY() & 0xff);
            snapshotBuffer[index++] = (byte) ((state.getRegIY() >>> 8) & 0xff);
            snapshotBuffer[index++] = (byte) (state.getRegIX() & 0xff);
            snapshotBuffer[index++] = (byte) ((state.getRegIX() >>> 8) & 0xff);

            int tmp = 0;
            if (state.isIFF1()) {
                tmp |= 1;
            }
            if (state.isIFF2()) {
                tmp |= 2;
            }
            if (state.isPendingEI()) {
                tmp |= 4;
            }
            if (state.isNMI()) {
                tmp |= 8;
            }
            if (state.isINTLine()) {
                tmp |= 16;
            }
            if (state.isHalted()) {
                tmp |= 32;
            }
            if (dmaEnabled) {
                tmp |= 64;
            }
            if (nDMAStatus == 1) {
                tmp |= 128;
            }
            snapshotBuffer[index++] = (byte) tmp;

            snapshotBuffer[index++] = (byte) state.getRegR();
            snapshotBuffer[index++] = (byte) state.getRegF();
            snapshotBuffer[index++] = (byte) state.getRegA();

            snapshotBuffer[index++] = (byte) (state.getRegSP() & 0xff);
            snapshotBuffer[index++] = (byte) ((state.getRegSP() >>> 8) & 0xff);

            snapshotBuffer[index++] = (byte) (state.getRegPC() & 0xff);
            snapshotBuffer[index++] = (byte) ((state.getRegPC() >>> 8) & 0xff);

            switch (state.getIM()) {
                case IM0:
                    snapshotBuffer[index++] = 0;
                    break;
                case IM1:
                    snapshotBuffer[index++] = 1;
                    break;
                case IM2:
                    snapshotBuffer[index++] = 2;
                    break;
            }

            snapshotBuffer[index++] = (byte) (state.getMemPtr() & 0xff);
            snapshotBuffer[index++] = (byte) ((state.getMemPtr() >>> 8) & 0xff);

            snapshotBuffer[index++] = (byte) portA0;
            snapshotBuffer[index++] = (byte) portA1;
            snapshotBuffer[index++] = (byte) portA3;

            snapshotBuffer[index++] = (byte) nRozliseni;
            //ulozim t-states
            long lTstates = cpu.clock.getTstates();

            for (int i = 7; i >= 0; i--) {
                snapshotBuffer[index + i] = (byte) (lTstates & 0xFF);
                lTstates >>= 8;
            }
            index += 8;
            // Přímé kopírování RAM        
            byte[][] ramPages = mem.getRamPages();
            for (int page = 0; page < ramPages.length; page++) {
                System.arraycopy(ramPages[page], 0, snapshotBuffer, index, mem.PAGE_SIZE);
                index += mem.PAGE_SIZE;
            }

            return snapshotBuffer;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Error: Index out of bounds at index: " + index);
            throw e;
        }
    }

    public final void loadSnapshotFromArray(byte[] snapshot) {
        int index = 0;

        // Reset VRAM
        /*
        nRozliseni = 255;
        changeResolution();
        for (int i = 0; i < 0x10000; i++) {
            mem.writeByte(i, (byte) 0);
        }
         */
        //Arrays.fill(dispAdr, -1);
       // Arrays.fill(px, (byte) 0);

        Z80State state = new Z80State();

        // Načtení stavu registrů
        state.setRegI(snapshot[index++]);
        state.setRegLx(snapshot[index++]);
        state.setRegHx(snapshot[index++]);
        state.setRegEx(snapshot[index++]);
        state.setRegDx(snapshot[index++]);
        state.setRegCx(snapshot[index++]);
        state.setRegBx(snapshot[index++]);
        state.setRegFx(snapshot[index++]);
        state.setRegAx(snapshot[index++]);
        state.setRegL(snapshot[index++]);
        state.setRegH(snapshot[index++]);
        state.setRegE(snapshot[index++]);
        state.setRegD(snapshot[index++]);
        state.setRegC(snapshot[index++]);
        state.setRegB(snapshot[index++]);

        int tmp = snapshot[index++] & 0xff | ((snapshot[index++] & 0xff) << 8);
        state.setRegIY(tmp);

        tmp = snapshot[index++] & 0xff | ((snapshot[index++] & 0xff) << 8);
        state.setRegIX(tmp);

        tmp = snapshot[index++];
        state.setIFF1((tmp & 1) != 0);
        state.setIFF2((tmp & 2) != 0);
        state.setPendingEI((tmp & 4) != 0);
        state.setNMI((tmp & 8) != 0);
        state.setINTLine((tmp & 16) != 0);
        state.setHalted((tmp & 32) != 0);
        dmaEnabled = (tmp & 64) != 0;
        if ((tmp & 128) != 0) {
            nDMAStatus = 1;
        } else {
            nDMAStatus = 0;
        }

        state.setRegR(snapshot[index++]);
        state.setRegF(snapshot[index++]);
        state.setRegA(snapshot[index++]);

        tmp = snapshot[index++] & 0xff | ((snapshot[index++] & 0xff) << 8);
        state.setRegSP(tmp);

        tmp = snapshot[index++] & 0xff | ((snapshot[index++] & 0xff) << 8);
        state.setRegPC(tmp);

        switch (snapshot[index++]) {
            case 0:
                state.setIM(Z80.IntMode.IM0);
                break;
            case 1:
                state.setIM(Z80.IntMode.IM1);
                break;
            case 2:
                state.setIM(Z80.IntMode.IM2);
                break;
        }

        tmp = snapshot[index++] & 0xff | ((snapshot[index++] & 0xff) << 8);
        state.setMemPtr(tmp);

        cpu.setZ80State(state);
        
        boolean dmaEnabledBckp=dmaEnabled;
        if (dmaEnabled) {
            dmaEnable();
        } else {
            dmaDisable();
        }

        outPort(0x0e, snapshot[index++]);
        outPort(0x0d, snapshot[index++]);
        outPort(0x03, snapshot[index++]);

        nRozliseni = snapshot[index++] & 0xff;
        

        long lTstates = 0;
        for (int i = 0; i < 8; i++) {
            lTstates = (lTstates << 8) | (snapshot[index++] & 0xFF);
        }
        cpu.clock.setTstates(lTstates);

        // Načtení celé paměti RAM
        for (int addr = 0; addr < 0x10000; addr++) {
            mem.writeByte(addr, snapshot[index++]);
        }
        int nDMAStatusBkp=nDMAStatus;
        dmaEnable();
        nDMAStatus=1;
        changeResolution();
        genDispTables();
        for (int i = 0xd800; i < 0x10000; i++) {
            processVram(i);
        }
        if (dmaEnabledBckp) {
            dmaEnable();            
        } else {
            dmaDisable();
        }
        nDMAStatus=nDMAStatusBkp;
    }

    private void cpu_debug(String prefix) {
        System.out.println(String.format("%10s: AF=%04X  BC=%04X  DE=%04X  HL=%04X", prefix, cpu.getRegAF(), cpu.getRegBC(), cpu.getRegDE(), cpu.getRegHL()));
        System.out.println(String.format("            AFx=%04X BCx=%04X DEx=%04X HLx=%04X", cpu.getRegAFx(), cpu.getRegBCx(), cpu.getRegDEx(), cpu.getRegHLx()));
        System.out.println(String.format("            IX=%04X  IY=%04X  SP=%04X  PC=%04X", cpu.getRegIX(), cpu.getRegIY(), cpu.getRegSP(), cpu.getRegPC()));
        System.out.println(String.format("            0e=%02X  0d=%02X  03=%02X",
                portA0, portA1, portA3));
        System.out.println();
    }
}
