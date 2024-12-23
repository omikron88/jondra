/*0
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;
import machine.Ondra;
import utils.Config;

/**
 *
 * @author Administrator
 */
public class JOndra extends javax.swing.JFrame {

    private Ondra m;
    private Screen scr;
    boolean bFullscreen = false;
    int nScaleNx = 0;

    private Debugger deb;
    private BinOpen bopn;
    private BinSave bsav;
    private KeyboardPicture kbrd;
    private boolean bFirstShowKbd = true;
    public String strHomeDirectory = "";
    public String strArgFile = "";
    public int nStartAddress = -1;
    //snazim se nabidnout co nejvhodnejsi filename pro sceenshot a snapshot
    //je vyplneno pri otevreni nejakeho suboru tap, wav, csw nebo bin
    private String strSnapshotNameProposal = "";
    private String strSceenshotNameProposal = "";

    /**
     * Creates new form JOndra
     */
    public JOndra(String args[]) {

        strHomeDirectory = JOndra.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (strHomeDirectory.contains("/")) {
            int pos = strHomeDirectory.lastIndexOf("/");
            strHomeDirectory = strHomeDirectory.substring(0, pos);
        }

        if (args.length > 0) {
            if (args.length == 1) {
                strArgFile = args[0];
            } else if (args.length == 2) {
                strArgFile = args[0];
                nStartAddress = -1;
                try {
                    if (args[1].length() > 0) {
                        String strAddress = args[1].toLowerCase();
                        strAddress = strAddress.replace("0x", "");
                        strAddress = strAddress.replace("h", "");
                        nStartAddress = Integer.valueOf(strAddress, 16);
                    }
                } catch (Exception ex) {
                    nStartAddress = -1;
                }

            }
        }
        Config.LoadConfig();
        initComponents();
        setIconImage((new ImageIcon(getClass().getResource("/icons/ondra.png")).getImage()));
        //presun polozky menu About doprava
        jMenuBar1.remove(jAbout);
        jMenuBar1.add(Box.createHorizontalGlue());
        jMenuBar1.add(jAbout);

        initEmulator();
        // Nastavení výchozího režimu
        setFullscreen(Config.bFullscreen);
        scr.setScanlines(Config.bScanlines);
        scr.setScaleNx(Config.bScaleNx);
        // Přidání KeyListeneru pro zachycení F11        
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_F12) {
                    // Přepnout fullscreen režim
                    scr.setScaleNx(Config.bScaleNx);
                    setFullscreen(!bFullscreen);
                }
            }
        });

    }

    public void updateWindowSize() {
        if (!scr.getFullscreenStatus()) {
            int ratio = scr.getRatio();
            BufferedImage image = scr.getImage();
            int imageWidth = image.getWidth() * ratio;
            int imageHeight = image.getHeight() * ratio;

            // Získání dekorací okna
            Insets insets = getInsets();
            int decorWidth = insets.left + insets.right;
            int decorHeight = insets.top + insets.bottom;

            if (ToolBar.isVisible()) {
                decorHeight += ToolBar.getHeight();
            }
            if (jMenuBar1.isVisible()) {
                decorHeight += jMenuBar1.getHeight();
            }
            if (statusPanel.isVisible()) {
                decorHeight += statusPanel.getHeight();
            }
            // Nastavení velikosti okna na základě obrazu + dekorací
            int frameWidth = imageWidth + decorWidth;
            int frameHeight = imageHeight + decorHeight;

            // Nastavení velikosti okna
            setSize(frameWidth, frameHeight);

            // Centrovat okno na obrazovce
            setLocationRelativeTo(null);

            // Zajištění, že panel `Screen` bude mít stejnou velikost jako obraz
            scr.setPreferredSize(new Dimension(imageWidth, imageHeight));
            scr.revalidate();
        }
    }

    public void setFullscreen(boolean fullscreenMode) {
        bFullscreen = fullscreenMode;
        scr.setFullScreen(fullscreenMode); // Nastavení fullscreen statusu v komponentě obrazovky
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        if (fullscreenMode) {
            // Přepnout na fullscreen režim
            dispose(); // Umožňuje změnit dekorace
            setUndecorated(true); // Skrytí dekorací
            setJMenuBar(null); // Skrytí menu
            statusPanel.setVisible(false); // Skryje status bar
            ToolBar.setVisible(false); // Skrytí toolbaru

            // Nastavení fullscreen přes GraphicsDevice
            gd.setFullScreenWindow(this);
        } else {
            // Přepnout na normální režim
            gd.setFullScreenWindow(null);
            dispose(); // Umožňuje změnit dekorace
            setUndecorated(false); // Zobrazení dekorací
            setJMenuBar(jMenuBar1); // Zobrazení menu
            ToolBar.setVisible(true); // Zobrazení toolbaru
            statusPanel.setVisible(true); // Zobrazí status bar

            // Nastavit velikost okna zpět na původní
            pack();
            setLocationRelativeTo(null);
            setVisible(true);
        }
        updateWindowSize();
    }

    public void LoadBinSilently(boolean bAutoStart) {
        if (strArgFile.length() > 0) {
            String strFile = "";
            if (strArgFile.contains("/") || strArgFile.contains("\\")) {
                strFile = strArgFile;
            } else {
                strFile = strHomeDirectory + "/" + strArgFile;
            }
            m.stopEmulation();
            BufferedInputStream fIn;
            try {
                m.mem.mapRom(false);
                fIn = new BufferedInputStream(new FileInputStream(strFile));
                boolean bFinish = false;
                while (!bFinish) {
                    int bType = fIn.read();
                    if (bType == -1) {
                        bFinish = true;
                        break;
                    }
                    int nMemAdr = 0;
                    int bBlockLen = 0;
                    if (bType == 1) {
                        //blok dat k ulozeni do RAM
                        nMemAdr = fIn.read() + 256 * fIn.read();
                        bBlockLen = fIn.read() + 256 * fIn.read();
                        byte[] contents = new byte[bBlockLen];
                        int bytesRead = fIn.read(contents);
                        if (bytesRead != -1) {
                            for (int i = 0; i < bytesRead; i++) {
                                m.mem.writeByte(nMemAdr, contents[i]);
                                nMemAdr++;
                            }
                        }
                    } else if (bType == 2) {
                        //blok ke spusteni                        
                        nMemAdr = fIn.read() + 256 * fIn.read();
                        m.cpu.setRegPC(nMemAdr);
                        bFinish = true;
                        break;
                    } else {
                        m.Reset(true);
                        break;
                    };
                }
            } catch (Exception e) {
            }
            //nastavim Ondru na spravnou rychlost
            m.setClockSpeed(20);
            if (bAutoStart) {
                m.startEmulation();
            }
        }
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
        bReset = new javax.swing.JButton();
        bPause = new javax.swing.JToggleButton();
        bNmi = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        bOpent = new javax.swing.JButton();
        bSavet = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        bOpens = new javax.swing.JButton();
        bSaves = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JToolBar.Separator();
        jLoadMem = new javax.swing.JButton();
        jSaveMem = new javax.swing.JButton();
        jDebbuger = new javax.swing.JButton();
        jSeparator8 = new javax.swing.JToolBar.Separator();
        bSettings = new javax.swing.JButton();
        jShowKeyboard = new javax.swing.JButton();
        statusPanel = new javax.swing.JPanel();
        jSeparator31 = new javax.swing.JSeparator();
        GreenLed = new javax.swing.JLabel();
        jSeparator32 = new javax.swing.JSeparator();
        YellowLed = new javax.swing.JLabel();
        jSeparator33 = new javax.swing.JSeparator();
        TapeLed = new javax.swing.JLabel();
        jSeparator34 = new javax.swing.JSeparator();
        jSeparator35 = new javax.swing.JSeparator();
        bRec = new javax.swing.JToggleButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jTapeLoad = new javax.swing.JMenuItem();
        jTapeSave = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        jOpenSnapshot = new javax.swing.JMenuItem();
        jSaveSnapshot = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        jLoadMemoryBlockMenu = new javax.swing.JMenuItem();
        jSaveMemoryBlockMenu = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        jScreenshot = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        jExit = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jReset = new javax.swing.JMenuItem();
        jPause = new javax.swing.JMenuItem();
        jNMI = new javax.swing.JMenuItem();
        jTools = new javax.swing.JMenu();
        jDebugger = new javax.swing.JMenuItem();
        jSettings = new javax.swing.JMenuItem();
        jKeyboardMenu = new javax.swing.JMenuItem();
        jAbout = new javax.swing.JMenu();

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

        bReset.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/reset.png"))); // NOI18N
        bReset.setToolTipText("Reset");
        bReset.setFocusable(false);
        bReset.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        bReset.setPreferredSize(new java.awt.Dimension(23, 23));
        bReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bResetActionPerformed(evt);
            }
        });
        ToolBar.add(bReset);

        bPause.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/pause.png"))); // NOI18N
        bPause.setToolTipText("Pause");
        bPause.setFocusable(false);
        bPause.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        bPause.setOpaque(true);
        bPause.setPreferredSize(new java.awt.Dimension(23, 23));
        bPause.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jPauseActionPerformed(evt);
            }
        });
        ToolBar.add(bPause);

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
        ToolBar.add(jSeparator1);

        bOpent.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/open.png"))); // NOI18N
        bOpent.setToolTipText("Open tape for Load");
        bOpent.setFocusable(false);
        bOpent.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        bOpent.setPreferredSize(new java.awt.Dimension(23, 23));
        bOpent.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bOpent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bOpentActionPerformed(evt);
            }
        });
        ToolBar.add(bOpent);

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

        bOpens.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/opensn.png"))); // NOI18N
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

        bSaves.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/savesn.png"))); // NOI18N
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
        ToolBar.add(jSeparator4);

        jLoadMem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/binaryopn.png"))); // NOI18N
        jLoadMem.setToolTipText("Upload binary file into memory");
        jLoadMem.setFocusable(false);
        jLoadMem.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLoadMem.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jLoadMem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jLoadMemActionPerformed(evt);
            }
        });
        ToolBar.add(jLoadMem);

        jSaveMem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/binarysav.png"))); // NOI18N
        jSaveMem.setToolTipText("Save memory into file");
        jSaveMem.setFocusable(false);
        jSaveMem.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jSaveMem.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jSaveMem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jSaveMemActionPerformed(evt);
            }
        });
        ToolBar.add(jSaveMem);

        jDebbuger.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/debugger.png"))); // NOI18N
        jDebbuger.setToolTipText("Start Debbuger");
        jDebbuger.setFocusable(false);
        jDebbuger.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jDebbuger.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jDebbuger.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jDebbugerActionPerformed(evt);
            }
        });
        ToolBar.add(jDebbuger);
        ToolBar.add(jSeparator8);

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

        jShowKeyboard.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/keyico.png"))); // NOI18N
        jShowKeyboard.setToolTipText("Show Ondra keyboard");
        jShowKeyboard.setFocusable(false);
        jShowKeyboard.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jShowKeyboard.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jShowKeyboard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jShowKeyboardActionPerformed(evt);
            }
        });
        ToolBar.add(jShowKeyboard);

        getContentPane().add(ToolBar, java.awt.BorderLayout.PAGE_START);

        statusPanel.setMaximumSize(new java.awt.Dimension(100, 32767));
        statusPanel.setMinimumSize(new java.awt.Dimension(100, 40));
        statusPanel.setPreferredSize(new java.awt.Dimension(100, 24));
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

        jSeparator35.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator35.setMaximumSize(new java.awt.Dimension(5, 32767));
        jSeparator35.setMinimumSize(new java.awt.Dimension(3, 16));
        jSeparator35.setPreferredSize(new java.awt.Dimension(3, 16));
        jSeparator35.setRequestFocusEnabled(false);
        statusPanel.add(jSeparator35);

        bRec.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/player_play.png"))); // NOI18N
        bRec.setToolTipText("Play - Record switch");
        bRec.setBorderPainted(false);
        bRec.setFocusPainted(false);
        bRec.setFocusable(false);
        bRec.setMaximumSize(new java.awt.Dimension(26, 32));
        bRec.setMinimumSize(new java.awt.Dimension(24, 24));
        bRec.setPreferredSize(new java.awt.Dimension(32, 32));
        bRec.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/player_rec.png"))); // NOI18N
        bRec.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bRecActionPerformed(evt);
            }
        });
        statusPanel.add(bRec);

        getContentPane().add(statusPanel, java.awt.BorderLayout.PAGE_END);

        jMenu1.setText("File");

        jTapeLoad.setText("Open TAPE for Load");
        jTapeLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTapeLoadActionPerformed(evt);
            }
        });
        jMenu1.add(jTapeLoad);

        jTapeSave.setText("Open TAPE for Save");
        jTapeSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTapeSaveActionPerformed(evt);
            }
        });
        jMenu1.add(jTapeSave);
        jMenu1.add(jSeparator5);

        jOpenSnapshot.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F8, 0));
        jOpenSnapshot.setText("Open snapshot");
        jOpenSnapshot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jOpenSnapshotActionPerformed(evt);
            }
        });
        jMenu1.add(jOpenSnapshot);

        jSaveSnapshot.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0));
        jSaveSnapshot.setText("Save snapshot");
        jSaveSnapshot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jSaveSnapshotActionPerformed(evt);
            }
        });
        jMenu1.add(jSaveSnapshot);
        jMenu1.add(jSeparator6);

        jLoadMemoryBlockMenu.setText("Load Memory Block");
        jLoadMemoryBlockMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jLoadMemoryBlockMenuActionPerformed(evt);
            }
        });
        jMenu1.add(jLoadMemoryBlockMenu);

        jSaveMemoryBlockMenu.setText("Save Memory Block");
        jSaveMemoryBlockMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jSaveMemoryBlockMenuActionPerformed(evt);
            }
        });
        jMenu1.add(jSaveMemoryBlockMenu);
        jMenu1.add(jSeparator3);

        jScreenshot.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0));
        jScreenshot.setText("Save screenshot");
        jScreenshot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jScreenshotActionPerformed(evt);
            }
        });
        jMenu1.add(jScreenshot);
        jMenu1.add(jSeparator7);

        jExit.setText("Exit");
        jExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jExitActionPerformed(evt);
            }
        });
        jMenu1.add(jExit);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Control");

        jReset.setText("Reset");
        jReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jResetActionPerformed(evt);
            }
        });
        jMenu2.add(jReset);

        jPause.setText("Pause");
        jPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jPauseActionPerformed1(evt);
            }
        });
        jMenu2.add(jPause);

        jNMI.setText("NMI");
        jNMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jNMIActionPerformed(evt);
            }
        });
        jMenu2.add(jNMI);

        jMenuBar1.add(jMenu2);

        jTools.setText("Tools");

        jDebugger.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
        jDebugger.setText("Debugger");
        jDebugger.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jDebuggerActionPerformed(evt);
            }
        });
        jTools.add(jDebugger);

        jSettings.setText("Settings");
        jSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jSettingsActionPerformed(evt);
            }
        });
        jTools.add(jSettings);

        jKeyboardMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0));
        jKeyboardMenu.setText("Keyboard");
        jKeyboardMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jKeyboardMenuActionPerformed(evt);
            }
        });
        jTools.add(jKeyboardMenu);

        jMenuBar1.add(jTools);

        jAbout.setText("About");
        jAbout.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jAboutMousePressed(evt);
            }
        });
        jMenuBar1.add(jAbout);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void bResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bResetActionPerformed
        //kdyby nekdo resetoval behem zrychleneho nahravani, tak timto se vrati rychlost na spravnou hodnotu
        m.stopEmulation();
        m.setClockSpeed(20);
        m.startEmulation();
        setProposalName("");
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
        scr.setScanlines(Config.bScanlines);
        if (!pau)
            m.startEmulation();
    }//GEN-LAST:event_bSettingsActionPerformed

    private void bOpentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bOpentActionPerformed
        boolean pau = m.isPaused();
        m.stopEmulation();

        fc.setDialogTitle("Open tape for LOAD");
        Config.LoadConfig();
        fc.setSelectedFile(new File(""));
        fc.setCurrentDirectory(new File(Config.nullToEmpty(new File(Config.strTapFilePath).getParent())));
        fc.resetChoosableFileFilters();
        fc.setAcceptAllFileFilterUsed(true);
        fc.setFileFilter(new FileNameExtensionFilter("Ondra wave tapes", "wav"));
        fc.setFileFilter(new FileNameExtensionFilter("Ondra compressed wave tapes", "csw"));
        fc.setFileFilter(new FileNameExtensionFilter("Ondra binary tapes", "tap"));
        int val = fc.showOpenDialog(this);

        if (val == JFileChooser.APPROVE_OPTION) {
            try {
                String strFileName = fc.getSelectedFile().getCanonicalPath();
                m.openLoadTape(strFileName);
                setProposalName(getFilenameOnly(strFileName));
                //kdyby nekdo vybiral behem zrychleneho nahravani, tak timto se vrati rychlost na spravnou hodnotu
                m.setClockSpeed(20);
                Config.strTapFilePath = strFileName;
                Config.SaveConfig();
            } catch (IOException ex) {
                Logger.getLogger(JOndra.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (!pau)
            m.startEmulation();
    }//GEN-LAST:event_bOpentActionPerformed

    private void bSavetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bSavetActionPerformed
        boolean pau = m.isPaused();
        m.stopEmulation();

        fc.setDialogTitle("Open tape for SAVE");
        Config.LoadConfig();
        fc.setCurrentDirectory(new File(Config.nullToEmpty(new File(Config.strTapFilePath).getParent())));
        fc.setSelectedFile(new File(""));
        fc.resetChoosableFileFilters();
        fc.setAcceptAllFileFilterUsed(true);
        fc.setFileFilter(new FileNameExtensionFilter("Ondra compressed wave tapes", "csw"));
        //fc.setFileFilter(new FileNameExtensionFilter("Ondra binary tapes", "tap"));
        int val = fc.showSaveDialog(this);

        if (val == JFileChooser.APPROVE_OPTION) {
            try {
                m.openSaveTape(fc.getSelectedFile().getCanonicalPath());
                Config.strTapFilePath = fc.getSelectedFile().getCanonicalPath();
                Config.SaveConfig();
            } catch (IOException ex) {
                Logger.getLogger(JOndra.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (!pau)
            m.startEmulation();
    }//GEN-LAST:event_bSavetActionPerformed

    private void jPauseActionPerformedDo(java.awt.event.ActionEvent evt) {
        if (m.isPaused()) {
            m.startEmulation();
        } else {

            m.stopEmulation();
        }
    }

    private void jPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jPauseActionPerformed
        jPauseActionPerformedDo(null);
    }//GEN-LAST:event_jPauseActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        m.closeClenaup();
    }//GEN-LAST:event_formWindowClosing

    private void bOpensActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bOpensActionPerformed
        boolean pau = m.isPaused();
        m.stopEmulation();

        fc.setDialogTitle("Open snapshot");
        Config.LoadConfig();
        fc.setCurrentDirectory(new File(Config.nullToEmpty(new File(Config.strSnapFilePath).getParent())));
        fc.setSelectedFile(new File(Config.strSnapFilePath));
        fc.resetChoosableFileFilters();
        fc.setAcceptAllFileFilterUsed(true);
        fc.setFileFilter(new FileNameExtensionFilter("Ondra snapshots", "osn"));
        int val = fc.showOpenDialog(this);

        if (val == JFileChooser.APPROVE_OPTION) {
            try {
                String strSnapName = fc.getSelectedFile().getCanonicalPath();
                if (!getExtension(strSnapName).equalsIgnoreCase("osn")) {
                    strSnapName += ".osn";
                }
                m.loadSnapshot(strSnapName);
                //kdyby nekdo vybiral behem zrychleneho nahravani, tak timto se vrati rychlost na spravnou hodnotu
                m.setClockSpeed(20);
                Config.strSnapFilePath = strSnapName;
                Config.SaveConfig();
            } catch (IOException ex) {
                Logger.getLogger(JOndra.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (!pau)
            m.startEmulation();
    }//GEN-LAST:event_bOpensActionPerformed

    public void setProposalName(String inName) {
        strSceenshotNameProposal = inName;
        strSnapshotNameProposal = inName;
    }

    public String getFilenameOnly(String fileName) {
        String strNewFileName = "";
        if (fileName != null) {
            int i = fileName.lastIndexOf(File.separator);
            if (i > 0) {
                strNewFileName = fileName.substring(i + 1);
            } else {
                strNewFileName = fileName;
            }
            i = strNewFileName.lastIndexOf('.');
            if (i > 0) {
                strNewFileName = strNewFileName.substring(0, i);
            }
        }
        return strNewFileName;
    }

    public String getExtension(String fileName) {
        String extension = "";
        if (fileName != null) {
            int i = fileName.lastIndexOf('.');
            if (i > 0) {
                extension = fileName.substring(i + 1);
            }
        }
        return extension;
    }

    public String removeExtension(String fileName) {
        String strNewFileName = "";
        String strTemp;
        if (fileName != null) {
            int i = fileName.lastIndexOf(File.separator);
            if (i > 0) {
                strNewFileName = fileName.substring(0, i + 1);
                strTemp = fileName.substring(i + 1);

            } else {
                strNewFileName = "";
                strTemp = fileName;
            }
            i = strTemp.lastIndexOf('.');
            if (i > 0) {
                strTemp = strTemp.substring(0, i);
            }
            strNewFileName += strTemp;
        }
        return strNewFileName;
    }

    //vlozi do jmena souboru index pokud tam neni, pote vrati dalsi jmeno v poradi indexu
    private String getNextFileName(String strCurrentFileName, String strDefault) {
        String strRet = strDefault;
        String strShort = new File(strCurrentFileName).getName();
        String strExtension = getExtension(strShort);
        if (!strExtension.isEmpty()) {
            strExtension = "." + strExtension;
        }
        if (!strShort.isEmpty()) {
            String strTmpNoExt = strShort;
            if (!getExtension(strShort).isEmpty()) {
                strTmpNoExt = strShort.substring(0, strShort.lastIndexOf('.'));
            }
            Pattern pattern = Pattern.compile("^(.*\\D)(\\d+)$");
            Matcher matcher = pattern.matcher(strTmpNoExt);
            if (matcher.find()) {
                int nFmt = matcher.group(2).length();
                int nNewVal = Integer.parseInt(matcher.group(2)) + 1;
                strRet = matcher.group(1) + String.format("%0" + nFmt + "d", nNewVal) + strExtension;
            } else {
                strRet = strTmpNoExt + "01" + strExtension;
            }
        }
        return strRet;
    }


    private void bSavesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bSavesActionPerformed
        boolean pau = m.isPaused();
        m.stopEmulation();

        fc.setDialogTitle("Save snapshot");
        Config.LoadConfig();
        fc.setCurrentDirectory(new File(Config.nullToEmpty(new File(Config.strSnapFilePath).getParent())));
        String strNewFileName = Config.strSnapFilePath;
        if (!strSnapshotNameProposal.isEmpty()) {
            strNewFileName = strSnapshotNameProposal;
        }
        fc.setSelectedFile(new File(removeExtension(getNextFileName(strNewFileName, "snap01"))));
        fc.resetChoosableFileFilters();
        fc.setAcceptAllFileFilterUsed(true);
        fc.setFileFilter(new FileNameExtensionFilter("Ondra snapshots", "osn"));
        int val = fc.showSaveDialog(this);

        if (val == JFileChooser.APPROVE_OPTION) {
            try {
                String strSnap = fc.getSelectedFile().getCanonicalPath();
                if (!getExtension(strSnap).equalsIgnoreCase("osn")) {
                    strSnap += ".osn";
                }
                m.saveSnapshot(strSnap);
                strSnapshotNameProposal = "";
                Config.strSnapFilePath = strSnap;
                Config.SaveConfig();
            } catch (IOException ex) {
                Logger.getLogger(JOndra.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (!pau)
            m.startEmulation();
    }//GEN-LAST:event_bSavesActionPerformed

    private void jLoadMemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jLoadMemActionPerformed
        jLoadMemoryBlockActionPerformed(null);
    }//GEN-LAST:event_jLoadMemActionPerformed

    private void jSaveMemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jSaveMemActionPerformed
        jSaveMemoryBlockActionPerformed(null);
    }//GEN-LAST:event_jSaveMemActionPerformed

    private void jDebbugerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jDebbugerActionPerformed
        jDebuggerStartActionPerformed(null);
    }//GEN-LAST:event_jDebbugerActionPerformed

    private void jAboutMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jAboutMousePressed
        JDialog dAbout = new About(new JFrame());
        dAbout.setLocationRelativeTo(this);
        dAbout.setVisible(true);        // TODO add your handling code here:
    }//GEN-LAST:event_jAboutMousePressed

    private void jDebuggerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jDebuggerActionPerformed
        jDebuggerStartActionPerformed(null);
    }//GEN-LAST:event_jDebuggerActionPerformed

    private void jSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jSettingsActionPerformed
        bSettingsActionPerformed(null);
    }//GEN-LAST:event_jSettingsActionPerformed

    private void jTapeLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTapeLoadActionPerformed
        bOpentActionPerformed(null);
    }//GEN-LAST:event_jTapeLoadActionPerformed

    private void jTapeSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTapeSaveActionPerformed
        bSavetActionPerformed(null);
    }//GEN-LAST:event_jTapeSaveActionPerformed

    private void jOpenSnapshotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jOpenSnapshotActionPerformed
        bOpensActionPerformed(null);
    }//GEN-LAST:event_jOpenSnapshotActionPerformed

    private void jSaveSnapshotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jSaveSnapshotActionPerformed
        bSavesActionPerformed(null);
    }//GEN-LAST:event_jSaveSnapshotActionPerformed

    private void jLoadMemoryBlockMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jLoadMemoryBlockMenuActionPerformed
        jLoadMemoryBlockActionPerformed(null);
    }//GEN-LAST:event_jLoadMemoryBlockMenuActionPerformed

    private void jSaveMemoryBlockMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jSaveMemoryBlockMenuActionPerformed
        jSaveMemoryBlockActionPerformed(null);
    }//GEN-LAST:event_jSaveMemoryBlockMenuActionPerformed

    private void jShowKeyboardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jShowKeyboardActionPerformed
        if (bFirstShowKbd) {
            kbrd.setLocationRelativeTo(this);
            bFirstShowKbd = false;
        }
        if (kbrd.isVisible()) {
            m.getKeyboard().removeKeyboardPicture();
            kbrd.hideDialog();
        } else {
            kbrd.showDialog();
            kbrd.setAlwaysOnTop(true);
            m.getKeyboard().setKeyboardPicture(kbrd);
            this.requestFocus();
        }
    }//GEN-LAST:event_jShowKeyboardActionPerformed

    private void jResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jResetActionPerformed
        bResetActionPerformed(null);
    }//GEN-LAST:event_jResetActionPerformed

    private void jPauseActionPerformed1(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jPauseActionPerformed1
        jPauseActionPerformedDo(null);
    }//GEN-LAST:event_jPauseActionPerformed1

    private void jNMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jNMIActionPerformed
        bNmiActionPerformed(null);
    }//GEN-LAST:event_jNMIActionPerformed

    private void jKeyboardMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jKeyboardMenuActionPerformed
        jShowKeyboardActionPerformed(null);
    }//GEN-LAST:event_jKeyboardMenuActionPerformed

    private void bRecActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bRecActionPerformed
        m.setTapeMode(bRec.isSelected());
    }//GEN-LAST:event_bRecActionPerformed

    private void jScreenshotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jScreenshotActionPerformed
        BufferedImage shot = m.getImage();
        boolean pau = m.isPaused();
        m.stopEmulation();

        fc.setDialogTitle("Save screenshot");
        Config.LoadConfig();
        fc.setCurrentDirectory(new File(Config.nullToEmpty(new File(Config.strShotFilePath).getParent())));
        String strNewFileName = Config.strShotFilePath;
        if (!strSceenshotNameProposal.isEmpty()) {
            strNewFileName = strSceenshotNameProposal;
        }
        fc.setSelectedFile(new File(removeExtension(getNextFileName(strNewFileName, "screen01"))));
        fc.resetChoosableFileFilters();
        fc.setAcceptAllFileFilterUsed(true);
        fc.setFileFilter(new FileNameExtensionFilter("Ondra screenshots", "png"));
        int val = fc.showSaveDialog(this);

        if (val == JFileChooser.APPROVE_OPTION) {
            try {
                String strSnap = fc.getSelectedFile().getCanonicalPath();
                if (!getExtension(strSnap).equalsIgnoreCase("png")) {
                    strSnap += ".png";
                }
                File outputfile = new File(strSnap);
                try {
                    ImageIO.write(shot, "png", outputfile);
                } catch (IOException ex) {
                    Logger.getLogger(JOndra.class.getName()).log(Level.SEVERE, null, ex);
                }
                strSceenshotNameProposal = "";
                Config.strShotFilePath = strSnap;
                Config.SaveConfig();
            } catch (IOException ex) {
                Logger.getLogger(JOndra.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (!pau) {
            m.startEmulation();
        }

    }//GEN-LAST:event_jScreenshotActionPerformed

    private void jExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jExitActionPerformed

        System.exit(0);        // TODO add your handling code here:
    }//GEN-LAST:event_jExitActionPerformed

    private void jLoadMemoryBlockActionPerformed(java.awt.event.ActionEvent evt) {
        boolean pau = m.isPaused();

        m.stopEmulation();
        bopn.setLocationRelativeTo(this);
        bopn.showDialog();
        bopn.setAlwaysOnTop(true);
    }

    private void jSaveMemoryBlockActionPerformed(java.awt.event.ActionEvent evt) {
        boolean pau = m.isPaused();

        m.stopEmulation();
        bsav.setLocationRelativeTo(this);
        bsav.showDialog();
        bsav.setAlwaysOnTop(true);
    }

    private void jDebuggerStartActionPerformed(java.awt.event.ActionEvent evt) {
        boolean pau = m.isPaused();

        m.stopEmulation();
        deb.showDialog();
        deb.setAlwaysOnTop(true);
    }

    private void initEmulator() {
        m = new Ondra();
        scr = new Screen();
        scr.setFullScreen(bFullscreen);
        scr.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize());

        m.setScreen(scr);
        scr.setImage(m.getImage());

        deb = new Debugger(m);
        bopn = new BinOpen(m);
        bopn.setParent(this);
        bsav = new BinSave(m);
        kbrd = new KeyboardPicture(this);
        m.setDebugger(deb);
        m.setFrame(this);

        m.setGreenLed(GreenLed);
        m.setYellowLed(YellowLed);
        m.setTapeLed(TapeLed);
        m.setRecButton(bRec);

        getContentPane().add(scr, BorderLayout.CENTER);
        pack();

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screen.width - getSize().width) / 2, (screen.height - getSize().height) / 2);
        setFocusTraversalKeysEnabled(false);
        addKeyListener(m.getKeyboard());
        m.start();
    }

    public Ondra getOndra() {
        return m;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String args[]) {

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
                new JOndra(args).setVisible(true);

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
    private javax.swing.JMenu jAbout;
    private javax.swing.JButton jDebbuger;
    private javax.swing.JMenuItem jDebugger;
    private javax.swing.JMenuItem jExit;
    private javax.swing.JMenuItem jKeyboardMenu;
    private javax.swing.JButton jLoadMem;
    private javax.swing.JMenuItem jLoadMemoryBlockMenu;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jNMI;
    private javax.swing.JMenuItem jOpenSnapshot;
    private javax.swing.JMenuItem jPause;
    private javax.swing.JMenuItem jReset;
    private javax.swing.JButton jSaveMem;
    private javax.swing.JMenuItem jSaveMemoryBlockMenu;
    private javax.swing.JMenuItem jSaveSnapshot;
    private javax.swing.JMenuItem jScreenshot;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JSeparator jSeparator31;
    private javax.swing.JSeparator jSeparator32;
    private javax.swing.JSeparator jSeparator33;
    private javax.swing.JSeparator jSeparator34;
    private javax.swing.JSeparator jSeparator35;
    private javax.swing.JToolBar.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JToolBar.Separator jSeparator8;
    private javax.swing.JMenuItem jSettings;
    private javax.swing.JButton jShowKeyboard;
    private javax.swing.JMenuItem jTapeLoad;
    private javax.swing.JMenuItem jTapeSave;
    private javax.swing.JMenu jTools;
    private javax.swing.JPanel statusPanel;
    // End of variables declaration//GEN-END:variables

}
