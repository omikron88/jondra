/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package machine;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Memory {

    public final int PAGE_SIZE = 2048;
    public final int PAGE_MASK = PAGE_SIZE - 1;
    public final byte PAGE_BIT = 11;

    public final byte tapes = (byte) 0x80;
    public final byte taper = (byte) ~tapes;
    
    private byte[][] Ram = new byte[32][PAGE_SIZE];
    private byte[][] Basic = new byte[8][PAGE_SIZE];
    private byte[][] Tesla = new byte[2][PAGE_SIZE];
    private byte[][] Vili  = new byte[2][PAGE_SIZE];
    private byte[][] Cust  = new byte[8][PAGE_SIZE];
    
    private byte[][] readPages = new byte[32][];
    private byte[][] writePages = new byte[32][];
    private byte[]   fakeROM = new byte[PAGE_SIZE];   // for "write" to ROM 
    private byte[]   IOVect;                          // memory mapped IO 
 
    private Ondra m;
    private Config cf;
    
    public Memory(Ondra machine, Config cnf) {
        m = machine;
        cf = cnf;
        loadRoms();
    }

    void setIOVect(byte[] iov) {
        IOVect = iov;
    }

    void setTapeIn(boolean x) {
        if (x) {            
            for(int n=0; n<PAGE_SIZE; n++) {
                IOVect[n] |= tapes;                
            }
        }
        else {
            for(int n=0; n<PAGE_SIZE; n++) {
                IOVect[n] &= taper;                
            }
        }
    }
    
    public void Reset(boolean dirty) {
        if (dirty) {
            char c = 0;
            int a = 0;
            for(int i=0; i<32; i++) {
                for(int j=0; j<PAGE_SIZE; j++) {
                    Ram[i][j] = (byte) c;
                    a = a++ & 127;
                    if (a==0) { c ^= 255; };
                }
            }
        }
        
        if (cf.getRomType()==cf.CUSTOM) {
            for(int i=0; i<PAGE_SIZE; i++) {
                Cust[0][i] = (byte) 255;
                Cust[1][i] = (byte) 255;
                Cust[2][i] = (byte) 255;
                Cust[3][i] = (byte) 255;
                Cust[4][i] = (byte) 255;
                Cust[5][i] = (byte) 255;
                Cust[6][i] = (byte) 255;
                Cust[7][i] = (byte) 255;    
            }
            loadCustomRom(cf.getRomA(), 0);
            loadCustomRom(cf.getRomB(), 4);
        }
        
        for(int i=0; i<32; i++) {
                readPages[i] = writePages[i] = Ram[i];   
            }
    }        
      
    public byte readRam(int address) {
        return Ram[address >>> PAGE_BIT][address & PAGE_MASK];
    }

    public byte readByte(int address) {
        return readPages[address >>> PAGE_BIT][address & PAGE_MASK];
    }
    
    public void writeByte(int address, byte value) {
        writePages[address >>> PAGE_BIT][address & PAGE_MASK] = value;
        if (address>=0xd800) {
            m.processVram(address, value);
        }
    }
    
    public void mapIO(boolean state) {
        if (!state) {
            readPages[28] = writePages[28] = Ram[28];
            readPages[29] = writePages[29] = Ram[29];
            readPages[30] = writePages[30] = Ram[30];
            readPages[31] = writePages[31] = Ram[31];
        }
        else {
            writePages[28] = fakeROM;
            writePages[29] = fakeROM;
            writePages[30] = fakeROM;
            writePages[31] = fakeROM;

            readPages[28] = IOVect;
            readPages[29] = IOVect;
            readPages[30] = IOVect;
            readPages[31] = IOVect;
        }
    }
    
    public void mapRom(boolean state) {
        if (!state) {
            readPages[0] = writePages[0] = Ram[0];
            readPages[1] = writePages[1] = Ram[1];
            readPages[2] = writePages[2] = Ram[2];
            readPages[3] = writePages[3] = Ram[3];
            readPages[4] = writePages[4] = Ram[4];
            readPages[5] = writePages[5] = Ram[5];
            readPages[6] = writePages[6] = Ram[6];
            readPages[7] = writePages[7] = Ram[7];
        }
        else {
            writePages[0] = fakeROM;
            writePages[1] = fakeROM;
            writePages[2] = fakeROM;
            writePages[3] = fakeROM;
            writePages[4] = fakeROM;
            writePages[5] = fakeROM;
            writePages[6] = fakeROM;
            writePages[7] = fakeROM;
            switch(cf.getRomType()) {
                case 0: {               // BASIC
                    readPages[0] = Basic[0];
                    readPages[1] = Basic[1];
                    readPages[2] = Basic[2];
                    readPages[3] = Basic[3];
                    readPages[4] = Basic[4];
                    readPages[5] = Basic[5];
                    readPages[6] = Basic[6];
                    readPages[7] = Basic[7];
                    break;
                }
                
                case 1: {               // TESLA
                    readPages[0] = Tesla[0];
                    readPages[1] = Tesla[0];
                    readPages[2] = Tesla[0];
                    readPages[3] = Tesla[0];
                    readPages[4] = Tesla[1];
                    readPages[5] = Tesla[1];
                    readPages[6] = Tesla[1];
                    readPages[7] = Tesla[1];
                    break;
                }
                    
                case 2: {               // ViLi
                    readPages[0] = Vili[0];
                    readPages[1] = Vili[0];
                    readPages[2] = Vili[0];
                    readPages[3] = Vili[0];
                    readPages[4] = Vili[1];
                    readPages[5] = Vili[1];
                    readPages[6] = Vili[1];
                    readPages[7] = Vili[1];
                    break;
                }
                 
                default: {               // Custom
                    readPages[0] = Cust[0];
                    readPages[1] = Cust[1];
                    readPages[2] = Cust[2];
                    readPages[3] = Cust[3];
                    readPages[4] = Cust[4];
                    readPages[5] = Cust[5];
                    readPages[6] = Cust[6];
                    readPages[7] = Cust[7];
                }
            }
        }
    }

    private void loadCustomRom(String name, int page ) {
        File f = new File(name);
        int size = (int) f.length();
        boolean res = false;
        
        switch(size) {
            case 2048: {
                res = loadRomAsFile(name, Cust, page + 0, PAGE_SIZE * 1);
                if (!res) break;
                loadRomAsFile(name, Cust, page + 1, PAGE_SIZE * 1);
                loadRomAsFile(name, Cust, page + 2, PAGE_SIZE * 1);
                loadRomAsFile(name, Cust, page + 3, PAGE_SIZE * 1);
                break;
            }
            
            case 4096: {
                res = loadRomAsFile(name, Cust, page + 0, PAGE_SIZE * 2);
                if (!res) break;
                loadRomAsFile(name, Cust, page + 2, PAGE_SIZE * 2);
                break;
            }
                
            case 8192: {
                res = loadRomAsFile(name, Cust, page + 0, PAGE_SIZE * 4);
                break;
            }
                
            default: {
                String msg =
                    java.util.ResourceBundle.getBundle("machine/Bundle").getString(
                    "ROM_SIZE_ERROR");
                System.out.println(String.format("%s: %s", msg, name));    
            }

        }
    }
    
    private void loadRoms() {
        String rd = cf.getRomsDirectory();

        if (!loadRomAsFile(rd+cf.getBasicA(), Basic, 0, PAGE_SIZE * 4)) {
            loadRomAsResource("/roms/Ondra_BASICEXP_V5_a.rom", Basic, 0, PAGE_SIZE * 4);
        }
        if (!loadRomAsFile(rd+cf.getBasicB(), Basic, 4, PAGE_SIZE * 4)) {
            loadRomAsResource("/roms/Ondra_BASICEXP_V5_b.rom", Basic, 4, PAGE_SIZE * 4);
        }

        if (!loadRomAsFile(rd+cf.getTeslaA(), Tesla, 0, PAGE_SIZE * 1)) {
            loadRomAsResource("/roms/Ondra_TESLA_V5_a.rom", Tesla, 0, PAGE_SIZE * 1);
        }
        if (!loadRomAsFile(rd+cf.getTeslaB(), Tesla, 1, PAGE_SIZE * 1)) {
            loadRomAsResource("/roms/Ondra_TESLA_V5_b.rom", Tesla, 1, PAGE_SIZE * 1);
        }

        if (!loadRomAsFile(rd+cf.getViLiA(), Vili, 0, PAGE_SIZE * 1)) {
            loadRomAsResource("/roms/Ondra_ViLi_v27_a.rom", Vili, 0, PAGE_SIZE * 1);
        }
        if (!loadRomAsFile(rd+cf.getTeslaB(), Vili, 1, PAGE_SIZE * 1)) {
            loadRomAsResource("/roms/Ondra_ViLi_v27_b.rom", Vili, 1, PAGE_SIZE * 1);
        }
    }

    private boolean loadRomAsResource(String filename, byte[][] rom, int page, int size) {

        InputStream inRom = Ondra.class.getResourceAsStream(filename);
        boolean res = false;

        if (inRom == null) {
            String msg =
                java.util.ResourceBundle.getBundle("machine/Bundle").getString(
                "RESOURCE_ROM_ERROR");
            System.out.println(String.format("%s: %s", msg, filename));
            return false;
        }

        try {
            for (int frag = 0; frag < size / PAGE_SIZE; frag++) {
                int count = 0;
                while (count != -1 && count < PAGE_SIZE) {
                    count += inRom.read(rom[page + frag], count, PAGE_SIZE - count);
                }

                if (count != PAGE_SIZE) {
                    String msg =
                        java.util.ResourceBundle.getBundle("machine/Bundle").getString(
                        "ROM_SIZE_ERROR");
                    System.out.println(String.format("%s: %s", msg, filename));
                } else {
                    res = true;
                }
            }
        } catch (IOException ex) {
            String msg =
                java.util.ResourceBundle.getBundle("machine/Bundle").getString(
                "RESOURCE_ROM_ERROR");
            System.out.println(String.format("%s: %s", msg, filename));
            Logger.getLogger(Ondra.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                inRom.close();
            } catch (IOException ex) {
                Logger.getLogger(Memory.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (res) {
            String msg =
                java.util.ResourceBundle.getBundle("machine/Bundle").getString(
                "ROM_RESOURCE_LOADED");
            System.out.println(String.format("%s: %s", msg, filename));
        }

        return res;
    }

    private boolean loadRomAsFile(String filename, byte[][] rom, int page, int size) {
        BufferedInputStream fIn = null;
        boolean res = false;

        try {
            try {
                fIn = new BufferedInputStream(new FileInputStream(filename));
            } catch (FileNotFoundException ex) {
                String msg =
                    java.util.ResourceBundle.getBundle("machine/Bundle").getString(
                    "FILE_ROM_ERROR");
                System.out.println(String.format("%s: %s", msg, filename));
                //Logger.getLogger(Spectrum.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }

            for (int frag = 0; frag < size / PAGE_SIZE; frag++) {
                int count = 0;
                while (count != -1 && count < PAGE_SIZE) {
                    count += fIn.read(rom[page + frag], count, PAGE_SIZE - count);
                }

                if (count != PAGE_SIZE) {
                    String msg =
                        java.util.ResourceBundle.getBundle("machine/Bundle").getString(
                        "ROM_SIZE_ERROR");
                    System.out.println(String.format("%s: %s", msg, filename));
                } else {
                    res = true;
                }
            }
        } catch (IOException ex) {
            String msg =
                java.util.ResourceBundle.getBundle("machine/Bundle").getString(
                "FILE_ROM_ERROR");
            System.out.println(String.format("%s: %s", msg, filename));
            Logger.getLogger(Ondra.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (fIn != null) {
                    fIn.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Memory.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (res) {
            String msg =
                java.util.ResourceBundle.getBundle("machine/Bundle").getString(
                "ROM_FILE_LOADED");
            System.out.println(String.format("%s: %s", msg, filename));
        }

        return res;
    }

    public boolean loadSnapshotRam(BufferedInputStream fIn) {
        boolean res = true;
        int i;

        for (i=0; i<32; i++) {
            try {
                fIn.read(Ram[i]);
            } catch (IOException ex) {
                Logger.getLogger(Memory.class.getName()).log(Level.SEVERE, null, ex);
                res = false;
                break;
            }
        }
        return res;
    }

    public boolean saveSnapshotRam(BufferedOutputStream fOut) {
        boolean res = true;
        int i;

        for (i=0; i<32; i++) {
            try {
                fOut.write(Ram[i]);
            } catch (IOException ex) {
                Logger.getLogger(Memory.class.getName()).log(Level.SEVERE, null, ex);
                res = false;
                break;
            }
        }
        return res;
    }

    public boolean loadSnapshotCustomRom(BufferedInputStream fIn) {
        boolean res = true;
        int i;

        for (i=0; i<8; i++) {
            try {
                fIn.read(Cust[i]);
            } catch (IOException ex) {
                Logger.getLogger(Memory.class.getName()).log(Level.SEVERE, null, ex);
                res = false;
                break;
            }
        }
        return res;
    }

    public boolean saveSnapshotCustomRom(BufferedOutputStream fOut) {
        boolean res = true;
        int i;

        for (i=0; i<8; i++) {
            try {
                fOut.write(Cust[i]);
            } catch (IOException ex) {
                Logger.getLogger(Memory.class.getName()).log(Level.SEVERE, null, ex);
                res = false;
                break;
            }
        }
        return res;
    }
}