/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * BinSave.java
 *
 * Created on Oct 14, 2019, 5:27:20 PM
 */
package gui;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import machine.Ondra;
import utils.Config;

/**
 *
 * @author Administrator
 */
public class BinSave extends javax.swing.JFrame {
    private Ondra m;
    /** Creates new form BinSave */
    public BinSave(Ondra inM) {
        initComponents();
        setIconImage((new ImageIcon(getClass().getResource("/icons/binarysav.png")).getImage()));       
        Config.LoadConfig();
        m = inM;
        this.addWindowListener(new java.awt.event.WindowAdapter() {

            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                m.startEmulation();
            }
        });
    }
    
    public void refreshDlg(){
      jTextBinFile.setText(Config.strSaveBinFilePath);
      jNumberTextSavAdrFrom.setText(String.format("%04X",Config.nSaveFromAddress));
      jNumberTextSavAdrTo.setText(String.format("%04X",Config.nSaveToAddress));
    }

    public void showDialog() {
        refreshDlg();
        setVisible(true);
    }
    
     private static String getPath() {
        String retVal = "";
        if(Config.strSaveBinFilePath.isEmpty()){
         retVal = Ondra.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        }else{
         retVal=Config.strSaveBinFilePath;  
        }
        if (retVal.contains("/")) {
            int pos = retVal.lastIndexOf("/");
            retVal = retVal.substring(0, pos + 1);
        }
        return retVal;
    }
     

   
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jTextBinFile = new javax.swing.JTextField();
        jNumberTextSavAdrTo = new utils.JNumberTextField();
        jNumberTextSavAdrFrom = new utils.JNumberTextField();
        jLabel2 = new javax.swing.JLabel();
        jButtonBinOpen = new javax.swing.JButton();
        jButtonOK = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Save memory block to file");

        jLabel1.setText("File");

        jTextBinFile.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextBinFileFocusLost(evt);
            }
        });

        jNumberTextSavAdrTo.setText("0000");
        jNumberTextSavAdrTo.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jNumberTextSavAdrToFocusLost(evt);
            }
        });

        jNumberTextSavAdrFrom.setText("0000");
        jNumberTextSavAdrFrom.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jNumberTextSavAdrFromFocusLost(evt);
            }
        });

        jLabel2.setText("from address:");

        jButtonBinOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/opnfldr.png"))); // NOI18N
        jButtonBinOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonBinOpenActionPerformed(evt);
            }
        });

        jButtonOK.setText("OK");
        jButtonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOKActionPerformed(evt);
            }
        });

        jLabel3.setText("to address:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonOK, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jTextBinFile, javax.swing.GroupLayout.PREFERRED_SIZE, 276, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonBinOpen, javax.swing.GroupLayout.PREFERRED_SIZE, 33, Short.MAX_VALUE))
                            .addComponent(jLabel1)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(jNumberTextSavAdrFrom, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(31, 31, 31)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jNumberTextSavAdrTo, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 50, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(2, 2, 2)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextBinFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonBinOpen, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jNumberTextSavAdrFrom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jNumberTextSavAdrTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(5, 5, 5)
                .addComponent(jButtonOK)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonBinOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonBinOpenActionPerformed
         JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select file for save data");
        fc.resetChoosableFileFilters();
        fc.setCurrentDirectory(new File(getPath()));
        fc.setAcceptAllFileFilterUsed(true);
        int val = fc.showOpenDialog(this);
        
        if (val==JFileChooser.APPROVE_OPTION) {
            try {
                Config.strSaveBinFilePath=fc.getSelectedFile().getCanonicalPath();
                //jTextBinFile.setText(fc.getSelectedFile().getCanonicalPath());
                Config.SaveConfig();
                refreshDlg();
            } catch (IOException ex) {
               
            }
        }        // TODO add your handling code here:
    }//GEN-LAST:event_jButtonBinOpenActionPerformed

    private void jNumberTextSavAdrFromFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jNumberTextSavAdrFromFocusLost
        int nNewAdr = Integer.valueOf(jNumberTextSavAdrFrom.getText(), 16);
        Config.nSaveFromAddress=nNewAdr;
        Config.SaveConfig();
    }//GEN-LAST:event_jNumberTextSavAdrFromFocusLost

    private void jNumberTextSavAdrToFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jNumberTextSavAdrToFocusLost
       int nNewAdr = Integer.valueOf(jNumberTextSavAdrTo.getText(), 16);
       Config.nSaveToAddress=nNewAdr;
       Config.SaveConfig();
    }//GEN-LAST:event_jNumberTextSavAdrToFocusLost

    private void jButtonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOKActionPerformed
        if (Config.nSaveFromAddress < Config.nSaveToAddress) {
            try {
                DataOutputStream os = new DataOutputStream(new FileOutputStream(Config.strSaveBinFilePath));
                try {
                    for (int i = Config.nSaveFromAddress; i <= Config.nSaveToAddress; i++) {

                        os.writeByte(m.mem.readByte(i));
                    }
                    os.close();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Can't save data. Error: " + ex.getMessage());
                }

            } catch (FileNotFoundException ex) {
               // JOptionPane.showMessageDialog(null, "Can't create file: " + Config.strSaveBinFilePath);
            }
        }
        m.startEmulation();
        dispose();
    }//GEN-LAST:event_jButtonOKActionPerformed

    private void jTextBinFileFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextBinFileFocusLost
        Config.strSaveBinFilePath = jTextBinFile.getText();
        //jTextBinFile.setText(fc.getSelectedFile().getCanonicalPath());
        Config.SaveConfig();
        refreshDlg();
    }//GEN-LAST:event_jTextBinFileFocusLost

   
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonBinOpen;
    private javax.swing.JButton jButtonOK;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private utils.JNumberTextField jNumberTextSavAdrFrom;
    private utils.JNumberTextField jNumberTextSavAdrTo;
    private javax.swing.JTextField jTextBinFile;
    // End of variables declaration//GEN-END:variables
}
