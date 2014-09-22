/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package machine;

import gui.Screen;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
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
    
    private final int T_DMAOFF = 40000;
    private final int T_DMAON  = 10000;
    
    private final int OSN_VERSION = 0x01; //version 0.1
    
    private Screen scr;
    private BufferedImage img;
    private byte px[];
    private Config cfg;
    private Keyboard key;
    public  Memory mem;
    private Timer tim;
    private MTimer task;
    public  Clock clk;
    private Z80 cpu;
    private Tape tap;
    
    public JLabel GreenLed, YellowLed, TapeLed;
    public JToggleButton RecButton;
    
    private boolean paused;
    
    private int dispAdr[];
    
    public  boolean dmaEnabled;
    public  byte portA0, portA1, portA3;
    private byte iov[];
   
    private int t_frame = T_DMAOFF;
    
    private boolean tapestart;
    
    public Ondra() {
        img = new BufferedImage(320, 256, BufferedImage.TYPE_BYTE_BINARY);
        px = 
            ((DataBufferByte) img.getRaster().getDataBuffer()).getBankData()[0];
        cfg = new Config();
        mem = new Memory(this, cfg);
        tim = new Timer("Timer");
        clk = new Clock();
        cpu = new Z80(clk, this, this);
        iov = new byte[mem.PAGE_SIZE];
        mem.setIOVect(iov);
        key = new Keyboard(iov);
        dmaEnabled = true;
        dispAdr = new int[10240];
        genDispTables();
        tap = new Tape(this);
        
        paused = true;
        
        Reset(true);
    }
    
    public void setConfig(Config c) {
        if (!cfg.equals(c)) {
            cfg = c;
            Reset(false);
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
    
    public Keyboard getKeyboard() {
        return key;
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

    public final void Reset(boolean dirty) {
        portA3 = portA0 = 0;
        t_frame = T_DMAOFF;
        mem.Reset(dirty);
        mem.mapRom(true);
        clk.reset();
        cpu.reset();
        key.Reset();
        tapestart = false;
        if (RecButton!=null) { tap.tapeStop(); }
    }
    
    public final void Nmi() {
        cpu.setNMI(true);
        cpu.execute(clk.getTstates()+8);
        cpu.setNMI(false);        
    }

    public void startEmulation() {
        if (!paused)
            return;
        
        scr.repaint();
        paused = false;
        task = new MTimer(this);
        tim.scheduleAtFixedRate(task, 250, 20);
       }
    
    public void stopEmulation() {
        if (paused)
            return;
        
        paused = true;
        task.cancel();
    }
    
    public boolean isPaused() {
        return paused;
    }

    private void genDispTables() {

        for(int n=0;n<10240;n++) {
            dispAdr[n] = -1;
        }
        
        int adr = 0;
        int vm;
        for(int y=255;y!=0;y--) {
            for (int x=0xff00; x!=0xd700; x-=0x0100) {
                vm = (y >>> 1) | ((y&1) << 7) | x;
                dispAdr[vm - 0xd800] = adr;
//                System.out.println(String.format("%04X", vm));
                //if (dmaEnabled) //not needed as this function is called just from the constructor and in that time dmaEnabled=true
                px[adr++] = mem.readRam(vm) ;
            }
        }
    }
    
    public void ms20() {        
        if (!paused) {

            scr.repaint();
            cpu.setINTLine(true);
            cpu.execute(clk.getTstates()+16);
            cpu.setINTLine(false);            
            cpu.execute(clk.getTstates()+t_frame);
            
        }  
    }
            
    @Override
    public void run() {
        startEmulation();
        
        boolean forever = true;
        while(forever) {
            try {
                sleep(Long.MAX_VALUE);
            } catch (InterruptedException ex) {
                Logger.getLogger(Ondra.class.getName()).log(Level.SEVERE, null, ex);
            }
        }        
    }    

    public void processVram(int address, int data) {
        data &= 0xff;
        int x = dispAdr[address - 0xd800];
        if (x!=-1 && dmaEnabled) {
            px[x] = (byte) data;
//            scr.repaint(x % 40, x / 40, 8, 1);
        }
    }
    
    private void dmaEnable() {
        for (int address=0xd800; address<0x10000; address++) {
            int x = dispAdr[address - 0xd800];
            if (x!=-1) {
                px[x] = mem.readRam(address) ;
            }
        }
        dmaEnabled = true;
    }
    
    private void dmaDisable() {
        dmaEnabled = false;
        for (int address=0xd800; address<0x10000; address++) {
            int x = dispAdr[address - 0xd800];
            if (x!=-1) {
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
        clk.addTstates(3);
        int value = mem.readByte(address) & 0xff;
//        if (address==0xffff) {
//        System.out.println(String.format("Peek: %04X,%02X (%04X)", address,value,cpu.getRegPC()));            
//        }
        return value;
    }

    @Override
    public void poke8(int address, int value) {
//        System.out.println(String.format("Poke: %04X,%02X (%04X)", address,value,cpu.getRegPC()));
        clk.addTstates(3);
        mem.writeByte(address, (byte) value);
    }

    @Override
    public int peek16(int address) {
        clk.addTstates(6);
        int lsb = mem.readByte(address) & 0xff;
        address = (address+1) & 0xffff;
        return ((mem.readByte(address) << 8) & 0xff00 | lsb);
    }

    @Override
    public void poke16(int address, int word) {
        clk.addTstates(6);
        mem.writeByte(address, (byte) word);
        address = (address+1) & 0xffff;
        mem.writeByte(address, (byte) (word >>> 8));
    }

    @Override
    public int inPort(int port) {
        clk.addTstates(4);
//        System.out.println(String.format("In: %04X (PC=%04X, portA0=%02X, portA1=%02X, portA3=%02X)", port,cpu.getRegPC(), portA0, portA1, portA3));
        return 0xff;
    }

    @Override
    public void outPort(int port, int value) {
        clk.addTstates(4);
        port &= 0xff;
        value &= 0xff;
//        System.out.println(String.format("Out: %02X,%02X (%04X)", port,value,cpu.getRegPC()));
        if ((port & 0x08)==0) {
            portA3 = (byte) value;
            if ((portA3&0x01)==0) {
                t_frame = T_DMAOFF;
                dmaDisable();
            }
            else {
                t_frame = T_DMAON;
                dmaEnable();
            }
            if ((portA3&0x02)==0) {
                mem.mapRom(true);
            }
            else {
                mem.mapRom(false);
            }
            if ((portA3&0x04)==0) {
                mem.mapIO(false);
            }
            else {
                mem.mapIO(true);
            }
        }
        if ((port & 0x01)==0) {
            portA0 = (byte) value;
            if ((portA0&0x01)==0) {
                GreenLed.setEnabled(true);
            }
            else {
                GreenLed.setEnabled(false);
            }
            if ((portA0&0x02)==0) {
                YellowLed.setEnabled(true);
            }
            else {
                YellowLed.setEnabled(false);
            }
            if ((portA0&0x10)!=0) {
                if (!tapestart) {
                 tapestart = true;
                 tap.tapeStart();
                }
            }
            else {
                if (tapestart) {
                    tapestart = false;
                    tap.tapeStop();
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
        int i=0, cnt=0, tmp;
        int version;
        
        try {
            fIn = new BufferedInputStream(new FileInputStream(filename));
            
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
                    case 0: state.setIM(Z80.IntMode.IM0); break;
                    case 1: state.setIM(Z80.IntMode.IM1); break;
                    case 2: state.setIM(Z80.IntMode.IM2); break;
                }
                
                tmp = fIn.read();
                tmp |= (fIn.read() << 8);
                state.setMemPtr(tmp);
                
                //cpu_debug("read");
                
                cpu.setZ80State(state);
                
                outPort(0x0e, fIn.read());
                outPort(0x0d, fIn.read());
                outPort(0x03, fIn.read());
                
                cfg.setRomType((byte)fIn.read());
                
                //cpu_debug("read");
            } catch (IOException ex) {
                Logger.getLogger(Ondra.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (cfg.getRomType() == cfg.CUSTOM) {
                res = mem.loadSnapshotCustomRom(fIn);
            }
            res = mem.loadSnapshotRam(fIn);
            
            for (i=0xd800; i<0x10000; i++) {
                processVram(i, mem.readRam(i));
            }
            
            try {
                fIn.close();
            } catch (IOException ex) {
                Logger.getLogger(Ondra.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            String msg =
                java.util.ResourceBundle.getBundle("machine/Bundle").getString(
                "FILE_RAM_ERROR");
            System.out.println(String.format("%s: %s", msg, filename));
            //Logger.getLogger(Spectrum.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public final void saveSnapshot(String filename) {
        BufferedOutputStream fOut;
        boolean res = false;
        int i=0, cnt=0;
        
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
                if (state.isIFF1())      { tmp |= 1; }
                if (state.isIFF2())      { tmp |= 2; }
                if (state.isPendingEI()) { tmp |= 4; }
                if (state.isNMI())       { tmp |= 8; }
                if (state.isINTLine())   { tmp |= 16; }
                if (state.isHalted())    { tmp |= 32; }
                
                fOut.write(tmp);
                
                fOut.write(state.getRegR());
                fOut.write(state.getRegF());
                fOut.write(state.getRegA());
                
                fOut.write(state.getRegSP() & 0xff);
                fOut.write((state.getRegSP() >>> 8) & 0xff);
                
                fOut.write(state.getRegPC() & 0xff);
                fOut.write((state.getRegPC() >>> 8) & 0xff);
                
                switch (state.getIM()) {
                    case IM0: fOut.write(0); break;
                    case IM1: fOut.write(1); break;
                    case IM2: fOut.write(2); break;
                }
                
                fOut.write(state.getMemPtr() & 0xff);
                fOut.write((state.getMemPtr() >>> 8) & 0xff);
                
                fOut.write(portA0);
                fOut.write(portA1);
                fOut.write(portA3);
                
                fOut.write(cfg.getRomType());
                
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
            String msg =
                java.util.ResourceBundle.getBundle("machine/Bundle").getString(
                "FILE_RAM_ERROR");
            System.out.println(String.format("%s: %s", msg, filename));
            //Logger.getLogger(Spectrum.class.getName()).log(Level.SEVERE, null, ex);
        }
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
