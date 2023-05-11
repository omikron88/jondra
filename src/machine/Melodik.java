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
public class Melodik {

    static double sampleRate = 44100.0;
    static int fps = 50;
    //velikost bufferu pro 20ms zvuku - jeden frame obrazovky
    static int BUFFER_SIZE = (1 * (int) sampleRate / fps);
    //bufer pro prehravani a pro plneni, budou se swapovat
    private byte[] playBuffer;
    private byte[] fillBuffer;

    SourceDataLine audioLine = null;
    Object objDeinit = new Object();
    int FULL_BUFFER_SIZE = 6 * BUFFER_SIZE;

    //udrzuje datovy tok na zukove zarizeni nepreruseny
    public SoundGuard guard = null;
    //limit pod ktery nesmi klesnout buffer zvukoveho zarizeni, jinak se zacnou posilat 0
    public int limit5ms = 2 * (int) sampleRate / 100;
    //blok ticha - same 0
    private byte[] silent = new byte[limit5ms];
    //indikuje prvni zaslani zvuku, aby bylo mozno vlozit 10 sec. ticha na zacatek
    public boolean bFirstFill = true;

    //thread, ktery cte data z bufferu a posila je na zvukove zarizeni
    PlayBuffer playThread = null;

    //emulace zvukoveho cipu SN76489
    public SN76489 sndChip = null;
    //frekvence zvukoveho cipu SN76489
    int clockSpeedHz = 2000000;

    //Melodik povolen/zakazan
    private boolean bEnabled = false;
    //mapa IO do RAM pro moznost detekovat pripojeny Melodik
    private byte io[];

    public Melodik(byte[] iovect) {
        io = iovect;
        sndChip = new SN76489();
    }

    public void init(long lInitTStates) {
        //inicializace 2 bufferu pro preklapeni - jeden vzdy hraje, druhy se plni 
        //generuji 16-bitovy wav, proto buffer 2*velikost
        playBuffer = new byte[2 * BUFFER_SIZE];
        fillBuffer = new byte[2 * BUFFER_SIZE];
        openAudio();
        //umozneni programum detekovat Melodik
        setMelodikDetectOn();
        if (bEnabled) {
            startPlaying();
            //thread, ktery kazde 2 msec. kontroluje, jestli je v bufferu zvuk.zarizeni dost dat
            Timer tim = new Timer("GuardTimer");
            guard = new SoundGuard();
            tim.scheduleAtFixedRate(guard, 1, 2);

        }
    }

    public void initChip() {
        sndChip.init(clockSpeedHz, (int) sampleRate);
    }

    public void deinit() {
        closeAudio();
        setMelodikDetectOff();
    }

    public void setMelodikDetectOn() {
        io[0x0F] = (byte) (io[0x0F] & (byte) 223);

    }

    public void setMelodikDetectOff() {
        io[0x0F] = (byte) (io[0x0F] | (byte) 32);
    }

    //dava prehravacimu threadu vedet, ze jsou data v bufferu pripravena pro prenos na zarizeni
    public void setDataReady() {
        if (playThread != null) {
            playThread.setDataReady();
        }
    }

    public void setEnabled(boolean bVal) {
        bEnabled = bVal;
    }

    public boolean isEnabled() {
        return bEnabled;
    }

    public void startPlaying() {
        if (audioLine == null) {
            openAudio();
        }
        //spusteni threadu pro plneni prehravaciho zarizeni
        playThread = new PlayBuffer();
        playThread.start();
    }

    //otevre audio prehravac
    public void openAudio() {
        if (bEnabled) {
            initChip();
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
            if (sndChip != null) {
                sndChip = null;
            }
        }
    }

    //posila Buffer na zarizeni v samostatnem threadu
    private class PlayBuffer extends Thread {

        private boolean bRunning = true;
        private CountDownLatch nTransfer = new CountDownLatch(1);

        public void setDataReady() {
            nTransfer.countDown();
        }

        public void setFinish() {
            bRunning = false;
        }

        public void run() {
            if (audioLine != null) {
                while (bRunning) {
                    try {
                        nTransfer.await();
                    } catch (InterruptedException ex) {
                    }
                    audioLine.write(playBuffer, 0, playBuffer.length);
                    nTransfer = new CountDownLatch(1);
                }
                synchronized (objDeinit) {
                    objDeinit.notifyAll();
                }
            }
        }
    }

    //Prohodi oba buffery
    public void switchBuffers() {
        if (bFirstFill) {
            //vlozim 10ms ticha na zacatek, abych mel pak cas pri vypadku provest nejake kroky
            bFirstFill = false;
            fillBufferByZero();
        }
        Object tmpBuffer = playBuffer;
        playBuffer = fillBuffer;
        fillBuffer = (byte[]) tmpBuffer;
    }

    //vyplni buffer samply ze zvukoveho cipu
    public void updateSound(long lTstates) {
        if (bEnabled) {
            sndChip.update(fillBuffer, 0, 2 * BUFFER_SIZE);
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
