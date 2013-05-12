/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package machine;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 *
 * @author admin
 */
public final class Keyboard implements KeyListener {
    
    private final int sb0 = 0x01;
    private final int sb1 = 0x02;
    private final int sb2 = 0x04;
    private final int sb3 = 0x08;
    private final int sb4 = 0x10;
    
    private final int rb0 = ~sb0;
    private final int rb1 = ~sb1;
    private final int rb2 = ~sb2;
    private final int rb3 = ~sb3;
    private final int rb4 = ~sb4;
    
    private byte io[];
    private int tb[];
    
    public Keyboard(byte[] iovect) {
        io = iovect;
        Reset();
    }
        
    public void Reset() {
        for(int n=0;n<io.length;n++) {
            io[n] = (byte) 0xff;
        }
    }
        
    @Override
    public void keyTyped(KeyEvent ke) {
  
    }

    @Override
    public void keyPressed(KeyEvent ke) {
        switch(ke.getKeyCode()) {
            case KeyEvent.VK_Q:
                io[0] &= rb4;
                break;
            case KeyEvent.VK_T:
                io[0] &= rb3;
                break;
            case KeyEvent.VK_W:
                io[0] &= rb2;
                break;
            case KeyEvent.VK_E:
                io[0] &= rb1;
                break;
            case KeyEvent.VK_R:
                io[0] &= rb0;
                break;

            case KeyEvent.VK_A:
                io[1] &= rb4;
                break;
            case KeyEvent.VK_G:
                io[1] &= rb3;
                break;
            case KeyEvent.VK_S:
                io[1] &= rb2;
                break;
            case KeyEvent.VK_D:
                io[1] &= rb1;
                break;
            case KeyEvent.VK_F:
                io[1] &= rb0;
                break;

            case KeyEvent.VK_ALT:
                io[2] &= rb4;
                break;
            case KeyEvent.VK_V:
                io[2] &= rb3;
                break;
            case KeyEvent.VK_Z:
                io[2] &= rb2;
                break;
            case KeyEvent.VK_X:
                io[2] &= rb1;
                break;
            case KeyEvent.VK_C:
                io[2] &= rb0;
                break;

            case KeyEvent.VK_SPACE:
                io[3] &= rb0;
                break;

            case KeyEvent.VK_SHIFT:
                io[4] &= rb4;
                break;
            case KeyEvent.VK_EQUALS:
                io[4] &= rb2;
                break;
            case KeyEvent.VK_TAB:
                io[4] &= rb1;
                break;

            case KeyEvent.VK_ENTER:
                io[5] &= rb4;
                break;
            case KeyEvent.VK_H:
                io[5] &= rb3;
                break;
            case KeyEvent.VK_L:
                io[5] &= rb2;
                break;
            case KeyEvent.VK_K:
                io[5] &= rb1;
                break;
            case KeyEvent.VK_J:
                io[5] &= rb0;
                break;

            case KeyEvent.VK_P:
                io[6] &= rb4;
                break;
            case KeyEvent.VK_Y:
                io[6] &= rb3;
                break;
            case KeyEvent.VK_O:
                io[6] &= rb2;
                break;
            case KeyEvent.VK_I:
                io[6] &= rb1;
                break;
            case KeyEvent.VK_U:
                io[6] &= rb0;
                break;

            case KeyEvent.VK_CONTROL:
                io[7] &= rb4;
                break;
            case KeyEvent.VK_B:
                io[7] &= rb3;
                break;
            case KeyEvent.VK_UP:
                io[7] &= rb2;
                break;
            case KeyEvent.VK_M:
                io[7] &= rb1;
                break;
            case KeyEvent.VK_N:
                io[7] &= rb0;
                break;
                
            case KeyEvent.VK_RIGHT:
                io[8] &= rb4;
                break;
            case KeyEvent.VK_DOWN:
                io[8] &= rb2;
                break;
            case KeyEvent.VK_LEFT:
                io[8] &= rb1;
                break;

            case KeyEvent.VK_NUMPAD2:
                io[9] &= rb4;
                break;
            case KeyEvent.VK_NUMPAD0:
                io[9] &= rb3;
                break;
            case KeyEvent.VK_NUMPAD8:
                io[9] &= rb2;
                break;
            case KeyEvent.VK_NUMPAD4:
                io[9] &= rb1;
                break;
            case KeyEvent.VK_NUMPAD6:
                io[9] &= rb0;
                break;

        }
    }

