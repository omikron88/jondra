/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.io.*;
import java.util.Arrays;

/**
 *
 * @author Administrator
 */
public class CswFile {
    
    private final String sig = "Compressed Square Wave";
    private final String app = "JOndra emulator\0"; // must be 16B exactly
    private final long defrate = 11025;
    
    private enum IOState {CLOSED, CREATE, READ, WRITE};
    
    private IOState state = IOState.CLOSED;
    private boolean changed;
    private boolean last;

    private File f;
    private FileOutputStream os; // Output stream used for writting data
    private FileInputStream is;  // Input stream used for reading data

    private long len;
    private long pulses;
    private long rate;
    
    public static CswFile openCswFile(File file) 
            throws IOException {
        CswFile csw = new CswFile();
        csw.f = file;

        if (!csw.f.exists()) {
            csw.f.createNewFile();
            csw.state = IOState.CREATE;
        }

        csw.is = new FileInputStream(csw.f);
        csw.os = new FileOutputStream(csw.f);
        
        if (csw.state==IOState.CREATE) {    // create new file
            csw.os.write(csw.sig.getBytes());   // signature
            csw.os.write((byte) 0x1a);          // terminator byte
            csw.os.write((byte) 2);             // major version
            csw.os.write((byte) 0);             // minor version
            csw.rate = csw.defrate;
            csw.pulses = 0;
            csw.changed = false;
            putLong(csw.os,csw.rate);           // sample rate
            putLong(csw.os,csw.pulses);         // num of pulses
            csw.os.write((byte) 1);             // compression
            csw.os.write((byte) 0);             // flags
            csw.os.write((byte) 0);             // extension length
            csw.os.write(csw.app.getBytes());   // app name

            csw.state = IOState.READ;
        }
        else {
            byte[] tmp = new byte[csw.sig.length()];
            
            csw.is.read(tmp);
            if (!Arrays.equals(tmp, csw.sig.getBytes())) {
                throw new IOException("Invalid file signature");
            }

            csw.is.read(tmp, 0, 3);
            if (tmp[0]!=0x1a) {
                throw new IOException("Invalid terminator");
            }
            if (tmp[1]<2) {
                throw new IOException("Invalid version");
            }

            csw.rate = getLong(csw.is);
            csw.pulses = getLong(csw.is);
            
            csw.is.read(tmp, 0, 3);
            if (tmp[0]!=1) {
                throw new IOException("Unsupported compression");
            }
            if (tmp[1]<2) {
                throw new IOException("Invalid version");
            }
            
            csw.state = IOState.READ;
        }
        
        return csw;
    }
    
    public void close() throws IOException {
        if (changed) {
            
        }
        os.close();
        is.close();
        os = null;
        is = null;
        f = null;
        
        state = IOState.CLOSED;
    }
    
    private static long getLong(FileInputStream i) 
            throws IOException {
        byte[] bu = new byte[4];
        long val;
        i.read(bu, 0, 4);
        val = (bu[3] & 0xff);
        val <<= 8;
        val |= (bu[2] & 0xff);
        val <<= 8;
        val |= (bu[1] & 0xff);
        val <<= 8;
        val |= (bu[0] & 0xff);
        return val;
    }

    private static void putLong(FileOutputStream o, long l) 
            throws IOException {
        o.write((byte) (l & 0xff));
        o.write((byte) ((l>>8) & 0xff));
        o.write((byte) ((l>>16) & 0xff));
        o.write((byte) ((l>>24) & 0xff));
    }
}
