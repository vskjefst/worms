import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;


public class SpaceChase extends JFrame implements WindowListener {
    private static int DEFAULT_FPS = 80;

    private SpacePanel wp;
    private JTextField jtfTime;

    public SpaceChase(long period) {
        super("The Space Chase");
        makeGUI(period);

        addWindowListener(this);
        pack();
        setResizable(false);
        setVisible(true);
    }

    private void makeGUI(long period) {
        Container c = getContentPane();

        wp = new SpacePanel(this, period);
        c.add(wp, "Center");

        JPanel ctrls = new JPanel();
        ctrls.setLayout(new BoxLayout(ctrls, BoxLayout.X_AXIS));

        jtfTime = new JTextField();
        jtfTime.setEditable(false);
        ctrls.add(jtfTime);

        c.add(ctrls, "South");
    }

    public void setTimeSpent(long t) {
        jtfTime.setText(t + "s");
    }

    @Override
    public void windowActivated(WindowEvent e) {
        wp.resumeGame();
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        wp.pauseGame();
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        wp.resumeGame();
    }

    @Override
    public void windowIconified(WindowEvent e) {
        wp.pauseGame();
    }

    @Override
    public void windowClosing(WindowEvent e) {
        wp.stopGame();
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    public static void main(String args[]) {
        int fps = DEFAULT_FPS;
        if (args.length != 0)
            fps = Integer.parseInt(args[0]);

        long period = (long) 1000.0 / fps;
        System.out.println("fps: " + fps + "; period: " + period + " ms");

        new SpaceChase(period * 1000000L);
    }
}


