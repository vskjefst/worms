import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class SpaceShip {

    private int xPosition, yPosition, xDesiredPosition, yDesiredPosition, xNewDesiredPosition, yNewDesiredPosition;
    private int xBaseSize = 10, yBaseSize = 10;
    private int scale = 1;
    private String debug = "";
    private boolean selected = false;
    private int index;
    private List<Point2D> currentPath = new ArrayList<Point2D>();

    public SpaceShip(int xPos, int yPos, int index) {
        this.xPosition = this.xDesiredPosition = this.xNewDesiredPosition = xPos;
        this.yPosition = this.yDesiredPosition = this.yNewDesiredPosition = yPos;
        this.index = index;
    }

    public void draw(Graphics g) {
        if(selected) {
            g.setColor(Color.RED);
        } else {
            g.setColor(Color.BLACK);
        }
        g.fillRect(xPosition, yPosition, xBaseSize * scale, yBaseSize * scale);
        g.drawString(debug, 20, 40 + (15 * index));
    }

    private void setPosition(final Point2D position) {
        this.xPosition = (int) position.getX();
        this.yPosition = (int) position.getY();
    }

    public void setDesiredPosition(int xDesiredPosition, int yDesiredPosition) {
        this.xNewDesiredPosition = xDesiredPosition;
        this.yNewDesiredPosition = yDesiredPosition;
    }

    public void move() {
        if((xDesiredPosition != xNewDesiredPosition || yDesiredPosition != yNewDesiredPosition)) {
            xDesiredPosition = xNewDesiredPosition;
            yDesiredPosition = yNewDesiredPosition;
            currentPath = calculatePath(xPosition, yPosition, xDesiredPosition, yDesiredPosition);
        } else if(!currentPath.isEmpty()) {
            Point2D point2D = currentPath.get(0);
            currentPath.remove(point2D);
            setPosition(point2D);
        }

        debug = "(" + index + ") current position: [" + xPosition + "," + yPosition + "]";
    }

    public void clickedOn(final int x, final int y) {
        if((x >= xPosition && x <= (xPosition + (xBaseSize * scale)))
                && (y >= yPosition && y <= (yPosition + (yBaseSize * scale)))) {
            setSelected(true);
        } else {
            setSelected(false);
        }
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public List<Point2D> calculatePath(int x, int y, int x2, int y2) {
        List<Point2D> point2DList = new ArrayList<Point2D>();

        int w = x2 - x;
        int h = y2 - y;
        int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0;
        if (w < 0) dx1 = -1;
        else if (w > 0) dx1 = 1;
        if (h < 0) dy1 = -1;
        else if (h > 0) dy1 = 1;
        if (w < 0) dx2 = -1;
        else if (w > 0) dx2 = 1;
        int longest = Math.abs(w);
        int shortest = Math.abs(h);
        if (!(longest > shortest)) {
            longest = Math.abs(h);
            shortest = Math.abs(w);
            if (h < 0) dy2 = -1;
            else if (h > 0) dy2 = 1;
            dx2 = 0;
        }
        int numerator = longest >> 1;
        for (int i = 0; i <= longest; i++) {
            point2DList.add(new Point(x, y));
            numerator += shortest;
            if (!(numerator < longest)) {
                numerator -= longest;
                x += dx1;
                y += dy1;
            } else {
                x += dx2;
                y += dy2;
            }
        }

        return point2DList;
    }

    public void insideSelectionRectangle(Rectangle selectionRectangle) {
        if(selectionRectangle.contains(xPosition, yPosition, xBaseSize * scale, yBaseSize * scale)) {
            setSelected(true);
        }
    }
}
