/*0
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import machine.Ondra;

/**
 *
 * @author Administrator
 */
public class JOndra extends javax.swing.JFrame {
    
    private Ondra m;
    private Screen scr;
        
    /**
     * Creates new form JOndra
     */
    public JOndra() {     
        initComponents();
        initEmulator();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fc = new javax.swing.JFileChooser();
        ToolBar = new javax.swing.JToolBar();
        bOpent = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        bSavet = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        bReset = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        bNmi = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JToolBar.Separator();
        bPause = new javax.swing.JToggleButton();
        jSeparator5 = new javax.swing.JToolBar.Separator();
        bSettings = new javax.swing.JButton();
        jSeparator6 = new javax.swing.JToolBar.Separator();
        bOpens = new javax.swing.JButton();
        jSeparator7 = new javax.swing.JToolBar.Separator();
        bSaves = new javax.swing.JButton();
        jSeparator8 = new javax.swing.JToolBar.Separator();
        statusPanel = new javax.swing.JPanel();
        jSeparator31 = new javax.swing.JSeparator();
        GreenLed = new javax.swing.JLabel();
        jSeparator32 = new javax.swing.JSeparator();
        YellowLed = new javax.swing.JLabel();
        jSeparator33 = new javax.swing.JSeparator();
        TapeLed = new javax.swing.JLabel();
        jSeparator34 = new javax.swing.JSeparator();
        bRec = new javax.swing.JToggleButton();
        jSeparator35 = new javax.swing.JSeparator();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Ondra SPO 186");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        ToolBar.setRollover(true);
        ToolBar.setPreferredSize(new java.awt.Dimension(100, 20));

        bOpent.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/open.png"))); // NOI18N
        bOpent.setToolTipText("Open tape for Load");
        bOpent.setFocusable(false);
        bOpent.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        bOpent.setPreferredSize(new java.awt.Dimension(20, 20));
        bOpent.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bOpent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bOpentActionPerformed(evt);
            }
        });
        ToolBar.add(bOpent);
        ToolBar.add(jSeparator1);

        bSavet.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/save.png"))); // NOI18N
        bSavet.setToolTipText("Open tape for Save");
        bSavet.setFocusable(false);
        bSavet.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        bSavet.setPreferredSize(new java.awt.Dimension(20, 20));
        bSavet.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bSavet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bSavetActionPerformed(evt);
            }
        });
        ToolBar.add(bSavet);
        ToolBar.add(jSeparator2);

        bReset.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/reset.png"))); // NOI18N
        bReset.setToolTipText("Reset");
        bReset.setFocusable(false);
        bReset.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        bReset.setPreferredSize(new java.awt.Dimension(20, 20));
        bReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bResetActionPerformed(evt);
            }
        });
        ToolBar.add(bReset);
        ToolBar.add(jSeparator3);

        bNmi.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/nmi.png"))); // NOI18N
        bNmi.setToolTipText("NMI");
        bNmi.setFocusable(false);
        bNmi.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        bNmi.setPreferredSize(new java.awt.Dimension(20, 20));
        bNmi.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bNmi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bNmiActionPerformed(evt);
            }
        });
        ToolBar.add(bNmi);
        ToolBar.add(jSeparator4);

        bPause.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/pause.png"))); // NOI18N
        bPause.setSelected(true);
        bPause.setToolTipText("Run/Pause");
        bPause.setFocusable(false);
        bPause.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        bPause.setPreferredSize(new java.awt.Dimension(16, 16));
        bPause.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/run.png"))); // NOI18N
        bPause.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bPauseActionPerformed(evt);
            }
        });
        ToolBar.add(bPause);
        ToolBar.add(jSeparator5);

        bSettings.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/settings.png"))); // NOI18N
        bSettings.setToolTipText("Settings");
        bSettings.setFocusable(false);
        bSettings.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        bSettings.setPreferredSize(new java.awt.Dimension(20, 20));
        bSettings.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bSettingsActionPerformed(evt);
            }
        });
        ToolBar.add(bSettings);
        ToolBar.add(jSeparator6);

        bOpens.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/osn_open.png"))); // NOI18N
        bOpens.setToolTipText("Open snapshot");
        bOpens.setFocusable(false);
        bOpens.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        bOpens.setPreferredSize(new java.awt.Dimension(20, 20));
        bOpens.setRequestFocusEnabled(false);
        bOpens.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bOpens.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bOpensActionPerformed(evt);
            }
        });
        ToolBar.add(bOpens);
        ToolBar.add(jSeparator7);

        bSaves.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/osn_save.png"))); // NOI18N
        bSaves.setToolTipText("Save snapshot");
        bSaves.setFocusable(false);
        bSaves.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        bSaves.setPreferredSize(new java.awt.Dimension(20, 20));
        bSaves.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bSaves.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bSavesActionPerformed(evt);
            }
        });
        ToolBar.add(bSaves);
        ToolBar.add(jSeparator8);

        getContentPane().add(ToolBar, java.awt.BorderLayout.PAGE_START);

        statusPanel.setPreferredSize(new java.awt.Dimension(100, 45));
        statusPanel.setLayout(new javax.swing.BoxLayout(statusPanel, javax.swing.BoxLayout.LINE_AXIS));

        jSeparator31.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator31.setMaximumSize(new java.awt.Dimension(5, 32767));
        jSeparator31.setMinimumSize(new java.awt.Dimension(3, 16));
        jSeparator31.setPreferredSize(new java.awt.Dimension(3, 16));
        jSeparator31.setRequestFocusEnabled(false);
        statusPanel.add(jSeparator31);

        GreenLed.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        GreenLed.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/green.png"))); // NOI18N
        GreenLed.setEnabled(false);
        GreenLed.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        GreenLed.setPreferredSize(new java.awt.Dimension(24, 24));
        statusPanel.add(GreenLed);

        jSeparator32.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator32.setMaximumSize(new java.awt.Dimension(5, 32767));
        jSeparator32.setMinimumSize(new java.awt.Dimension(3, 16));
        jSeparator32.setPreferredSize(new java.awt.Dimension(3, 16));
        jSeparator32.setRequestFocusEnabled(false);
        statusPanel.add(jSeparator32);

        YellowLed.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        YellowLed.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/yellow.png"))); // NOI18N
        YellowLed.setEnabled(false);
        YellowLed.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        YellowLed.setPreferredSize(new java.awt.Dimension(24, 24));
        statusPanel.add(YellowLed);

        jSeparator33.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator33.setMaximumSize(new java.awt.Dimension(5, 32767));
        jSeparator33.setMinimumSize(new java.awt.Dimension(3, 16));
        jSeparator33.setPreferredSize(new java.awt.Dimension(3, 16));
        jSeparator33.setRequestFocusEnabled(false);
        statusPanel.add(jSeparator33);

        TapeLed.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        TapeLed.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/tape.png"))); // NOI18N
        TapeLed.setToolTipText("Tape runs");
        TapeLed.setEnabled(false);
        TapeLed.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        statusPanel.add(TapeLed);

        jSeparator34.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator34.setMaximumSize(new java.awt.Dimension(5, 32767));
        jSeparator34.setMinimumSize(new java.awt.Dimension(3, 16));
        jSeparator34.setPreferredSize(new java.awt.Dimension(3, 16));
        jSeparator34.setRequestFocusEnabled(false);
        statusPanel.add(jSeparator34);

        bRec.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/player_play.png"))); // NOI18N
        bRec.setToolTipText("Play - Record switch");
        bRec.setBorderPainted(false);
        bRec.setFocusPainted(false);
        bRec.setFocusable(false);
        bRec.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/player_rec.png"))); // NOI18N
        bRec.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bRecActionPerformed(evt);
            }
        });
        statusPanel.add(bRec);

        jSeparator35.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator35.setMaximumSize(new java.awt.Dimension(5, 32767));
        jSeparator35.setMinimumSize(new java.awt.Dimension(3, 16));
        jSeparator35.setPreferredSize(new java.awt.Dimension(3, 16));
        jSeparator35.setRequestFocusEnabled(false);
        statusPanel.add(jSeparator35);

        getContentPane().add(statusPanel, java.awt.BorderLayout.PAGE_END);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void bResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bResetActionPerformed
        m.Reset(false);
    }//GEN-LAST:event_bResetActionPerformed

    private void bNmiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bNmiActionPerformed
        m.Nmi();
    }//GEN-LAST:event_bNmiActionPerformed

    private void bSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bSettingsActionPerformed
        boolean pau = m.isPaused();
        m.stopEmulation();
        
        Settings set = new Settings();
        set.showDialog(m.getConfig());
        if (set.isResetNeeded()) {
            m.Reset(false);
        }
        set.dispose();
        if (!pau) m.startEmulation();
    }//GEN-LAST:event_bSettingsActionPerformed

    private void bOpentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bOpentActionPerformed
        boolean pau = m.isPaused();
        m.stopEmulation();

        fc.setDialogTitle("Open tape for LOAD");
        fc.resetChoosableFileFilters();
        fc.setAcceptAllFileFilterUsed(true);
        fc.setFileFilter(new FileNameExtensionFilter("Ondra wave tapes", "wav"));
        fc.setFileFilter(new FileNameExtensionFilter("Ondra compressed wave tapes", "csw"));
        fc.setFileFilter(new FileNameExtensionFilter("Ondra binary tapes", "tap"));
        int val = fc.showOpenDialog(this);
        
        if (val==JFileChooser.APPROVE_OPTION) {
            try {
                m.openLoadTape(fc.getSelectedFile().getCanonicalPath());
            } catch (IOException ex) {
                Logger.getLogger(JOndra.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (!pau) m.startEmulation();
    }//GEN-LAST:event_bOpentActionPerformed

    private void bSavetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bSavetActionPerformed
        boolean pau = m.isPaused();
        m.stopEmulation();

        fc.setDialogTitle("Open tape for SAVE");
        fc.resetChoosableFileFilters();
        fc.setAcceptAllFileFilterUsed(true);
        fc.setFileFilter(new FileNameExtensionFilter("Ondra compressed wave tapes", "csw"));
        fc.setFileFilter(new FileNameExtensionFilter("Ondra binary tapes", "tap"));
        int val = fc.showSaveDialog(this);
        
        if (val==JFileChooser.APPROVE_OPTION) {
            try {
                m.openSaveTape(fc.getSelectedFile().getCanonicalPath());
            } catch (IOException ex) {
                Logger.getLogger(JOndra.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (!pau) m.startEmulation();
    }//GEN-LAST:event_bSavetActionPerformed

    private void bPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bPauseActionPerformed
        if (bPause.isSelected()) {
            m.startEmulation();
        } 
        else {
            m.stopEmulation();
        }
    }//GEN-LAST:event_bPauseActionPerformed

    private void bRecActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bRecActionPerformed
        m.setTapeMode(bRec.isSelected());
    }//GEN-LAST:event_bRecActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        m.closeClenaup();
    }//GEN-LAST:event_formWindowClosing

    private void bOpensActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bOpensActionPerformed
        boolean pau = m.isPaused();
        m.stopEmulation();

        fc.setDialogTitle("Open snapshot");
        fc.resetChoosableFileFilters();
        fc.setAcceptAllFileFilterUsed(true);
        fc.setFileFilter(new FileNameExtensionFilter("Ondra snapshots", "osn"));
        int val = fc.showOpenDialog(this);
        
        if (val==JFileChooser.APPROVE_OPTION) {
            try {
                m.loadSnapshot(fc.getSelectedFile().getCanonicalPath());
            } catch (IOException ex) {
                Logger.getLogger(JOndra.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (!pau) m.startEmulation();
    }//GEN-LAST:event_bOpensActionPerformed

    private void bSavesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bSavesActionPerformed
        boolean pau = m.isPaused();
        m.stopEmulation();

        fc.setDialogTitle("Save snapshot");
        fc.resetChoosableFileFilters();
        fc.setAcceptAllFileFilterUsed(true);
        fc.setFileFilter(new FileNameExtensionFilter("Ondra snapshots", "osn"));
        int val = fc.showSaveDialog(this);
        
        if (val==JFileChooser.APPROVE_OPTION) {
            try {
                m.saveSnapshot(fc.getSelectedFile().getCanonicalPath());
            } catch (IOException ex) {
                Logger.getLogger(JOndra.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (!pau) m.startEmulation();
    }//GEN-LAST:event_bSavesActionPerformed

    private void initEmulator() {
        m = new Ondra();
        scr = new Screen();
        
        m.setScreen(scr);
        scr.setImage(m.getImage());
        
        m.setGreenLed(GreenLed);
        m.setYellowLed(YellowLed);        
        m.setTapeLed(TapeLed);
        m.setRecButton(bRec);

        getContentPane().add(scr, BorderLayout.CENTER);
        pack();
        
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screen.width-getSize().width)/2, (screen.height-getSize().height)/2);
        setFocusTraversalKeysEnabled(false);
        addKeyListener(m.getKeyboard());
        m.start();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(JOndra.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(JOndra.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(JOndra.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(JOndra.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /*
         * Create and display the form
         */
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                new JOndra().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel GreenLed;
    private javax.swing.JLabel TapeLed;
    private javax.swing.JToolBar ToolBar;
    private javax.swing.JLabel YellowLed;
    private javax.swing.JButton bNmi;
    private javax.swing.JButton bOpens;
    private javax.swing.JButton bOpent;
    private javax.swing.JToggleButton bPause;
    private javax.swing.JToggleButton bRec;
    private javax.swing.JButton bReset;
    private javax.swing.JButton bSaves;
    private javax.swing.JButton bSavet;
    private javax.swing.JButton bSettings;
    private javax.swing.JFileChooser fc;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JSeparator jSeparator31;
    private javax.swing.JSeparator jSeparator32;
    private javax.swing.JSeparator jSeparator33;
    private javax.swing.JSeparator jSeparator34;
    private javax.swing.JSeparator jSeparator35;
    private javax.swing.JToolBar.Separator jSeparator4;
    private javax.swing.JToolBar.Separator jSeparator5;
    private javax.swing.JToolBar.Separator jSeparator6;
    private javax.swing.JToolBar.Separator jSeparator7;
    private javax.swing.JToolBar.Separator jSeparator8;
    private javax.swing.JPanel statusPanel;
    // End of variables declaration//GEN-END:variables

}
