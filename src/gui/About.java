/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tmilata
 */
public class About extends JDialog {
  public About(JFrame parent) {
    super(parent, "About application", true);

    Box b = Box.createVerticalBox();
    b.add(Box.createGlue());    
    Box b2 = Box.createHorizontalBox();   
    b2.add(new JLabel("Ondra emulator written in pure Java"));
    Box b3 = Box.createHorizontalBox();
    b3.add(new JLabel("Build date " + buildDate()));
    b.add(b2, "Center");
    b.add(b3, "Center");
    b.add(Box.createGlue());
    setIconImage((new ImageIcon(getClass().getResource("/icons/ondra.png")).getImage()));
    getContentPane().add(b, "Center");

    JPanel p2 = new JPanel();
    JButton ok = new JButton("Ok");
    p2.add(ok);
    getContentPane().add(p2, "South");

    ok.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        setVisible(false);
      }
    });

    setSize(250, 150);
  }

  /**
   * Datum sestaveni: cas zapsani manifestu do JARu (build.sh i NetBeans ho
   * nastavi pri baleni), pri spusteni z nezabalenych trid cas prekladu About.class.
   */
  private static String buildDate() {
    long t = 0;
    try {
      URL src = About.class.getProtectionDomain().getCodeSource().getLocation();
      File f = new File(src.toURI());
      if (f.isFile()) {
        try (JarFile jar = new JarFile(f)) {
          ZipEntry e = jar.getEntry(JarFile.MANIFEST_NAME);
          if (e != null) {
            t = e.getTime();
          }
        }
      } else {
        t = About.class.getResource("About.class").openConnection().getLastModified();
      }
    } catch (Exception ex) {
      t = 0;
    }
    return t > 0 ? new SimpleDateFormat("d.M.yyyy").format(new Date(t)) : "unknown";
  }
}
