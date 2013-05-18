/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package machine;

import gui.Screen;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import utils.TapeSignalProc;
import utils.WavFile;
import utils.WavFileException;
import z80core.Z80;

/**
 *
 * @author Administrator
 */
public class Ondra extends Thread 
 implements z80core.MemIoOps, z80core.NotifyOps, ClockTimeoutListener {
    
    private final int T_DMAOFF = 40000;
    private final int T_DMAON  = 10000;
    
    private Screen scr;
    private BufferedImage img;
    private byte px[];
    private Config cfg;
    private Keyboard key;
    private Memory mem;
    private Timer tim;
    private MTimer task;
    private Clock clk;
    private Z80 cpu;
    
    private JLabel GreenLed, YellowLed, TapeLed;
    
    private boolean paused;
    
    private int dispAdr[];
    
    private byte portA0, portA1, portA3;
    private byte iov[];
   
    private int t_frame = T_DMAOFF;
    
    private WavFile wav = null;
    private File loadF = null;
    private boolean tape = false;
    private int tbuff[] = new int[1];
//    private int lastt;
    private TapeSignalProc tsp;
    private int sampleT;
    
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
        dispAdr = new int[10240];
        genDispTables();

        tsp = new TapeSignalProc(256);
        
        paused = true;
        
        Reset(true);
        clk.addClockTimeoutListener(this);
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

    public final void Reset(boolean dirty) {
        portA3 = portA0 = 0;
        t_frame = T_DMAOFF;
        mem.Reset(dirty);
        mem.mapRom(true);
        clk.reset();
        cpu.reset();
        key.Reset();
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
                px[adr++] = mem.readRam((y >>> 1) | ((y&1) << 7) | x ) ;
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
        if (x!=-1) {
            px[x] = (byte) data;
//            scr.repaint(x % 40, x / 40, 8, 1);
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
        port &= 0xff;
//        System.out.println(String.format("In: %02X (%04X)", port,cpu.getRegPC()));
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
            }
            else {
                t_frame = T_DMAON;
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
                if ((wav!=null) && (!tape)) {
                 clk.addClockTimeoutListener(this);
                 clk.setTimeout(sampleT);
                 tape = true;
                 TapeLed.setEnabled(true);
                }
            }
            else {
                if (tape) {
                    clk.removeClockTimeoutListener(this);
                    tape = false;
                    TapeLed.setEnabled(false);
                }
            }
        }
    }

    @Override
    public void contendedStates(int address, int tstates) {
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

    @Override
    public void clockTimeout() {
        clk.setTimeout(sampleT);
        try {
            wav.readFrames(tbuff, 1);
        } catch (IOException ex) {
            Logger.getLogger(Ondra.class.getName()).log(Level.WARNING, null, ex);
        } catch (WavFileException ex) {
            Logger.getLogger(Ondra.class.getName()).log(Level.WARNING, null, ex);
        }
//        System.out.println(tbuff[0]);

//        if (tbuff[0]>(lastt+cfg.getTapeSens())) {
//            mem.setTapeIn(true);
//        }
//        if (tbuff[0]<(lastt-cfg.getTapeSens())) {
//            mem.setTapeIn(false);
//    }
//        lastt = tbuff[0];
  
          mem.setTapeIn(tsp.addSample(tbuff[0]));  
    }
    
    public void openLoadTape(String canonicalPath) throws 
                                IOException, WavFileException {
        if (wav!=null) {
            wav.close();
            wav = null;
        }
        loadF = new File(canonicalPath);
        wav = utils.WavFile.openWavFile(loadF);
        wav.display();
        sampleT = (int) (2e6 / wav.getSampleRate());
    }
}
