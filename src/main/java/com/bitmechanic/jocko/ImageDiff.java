package com.bitmechanic.jocko;

import java.io.File;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Feb 10, 2010
 */
public class ImageDiff {

    private File diffFile;
    private File compositeFile;
    private boolean hasDifference;
    private String message;

    public ImageDiff() {
        this(null, null, false, null);
    }

    public ImageDiff(File diffFile, File compositeFile, boolean hasDifference, String message) {
        this.diffFile = diffFile;
        this.compositeFile = compositeFile;
        this.hasDifference = hasDifference;
        this.message = message;
    }

    public boolean hasDifference() {
        return hasDifference;
    }

    public File getDifferenceFile() {
        return diffFile;
    }

    public File getCompositeFile() {
        return compositeFile;
    }

    public String getMessage() {
        return message;
    }

    public void deleteFiles() {
        if (diffFile != null)
            diffFile.delete();
        if (compositeFile != null)
            compositeFile.delete();
    }

}
