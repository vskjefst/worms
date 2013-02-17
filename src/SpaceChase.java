import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;


public class SpaceChase extends JFrame implements WindowListener {
    private static int DEFAULT_FPS = 80;

    private SpacePanel wp;        // where the worm is drawn
    private JTextField jtfTime;  // displays time spent in game


    public SpaceChase(long period) {
        super("The Space Chase");
        makeGUI(period);

        addWindowListener(this);
        pack();
        setResizable(false);
        setVisible(true);
    }  // end of SpaceChase() constructor


    private void makeGUI(long period) {
        Container c = getContentPane();    // default BorderLayout used

        wp = new SpacePanel(this, period);
        c.add(wp, "Center");

        JPanel ctrls = new JPanel();   // a row of textfields
        ctrls.setLayout(new BoxLayout(ctrls, BoxLayout.X_AXIS));


        jtfTime = new JTextField();
        jtfTime.setEditable(false);
        ctrls.add(jtfTime);

        c.add(ctrls, "South");
    }  // end of makeGUI()


    public void setTimeSpent(long t) {
        jtfTime.setText(t + "s");
    }


    // ----------------- window listener methods -------------

    public void windowActivated(WindowEvent e) {
        wp.resumeGame();
    }

    public void windowDeactivated(WindowEvent e) {
        wp.pauseGame();
    }


    public void windowDeiconified(WindowEvent e) {
        wp.resumeGame();
    }

    public void windowIconified(WindowEvent e) {
        wp.pauseGame();
    }


    public void windowClosing(WindowEvent e) {
        wp.stopGame();
    }


    public void windowClosed(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    // ----------------------------------------------------

    public static void main(String args[]) {
        int fps = DEFAULT_FPS;
        if (args.length != 0)
            fps = Integer.parseInt(args[0]);

        long period = (long) 1000.0 / fps;
        System.out.println("fps: " + fps + "; period: " + period + " ms");

        new SpaceChase(period * 1000000L);    // ms --> nanosecs
    }

} // end of SpaceChase class


