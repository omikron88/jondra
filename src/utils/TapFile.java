/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 *
 * @author tmilata
 */
public class TapFile {

    private File f;
    public static String strErrorMsg = "";

    int nFrameSize = 70;
    int nFrameDecrement = 0;

    tapbuffer tapeBuffer = null;
    long nBufferPos = 0;
    //indikuje, zda byl precten cely soubor
    public boolean bFinished = false;
    FileInputStream streamFile = null;
    int nActualBit = 3;
    boolean bCrLfType = false;

    long nMaxLen = 0;

    private class tapbuffer {

        byte[] byteBuffer = null;
        //write pozice bajtu v bufferu a bitu ve vybranem bajtu
        int nWBufferPosition = 0;
        int nWBitPosition = 0;
        //read pozice bajtu v bufferu a bitu ve vybranem bajtu
        int nRBufferPosition = 0;
        int nRBitPosition = 0;
        //indikator, ze jiz byla prectena vsechna data z bufferu
        boolean bAllBufferReaded = false;

        public tapbuffer(int nSize) {
            byteBuffer = new byte[nSize];
            Arrays.fill(byteBuffer, (byte) 0);
            nWBufferPosition = 0;
            nWBitPosition = 0;
            nRBufferPosition = 0;
            nRBitPosition = 0;
        }

        //pridam serii nul pred pilotni signal
        public void fillInitPause() {
            tapeBuffer.nWBufferPosition += 100;
        }

        //vlozi zkracenou verzi pilotniho signalu do bufferu
        public void fillPilot() {
            if (byteBuffer != null) {
                //pridam serii jednicek - pilotni signal
                for (int i = nWBufferPosition; i < nWBufferPosition + 200; i++) {
                    byteBuffer[i] = (byte) 255;
                }
                nWBufferPosition += 200;
                //pilotni signal je zakoncen 1x nulou
                byteBuffer[nWBufferPosition] = (byte) 127;
                nWBufferPosition++;
            }
        }

        //vlozi bit do bufferu v notaci Ondry, tj. negace 1. bitu a pak postupne cely bajt
        public void putByte(byte value) {
            int bit = (byte) (((byte) ((value & 1) ^ 1)) << nWBitPosition);
            byteBuffer[nWBufferPosition] = (byte) ((byteBuffer[nWBufferPosition]) | bit);
            IncWBitPosition();
            for (int i = 0; i < 8; i++) {
                int mask = (byte) (1 << i) & 0xff;
                int tmpval = value & mask;
                if (tmpval != 0) {
                    //bit 1, bit0 me nezajima, protoze buffer je predvyplnen 0
                    bit = (((byte) 1) << nWBitPosition);
                    byteBuffer[nWBufferPosition] = (byte) ((byteBuffer[nWBufferPosition]) | bit);
                }
                IncWBitPosition();
            }
        }

        //inkrementuje pozici bitu o 1
        public void IncWBitPosition() {
            nWBitPosition++;
            if (nWBitPosition > 7) {
                nWBitPosition = 0;
                nWBufferPosition++;
            }
        }

        //vrati dalsi bit z bufferu
        public boolean readBit() {
            boolean bBit = false;
            if (byteBuffer != null) {
                int mask = (byte) (1 << nRBitPosition) & 0xff;
                int tmpval = (byteBuffer[nRBufferPosition] & 0xff) & mask;
                if (tmpval != 0) {
                    bBit = true;
                }
                if (nRBufferPosition == nWBufferPosition) {
                    //posledni bajt
                    nRBitPosition++;
                    if (nRBitPosition > nWBitPosition) {
                        //jsem na konci
                        bAllBufferReaded = true;
                    }
                } else {
                    //posun na dalsi bit
                    nRBitPosition++;
                    if (nRBitPosition > 7) {
                        nRBitPosition = 0;
                        nRBufferPosition++;
                    }
                }
            }
            return bBit;
        }

    }

