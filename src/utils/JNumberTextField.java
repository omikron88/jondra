/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.awt.Container;
import java.awt.Point;
import java.awt.datatransfer.Clipboard;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.text.DefaultFormatter;

public class JNumberTextField extends JFormattedTextField {
    private static final long serialVersionUID = 1L;
    DefaultFormatter format=new DefaultFormatter();
    int nNumberOfDigits=4;
    boolean bSave=false;
    
    public JNumberTextField() { 
      super();   
      bSave=false;
      format.setOverwriteMode(true);
      super.setFormatter(format); 
    }  
    
    public void setNumberOfDigits(int inNumbers){
        nNumberOfDigits=inNumbers;
    }
    
    public boolean getSaveNeeded(){
        return bSave;
    }
        
    @Override
    public void processKeyEvent(KeyEvent ev) {
        boolean bKeyPressed=false;
        int keyCode = ev.getKeyCode();
        char keyChar = Character.toUpperCase(ev.getKeyChar());
        if ((keyCode == KeyEvent.VK_LEFT) || (keyCode == KeyEvent.VK_RIGHT) || (keyCode == KeyEvent.VK_HOME) || (keyCode == KeyEvent.VK_END)) {
            super.processKeyEvent(ev);
            bKeyPressed=true;
        }
        if ((keyCode == KeyEvent.VK_ENTER)) {
           bSave=true;
           transferFocus();
           super.processKeyEvent(ev);
            bKeyPressed=true; 
        }
        
        if ((keyCode == KeyEvent.VK_ESCAPE)) {
            bSave=false;
           transferFocus();
           super.processKeyEvent(ev);
            bKeyPressed=true; 
        }
        
        if (ev.getID() == KeyEvent.KEY_PRESSED) {
            if (((ev.isControlDown()) && (keyCode == KeyEvent.VK_C)) || ((ev.isControlDown()) && (keyCode == KeyEvent.VK_INSERT))) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                StringSelection strSel = new StringSelection(getText());
                clipboard.setContents(strSel, null);
            }
        }

        if (ev.getID() == KeyEvent.KEY_PRESSED) {
            if (((ev.isControlDown()) && (keyCode == KeyEvent.VK_V)) || ((ev.isShiftDown()) && (keyCode == KeyEvent.VK_INSERT))) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                Transferable transf = clipboard.getContents(null);
                if (transf.isDataFlavorSupported(DataFlavor.stringFlavor)) {                    
                    try {
                        String strPasteDate = (String) transf.getTransferData(DataFlavor.stringFlavor);
                        if (strPasteDate.startsWith("#")) {
                            strPasteDate = strPasteDate.substring(1);
                        }
                        int nPaste=Integer.valueOf(strPasteDate,16);
                        if((nPaste>=0)&&(nPaste<=(Math.pow(16,nNumberOfDigits)))){    
                         setText(String.format("%0"+nNumberOfDigits+"X",nPaste));
                        }
                    } catch (Exception ex) {}                      
                }
            }
        }
        
        
        if (keyCode == KeyEvent.VK_BACK_SPACE){ 
            if (ev.getID() == KeyEvent.KEY_RELEASED) {                
                int nSelEnd = getSelectionEnd();
                if (nSelEnd- getSelectionStart() > 0) {
                    setCaretPosition(nSelEnd);
                }
                int caretPosition = getCaretPosition();
                if (caretPosition > 0) {
                    String text = getText();
                    String strNewText = text.substring(0, caretPosition - 1) + '0' + text.substring(caretPosition);
                    setText(strNewText);
                    setCaretPosition(caretPosition - 1);
                }
            }
        }
        if (keyCode == KeyEvent.VK_DELETE){ 
            if (ev.getID() == KeyEvent.KEY_RELEASED) {
                int nSelStart = getSelectionStart();
                if (getSelectionEnd() - nSelStart > 0) {
                    setCaretPosition(nSelStart);
                }
                int caretPosition = getCaretPosition();
                if (caretPosition < nNumberOfDigits) {
                    String text = getText();
                    String strNewText = text.substring(0, caretPosition) + '0' + text.substring(caretPosition+1);
                    setText(strNewText);
                    setCaretPosition(caretPosition + 1 );
                }
            }
        }
        
        
        if ((Character.isDigit(keyChar))||(keyChar=='A')||(keyChar=='B')||(keyChar=='C')||(keyChar=='D')||(keyChar=='E')||(keyChar=='F')) {
            int nSelStart=getSelectionStart();
            if(getSelectionEnd()-nSelStart>0){
                setCaretPosition(nSelStart);
            }
            super.processKeyEvent(ev);
            bKeyPressed=true;
        }
        if(bKeyPressed){
        String text = getText();
        int caretPosition = getCaretPosition();
        String strNewText=text.substring(0,nNumberOfDigits);
        setText(strNewText);
        if(caretPosition>nNumberOfDigits) caretPosition=nNumberOfDigits;
        setCaretPosition(caretPosition);
        }
      
        ev.consume();
        return;
    }

    /**
     * As the user is not even able to enter a dot ("."), only integers (whole numbers) may be entered.
     */
    public Long getNumber() {
        Long result = null;
        String text = getText();
        if (text != null && !"".equals(text)) {
            result = Long.valueOf(text);
        }
        return result;
    }

    @Override
    public void processFocusEvent(FocusEvent fe) {
        super.processFocusEvent(fe);
        if (fe.getID() == FocusEvent.FOCUS_GAINED) {
                //nastavim overvrite mod
                String text = getText();
                super.setFormatter(format);
                setText(text);
                //preposlu proklik do textoveho pole, abych v jednom kliku ziskal focus a zaroven nastavil spravnou pozici kurzoru v textu
                Container parent = (Container) this.getParent();
            if (parent != null) {
                Point mousePoint = parent.getMousePosition(true);
                if (mousePoint != null) {
                    JComponent jTextField = (JComponent) parent.getComponentAt(mousePoint);
                    if (jTextField != null) {
                        Point componentPoint = jTextField.getLocation();
                        int nMouseX = mousePoint.x - componentPoint.x;
                        int nMouseY = mousePoint.y - componentPoint.y;
                        super.processMouseEvent(new MouseEvent(this, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), MouseEvent.BUTTON1_MASK, nMouseX, nMouseY, 1, false));
                    }
                }
            }
        }
    }


 
}
