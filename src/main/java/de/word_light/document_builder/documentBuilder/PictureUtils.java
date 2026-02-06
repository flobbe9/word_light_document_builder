package de.word_light.document_builder.documentBuilder;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.poi.common.usermodel.PictureType;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import de.word_light.document_builder.exception.ApiException;
import io.micrometer.common.util.StringUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;


/**
 * Util class for adding pictures to an {@link XWPFDocument}.
 * 
 * @since 0.0.1
 */
@Getter
@Setter
@Log4j2
public class PictureUtils {

    /** 
     * Used for calculating picture dimensions to centimeters.
     * @see org.apache.poi.util.Units
     */
    public static final Integer EMU_PER_CENTIMETER = 360000;   
    private static final int EMU_PER_INCH = 914_400;
     
    private Map<String, byte[]> pictures;


    public PictureUtils(Map<String, byte[]> pictures) {
        this.pictures = pictures;
    }

    /**
     * Adds picture to given {@link XWPFRun} if {@code fileName} is found in {@link #pictures} list. 
     * If {@code this.pictures} is empty do nothing. <p>
     * 
     * Dimensions should fit the original.
     * 
     * @param run to add the picture to
     * @param fileName of the picture. Has to match at least one file name from {@link #pictures}.
     *                 In case of duplicates the first match will be used.
     *                 Assuming format like "${someFileName.png}"
     * @param pictureType format of the picture
     */
    void addPicture(XWPFRun run, String fileName) {
        // case: no pictures uploaded
        if (this.pictures == null || this.pictures.isEmpty()) {
            log.warn("Did not add pictures. 'pictures' list is either null or empty.");
            return;
        }

        // remove ${} braces
        fileName = getRawPictureName(fileName);

        // validate
        PictureType pictureType = getPictureType(fileName);
        if (pictureType == null)
            throw new ApiException("Did not add pictures. " + fileName + " is not of a valid picture type.");
        
        // add to run
        byte[] pictureBytes = this.pictures.get(fileName);
        try (InputStream bis = new ByteArrayInputStream(pictureBytes)) {
            BufferedImage bimg = ImageIO.read(bis);

            // reset the stream cursor since the stream has already been read by the buffered image
            bis.reset();
            run.addPicture(
                bis, 
                pictureType.ordinal(),
                fileName, 
                pxToEmu(bimg.getWidth()),
                pxToEmu(bimg.getHeight())
            );

            // lock aspect ratio
            try {
                run.getCTR()
                    .getDrawingArray(0)
                    .getInlineArray(0)
                    .addNewCNvGraphicFramePr()
                    .addNewGraphicFrameLocks()
                    .setNoChangeAspect(true);

            } catch (NullPointerException | IndexOutOfBoundsException e) {
                log.warn("Failed to lock picture aspect ratio for file {}", fileName);
                e.printStackTrace();
            }

        } catch (Exception e) {
            throw new ApiException("Failed to add picture.", e);
        }
    }

    /**
     * Checks if given string ends on a picture extension like ".jpg" or ".png" and returns the {@link PictureType}.<p>
     * 
     * ".jpeg" is not supported.
     * 
     * @param fileName to find the pictureType of
     * @return the pictureType if fileName ends on an extension from {@link PictureType} or null
     */
    public static PictureType getPictureType(String fileName) {
        if (fileName == null)
            return null;

        // check file extension for matching picture extension
        for (PictureType pictureType : PictureType.values()) {
            if (fileName.toLowerCase().endsWith(pictureType.getExtension()))
                return pictureType;
        };

        return null;
    }

    /**
     * Determine if given text should be treated as file name for a picture in a document.
     * 
     * @param text of basic paragraph to check
     * @return true if text is formatted like: "${someFileName.someValidPictureSuffix}", i.e. "${beautifulView.png}".
     */
    public static boolean isPicture(String text) {
        // case: null or blank
        if (StringUtils.isBlank(text))
            return false;

        boolean hasBraces = text.startsWith("${") && text.endsWith("}");

        return hasBraces && getPictureType(getRawPictureName(text)) != null;
    }

    /**
     * Call {@link #pxToEmu(int, int)} assuming 96dpi.
     * 
     * @param px
     * @return
     */
    private int pxToEmu(int px) {
        return pxToEmu(px, Units.PIXEL_DPI);
    }

    /**
     * Emu is a very small unit used in ms word.
     * 
     * @param px to convert
     * @param dpi of the image or container that's beeing measured
     * @return emus
     * @throws IllegalArgumentException if dpi is less than 1
     * @see {@link Units}
     */
    private int pxToEmu(int px, int dpi) {
        if (dpi <= 0)
            throw new IllegalArgumentException("'dpi' must be greater than 0");

        return Math.round(px * (EMU_PER_INCH / dpi));
    }
    
    /**
     * @param pictureName unaltered text from basicParagraph that is expected to be formatted like {@code "${somePictureName.png}"}
     * @return given picture name without curly braces, i.e. {@code getRawPictureName("${somePictureName.png}")} would return
     *         {@code "somePictureName.png"}. Does not alter {@code pictureName}.
     *         If picture is not formatted as expected, return {@code pictureName}.
     *         Return {@code null} if {@code pictureName} is {@code null} or too short
     */
    private static String getRawPictureName(String pictureName) {
        try {
            // case: not formatted correctly, assuming is raw already
            if (!pictureName.startsWith("${") || !pictureName.endsWith("}"))
                return pictureName;

            return pictureName.substring(2, pictureName.length() - 1);

        } catch (IndexOutOfBoundsException | NullPointerException e) {
            return null;
        }
    }
}