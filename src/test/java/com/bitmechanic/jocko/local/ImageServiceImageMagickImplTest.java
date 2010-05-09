package com.bitmechanic.jocko.local;

import com.bitmechanic.jocko.BoundingBox;
import com.bitmechanic.jocko.ImageDiff;
import com.bitmechanic.jocko.ImageInfo;
import com.bitmechanic.util.IOUtil;
import com.bitmechanic.util.PropertyUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Feb 10, 2010
 */
public class ImageServiceImageMagickImplTest  {

    ImageServiceImageMagickImpl service;
    ImageDiff diff;

    @Before
    public void setUp() throws Exception {
        Properties props = PropertyUtil.loadProperties("jocko-test.properties");

        service = new ImageServiceImageMagickImpl();
        service.setCompositeBinary(props.getProperty("compositeBinary"));
        service.setConvertBinary(props.getProperty("convertBinary"));
        service.setIdentifyBinary(props.getProperty("identifyBinary"));
        service.setCompareBinary(props.getProperty("compareBinary"));
        service.setPdiffBinary(props.getProperty("pdiffBinary"));
    }

    @After
    public void tearDown() {
        if (diff != null) {
            diff.deleteFiles();
        }
    }

    @Test
    public void testNoDiffIfSameFiles() throws Exception {
        File origImage = new File("test/images/image1.jpg");
        File newImage  = new File("test/images/image1.jpg");
        diff = service.diffImages(origImage, newImage);
        assertFalse(diff.hasDifference());
        assertNull(diff.getDifferenceFile());
    }

    @Test
    public void testDifferenceDetectedIfFilesDifferent() throws Exception {
        File origImage = new File("test/images/image1.jpg");
        File newImage  = new File("test/images/image2.jpg");
        diff = service.diffImages(origImage, newImage);
        assertTrue(diff.hasDifference());

        File diffFile = diff.getDifferenceFile();
        assertNotNull(diffFile);
        assertTrue(diffFile.exists());
        assertTrue(diffFile.length() > 0);

        File compositeFile = diff.getCompositeFile();
        assertNotNull(compositeFile);
        assertTrue(compositeFile.exists());
        assertTrue(compositeFile.length() > 0);
    }

    @Test
    public void testDifferenceDetectedIfFilesDifferent2() throws Exception {
        File origImage = new File("test/images/screen-a.jpg");
        File newImage  = new File("test/images/screen-b.jpg");
        diff = service.diffImages(origImage, newImage);
        assertTrue(diff.hasDifference());

        List<BoundingBox> boundingBoxes = Arrays.asList(new BoundingBox(1, 1, 1024, 40), new BoundingBox(1, 735, 1024, 768));
        diff = service.diffImages(origImage, newImage, boundingBoxes);
        assertFalse(diff.hasDifference());
    }

    @Test(expected=IllegalStateException.class)
    public void testFailsIfFirstImageIsNotFound() throws Exception {
        File origImage = new File("test/images/imageNotFound.jpg");
        File newImage  = new File("test/images/image1.jpg");
        diff = service.diffImages(origImage, newImage);
    }

    @Test(expected=IllegalStateException.class)
    public void testFailsIfSecondImageIsNotFound() throws Exception {
        File origImage = new File("test/images/image1.jpg");
        File newImage  = new File("test/images/imageNotFound.jpg");
        diff = service.diffImages(origImage, newImage);
    }

    @Test
    public void testReportsDifferenceIfImagesAreDifferentSizes() throws Exception {
        File origImage = new File("test/images/image1.jpg");
        File newImage  = new File("test/images/image3.jpg");
        diff = service.diffImages(origImage, newImage);
        assertTrue(diff.hasDifference());

        origImage = new File("test/images/step-29-baseline.png");
        newImage  = new File("test/images/step-29-current.png");
        diff = service.diffImages(origImage, newImage);
        assertTrue(diff.hasDifference());

        origImage = new File("test/images/diff-a.png");
        newImage  = new File("test/images/diff-b.png");
        diff = service.diffImages(origImage, newImage);
        assertTrue(diff.hasDifference());

        origImage = new File("test/images/diff2-a.png");
        newImage  = new File("test/images/diff2-b.png");
        diff = service.diffImages(origImage, newImage, Collections.singletonList(new BoundingBox(0,0,0,0)));
        assertTrue(diff.hasDifference());

//        origImage = new File("test/images/diff3-baseline.png");
//        newImage  = new File("test/images/diff3-current.png");
//        diff = service.diffImages(origImage, newImage, Arrays.asList(new BoundingBox(0,0,3,3), new BoundingBox(0,0,3,3), new BoundingBox(708,447, 1020, 868)));
//        assertTrue(diff.hasDifference());
    }

    @Test(expected=IllegalStateException.class)
    public void testFailsIfFirstImageIsNull() throws Exception {
        File image = new File("test/images/image1.jpg");
        diff = service.diffImages(null, image);
    }

    @Test
    public void testIgnoresDifferencesInsideMaskingBox() throws Exception {
        List<BoundingBox> boundingBoxes = Collections.singletonList(new BoundingBox(40, 30, 100, 125));
        File origImage = new File("test/images/image1.jpg");
        File newImage  = new File("test/images/image2.jpg");
        diff = service.diffImages(origImage, newImage, boundingBoxes);
        assertFalse(diff.hasDifference());
    }

    @Test
    public void testIgnoresDifferencesInsideMaskingBoxes() throws Exception {
        List<BoundingBox> boundingBoxes = new ArrayList<BoundingBox>();
        boundingBoxes.add(new BoundingBox(40, 30, 100, 60));
        boundingBoxes.add(new BoundingBox(40, 60, 100, 125));        
        File origImage = new File("test/images/image1.jpg");
        File newImage  = new File("test/images/image2.jpg");
        diff = service.diffImages(origImage, newImage, boundingBoxes);
        assertFalse(diff.hasDifference());
    }

    @Test(expected=IllegalStateException.class)
    public void testFailsIfBoundingBoxLargerThanImage() throws Exception {
        List<BoundingBox> boundingBoxes = Collections.singletonList(new BoundingBox(0, 0, 9999, 9999));
        File origImage = new File("test/images/image1.jpg");
        File newImage  = new File("test/images/image2.jpg");
        diff = service.diffImages(origImage, newImage, boundingBoxes);
    }

    @Test
    public void testTrimImage() throws Exception {
        File image = new File("test/images/image1.jpg");
        File dest  = File.createTempFile("test", ".jpg");
        dest.deleteOnExit();
        IOUtil.copyFile(image, dest);

        ImageInfo info = service.getImageInfo(dest.getAbsolutePath());
        service.trimImage(dest, 10, 2, 30, 5);
        
        ImageInfo info2 = service.getImageInfo(dest.getAbsolutePath());
        assertEquals(info.getWidth() - 7, info2.getWidth());
        assertEquals(info.getHeight() - 40, info2.getHeight());

        dest.delete();
    }

}
