package com.bitmechanic.jocko;

import java.io.Serializable;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: May 8, 2010
 */
public class BoundingBox implements Serializable {

    private static final long serialVersionUID = 1L;

    private int topLeftX, topLeftY;
    private int bottomRightX, bottomRightY;

    public BoundingBox() {
        // used by gson during de-serialization... not useful for mortal classes
    }

    public BoundingBox(int topLeftX, int topLeftY, int bottomRightX, int bottomRightY) {
        this.topLeftX = topLeftX;
        this.topLeftY = topLeftY;
        this.bottomRightX = bottomRightX;
        this.bottomRightY = bottomRightY;
    }

    public int getBottomRightX() {
        return bottomRightX;
    }

    public int getBottomRightY() {
        return bottomRightY;
    }

    public int getTopLeftX() {
        return topLeftX;
    }

    public int getTopLeftY() {
        return topLeftY;
    }

    public int getWidth() {
        return bottomRightX-topLeftX;
    }

    public int getHeight() {
        return bottomRightY-topLeftY;
    }

    public boolean isValid() {
        return topLeftX >= 0 && topLeftY >= 0 && bottomRightX >= 0 && bottomRightY >= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoundingBox)) return false;

        BoundingBox that = (BoundingBox) o;

        if (bottomRightX != that.bottomRightX) return false;
        if (bottomRightY != that.bottomRightY) return false;
        if (topLeftX != that.topLeftX) return false;
        if (topLeftY != that.topLeftY) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = topLeftX;
        result = 31 * result + topLeftY;
        result = 31 * result + bottomRightX;
        result = 31 * result + bottomRightY;
        return result;
    }

    @Override
    public String toString() {
        return "BoundingBox{" +
                "bottomRightX=" + bottomRightX +
                ", topLeftX=" + topLeftX +
                ", topLeftY=" + topLeftY +
                ", bottomRightY=" + bottomRightY +
                '}';
    }

}
