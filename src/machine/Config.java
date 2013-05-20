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
    public final byte VILI  = 2;
    public final byte CUSTOM  = 3;
   
    private String  BASIC_a = "Ondra_BASICEXP_V5_a.rom";
    private String  BASIC_b = "Ondra_BASICEXP_V5_b.rom";
    private String  TESLA_a = "Ondra_TESLA_V5_a.rom";
    private String  TESLA_b = "Ondra_TESLA_V5_b.rom";
    private String  VILI_a  = "Ondra_ViLi_a.rom";
    private String  VILI_b  = "Ondra_ViLi_b.rom";

    private byte Rom = VILI; 
    
    private String RomDir = "roms/";
    private String RomA = "";
    private String RomB = "";
    
    
    public void setRomType(byte b) {
        if (b!=Rom) {
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

}
