package com.bitmechanic.jocko;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Jan 9, 2010
 */
public interface ImageService {

    /**
     * Determines information about the given image
     *
     * @param imageFilename
     * @return ImageInfo object for this image
     * @throws IOException If imageFilename cannot be found, or does not map to a valid image
     */
    public ImageInfo getImageInfo(String imageFilename) throws IOException;

    /**
     * Resizes an image to a max height/width bounding box.  Maintains the
     * original image's aspect ratio.
     * <p/>
     * Example:
     * <p/>
     * Original image is: 700x900 (aspect ratio: 0.778)
     * Method is called: resizeImage(origFile, newFile, 500, 500);
     * newFile should have dimensions: 389x500
     * newFile should not be cropped, just resized
     *
     * @param originalFilename Filename of original image file to resize
     * @param resizedFilename  Filename to store resized file into
     * @param maxWidth         Max width of resized image (pixels)
     * @param maxHeight        Max height of resized image (pixels)
     */
    public void resizeImage(String originalFilename, String resizedFilename, int maxWidth, int maxHeight) throws IOException;

    /**
     * Crops and resizes an image to an exact height/width.  Center crops the
     * image before resizing it.
     * <p/>
     * Example:
     * <p/>
     * Original image is: 700x900 (aspect ratio: 0.778)
     * Desired width/height: 200x300 (aspect ratio: 0.667)
     * Resized image:
     * - First would be cropped to: 600x900 (50px would be removed from right and left sides of image)
     * - Then would be resized to: 200x300
     * - resizedFilename would be a file that is 200x300
     * <p/>
     * If the originalFilename is an image with the wrong orientation, then
     * simply call resizeImage with the same width/height.
     * <p/>
     * Example:
     * <p/>
     * Original image is: 900x700 (aspect ratio: 1.28 - landscape)
     * Desired width/height: 200x300 (aspect ratio: 0.667 - portrait)
     * Resized image: 200x156
     */
    public void cropAndResizeImage(String originalFilename, String resizedFilename, int width, int height) throws IOException;

    public void trimImage(File image, int trimTop, int trimRight, int trimBottom, int trimLeft) throws IOException;

    public ImageDiff diffImages(File origImage, File newImage) throws IOException;

    public ImageDiff diffImages(File origImage, File newImage, List<BoundingBox> boundingBoxesToIgnore) throws IOException;

    public void convertFormat(String originalFilename, String newFilename) throws IOException;
}
