package com.bitmechanic.jocko;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Jan 9, 2010
 */
public class ImageInfo {

    // pixels
    private int height;
    private int width;

    // example:  image/gif
    private String mimeType;

    public boolean isPortrait() {
        return height >= width;
    }

    public boolean isLandscape() {
        return width >= height;
    }

    public boolean isSquared() {
        return (height == width);
    }

    public int getTotalPixels() {
        return height * width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public String toString() {
        return "ImageInfo{" +
                "height=" + height +
                ", width=" + width +
                ", mimeType='" + mimeType + '\'' +
                '}';
    }
}
