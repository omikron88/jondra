/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package machine;

import java.util.concurrent.CountDownLatch;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 *
 * @author tmilata
 */
public class Sound {
    
    static double sampleRate = 44100.0;
    //velikost bufferu pro 20ms zvuku
    static int BUFFER_SIZE = (1 * (int) sampleRate / 50);
    //koeficient prepoctu taktu na velikost bufferu
    static double dStates2BufferRatio=(double)(312*128)/(double)(2*BUFFER_SIZE);
    
     //bufer pro prehravani a pro plneni, budou se swapovat
     public SoundBuffer playBuffer = null;
     public SoundBuffer fillBuffer = null;
     //pole se samply
     SoundSample[] samples = new SoundSample[8];     
     //thread, ktery cte data z bufferu a posila je na zvukove zarizeni
     PlayBuffer playThread = null;
     //zvukove zarizeni
     SourceDataLine audioLine = null;
     
     //zvuk povolen/zakazan
     private boolean bEnabled=false;
     
     public void init() {         
        //nacteni samplu pro jednotlive tony     
        samples[0] = new SoundSample(new byte[]{0, 0});
        samples[1] = new SoundSample("/sound/1.sample");
        samples[2] = new SoundSample("/sound/2.sample");
        samples[3] = new SoundSample("/sound/3.sample");
        samples[4] = new SoundSample("/sound/4.sample");
        samples[5] = new SoundSample("/sound/5.sample");
        samples[6] = new SoundSample("/sound/6.sample");
        samples[7] = new SoundSample("/sound/7.sample");
        //inicializace 2 bufferu pro preklapeni - jeden vzdy hraje, druhy se plni 
        playBuffer = new SoundBuffer(0,samples);
        fillBuffer = new SoundBuffer(0,samples);  
        //inicializace prehravaciho zarizeni
        openAudio();
        if (bEnabled) {
         //spusteni threadu pro plneni prehravaciho zarizeni
         playThread = new PlayBuffer();
         playThread.start();
        }
    }
    
    public void deinit() {
        closeAudio();
        playBuffer = null;
        fillBuffer = null;
    }
    
    //otevre audio prehravac
    public void openAudio() {
        if (bEnabled) {
            final boolean bigEndian = false;
            final boolean signed = true;
            final int bits = 16;
            final int channels = 1;
            AudioFormat format = new AudioFormat((float) sampleRate, bits, channels, signed, bigEndian);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            try {
                audioLine = (SourceDataLine) AudioSystem.getLine(info);
                audioLine.open(format, 3 * BUFFER_SIZE);
                audioLine.start();
            } catch (Exception ex) {
                //pocitac nema zvukovou kartu, nebo neumi uvedeny audioformat
                bEnabled = false;
            }
        }
    }

    //ukonci audio prehravac
    public void closeAudio() {
        if (bEnabled) {
            if (audioLine != null) {
                audioLine.flush();
                audioLine.drain();
                audioLine.close();
                audioLine = null;
            }
        }
    }
    
    public void setEnabled(boolean bVal){
        bEnabled=bVal;
    }
    
    public boolean isEnabled(){
        return bEnabled;
    }
    
    //vyprazdni playBuffer s prenosem aktivniho samplu
    //a prohodi oba buffery
    public void switchBuffers(long nInitTStates) {
        SoundBuffer tmpBuffer=playBuffer;
        tmpBuffer.emptyTransferActiveSample(nInitTStates,fillBuffer);
        playBuffer=fillBuffer;
        fillBuffer=tmpBuffer;        
    }
    
   //dava prehravacimu threadu vedet, ze jsou data v bufferu pripravena pro prenos na zarizeni
   public void setDataReady() {
    playThread.setDataReady();
   }
    
    //posila Buffer na zarizeni v samostatnem threadu
    private class PlayBuffer extends Thread {
        private CountDownLatch nTransfer = new CountDownLatch(1);
        
        public void setDataReady() {
           nTransfer.countDown();
        }

        public void run() {
            if (audioLine != null) {
                while (true) {                  
                    try {
                        nTransfer.await();
                    } catch (InterruptedException ex) {
                    }
                    audioLine.write(playBuffer.data, 0, playBuffer.data.length);  
                    nTransfer=new CountDownLatch(1);
                }
            }
        }
    }




}
