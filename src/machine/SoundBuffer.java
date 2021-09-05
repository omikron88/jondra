/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package machine;

import java.util.Arrays;

/**
 *
 * @author tmilata
 */
//Buffer pro zvukova data
public class SoundBuffer {
    
    byte[] data = null;
    int nActiveSample;
    
    //pocatecni stav pocitadla t-states
    public long lInitTStates;
    SoundSample[] samples;

    public SoundBuffer(long lInitTStates,SoundSample[] InSamples) {
        this.lInitTStates = lInitTStates;
        samples=InSamples;
        data = new byte[2 * Sound.BUFFER_SIZE];//generuji 16-bitovy wav, proto 2*velikost bufferu                          
        nActiveSample = 0;
        Arrays.fill(data, (byte) 0);
    }
    
    //naplni buffer samplem od pozice vypoctene podle taktu do konce bufferu
    public void fillWithSample(int nSampleNum, long lTstates) {
        if (nActiveSample != nSampleNum) {
            samples[nActiveSample].resetposition();
            nActiveSample = nSampleNum;   
        }
        long lStart = (long) ((double) (lTstates - lInitTStates) / Sound.dStates2BufferRatio);
        if ((lStart % 2) != 0) {
            lStart--;
        }        
        for (int i = (int) lStart; i < 2 * Sound.BUFFER_SIZE; i++) {
            data[i] = samples[nSampleNum].getNextByte();
        }
    }

    //vyprazdni buffer
    public void emptyBuffer(long InInitTStates) {
        lInitTStates = InInitTStates;
        nActiveSample = 0;
        Arrays.fill(data, (byte) 0);
    }
    
    public void fillAllWithActiveSample(){
        for (int i = 0; i < 2 * Sound.BUFFER_SIZE; i++) {
            data[i] = samples[nActiveSample].getNextByte();
        }
    }

    //vyprazdni buffer, prenese aktivni sampl do noveho bufferu
    public void emptyTransferActiveSample(long InInitTStates, SoundBuffer bfrLast) {
        emptyBuffer(InInitTStates);
        if (bfrLast != null) {
            nActiveSample = bfrLast.nActiveSample;
        } 
       fillAllWithActiveSample();
    }
    
    
}
