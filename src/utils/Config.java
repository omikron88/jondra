/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import gui.JOndra;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 *
 * @author Administrator
 */
public class Config {

    //ulozene udaje pro nahravani binarniho souboru
    public static String strBinFilePath = "";
    public static int nBeginBinAddress = 0;
    public static boolean bRunBin = false;
    public static int nRunBinAddress = 0;
    public static boolean bAllRam = true;
    public static boolean bHeaderOn = true;
    //ulozene udaje pro debugger
    public static boolean bBP1 = false;
    public static int nBP1Address = 0;
    public static boolean bBP2 = false;
    public static int nBP2Address = 0;
    public static boolean bBP3 = false;
    public static int nBP3Address = 0;
    public static boolean bBP4 = false;
    public static int nBP4Address = 0;
    public static boolean bBP5 = false;
    public static int nBP5Address = 0;
    public static boolean bBP6 = false;
    public static int nBP6Address = 0;
    public static boolean bBP7 = false;
    public static int nBP7Address = 0;
    public static int nMemAddress = 0;
    public static boolean bShowCode = false;
    public static boolean bEnableTimeline = false;
    public static boolean bAudio = true;
    public static boolean bMelodik = true;
    public static boolean bFullscreen = false;
    public static boolean bScanlines = false;
    public static boolean bScaleNx = false;
    //ulozene udaje pro ukladani do binarniho souboru
    public static String strSaveBinFilePath = "";
    public static int nSaveFromAddress = 0;
    public static int nSaveToAddress = 0;

    public static String strTapFilePath = "";
    public static String strSnapFilePath = "";
    public static String strShotFilePath = "";
    
    public static String strRomAFilePath = "";
    public static String strRomBFilePath = "";
    public static int nRomType = 0;

    public static String getMyPath() {
        String retVal = "";
        retVal = JOndra.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (retVal.contains("/")) {
            int pos = retVal.lastIndexOf("/");
            retVal = retVal.substring(0, pos + 1);
        }
        return retVal;
    }

    public static void SaveConfig() {
        Properties prop = new Properties();
        prop.setProperty("BINFILEPATH", strBinFilePath);
        prop.setProperty("BEGINBINADDRESS", String.valueOf(nBeginBinAddress));
        prop.setProperty("BRUNBIN", String.valueOf(bRunBin));
        prop.setProperty("RUNBINADDRESS", String.valueOf(nRunBinAddress));
        prop.setProperty("BALLRAM", String.valueOf(bAllRam));
        prop.setProperty("BHEADER", String.valueOf(bHeaderOn));
        prop.setProperty("BP1CHCK", String.valueOf(bBP1));
        prop.setProperty("BP1ADDRESS", String.valueOf(nBP1Address));
        prop.setProperty("BP2CHCK", String.valueOf(bBP2));
        prop.setProperty("BP2ADDRESS", String.valueOf(nBP2Address));
        prop.setProperty("BP3CHCK", String.valueOf(bBP3));
        prop.setProperty("BP3ADDRESS", String.valueOf(nBP3Address));
        prop.setProperty("BP4CHCK", String.valueOf(bBP4));
        prop.setProperty("BP4ADDRESS", String.valueOf(nBP4Address));
        prop.setProperty("BP5CHCK", String.valueOf(bBP5));
        prop.setProperty("BP5ADDRESS", String.valueOf(nBP5Address));
        prop.setProperty("BP6CHCK", String.valueOf(bBP6));
        prop.setProperty("BP6ADDRESS", String.valueOf(nBP6Address));
        prop.setProperty("BP7CHCK", String.valueOf(bBP7));
        prop.setProperty("BP7ADDRESS", String.valueOf(nBP7Address));
        prop.setProperty("MEMADDRESS", String.valueOf(nMemAddress));
        prop.setProperty("BSHOWCODE", String.valueOf(bShowCode));
        prop.setProperty("BTIMELINE", String.valueOf(bEnableTimeline));
        prop.setProperty("AUDIO", String.valueOf(bAudio));
        prop.setProperty("MELODIK", String.valueOf(bMelodik));
        prop.setProperty("FULLSCREEN", String.valueOf(bFullscreen));
        prop.setProperty("SCANLINES", String.valueOf(bScanlines));
        prop.setProperty("SCALENX", String.valueOf(bScaleNx));        

        prop.setProperty("BINSAVEFILEPATH", String.valueOf(strSaveBinFilePath));
        prop.setProperty("BINSAVEADDRESSFROM", String.valueOf(nSaveFromAddress));
        prop.setProperty("BINSAVEADDRESSTO", String.valueOf(nSaveToAddress));

        prop.setProperty("TAPFILEPATH", strTapFilePath);
        prop.setProperty("SNAFILEPATH", strSnapFilePath);
        prop.setProperty("SHOTFILEPATH", strShotFilePath);
        
        prop.setProperty("ROMAFILEPATH", strRomAFilePath);
        prop.setProperty("ROMBFILEPATH", strRomBFilePath);
        prop.setProperty("ROMTYPE", String.valueOf(nRomType));        

        String fileName = getMyPath() + "JOndra.config";
        OutputStream os;
        try {
            os = new FileOutputStream(fileName);
            try {
                prop.store(os, "JOndra config file");
            } catch (IOException ex) {
                System.out.println("Nelze ulozit " + fileName);
            }
            os.close();
        } catch (Exception ex) {

        }
    }

