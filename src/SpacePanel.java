import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SpacePanel extends JPanel implements Runnable {
    private static final int PWIDTH = 500;
    private static final int PHEIGHT = 400;
    private static long MAX_STATS_INTERVAL = 1000000000L;
    private static final int NO_DELAYS_PER_YIELD = 16;
    private static int MAX_FRAME_SKIPS = 5;
    private static int NUM_FPS = 10;

    // used for gathering statistics
    private long statsInterval = 0L;    // in ns
    private long prevStatsTime;
    private long totalElapsedTime = 0L;
    private long gameStartTime;
    private int timeSpentInGame = 0;    // in seconds

    private long frameCount = 0;
    private double fpsStore[];
    private long statsCount = 0;
    private double averageFPS = 0.0;

    private long framesSkipped = 0L;
    private long totalFramesSkipped = 0L;
    private double upsStore[];
    private double averageUPS = 0.0;


    private DecimalFormat df = new DecimalFormat("0.##");  // 2 dp
    private DecimalFormat timedf = new DecimalFormat("0.####");  // 4 dp


    private Thread animator;           // the thread that performs the animation
    private boolean running = false;   // used to stop the animation thread
    private boolean isPaused = false;

    private long period;                // period between drawing in _nanosecs_


    private SpaceChase wcTop;
    private List<SpaceShip> spaceShipList = new ArrayList<SpaceShip>();

    // used at game termination
    private boolean gameOver = false;
    private Font font;

    // off screen rendering
    private Graphics2D dbg;
    private Image dbImage = null;

    private int xClick, yClick;
    private int xSelectionRectangle, ySelectionRectangle;

    public SpacePanel(SpaceChase wc, long period) {
        wcTop = wc;

        spaceShipList.add(new SpaceShip(10, 10, 0));
        spaceShipList.add(new SpaceShip(100, 100, 1));
        this.period = period;

        setBackground(Color.white);
        setPreferredSize(new Dimension(PWIDTH, PHEIGHT));

        setFocusable(true);
        requestFocus();    // the JPanel now has focus, so receives key events
        readyForTermination();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                xClick = e.getX();
                yClick = e.getY();
                if (e.getButton() == MouseEvent.BUTTON1) {
                    System.out.println("Mouse:Left");
                    mouseLeftClick(xClick, yClick);
                } else if (e.getButton() == MouseEvent.BUTTON2) {
                    System.out.println("Mouse:Middle");
                    mouseMiddleClick(xClick, yClick);
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    System.out.println("Mouse:Right");
                    mouseRightClick(xClick, yClick);
                } else if (e.getButton() == MouseEvent.MOUSE_RELEASED) {
                    System.out.println("Mouse:Released");
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                xSelectionRectangle = e.getX();
                ySelectionRectangle = e.getY();
                System.out.println(xClick + ", " + yClick + ", " + e.getX() + ", " + e.getY());
            }
        });

        // set up message font
        font = new Font("SansSerif", Font.BOLD, 12);

        // initialise timing elements
        fpsStore = new double[NUM_FPS];
        upsStore = new double[NUM_FPS];
        for (int i = 0; i < NUM_FPS; i++) {
            fpsStore[i] = 0.0;
            upsStore[i] = 0.0;
        }
    }  // end of SpacePanel()

    private void mouseRightClick(final int x, final int y) {
        for (SpaceShip spaceShip : spaceShipList) {
            if (spaceShip.isSelected()) {
                spaceShip.setDesiredPosition(x, y);
            }
        }
    }

    private void mouseMiddleClick(final int x, final int y) {

    }

    private void mouseLeftClick(int x, int y) {
        for (SpaceShip spaceShip : spaceShipList) {
            spaceShip.clickedOn(x, y);
        }
    }

    private void readyForTermination() {
        addKeyListener(new KeyAdapter() {
            // listen for esc, q, end, ctrl-c on the canvas to
            // allow a convenient exit from the full screen configuration
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if ((keyCode == KeyEvent.VK_ESCAPE) || (keyCode == KeyEvent.VK_Q) ||
                        (keyCode == KeyEvent.VK_END) ||
                        ((keyCode == KeyEvent.VK_C) && e.isControlDown())) {
                    running = false;
                }
            }
        });
    }  // end of readyForTermination()


    public void addNotify()
    // wait for the JPanel to be added to the JFrame before starting
    {
        super.addNotify();   // creates the peer
        startGame();         // start the thread
    }


    private void startGame()
    // initialise and start the thread
    {
        if (animator == null || !running) {
            animator = new Thread(this);
            animator.start();
        }
    } // end of startGame()


    // called by the JFrame's window listener methods


    public void resumeGame()
    // called when the JFrame is activated / deiconified
    {
        isPaused = false;
    }


    public void pauseGame()
    // called when the JFrame is deactivated / iconified
    {
        isPaused = true;
    }

    public void stopGame()
    // called when the JFrame is closing
    {
        running = false;
    }


    // ----------------------------------------------


    public void run()
        /* The frames of the animation are drawn inside the while loop. */ {
        long beforeTime, afterTime, timeDiff, sleepTime;
        long overSleepTime = 0L;
        int noDelays = 0;
        long excess = 0L;

        gameStartTime = System.nanoTime();
        prevStatsTime = gameStartTime;
        beforeTime = gameStartTime;

        running = true;

        while (running) {
            gameUpdate();
            gameRender();
            paintScreen();

            afterTime = System.nanoTime();
            timeDiff = afterTime - beforeTime;
            sleepTime = (period - timeDiff) - overSleepTime;

            if (sleepTime > 0) {   // some time left in this cycle
                try {
                    Thread.sleep(sleepTime / 1000000L);  // nano -> ms
                } catch (InterruptedException ex) {
                    System.out.println("Thread sleep interrupted.");
                }
                overSleepTime = (System.nanoTime() - afterTime) - sleepTime;
            } else {    // sleepTime <= 0; the frame took longer than the period
                excess -= sleepTime;  // store excess time value
                overSleepTime = 0L;

                if (++noDelays >= NO_DELAYS_PER_YIELD) {
                    Thread.yield();   // give another thread a chance to run
                    noDelays = 0;
                }
            }

            beforeTime = System.nanoTime();

            /* If frame animation is taking too long, update the game state
         without rendering it, to get the updates/sec nearer to
         the required FPS. */
            int skips = 0;
            while ((excess > period) && (skips < MAX_FRAME_SKIPS)) {
                excess -= period;
                gameUpdate();    // update state but don't render
                skips++;
            }
            framesSkipped += skips;

            storeStats();
        }

        printStats();
        System.exit(0);
    }


    private void gameUpdate() {
        if (!isPaused && !gameOver) {
            for (SpaceShip spaceShip : spaceShipList) {
                spaceShip.move();
            }
        }
    }


    private void gameRender() {
        if (dbImage == null) {
            dbImage = createImage(PWIDTH, PHEIGHT);
            if (dbImage == null) {
                System.out.println("dbImage is null");
                return;
            } else
                dbg = (Graphics2D) dbImage.getGraphics();
        }

        dbg.setColor(Color.white);
        dbg.fillRect(0, 0, PWIDTH, PHEIGHT);

        dbg.setColor(Color.blue);
        dbg.setFont(font);

        dbg.drawString("Average FPS/UPS: " + df.format(averageFPS) + ", " + df.format(averageUPS), 20, 25);

        if(xSelectionRectangle != 0 || ySelectionRectangle != 0) {
            dbg.drawRect(xClick, yClick, xSelectionRectangle - xClick, ySelectionRectangle - yClick);
        }

        dbg.setColor(Color.black);

        for (SpaceShip spaceShip : spaceShipList) {
            spaceShip.draw(dbg);
        }
    }


    private void paintScreen() {
        Graphics g;
        try {
            g = this.getGraphics();
            if ((g != null) && (dbImage != null)) {
                g.drawImage(dbImage, 0, 0, null);
                g.dispose();
            }
        } catch (Exception e) {
            System.out.println("Graphics context error: " + e);
        }
    }


    private void storeStats()
        /* The statistics:
             - the summed periods for all the iterations in this interval
               (period is the amount of time a single frame iteration should take),
               the actual elapsed time in this interval,
               the error between these two numbers;

             - the total frame count, which is the total number of calls to run();

             - the frames skipped in this interval, the total number of frames
               skipped. A frame skip is a game update without a corresponding render;

             - the FPS (frames/sec) and UPS (updates/sec) for this interval,
               the average FPS & UPS over the last NUM_FPSs intervals.

           The data is collected every MAX_STATS_INTERVAL  (1 sec).
        */ {
        frameCount++;
        statsInterval += period;

        if (statsInterval >= MAX_STATS_INTERVAL) {     // record stats every MAX_STATS_INTERVAL
            long timeNow = System.nanoTime();
            timeSpentInGame = (int) ((timeNow - gameStartTime) / 1000000000L);  // ns --> secs
            wcTop.setTimeSpent(timeSpentInGame);

            long realElapsedTime = timeNow - prevStatsTime;   // time since last stats collection
            totalElapsedTime += realElapsedTime;

            double timingError =
                    ((double) (realElapsedTime - statsInterval) / statsInterval) * 100.0;

            totalFramesSkipped += framesSkipped;

            double actualFPS = 0;     // calculate the latest FPS and UPS
            double actualUPS = 0;
            if (totalElapsedTime > 0) {
                actualFPS = (((double) frameCount / totalElapsedTime) * 1000000000L);
                actualUPS = (((double) (frameCount + totalFramesSkipped) / totalElapsedTime)
                        * 1000000000L);
            }

            // store the latest FPS and UPS
            fpsStore[(int) statsCount % NUM_FPS] = actualFPS;
            upsStore[(int) statsCount % NUM_FPS] = actualUPS;
            statsCount = statsCount + 1;

            double totalFPS = 0.0;     // total the stored FPSs and UPSs
            double totalUPS = 0.0;
            for (int i = 0; i < NUM_FPS; i++) {
                totalFPS += fpsStore[i];
                totalUPS += upsStore[i];
            }

            if (statsCount < NUM_FPS) { // obtain the average FPS and UPS
                averageFPS = totalFPS / statsCount;
                averageUPS = totalUPS / statsCount;
            } else {
                averageFPS = totalFPS / NUM_FPS;
                averageUPS = totalUPS / NUM_FPS;
            }

            framesSkipped = 0;
            prevStatsTime = timeNow;
            statsInterval = 0L;   // reset
        }
    }  // end of storeStats()


    private void printStats() {
        System.out.println("Frame Count/Loss: " + frameCount + " / " + totalFramesSkipped);
        System.out.println("Average FPS: " + df.format(averageFPS));
        System.out.println("Average UPS: " + df.format(averageUPS));
        System.out.println(timeSpentInGame + "s");
    }  // end of printStats()

}  // end of SpacePanel class
