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
    public int nActiveSample;

    //pocatecni stav pocitadla t-states
    public long lInitTStates;
    SoundSample[] samples;

    public SoundBuffer(long lInitTStates, SoundSample[] InSamples) {
        this.lInitTStates = lInitTStates;
        samples = InSamples;
        data = new byte[2 * Sound.BUFFER_SIZE];//generuji 16-bitovy wav, proto 2*velikost bufferu                          
        nActiveSample = 0;
        Arrays.fill(data, (byte) 0);
    }

    //naplni buffer samplem od pozice vypoctene podle taktu do konce bufferu
    public void fillWithSample(int nSampleNum, long lTstates) {
        int nLastSample = nActiveSample;
        if (nActiveSample != nSampleNum) {
            samples[nActiveSample].resetposition();
            nActiveSample = nSampleNum;
        }
        if (nLastSample != nSampleNum) {
            long lStart = (long) ((double) (lTstates - lInitTStates) / Sound.dStates2BufferRatio);
            if ((lStart % 2) != 0) {
                lStart--;
            }
            if ((lStart >= 0) && (lStart < 2 * Sound.BUFFER_SIZE)) {
                for (int i = (int) lStart; i < 2 * Sound.BUFFER_SIZE; i++) {
                    data[i] = samples[nSampleNum].getNextByte();
                }
            }
        }
    }

    //vyprazdni buffer
    public void emptyBuffer(long InInitTStates) {
        lInitTStates = InInitTStates;
        nActiveSample = 0;
        Arrays.fill(data, (byte) 0);
    }

    //naplni cely buffer aktivnim samplem
    public void fillAllWithActiveSampleOld() {
        int bufferOffset = 0;
        int remaining = 2 * Sound.BUFFER_SIZE;
        while (remaining > 0) {
            int bytesToCopy = Math.min(remaining, samples[nActiveSample].nLen - samples[nActiveSample].nPos);
            System.arraycopy(samples[nActiveSample].sample, samples[nActiveSample].nPos, data, bufferOffset, bytesToCopy);

            bufferOffset += bytesToCopy;
            remaining -= bytesToCopy;
            samples[nActiveSample].nPos = (samples[nActiveSample].nPos + bytesToCopy) % samples[nActiveSample].nLen;
        }

    }
    
    public void fillAllWithActiveSampleFst() {
    byte[] sampleData = samples[nActiveSample].sample;
    int sampleLength = samples[nActiveSample].nLen;
    int pos = samples[nActiveSample].nPos;

    for (int i = 0; i < 2 * Sound.BUFFER_SIZE; i++) {
        data[i] = sampleData[pos];
        pos = (pos + 1) % sampleLength;
    }

    samples[nActiveSample].nPos = pos;
}

        public void fillAllWithActiveSample() {
    byte[] sampleData = samples[nActiveSample].sample;
    int sampleLength = samples[nActiveSample].nLen;
    int pos = samples[nActiveSample].nPos;

    for (int i = 0; i < 2 * Sound.BUFFER_SIZE; i++) {
        data[i] = sampleData[pos];
        pos = (pos + 1) % sampleLength;
    }

    samples[nActiveSample].nPos = pos;
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
