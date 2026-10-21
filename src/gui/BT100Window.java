package gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import machine.BT100;
import machine.Ondra;

/**
 * Okno s virtualnim papirem tiskarny Tesla BT-100: body tak, jak je jehla
 * natiskla, poloha hlavy, ulozeni obrazku a novy papir.
 */
public class BT100Window extends JFrame {

    /** pixelu na bod papiru v okne (ulozeny obrazek ma SAVE_SCALE) */
    static final int SCALE = 1;
    static final int SAVE_SCALE = 2;
    /** radku bodu, ktere ma papir vzdy (aby nebyl na zacatku prazdny prouzek) */
    static final int MIN_ROWS = 300;
    static final int MARGIN = 12;

    private final BT100 bt;
    private final Paper paper = new Paper();
    private JScrollPane scroll;
    private final JLabel info = new JLabel();
    private final JCheckBox follow = new JCheckBox("Sledovat tisk", true);
    private final JToggleButton power = new JToggleButton();
    private final Ondra m;
    private long seen = -1;
    private int lastRow = -1;
    private boolean closedByUser;

    private static BT100Window win;

    /** volat periodicky z GUI: okno je videt, kdyz je BT100 zapnuta */
    public static void update(Ondra m, JFrame owner) {
        boolean en = m.bt100.isEnabled();
        if (!en) {
            if (win != null && win.isVisible()) {
                win.setVisible(false);
            }
            return;
        }
        if (win == null) {
            win = new BT100Window(m);
            win.setLocationRelativeTo(owner);
            win.setVisible(true);
        } else if (!win.isVisible() && (!win.closedByUser || m.bt100.getChanges() != win.seen)) {
            win.closedByUser = false;
            win.setVisible(true);
        }
    }

    public BT100Window(Ondra m) {
        super("Tesla BT100 - papír");
        this.m = m;
        this.bt = m.bt100;
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                closedByUser = true;
                seen = bt.getChanges();
            }
        });

        JButton save = new JButton("Uložit obrázek…");
        save.addActionListener(e -> saveImage());
        JButton fresh = new JButton("Nový papír");
        fresh.addActionListener(e -> {
            bt.newPaper();
            scroll.getVerticalScrollBar().setValue(0);
            refresh(true);
        });
        // vypinac tiskarny: zapnuta ma snimace na vstupech joysticku, ktere Ondra
        // propojuje s maticí klavesnice - zapinat jen na tisk (vyzva SSM Wordu)
        power.setToolTipText("Zapnutí / vypnutí tiskárny (zapnutá ruší klávesnici Ondry)");
        power.addActionListener(e -> {
            bt.setPower(power.isSelected(), m.clk.getTstates());
            showPower();
        });
        showPower();
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(power);
        top.add(save);
        top.add(fresh);
        top.add(follow);
        top.add(info);

        scroll = new JScrollPane(paper);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        scroll.getViewport().setBackground(new Color(90, 90, 90));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(scroll, BorderLayout.CENTER);
        setSize(Math.max(700, BT100.WIDTH * SCALE + 2 * MARGIN + 40), 420);

        new Timer(100, e -> refresh(false)).start();
        refresh(true);
    }

    private void showPower() {
        boolean on = bt.isPowered();
        power.setSelected(on);
        power.setText(on ? "BT100: ZAP" : "BT100: VYP");
        power.setForeground(on ? new Color(0, 130, 0) : new Color(160, 0, 0));
    }

    private void refresh(boolean force) {
        if (power.isSelected() != bt.isPowered()) {
            showPower();
        }
        long ch = bt.getChanges();
        if (!force && ch == seen) {
            return;
        }
        if (!isVisible() && closedByUser) {
            return;
        }
        seen = ch;
        int rows = Math.max(bt.getPaperRows(), MIN_ROWS);
        Dimension d = new Dimension(BT100.WIDTH * SCALE + 2 * MARGIN, rows * SCALE + 2 * MARGIN);
        if (!d.equals(paper.getPreferredSize())) {
            paper.setPreferredSize(d);
            paper.revalidate();
        }
        int row = bt.getRow();
        info.setText("  řádek bodů " + row + "   vozík " + Math.round(bt.getCarriage()));
        if (follow.isSelected() && row != lastRow) {
            int y = MARGIN + row * SCALE;
            paper.scrollRectToVisible(new Rectangle(0, Math.max(0, y - 120), 1, 240));
        }
        lastRow = row;
        paper.repaint();
    }

    private void saveImage() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Uložit papír jako obrázek");
        fc.setFileFilter(new FileNameExtensionFilter("PNG obrázek", "png"));
        fc.setSelectedFile(new File("bt100.png"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".png")) {
            f = new File(f.getPath() + ".png");
        }
        try {
            ImageIO.write(bt.render(SAVE_SCALE, 1), "png", f);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Obrázek nelze uložit: " + ex.getMessage(),
                    "Tesla BT100", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** papir: kreslí se jen viditelna cast */
    private class Paper extends JPanel {

        Paper() {
            setBackground(new Color(90, 90, 90));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            Rectangle clip = g.getClipBounds();
            int rows = Math.max(bt.getPaperRows(), MIN_ROWS);
            g.setColor(new Color(250, 248, 238));
            g.fillRect(MARGIN, MARGIN, BT100.WIDTH * SCALE, rows * SCALE);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int r0 = Math.max(0, (clip.y - MARGIN) / SCALE - 1);
            int r1 = Math.min(rows, (clip.y + clip.height - MARGIN) / SCALE + 1);
            g.setColor(new Color(40, 40, 60));
            int dd = SCALE + SCALE / 2;
            for (int r = r0; r < r1; r++) {
                for (int c = 0; c < BT100.WIDTH; c++) {
                    if (bt.dot(r, c)) {
                        if (SCALE < 2) {
                            g.fillRect(MARGIN + c * SCALE, MARGIN + r * SCALE, SCALE, SCALE);
                        } else {
                            g.fillOval(MARGIN + c * SCALE, MARGIN + r * SCALE, dd, dd);
                        }
                    }
                }
            }
            // hlava: poloha vozíku na radku pod jehlou
            int hx = MARGIN + (int) Math.round(bt.getCarriage() * SCALE);
            int hy = MARGIN + bt.getRow() * SCALE;
            g.setColor(bt.isNeedleDown() ? new Color(200, 0, 0) : new Color(220, 80, 80, 170));
            g.setStroke(new BasicStroke(1.5f));
            g.drawRect(hx - 3, hy - 6, 7, 14);
            g.setColor(new Color(255, 0, 0, 50));       // radek pod jehlou
            g.drawLine(MARGIN, hy + SCALE + 1, MARGIN + BT100.WIDTH * SCALE, hy + SCALE + 1);
        }
    }
}
