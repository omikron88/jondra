/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package machine;

import java.io.File;
import java.io.IOException;
import utils.CswFile;
import utils.TapeSignalProc;
import utils.WavFile;

/**
 *
 * @author admin
 */
public class Tape {
    
    private Ondra m;
    private TapeSignalProc tsp;
    private int[] tbuff = new int[1];

    private File LoadTape, SaveTape;
    private CswFile lcsw, scsw;
    private WavFile lwav;
    private long lrate, srate;

    private enum st {CLOSE, CSW, WAV};
    private st lst, sst;
    private boolean record;
    
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
                lrate = lcsw.getSampleRate();
                lst = st.CSW;
            } catch (IOException ex) {
                lst = st.CLOSE;
                try {
                    lwav = WavFile.openWavFile(LoadTape);
                    lrate = lwav.getSampleRate();
                    lst = st.WAV;
                } catch (Exception ex1) {
                    lst = st.CLOSE;
                }
            }
        }
    }

    public void openSaveTape(String canonicalPath) {
        SaveTape = new File(canonicalPath);
        try {
            scsw = CswFile.openCswFile(LoadTape);
            srate = scsw.getSampleRate();
            sst = st.CSW;
        } catch (IOException ex) {
            sst = st.CLOSE;
        }
    }

    public void tapeStart() {
        m.TapeLed.setEnabled(true);
        
    }

    public void tapeStop() {
        m.TapeLed.setEnabled(false);
        
    }

    void setTapeMode(boolean rec) {
        
    }
}
