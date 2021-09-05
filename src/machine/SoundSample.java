/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package machine;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author tmilata
 */
//umoznuje praci se samplem - coz je wav bez headeru
public class SoundSample {

    byte[] sample = null;
    int nLen;
    int nPos;

    public SoundSample(byte[] inData) {
        sample = inData;
        nLen = sample.length;
        nPos = 0;
    }

    public SoundSample(String strPath) {
        loadFromResource(strPath);
        nLen = sample.length;
        nPos = 0;
    }

    public void loadFromResource(String strPath) {
        InputStream is = SoundSample.class.getResourceAsStream(strPath);
        if (is != null) {
            try {
                sample = new byte[is.available()];
                is.read(sample);
                is.close();
            } catch (IOException ex) {
            }
        }
    }

    public void resetposition() {
        nPos = 0;
    }


    public byte getNextByte() {
        byte bRet = sample[nPos];
        nPos++;
        if (nPos >= nLen) {
            nPos = 0;
        }
        return bRet;
    }
}
