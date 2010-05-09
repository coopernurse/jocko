package com.bitmechanic.jocko.local;

import com.bitmechanic.jocko.BoundingBox;
import com.bitmechanic.jocko.ImageDiff;
import com.bitmechanic.jocko.ImageInfo;
import com.bitmechanic.jocko.ImageService;
import com.bitmechanic.util.Contract;
import com.bitmechanic.util.ExecResult;
import com.bitmechanic.util.ExecUtil;
import com.bitmechanic.util.IOUtil;
import com.bitmechanic.util.StringUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Jan 9, 2010
 */
public class ImageServiceImageMagickImpl implements ImageService {

    private static Log log = LogFactory.getLog(ImageServiceImageMagickImpl.class);

    private static final Map<String, String> validMimeTypes = new HashMap<String, String>();

    static {
        validMimeTypes.put("GIF", "image/gif");
        validMimeTypes.put("JPG", "image/jpeg");
        validMimeTypes.put("JPEG", "image/jpeg");
        validMimeTypes.put("BMP", "image/bmp");
        validMimeTypes.put("PNG", "image/png");
    }

    // path to the ImageMagick 'composite' binary
    private String compositeBinary;

    // path to the ImageMagick 'convert' binary
    private String convertBinary;

    // path to the ImageMagick 'identify' binary
    private String identifyBinary;

    // path to the ImageMagick 'compare' binary
    private String compareBinary;

    // path to perceptualdiff binary - see: http://pdiff.sourceforge.net/
    private String pdiffBinary;

    public void setCompositeBinary(String compositeBinary) {
        this.compositeBinary = compositeBinary;
    }

    public String getCompositeBinary() {
        return compositeBinary;
    }

    public String getConvertBinary() {
        return convertBinary;
    }

    public void setConvertBinary(String convertBinary) {
        this.convertBinary = convertBinary;
    }

    public String getIdentifyBinary() {
        return identifyBinary;
    }

    public void setIdentifyBinary(String identifyBinary) {
        this.identifyBinary = identifyBinary;
    }

    public String getCompareBinary() {
        return compareBinary;
    }

    public void setCompareBinary(String compareBinary) {
        this.compareBinary = compareBinary;
    }

    public String getPdiffBinary() {
        return pdiffBinary;
    }

    public void setPdiffBinary(String pdiffBinary) {
        this.pdiffBinary = pdiffBinary;
    }

    public ImageInfo getImageInfo(String imageFilename) throws IOException {
        Contract.notNullOrEmpty(identifyBinary, "identifyBinary cannot be empty");
        Contract.notNullOrEmpty(imageFilename, "imageFilename cannot be empty");

        File imageFile = new File(imageFilename);
        Contract.ensure(imageFile.canRead(), "cannot read file: " + imageFilename);

        if (!imageFile.exists())
            throw new IOException("The specified image does not exist: " + imageFilename);

        ProcessBuilder pb = new ProcessBuilder(identifyBinary, imageFilename);

        Process process = pb.start();

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            // try to read the output anyway.
        }

        Scanner errorScanner = null;

        try {

            errorScanner = new Scanner(process.getErrorStream());

            try {
                String line = errorScanner.nextLine();
                throw new IOException("Cannot get file info: " + line);
            } catch (NoSuchElementException nsee) {
                // This is good, no errors on the error stream
            }

        } finally {
            if (errorScanner != null) {
                errorScanner.close();
            }
        }

        // proceed with parsing
        Scanner scanner = null;