    private static int parseIntSafe(String strInt, int nDefault) {
        int nRet = nDefault;
        if (strInt == null) {
            nRet = nDefault;
        } else {
            try {
                nRet = Integer.parseInt(strInt);
            } catch (Exception e) {
                nRet = nDefault;
            }
        }
        return nRet;
    }

    private static boolean parseBooleanSafe(String strBoolean, boolean bDefault) {
        boolean bRet = bDefault;
        if (strBoolean == null) {
            bRet = bDefault;
        } else {
            try {
                bRet = Boolean.parseBoolean(strBoolean);
            } catch (Exception e) {
                bRet = bDefault;
            }
        }
        return bRet;
    }

    public static String nullToEmpty(String strIn) {
        if (strIn == null) {
            strIn = "";
        }
        return strIn;
    }

    public static void LoadConfig() {
        Properties prop = new Properties();
        String fileName = getMyPath() + "JOndra.config";
        InputStream is;
        try {
            is = new FileInputStream(fileName);
            try {
                prop.load(is);
            } catch (IOException ex) {
                System.out.println("Nelze rozparsovat " + fileName);
            }
        } catch (FileNotFoundException ex) {
            SaveConfig();
            return;
        }

        strBinFilePath = nullToEmpty(prop.getProperty("BINFILEPATH"));
        nBeginBinAddress = parseIntSafe(prop.getProperty("BEGINBINADDRESS"), 0);
        bRunBin = parseBooleanSafe(prop.getProperty("BRUNBIN"), false);
        nRunBinAddress = parseIntSafe(prop.getProperty("RUNBINADDRESS"), 0);
        bAllRam = parseBooleanSafe(prop.getProperty("BALLRAM"), true);
        bHeaderOn = parseBooleanSafe(prop.getProperty("BHEADER"), true);
        bBP1 = parseBooleanSafe(prop.getProperty("BP1CHCK"), false);
        nBP1Address = parseIntSafe(prop.getProperty("BP1ADDRESS"), 0);
        bBP2 = parseBooleanSafe(prop.getProperty("BP2CHCK"), false);
        nBP2Address = parseIntSafe(prop.getProperty("BP2ADDRESS"), 0);
        bBP3 = parseBooleanSafe(prop.getProperty("BP3CHCK"), false);
        nBP3Address = parseIntSafe(prop.getProperty("BP3ADDRESS"), 0);
        bBP4 = parseBooleanSafe(prop.getProperty("BP4CHCK"), false);
        nBP4Address = parseIntSafe(prop.getProperty("BP4ADDRESS"), 0);
        bBP5 = parseBooleanSafe(prop.getProperty("BP5CHCK"), false);
        nBP5Address = parseIntSafe(prop.getProperty("BP5ADDRESS"), 0);
        bBP6 = parseBooleanSafe(prop.getProperty("BP6CHCK"), false);
        nBP6Address = parseIntSafe(prop.getProperty("BP6ADDRESS"), 0);
        bBP7 = parseBooleanSafe(prop.getProperty("BP7CHCK"), false);
        nBP7Address = parseIntSafe(prop.getProperty("BP7ADDRESS"), 0);
        nMemAddress = parseIntSafe(prop.getProperty("MEMADDRESS"), 0);
        bShowCode = parseBooleanSafe(prop.getProperty("BSHOWCODE"), false);        
        bEnableTimeline = parseBooleanSafe(prop.getProperty("BTIMELINE"), false);        
        bAudio = parseBooleanSafe(prop.getProperty("AUDIO"), true);
        bMelodik = parseBooleanSafe(prop.getProperty("MELODIK"), true);
        bFullscreen = parseBooleanSafe(prop.getProperty("FULLSCREEN"), false);
        bScanlines = parseBooleanSafe(prop.getProperty("SCANLINES"), false);
        bScaleNx = parseBooleanSafe(prop.getProperty("SCALENX"), false);
        strSaveBinFilePath = nullToEmpty(prop.getProperty("BINSAVEFILEPATH"));
        strTapFilePath = nullToEmpty(prop.getProperty("TAPFILEPATH"));
        strSnapFilePath = nullToEmpty(prop.getProperty("SNAFILEPATH"));
        strShotFilePath = nullToEmpty(prop.getProperty("SHOTFILEPATH"));
        nSaveFromAddress = parseIntSafe(prop.getProperty("BINSAVEADDRESSFROM"), 0);
        nSaveToAddress = parseIntSafe(prop.getProperty("BINSAVEADDRESSTO"), 0);        
        strRomAFilePath = nullToEmpty(prop.getProperty("ROMAFILEPATH"));
        strRomBFilePath = nullToEmpty(prop.getProperty("ROMBFILEPATH")); 
        nRomType = parseIntSafe(prop.getProperty("ROMTYPE"),2);         
    }
}
