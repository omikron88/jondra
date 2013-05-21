/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/**
 *
 * @author Administrator
 */
public class CswFile {
    
    private final String sig = "Compressed Square Wave";
    private final String app = "JOndra emulator\0"; // must be 16B exactly
    private final long defrate = 22050;
    
    private enum IOState {CLOSED, CREATE, READ, WRITE};
    
    private IOState state = IOState.CLOSED;
    private boolean changed;
    private boolean last;

    private File f;
    private RandomAccessFile r;

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

        csw.r = new RandomAccessFile(csw.f,"rw");

        if (csw.state==IOState.CREATE) {    // create new file
            csw.r.write(csw.sig.getBytes());    // signature
            csw.r.writeByte(0x1a);          // terminator byte
            csw.r.writeByte(2);             // major version
            csw.r.writeByte(0);             // minor version
            csw.rate = csw.defrate;
            csw.pulses = 0;
            csw.changed = false;
            putLong(csw.r, csw.rate);              // sample rate
            putLong(csw.r, csw.pulses);            // num of pulses
            csw.r.writeByte(1);             // compression
            csw.r.writeByte(0);             // flags
            csw.r.writeByte(0);             // extension length
            csw.r.write(csw.app.getBytes());   // app name

            csw.state = IOState.READ;
        }
        else {
            byte[] tmp = new byte[csw.sig.length()];
            
            csw.r.read(tmp);
            if (!Arrays.equals(tmp, csw.sig.getBytes())) {
                throw new IOException("Invalid file signature");
            }

            csw.r.read(tmp, 0, 3);
            if (tmp[0]!=0x1a) {
                throw new IOException("Invalid terminator");
            }
            if (tmp[1]<2) {
                throw new IOException("Invalid version");
            }

            csw.rate = getLong(csw.r);
            csw.pulses = getLong(csw.r);
            csw.changed = false;
            
            csw.r.read(tmp, 0, 3);
            if (tmp[0]!=1) {
                throw new IOException("Unsupported compression");
            }
            csw.r.skipBytes(16);
            
            if (tmp[2]!=0) {
                csw.r.skipBytes((int) (tmp[2] & 0xff));
            }
            
            
            csw.state = IOState.READ;
        }
        
        return csw;
    }
    
    public void close() throws IOException {
        if (changed) { updateCount(); }
        r.close();
        r = null;
        f = null;
        
        state = IOState.CLOSED;
    }
    
    public void setRecord(boolean rec) throws IOException {
        if (state!=IOState.CLOSED) {
            if (changed) { updateCount(); }
            if (rec) {
                r.seek(r.length());
                len = 1;
                state = IOState.WRITE;
            }
            else {
                state = IOState.READ;                
            }
        }
    }
    
    public boolean readSample() {
        if (state==IOState.READ) {
            if (len==0) {
                int b;
                try {
                    b = r.readByte();
                    last = !last;
                    if (b==0) { len = getLong(r); }
                    else { len = (long) b & 0xff; }
                } catch (IOException ex) {

                }
            }
            if (len!=0) { --len; }         
        }
        
        return last;
    }
    
    public void writeSample(boolean s) {
        if (state==IOState.WRITE) {
            changed = true;
            if (s==last) { len++; }
            else if (len!=0) {
                pulses += len;
                if (len>0xff) {
                    try {
                        r.writeByte(0);
                        putLong(r, len);
                    } catch (IOException ex) {

                    }    
                }
                else { 
                    try {
                        r.writeByte((int) (len & 0xff));
                    } catch (IOException ex) {

                    }
                }
                last = s;
                len = 1;
            }
        }
    }
    
    public long getSampleRate() {
        return rate;
    }
    
    private void updateCount() {
        try {
            if (len!=0) writeSample(!last);
            long pos = r.getFilePointer();
            r.seek(0x1d);
            putLong(r, pulses);
            changed = false;
            r.seek(pos);
        } catch (IOException ex) {

        }
    }
    
    private static long getLong(RandomAccessFile rr) 
            throws IOException {
        byte[] bu = new byte[4];
        long val;
        rr.read(bu, 0, 4);
        val = (bu[3] & 0xff);
        val <<= 8;
        val |= (bu[2] & 0xff);
        val <<= 8;
        val |= (bu[1] & 0xff);
        val <<= 8;
        val |= (bu[0] & 0xff);
        return val;
    }

    private static void putLong(RandomAccessFile rr, long l) 
            throws IOException {
        rr.write((int) (l & 0xff));
        rr.write((int) ((l>>8) & 0xff));
        rr.write((int) ((l>>16) & 0xff));
        rr.write((int) ((l>>24) & 0xff));
    }
}
