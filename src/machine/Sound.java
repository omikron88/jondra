/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package machine;

import java.util.Timer;
import java.util.TimerTask;
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
     Object objDeinit=new Object();
     
    int FULL_BUFFER_SIZE=6*BUFFER_SIZE;
    //udrzuje datovy tok na zukove zarizeni nepreruseny
    public SoundGuard guard=null;
    //limit pod ktery nesmi klesnout buffer zvukoveho zarizeni, jinak se zacnou posilat 0
    public int limit5ms = 2*(int)sampleRate/100;
    //blok ticha - same 0
    private byte[] silent = new byte[limit5ms];
    //indikuje prvni zaslani zvuku z Ondry, aby bylo mozno vlozit 10 sec. ticha na zacatek
    public boolean bFirstFill=true;
    
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
             startPlaying();
             //thread, ktery kazde 2 msec. kontroluje, jestli je v bufferu zvuk.zarizeni dost dat
             Timer tim = new Timer("GuardTimer");
             guard=new SoundGuard();
             tim.scheduleAtFixedRate(guard, 1, 2);

         }
    }
    
    public void deinit() {
        guard.cancel();
        guard=null;
        playThread.setDataReady();
        playThread.setFinish();
        try {
            synchronized(objDeinit){
             objDeinit.wait();
            }
        } catch (InterruptedException ex) {         
        }
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
                audioLine.open(format, FULL_BUFFER_SIZE);
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
        if(bFirstFill){
             //vlozim 10ms ticha na zacatek, abych mel pak cas pri vypadku provest nejake kroky
             bFirstFill=false;
             fillBufferByZero();
         }
        SoundBuffer tmpBuffer=playBuffer;
        tmpBuffer.emptyTransferActiveSample(nInitTStates,fillBuffer);
        playBuffer=fillBuffer;
        fillBuffer=tmpBuffer;        
    }
    
     public void startPlaying() {
        if (audioLine == null) {
          openAudio();  
        }
        //spusteni threadu pro plneni prehravaciho zarizeni
        playThread = new PlayBuffer();
        playThread.start();
    }
    
   //dava prehravacimu threadu vedet, ze jsou data v bufferu pripravena pro prenos na zarizeni
   public void setDataReady() {
    playThread.setDataReady();
   }
    
    //posila Buffer na zarizeni v samostatnem threadu
    private class PlayBuffer extends Thread {
        private boolean bRunning=true;
        private CountDownLatch nTransfer = new CountDownLatch(1);
        
        public void setDataReady() {
           nTransfer.countDown();
        }
        
        public void setFinish() {
           bRunning=false;
        }

        public void run() {
            if (audioLine != null) {
                while (bRunning) {                  
                    try {
                        nTransfer.await();
                    } catch (InterruptedException ex) {
                    }
                     audioLine.write(playBuffer.data, 0, playBuffer.data.length);  
                    nTransfer=new CountDownLatch(1);
                }
                synchronized(objDeinit){
                    objDeinit.notifyAll();
                }
            }
        }
    }
        public void fillBufferByZero() {
        if (!bFirstFill) {
            if (audioLine != null) {
                int nBufferFilled = FULL_BUFFER_SIZE - audioLine.available();
                if (nBufferFilled <= limit5ms) {
                    //neco je spatne, v bufferu zvuk.zarizeni je uz jen 5ms dat
                    //poslu 10ms ticha
                    audioLine.write(silent, 0, silent.length);
                    audioLine.write(silent, 0, silent.length);
                }
            }
        }
    }
    //thread hlidajici nepreruseny tok dat do zvukoveho zarizeni
    //jinak by v Linuxu bylo slyset chrceni
    public class SoundGuard extends TimerTask {
       private long now, diff;
        @Override
        public synchronized void run() {           
            now = System.currentTimeMillis();
            diff = now - scheduledExecutionTime();
            if (diff < 2) {
                try {
                    Thread.sleep(2 - diff);
                } catch (InterruptedException ex) {
                }
                fillBufferByZero();
            }

        }
    }
}