        try {

            scanner = new Scanner(process.getInputStream());

            Pattern pattern = Pattern.compile("\\s[A-Z]+\\s[0-9]+x[0-9]+\\s");

            String extractedInfo = scanner.findInLine(pattern);

            if (extractedInfo == null)
                throw new IllegalStateException("Cannot get file info from line: " + scanner.nextLine());

            // matching the pattern makes these lines safe to execute.
            ImageInfo imageInfo = new ImageInfo();

            String[] info = extractedInfo.trim().split("\\s");

            String mimeType = validMimeTypes.get(info[0]);

            if (mimeType == null) {
                throw new IOException("Invalid image type: " + info[0]);
            }

            imageInfo.setMimeType(mimeType);

            String[] size = info[1].split("x");

            imageInfo.setWidth(Integer.parseInt(size[0]));
            imageInfo.setHeight(Integer.parseInt(size[1]));

            return imageInfo;

        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }

    }

    public void resizeImage(String originalFilename, String resizedFilename,
                            int maxWidth, int maxHeight) throws IOException {

        File imageFile = new File(originalFilename);

        if (!imageFile.exists())
            throw new IOException("The specified image does not exist: " + originalFilename);

        ProcessBuilder pb = new ProcessBuilder(convertBinary, originalFilename, "-resize", maxWidth + "x" + maxHeight, resizedFilename);

        Process process = pb.start();

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            // try to read the output anyway.
        }

        Scanner scanner = null;

        try {

            scanner = new Scanner(process.getErrorStream());

            try {
                String line = scanner.nextLine();
                throw new IOException("Cannot resize image: " + line);
            } catch (NoSuchElementException nsee) {
                // This is good, no errors on the error stream.
            }

        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }

    }

    public void cropAndResizeImage(String originalFilename,
                                   String resizedFilename, int width, int height) throws IOException {

        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("width and height must be greater than zero");

        ImageInfo originalImageInfo = getImageInfo(originalFilename);

        // different orientation and not squared
        if (!originalImageInfo.isSquared() && (originalImageInfo.isPortrait() != (height >= width))) {
            resizeImage(originalFilename, resizedFilename, width, height);
            return;
        }

        int originalImageWidth = originalImageInfo.getWidth();

        int originalImageHeight = originalImageInfo.getHeight();

        float originalAspectRatio = (float) originalImageWidth / (float) originalImageHeight;

        float desiredAspectRatio = (float) width / (float) height;

        int shaveHeight = 0;

        int shaveWidth = 0;

        if (originalAspectRatio > desiredAspectRatio) {
            // shave width
            shaveWidth = Math.round((originalImageWidth - (originalImageHeight * desiredAspectRatio)) / 2);
        } else {
            // shave height
            shaveHeight = Math.round((originalImageHeight - (originalImageWidth / desiredAspectRatio)) / 2);
        }

        ProcessBuilder pb = new ProcessBuilder(convertBinary, originalFilename, "-shave", shaveWidth + "x" + shaveHeight, "+repage", resizedFilename);

        Process process = pb.start();

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            // try to read the output anyway.
        }

        Scanner scanner = null;

        try {

            scanner = new Scanner(process.getErrorStream());

            try {
                String line = scanner.nextLine();
                throw new IOException("Cannot crop image: " + line);
            } catch (NoSuchElementException nsee) {
                // This is good, no errors on the error stream.
            }

        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }

        resizeImage(resizedFilename, resizedFilename, width, height);

    }

    public ImageDiff diffImages(File origImage, File newImage) throws IOException {
        return diffImages(origImage, newImage, null);
    }

    public ImageDiff diffImages(File origImage, File newImage, List<BoundingBox> boundingBoxesToIgnore) throws IOException {
        Contract.notNull(origImage, "origImage cannot be null");
        Contract.notNull(newImage, "newImage cannot be null");
        Contract.ensure(origImage.canRead(), "Cannot read file " + origImage);
        Contract.ensure(newImage.canRead(), "Cannot read file " + newImage);
        Contract.notNullOrEmpty(compositeBinary, "compositeBinary cannot be empty");
        Contract.notNullOrEmpty(convertBinary, "convertBinary cannot be empty");
        Contract.notNullOrEmpty(compareBinary, "compareBinary cannot be empty");

        File origImageMasked = null;
        File newImageMasked  = null;

        try {

            // Get bytes for old screen and write to disk
            File diffFile = File.createTempFile("pdiff-tmp", ".png");
            File compositeFile = null;

            // mask out regions of image based on bounding boxes
            if (boundingBoxesToIgnore != null) {
                StringBuffer rectangles = new StringBuffer();
                for (BoundingBox box : boundingBoxesToIgnore) {
                    if (box.getHeight() > 0 && box.getWidth() > 0) {
                        ImageInfo info = getImageInfo(origImage.getAbsolutePath());

                        if (info.getHeight() < box.getHeight() || info.getWidth() < box.getWidth()) {
                            throw new IllegalStateException("box is larger than origImage... box=" + box + " image=" + info);
                        }

                        info = getImageInfo(newImage.getAbsolutePath());

                        if (info.getHeight() < box.getHeight() || info.getWidth() < box.getWidth()) {
                            throw new IllegalStateException("box is larger than newImage... box=" + box + " image=" + info);
                        }

                        String rectangle = String.format("-draw|rectangle %d,%d %d,%d|", box.getTopLeftX(), box.getTopLeftY(), box.getBottomRightX(), box.getBottomRightY());
                        rectangles.append(rectangle);
                    }
                }

                origImageMasked = File.createTempFile("diffImage", ".png");
                newImageMasked  = File.createTempFile("diffImage", ".png");

                String cmd = String.format("%s|%s|-fill|white|%s%s", convertBinary, origImage.getAbsolutePath(), rectangles.toString(), origImageMasked.getAbsolutePath());
                log.debug("Executing: " + cmd);
                ExecUtil.execFailFast(cmd, "\\|");
                origImage = origImageMasked;

                cmd = String.format("%s|%s|-fill|white|%s%s", convertBinary, newImage.getAbsolutePath(), rectangles.toString(), newImageMasked.getAbsolutePath());
                log.debug("Executing: " + cmd);
                ExecUtil.execFailFast(cmd, "\\|");
                newImage = newImageMasked;
            }

            return diffImages(origImage, newImage, diffFile, compositeFile);
        }
        finally {
            if (origImageMasked != null)
                origImageMasked.delete();
            if (newImageMasked != null)
                newImageMasked.delete();
        }
    }

    private ImageDiff diffImages(File origImage, File newImage, File diffFile, File compositeFile) throws IOException {
        // Compare baseline image with current screen shot
        String stdout = compareImages(origImage, newImage, diffFile);
        String message = null;

        if (stdout.toLowerCase().indexOf("image size differs") > -1) {
                log.debug("Images are different sizes.  Attempting to make them the same size.");
                String fileExt = getFileExtension(origImage.getAbsolutePath());
                File tmpImage = File.createTempFile("tmp", fileExt);
                IOUtil.copyFile(origImage, tmpImage);
                try {
                    resizeToMatchImage(tmpImage, newImage);
                    log.debug("Resize done.  Re-comparing images.");

                    // recurse
                    return diffImages(tmpImage, newImage, diffFile, compositeFile);
                }
                finally {
                    tmpImage.delete();
                }
        }
        else {
            boolean hasDifference = false;
            if (stdout.toLowerCase().indexOf("images too dissimilar") > -1) {
                log.debug("Images too dissimilar.");
                hasDifference = true;
                message = "Images too dissimilar";
                diffFile.delete();
                diffFile = null;
            }
            else {
                //
                // Parse the output from compare -- the first value should be a number
                //
                // Example:  5194.06 (0.0792562) @ 0,211
                //
                int firstSpace = stdout.indexOf(" ");
                String meanErrorAsString;
                if (firstSpace == -1)
                    firstSpace = stdout.indexOf("\n");
                if (firstSpace == -1)
                    throw new IOException("Unable to parse output from compare: " + stdout);
                else
                    meanErrorAsString = stdout.substring(0, firstSpace);
                
                double meanError = Double.parseDouble(meanErrorAsString);
                if (meanError < 20) {
                    // no diff found
                    diffFile.delete();
                    diffFile = null;
                    message = null;
                }
                else {
                    // For some reason, compare will sometimes create multiple diff files on Windows
                    // We haven't seen this on mac/linux yet.  We'll check for a zero byte diffFile,
                    // and then look for a file named [name]-0.[ext]  and move it if it exists
                    if (!diffFile.exists() || diffFile.length() == 0) {
                        log.debug("diff file does not exist");

                        String diffPath = diffFile.getAbsolutePath();
                        String fileExt  = getFileExtension(diffPath);
                        int pos = diffPath.lastIndexOf(fileExt);
                        if (pos > -1) {
                            diffPath = diffPath.substring(0, pos);
                        }
                        diffPath = diffPath + "-0" + fileExt;

                        diffFile.delete();
                        diffFile = new File(diffPath);
                        log.debug("looking for diff file in: " + diffPath);
                        if (diffFile.exists() && diffFile.length() > 0) {
                            log.debug("diff file found");
                        }
                        else {
                            log.debug("diff file not found");
                            diffFile = null;
                        }
                    }

                    hasDifference = true;
                    message = "Got difference with mean error: " + meanError;

                    // composite -blend 70x30 diff.png [baseline image] composite.png
                    // diff found - generate composite image
                    if (diffFile != null) {
                        compositeFile = File.createTempFile("compositeImage", ".png");
                        String cmd = String.format("%s|-blend|30x70|%s|%s|%s", compositeBinary, diffFile.getAbsolutePath(), origImage.getAbsolutePath(), compositeFile.getAbsolutePath());
                        log.debug("Executing: " + cmd);
                        ExecUtil.exec(cmd, "\\|");
                    }
                }
            }

            return new ImageDiff(diffFile, compositeFile, hasDifference, message);
        }
    }

    private void resizeToMatchImage(File imageToResize, File comparisonImage) throws IOException {
        ImageInfo image1Info = getImageInfo(imageToResize.getAbsolutePath());
        ImageInfo image2Info = getImageInfo(comparisonImage.getAbsolutePath());

        int heightDiff = Math.abs(image1Info.getHeight() - image2Info.getHeight());
        int widthDiff  = Math.abs(image1Info.getWidth() - image2Info.getWidth());

        int bottomTrim = 0;
        int rightTrim  = 0;
        int bottomPad  = 0;
        int rightPad   = 0;
        
        if (image1Info.getHeight() > image2Info.getHeight()) {
            bottomTrim = heightDiff;
        }
        else if (image1Info.getHeight() < image2Info.getHeight()) {
            bottomPad = heightDiff;
        }

        if (image1Info.getWidth() > image2Info.getWidth()) {
            rightTrim = widthDiff;
        }
        else if (image1Info.getWidth() < image2Info.getWidth()) {
            rightPad = widthDiff;
        }

        if (bottomTrim > 0 || rightTrim > 0) {
            trimImage(imageToResize, image1Info, 0, rightTrim, bottomTrim, 0, false);
        }

        if (bottomPad > 0 || rightPad > 0) {
            padImage(imageToResize, rightPad, bottomPad);
        }
    }

    private String compareImages(File origImage, File newImage, File diffFile) throws IOException {
        String cmd = String.format("%s -metric MAE %s %s %s", compareBinary, origImage.getAbsolutePath(), newImage.getAbsolutePath(), diffFile.getAbsolutePath());
        log.debug("Executing: " + cmd);
        ExecResult result = ExecUtil.exec(cmd);
        String stdout  = result.getStdOutAsString();
        log.debug("compare output: " + stdout);
        if (!StringUtil.hasText(stdout)) {
            stdout = result.getStdErrAsString();
            log.debug("compare stderr: " + stdout);
        }

        return stdout;
    }

    public void convertFormat(String originalFilename, String newFilename) throws IOException {
        Contract.notNullOrEmpty(originalFilename, "originalFilename cannot be empty");
        Contract.notNullOrEmpty(newFilename, "newFilename cannot be empty");
        Contract.notNullOrEmpty(convertBinary, "convertBinary cannot be empty");

        String cmd = String.format("%s %s %s", convertBinary, originalFilename, newFilename);
        log.debug("Executing: " + cmd);
        ExecUtil.execFailFast(cmd);
    }

    public void trimImage(File image, int trimTop, int trimRight, int trimBottom, int trimLeft) throws IOException {
        ImageInfo info = getImageInfo(image.getAbsolutePath());
        trimImage(image, info, trimTop, trimRight, trimBottom, trimLeft, true);
    }

    private void trimImage(File image, ImageInfo info, int trimTop, int trimRight, int trimBottom, int trimLeft, boolean keepAspectRatio) throws IOException {
        String fname   = image.getAbsolutePath();
        String fileExt = getFileExtension(fname);

        File tmp = File.createTempFile("tmpimg-", fileExt);
        int newWidth  = info.getWidth() - trimRight - trimLeft;
        int newHeight = info.getHeight() - trimTop - trimBottom;
        int topLeftX  = trimLeft;
        int topLeftY  = trimTop;

        String keepAspectStr = keepAspectRatio ? "" : "!";

        try {
            String cmd = String.format("%s|-crop|%dx%d%s+%d+%d|%s|%s", convertBinary, newWidth, newHeight, keepAspectStr, topLeftX, topLeftY, fname, tmp.getAbsolutePath());
            log.debug("Executing: " + cmd);
            ExecUtil.execFailFast(cmd, "\\|");
            IOUtil.renameFile(tmp, image);
        }
        finally {
            tmp.delete();
        }
    }

    public void padImage(File image, int padRight, int padBottom) throws IOException {
        String fname   = image.getAbsolutePath();
        String fileExt = getFileExtension(fname);

        File tmp = File.createTempFile("tmpimg-", fileExt);

        try { // -fill white -gravity east -splice 20x0 -gravity south -splice 0x100 image5.jpg
            String cmd = String.format("%s|%s|-fill|white|-gravity|east|-splice|%dx0|-gravity|south|-splice|0x%d|%s", convertBinary, fname, padRight, padBottom, tmp.getAbsolutePath());
            log.debug("Executing: " + cmd);
            ExecUtil.execFailFast(cmd, "\\|");
            IOUtil.renameFile(tmp, image);
        }
        finally {
            tmp.delete();
        }
    }

    private String getFileExtension(String filename) {
        String fileExt = ".jpg";
        int pos = filename.lastIndexOf(".");
        if (pos > -1)
            fileExt = filename.substring(pos);
        return fileExt;
    }
}