    @Override
    public void keyReleased(KeyEvent ke) {
        switch(ke.getKeyCode()) {
            case KeyEvent.VK_Q:
                io[0] |= sb4;
                break;
            case KeyEvent.VK_T:
                io[0] |= sb3;
                break;
            case KeyEvent.VK_W:
                io[0] |= sb2;
                break;
            case KeyEvent.VK_E:
                io[0] |= sb1;
                break;
            case KeyEvent.VK_R:
                io[0] |= sb0;
                break;
            
            case KeyEvent.VK_A:
                io[1] |= sb4;
                break;
            case KeyEvent.VK_G:
                io[1] |= sb3;
                break;
            case KeyEvent.VK_S:
                io[1] |= sb2;
                break;
            case KeyEvent.VK_D:
                io[1] |= sb1;
                break;
            case KeyEvent.VK_F:
                io[1] |= sb0;
                break;
            
            case KeyEvent.VK_ALT:
                io[2] |= sb4;
                break;
            case KeyEvent.VK_V:
                io[2] |= sb3;
                break;
            case KeyEvent.VK_Z:
                io[2] |= sb2;
                break;
            case KeyEvent.VK_X:
                io[2] |= sb1;
                break;
            case KeyEvent.VK_C:
                io[2] |= sb0;
                break;
            
            case KeyEvent.VK_SPACE:
                io[3] |= sb0;
                break;

            case KeyEvent.VK_SHIFT:
                io[4] |= sb4;
                break;
            case KeyEvent.VK_EQUALS:
                io[4] |= sb2;
                break;
            case KeyEvent.VK_TAB:
                io[4] |= sb1;
                break;

            case KeyEvent.VK_ENTER:
                io[5] |= sb4;
                break;
            case KeyEvent.VK_H:
                io[5] |= sb3;
                break;
            case KeyEvent.VK_L:
                io[5] |= sb2;
                break;
            case KeyEvent.VK_K:
                io[5] |= sb1;
                break;
            case KeyEvent.VK_J:
                io[5] |= sb0;
                break;
            
            case KeyEvent.VK_P:
                io[6] |= sb4;
                break;
            case KeyEvent.VK_Y:
                io[6] |= sb3;
                break;
            case KeyEvent.VK_O:
                io[6] |= sb2;
                break;
            case KeyEvent.VK_I:
                io[6] |= sb1;
                break;
            case KeyEvent.VK_U:
                io[6] |= sb0;
                break;
            
            case KeyEvent.VK_CONTROL:
                io[7] |= sb4;
                break;
            case KeyEvent.VK_B:
                io[7] |= sb3;
                break;
            case KeyEvent.VK_UP:
                io[7] |= sb2;
                break;
            case KeyEvent.VK_M:
                io[7] |= sb1;
                break;
            case KeyEvent.VK_N:
                io[7] |= sb0;
                break;
            
            case KeyEvent.VK_RIGHT:
                io[8] |= sb4;
                break;
            case KeyEvent.VK_DOWN:
                io[8] |= sb2;
                break;
            case KeyEvent.VK_LEFT:
                io[8] |= sb1;
                break;
            
            case KeyEvent.VK_NUMPAD2:
                io[9] |= sb4;
                break;
            case KeyEvent.VK_NUMPAD0:
                io[9] |= sb3;
                break;
            case KeyEvent.VK_NUMPAD8:
                io[9] |= sb2;
                break;
            case KeyEvent.VK_NUMPAD4:
                io[9] |= sb1;
                break;
            case KeyEvent.VK_NUMPAD6:
                io[9] |= sb0;
                break;
            
        }
    }

}
