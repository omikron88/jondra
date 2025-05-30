/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package machine;

import java.io.File;
import java.io.IOException;
import utils.CswFile;
import utils.TapFile;
import utils.TapeSignalProc;
import utils.WavFile;

/**
 *
 * @author admin
 */
public class Tape implements ClockTimeoutListener {
    
    private Ondra m;
    private TapeSignalProc tsp;
    private int[] tbuff = new int[1];

    private File LoadTape, SaveTape;
    private CswFile lcsw, scsw;
    private WavFile lwav;
    private TapFile ltap;    
    private int lrate, srate;

    private enum st {CLOSE, CSW, WAV, TAP};
    private st lst, sst;
    private boolean record;
    private StringBuilder bitDump = new StringBuilder();
    
    public Tape(Ondra machine) {
        m = machine;
        tsp = new TapeSignalProc(256);
        lst = sst = st.CLOSE;
        record = false;           
    }

    public void openLoadTape(String canonicalPath) {
        LoadTape = new File(canonicalPath);
        if (LoadTape.exists()) {
            try {
                lcsw = CswFile.openCswFile(LoadTape);
                lrate = (int) (2e6 / lcsw.getSampleRate());
                lst = st.CSW;
            } catch (IOException ex) {
                lst = st.CLOSE;
                try {
                    ltap = TapFile.openTapFile(LoadTape);   
                    lrate=4;
                    lst = st.TAP;                   
                 } catch (IOException ex2) {
                   lst = st.CLOSE;                
                   try {
                     lwav = WavFile.openWavFile(LoadTape);
                     lrate = (int) (2e6 / lwav.getSampleRate());
                     lst = st.WAV;
                   } catch (Exception ex1) {
                     lst = st.CLOSE;
                   }
                 }
            }
        }
    }

    public void openSaveTape(String canonicalPath) {
        if (sst==st.CSW) {
            try {
                scsw.close();
            } catch (IOException ex) {
            }
         }
        
        SaveTape = new File(canonicalPath);
        try {
            scsw = CswFile.openCswFile(SaveTape);
            srate = (int) (2e6 / scsw.getSampleRate());
            sst = st.CSW;
            scsw.setRecord(true);
        } catch (IOException ex) {
            sst = st.CLOSE;
        }
    }

    public void tapeStart() {
        if(lst==st.TAP){
        //urychlim nahravani TAPu, zrychlenim Ondry 10x
         m.stopEmulation();
         m.setClockSpeed(2);
         m.startEmulation();
        }
        m.TapeLed.setEnabled(true);
        m.RecButton.setEnabled(false);
        m.clk.addClockTimeoutListener(this);
        m.clk.setTimeout(512);
    }

    public void tapeStop() {
        m.TapeLed.setEnabled(false);
        m.RecButton.setEnabled(true);
        m.clk.removeClockTimeoutListener(this);
    }

    void setTapeMode(boolean rec) {
        record = rec;
    }

@Override
    public void clockTimeout() {
        if (record) {
            if (sst==st.CSW) {
                m.clk.setTimeout(srate);
                scsw.writeSample((m.portA3&0x08)!=0);
            } 
            else {
                m.clk.setTimeout(512);
            }
        }
        else {
            switch (lst) {
                case CSW: {
                    m.clk.setTimeout(lrate);
                    m.mem.setTapeIn(lcsw.readSample());
                    break;
                }
                
                case TAP: {                    
                    if (ltap.bFinished) {
                        //jsem na konci, vypnu urychleni nahravani TAPu zpomalenim Ondry
                       if(m.getClockSpeed()!=20){
                        m.stopEmulation();
                        lst=st.CLOSE;
                        m.TapeLed.setEnabled(false);
                        m.RecButton.setEnabled(true);
                        m.setClockSpeed(20);
                        m.startEmulation(); 
                       }
                    } else {
                        m.clk.setTimeout(lrate);
                        // Uložíme bit do bitDump
                        int sampleValue = ltap.generateFrame() ? 1 : 0;
                        bitDump.append(sampleValue != 0 ? '1' : '0');
                        m.mem.setTapeIn(sampleValue == 1 ? true : false);
                    }                                               
                    break;
                }
                
                case WAV: {
                    m.clk.setTimeout(lrate);
                    try {
                        lwav.readFrames(tbuff, 1);
                    } catch (Exception ex) {
                    } 
                    m.mem.setTapeIn(tsp.addSample(tbuff[0]));

                    break;
                }
                
                default: {
                    m.clk.setTimeout(512);
                }    
            }
        }
    }

    public void closeCleanup() {
        if (sst==st.CSW) {
            try {
                scsw.close();
            } catch (IOException ex) {
            }
        }        
    }
}
