import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SpacePanel extends JPanel implements Runnable {
    private static final int PWIDTH = 1024;
    private static final int PHEIGHT = 768;
    private static long MAX_STATS_INTERVAL = 1000000000L;
    private static final int NO_DELAYS_PER_YIELD = 16;
    private static int MAX_FRAME_SKIPS = 5;
    private static int NUM_FPS = 10;

    private long statsInterval = 0L;
    private long prevStatsTime;
    private long totalElapsedTime = 0L;
    private long gameStartTime;
    private int timeSpentInGame = 0;

    private long frameCount = 0;
    private double fpsStore[];
    private long statsCount = 0;
    private double averageFPS = 0.0;

    private long framesSkipped = 0L;
    private long totalFramesSkipped = 0L;
    private double upsStore[];
    private double averageUPS = 0.0;

    private DecimalFormat df = new DecimalFormat("0.##");

    private Thread animator;
    private boolean running = false;
    private boolean isPaused = false;
    private long periodBetweenDrawing;

    private SpaceChase wcTop;
    private List<SpaceShip> spaceShipList = new ArrayList<SpaceShip>();

    private boolean gameOver = false;
    private Font font;

    private Graphics2D dbg;
    private Image dbImage = null;

    private double xClick, yClick;
    private double xSelectionRectangle, ySelectionRectangle;
    private Rectangle selectionRectangle = new Rectangle();
    private int numberOfShips = 45;
    private boolean leftMouseButtonPressed;

    public SpacePanel(SpaceChase wc, long periodBetweenDrawing) {
        wcTop = wc;

        Random random = new Random();
        for (int i = 0; i < numberOfShips; i++) {
            spaceShipList.add(new SpaceShip(random.nextInt(PWIDTH - 10), random.nextInt(PHEIGHT - 10), i));
        }

        this.periodBetweenDrawing = periodBetweenDrawing;

        setBackground(Color.white);
        setPreferredSize(new Dimension(PWIDTH, PHEIGHT));

        setFocusable(true);
        requestFocus();
        readyForTermination();

        addMouseListener();
        addMouseMotionListener();
        addMouseWheelListener();

        font = new Font("SansSerif", Font.BOLD, 12);

        fpsStore = new double[NUM_FPS];
        upsStore = new double[NUM_FPS];
        for (int i = 0; i < NUM_FPS; i++) {
            fpsStore[i] = 0.0;
            upsStore[i] = 0.0;
        }
    }

    private void addMouseWheelListener() {
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double scale = 1 - ((float) e.getUnitsToScroll() / 100);
                dbg.scale(scale, scale);
                System.out.println(scale);
            }
        });
    }

    private void addMouseMotionListener() {
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (leftMouseButtonPressed) {
                    xSelectionRectangle = scaleX(e.getX());
                    ySelectionRectangle = scaleY(e.getY());
                }
            }
        });
    }

    private void addMouseListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    mouseLeftClick(xClick, yClick);
                } else if (e.getButton() == MouseEvent.BUTTON2) {
                    mouseMiddleClick(xClick, yClick);
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    mouseRightClick(xClick, yClick);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                xClick = scaleX(e.getX());
                yClick = scaleY(e.getY());
                System.out.println("CLICK: " + xClick + ", " + yClick);
                if (e.getButton() == MouseEvent.BUTTON1) {
                    leftMouseButtonPressed = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    leftMouseButtonPressed = false;
                    for (SpaceShip spaceShip : spaceShipList) {
                        spaceShip.setSelected(false);
                        spaceShip.insideSelectionRectangle(selectionRectangle);
                    }
                    selectionRectangle = new Rectangle();
                    xSelectionRectangle = ySelectionRectangle = 0;
                }
            }
        });
    }

    private double scaleX(int x) {
        return x / dbg.getTransform().getScaleX();
    }

    private double scaleY(int y) {
        return y / dbg.getTransform().getScaleY();
    }

    private void mouseRightClick(final double x, final double y) {
        for (SpaceShip spaceShip : spaceShipList) {
            if (spaceShip.isSelected()) {
                spaceShip.setDesiredPosition(x, y);
            }
        }
    }

    private void mouseMiddleClick(final double x, final double y) {

    }

    private void mouseLeftClick(double x, double y) {
        for (SpaceShip spaceShip : spaceShipList) {
            spaceShip.setSelected(false);
        }
        for (int i = spaceShipList.size() - 1; i >= 0; i--) {
            if (spaceShipList.get(i).clickedOn(x, y)) {
                break;
            }
        }
    }

    private void readyForTermination() {
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if ((keyCode == KeyEvent.VK_ESCAPE) || (keyCode == KeyEvent.VK_Q) ||
                        (keyCode == KeyEvent.VK_END) ||
                        ((keyCode == KeyEvent.VK_C) && e.isControlDown())) {
                    running = false;
                }
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        startGame();
    }

    private void startGame() {
        if (animator == null || !running) {
            animator = new Thread(this);
            animator.start();
        }
    }

    public void resumeGame() {
        isPaused = false;
    }

    public void pauseGame() {
        isPaused = true;
    }

    public void stopGame() {
        running = false;
    }

    public void run() {
        long beforeTime, afterTime, timeDiff, sleepTime;
        long overSleepTime = 0L;
        int noDelays = 0;
        long excess = 0L;
        int skips = 0;

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
            sleepTime = (periodBetweenDrawing - timeDiff) - overSleepTime;

            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime / 1000000L);
                } catch (InterruptedException ex) {
                    System.out.println("Thread sleep interrupted.");
                }
                overSleepTime = (System.nanoTime() - afterTime) - sleepTime;
            } else {
                excess -= sleepTime;
                overSleepTime = 0L;

                if (++noDelays >= NO_DELAYS_PER_YIELD) {
                    Thread.yield();
                    noDelays = 0;
                }
            }

            beforeTime = System.nanoTime();

            skips = 0;
            while ((excess > periodBetweenDrawing) && (skips < MAX_FRAME_SKIPS)) {
                excess -= periodBetweenDrawing;
                gameUpdate();
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

        if (xSelectionRectangle != 0 || ySelectionRectangle != 0) {
            if (xSelectionRectangle > xClick && ySelectionRectangle > yClick) {
                selectionRectangle = new Rectangle((int) xClick, (int) yClick, (int) (xSelectionRectangle - xClick), (int) (ySelectionRectangle - yClick));
            } else if (xSelectionRectangle < xClick && ySelectionRectangle > yClick) {
                selectionRectangle = new Rectangle((int) xSelectionRectangle, (int) yClick, (int) (xClick - xSelectionRectangle), (int) (ySelectionRectangle - yClick));
            } else if (xSelectionRectangle < xClick && ySelectionRectangle < yClick) {
                selectionRectangle = new Rectangle((int) xSelectionRectangle, (int) ySelectionRectangle, (int) (xClick - xSelectionRectangle), (int) (yClick - ySelectionRectangle));
            } else {
                selectionRectangle = new Rectangle((int) xClick, (int) ySelectionRectangle, (int) (xSelectionRectangle - xClick), (int) (yClick - ySelectionRectangle));
            }

            dbg.drawRect(selectionRectangle.x, selectionRectangle.y, selectionRectangle.width, selectionRectangle.height);
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

    private void storeStats() {
        frameCount++;
        statsInterval += periodBetweenDrawing;

        if (statsInterval >= MAX_STATS_INTERVAL) {
            long timeNow = System.nanoTime();
            timeSpentInGame = (int) ((timeNow - gameStartTime) / 1000000000L);
            wcTop.setTimeSpent(timeSpentInGame);

            long realElapsedTime = timeNow - prevStatsTime;
            totalElapsedTime += realElapsedTime;

            totalFramesSkipped += framesSkipped;

            double actualFPS = 0;
            double actualUPS = 0;
            if (totalElapsedTime > 0) {
                actualFPS = (((double) frameCount / totalElapsedTime) * 1000000000L);
                actualUPS = (((double) (frameCount + totalFramesSkipped) / totalElapsedTime)
                        * 1000000000L);
            }

            fpsStore[(int) statsCount % NUM_FPS] = actualFPS;
            upsStore[(int) statsCount % NUM_FPS] = actualUPS;
            statsCount = statsCount + 1;

            double totalFPS = 0.0;
            double totalUPS = 0.0;
            for (int i = 0; i < NUM_FPS; i++) {
                totalFPS += fpsStore[i];
                totalUPS += upsStore[i];
            }

            if (statsCount < NUM_FPS) {
                averageFPS = totalFPS / statsCount;
                averageUPS = totalUPS / statsCount;
            } else {
                averageFPS = totalFPS / NUM_FPS;
                averageUPS = totalUPS / NUM_FPS;
            }

            framesSkipped = 0;
            prevStatsTime = timeNow;
            statsInterval = 0L;
        }
    }


    private void printStats() {
        System.out.println("Frame Count/Loss: " + frameCount + " / " + totalFramesSkipped);
        System.out.println("Average FPS: " + df.format(averageFPS));
        System.out.println("Average UPS: " + df.format(averageUPS));
        System.out.println(timeSpentInGame + "s");
    }

}
