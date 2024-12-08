/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    b3.add(new JLabel("Build date 8.12.2024"));
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
}
