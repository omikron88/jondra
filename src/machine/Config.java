/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package machine;

/**
 *
 * @author Administrator
 */
public class Config {

    public final byte BASIC = 0;
    public final byte TESLA = 1;
    public final byte VILI = 2;
    public final byte PLUS = 3;
    public final byte CUSTOM = 100;

    private String BASIC_a = "Ondra_BASICEXP_V5_a.rom";
    private String BASIC_b = "Ondra_BASICEXP_V5_b.rom";
    private String TESLA_a = "Ondra_TESLA_V5_a.rom";
    private String TESLA_b = "Ondra_TESLA_V5_b.rom";
    private String VILI_a = "Ondra_ViLi_v27_a.rom";
    private String VILI_b = "Ondra_ViLi_v27_b.rom";
    private String PLUS_a = "Ondra_PLUS_a.rom";
    private String PLUS_b = "Ondra_PLUS_b.rom";

    private byte Rom = (byte) utils.Config.nRomType;

    private String RomDir = "roms/";
    private String RomA = utils.Config.strRomAFilePath;
    private String RomB = utils.Config.strRomBFilePath;

    private boolean bAudio = true;
    private boolean bMelodik = true;
    private boolean bFullscreen = false;
    private boolean bScanlines = false;
    private boolean bScaleNx = false;

    public boolean getAudio() {
        return bAudio;
    }

    public void setAudio(boolean bInAudio) {
        bAudio = bInAudio;
    }

    public boolean getMelodik() {
        return bMelodik;
    }

    public void setMelodik(boolean bInMel) {
        bMelodik = bInMel;
    }

    public boolean getFullscreen() {
        return bFullscreen;
    }

    public void setFullscreen(boolean bInFull) {
        bFullscreen = bInFull;
    }

    public boolean getScanlines() {
        return bScanlines;
    }

    public void setScanlines(boolean bInScan) {
        bScanlines = bInScan;
    }
    
     public boolean getScaleNx() {
        return bScaleNx;
    }

    public void setScaleNx(boolean bInScale) {
        bScaleNx = bInScale;
    }

    public void setRomType(byte b) {
        if (b != Rom) {
            Rom = b;
        }
    }

    public byte getRomType() {
        return Rom;
    }

    public void setRomA(String s) {
        if (!s.equals(RomA)) {
            RomA = s;
        }
    }

    public String getRomA() {
        return RomA;
    }

    public void setRomB(String s) {
        if (!s.equals(RomB)) {
            RomB = s;
        }
    }

    public String getRomB() {
        return RomB;
    }

    public String getRomsDirectory() {
        return RomDir;
    }

    public String getBasicA() {
        return BASIC_a;
    }

    public String getBasicB() {
        return BASIC_b;
    }

    public String getTeslaA() {
        return TESLA_a;
    }

    public String getTeslaB() {
        return TESLA_b;
    }

    public String getViLiA() {
        return VILI_a;
    }

    public String getViLiB() {
        return VILI_b;
    }

    public String getPlusA() {
        return PLUS_a;
    }

    public String getPlusB() {
        return PLUS_b;
    }
}