    //overi syntaxi tap souboru
    public static TapFile openTapFile(File file) throws IOException {
        TapFile ondtap = null;
        InputStream inputStream = null;
        boolean bCrlf = true;
        byte[] crlfArray = new byte[2];
        //overim syntaxi souboru TAP
        inputStream = new FileInputStream(file);
        long nMaxLen = file.length();
        long nCounter = 0;
        while (nCounter < nMaxLen) {
            byte[] byteArray = new byte[25];
            if (!bCrlf) {
                byteArray[0] = crlfArray[0];
                byteArray[1] = crlfArray[1];
                inputStream.read(byteArray, 2, 23);
            } else {
                inputStream.read(byteArray, 0, 25);
            }

            if (byteArray[0] != 'H') {
                inputStream.close();
                strErrorMsg = "Wrong header flagbyte! Maybe not a ondra tape.";
                throw new IOException();
            }
            //osetrim nepovinne CRLF bajty
            bCrlf = false;
            try {
                crlfArray[0] = 0;
                crlfArray[1] = 0;
                inputStream.read(crlfArray, 0, 2);
                if ((crlfArray[0] == 0x0A) && (crlfArray[1] == 0x0D)) {
                    bCrlf = true;
                    nCounter += 2;
                }
            } catch (Exception ex) {
            }

            int tmp_size = 256 * (byteArray[22] & 0xff) + (byteArray[21] & 0xff);
            if (byteArray[16] != 'D') {
                inputStream.close();
                strErrorMsg = "Wrong header byte! Maybe not a ondra tape.";
                throw new IOException();
            }
            int nDataByte = crlfArray[0];
            if (bCrlf) {
                nDataByte = inputStream.read() & 0xff;
            }
            if (nDataByte != 'D') {
                inputStream.close();
                strErrorMsg = "Wrong data byte! Maybe not a ondra tape.";
                throw new IOException();
            }

            byte[] byteArrayTmp = new byte[tmp_size];
            if (bCrlf) {
                inputStream.read(byteArrayTmp, 0, tmp_size);
            } else {
                byteArrayTmp[0] = crlfArray[1];
                inputStream.read(byteArrayTmp, 1, tmp_size - 1);
            }
            int nReadCRC = inputStream.read() & 0xff;
            int nComputeCRC = 0;
            for (int i = 0; i < tmp_size; i++) {
                nComputeCRC = 0xff & (byte) (nComputeCRC + byteArrayTmp[i]);
            }

            if (nReadCRC != nComputeCRC) {
                inputStream.close();
                strErrorMsg = "Wrong CRC of binary block";
                throw new IOException();
            }
            //osetrim nepovinne CRLF bajty
            bCrlf = false;
            try {
                crlfArray[0] = 0;
                crlfArray[1] = 0;
                inputStream.read(crlfArray, 0, 2);
                if ((crlfArray[0] == 0x0A) && (crlfArray[1] == 0x0D)) {
                    bCrlf = true;
                    nCounter += 2;
                }
            } catch (Exception ex) {
            }

            nCounter += 27 + tmp_size;
        }
        inputStream.close();
        ondtap = new TapFile();
        ondtap.f = file;
        ondtap.nMaxLen = nMaxLen;
        ondtap.bCrLfType = bCrlf;
        return ondtap;
    }

    //transformuje tap blok do bitove notace pro Ondru
    private boolean fillBuffer() {
        if (!bFinished) {
            try {
                if (streamFile == null) {
                    streamFile = new FileInputStream(f);
                }
                int nCrLfCorrect = 0;
                if (bCrLfType) {
                    nCrLfCorrect = 2;
                }
                byte[] byteArrayHeader = new byte[25 + nCrLfCorrect];
                if (streamFile.read(byteArrayHeader, 0, 25 + nCrLfCorrect) == -1) {
                    throw new IOException();
                }
                int nBlockSize = 256 * (byteArrayHeader[22] & 0xff) + (byteArrayHeader[21] & 0xff);
                //vypocitam velikost buferu = pauza+pilotni ton+header+pilotni ton+data
                tapeBuffer = new tapbuffer(1024 + nBlockSize);

                //naplnim buffer
                //pauza na zacatku bloku
                tapeBuffer.fillInitPause();
                //pilotni ton
                tapeBuffer.fillPilot();
                //vlozim do bufferu header
                for (int i = 0; i < 25; i++) {
                    tapeBuffer.putByte((byte) byteArrayHeader[i]);
                }
                //vlozim final byte
                tapeBuffer.putByte((byte) 0);
                //vlozim pauzu mezi bloky
                tapeBuffer.nWBitPosition = 0;
                tapeBuffer.nWBufferPosition += 2;
                //pilotni ton
                tapeBuffer.fillPilot();
                //data
                byte[] byteArrayData = new byte[nBlockSize + 2 + nCrLfCorrect];
                Arrays.fill(byteArrayData, (byte) 0);
                //nactu vcetne CRC bajtu na konci
                streamFile.read(byteArrayData, 0, nBlockSize + 2 + nCrLfCorrect);
                //vlozim do bufferu
                for (int i = 0; i < nBlockSize + 2; i++) {
                    tapeBuffer.putByte((byte) byteArrayData[i]);
                }
                //vlozim final byte
                tapeBuffer.putByte((byte) 0);
                //vlozim pauzu
                tapeBuffer.nWBitPosition = 0;
                tapeBuffer.nWBufferPosition += 2; 

            } catch (FileNotFoundException ex) {
            } catch (IOException ex1) {
                bFinished = true;
                if (streamFile != null) {
                    try {
                        streamFile.close();
                    } catch (IOException ex) {
                    }
                }
            }
        }
        return !bFinished;
    }

    //cte bit po bitu z bufferu, kde je zpracovany blok tap souboru
    public boolean readNextBit() {
        boolean bRet = false;
        if (tapeBuffer == null) {
            if (fillBuffer()) {
                bRet = tapeBuffer.readBit();
            }
        } else {
            bRet = tapeBuffer.readBit();
        }
        if (tapeBuffer.bAllBufferReaded) {
            fillBuffer();
        }
        return bRet;
    }

    //generuje frame ve formatu Wav
    public boolean generateFrame() {
        boolean bRet = false;
        if (nActualBit == 3) {
            //prvni spusteni
            nActualBit = readNextBit() ? 1 : 0;
            nFrameDecrement = 2 * nFrameSize;
        }
        if (nFrameDecrement > nFrameSize) {
            //druha pulka frame
            bRet = (nActualBit != 0);
        } else {
            //prvni pulka frame
            bRet = !(nActualBit != 0);
        }
        nFrameDecrement--;
        if (nFrameDecrement == 0) {
            //dalsi bit
            nActualBit = readNextBit() ? 1 : 0;
            nFrameDecrement = 2 * nFrameSize;
        }
        return bRet;
    }
}
