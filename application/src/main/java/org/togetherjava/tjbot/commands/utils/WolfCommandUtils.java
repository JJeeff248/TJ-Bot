package org.togetherjava.tjbot.commands.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.awt.image.BufferedImage;

public class WolfCommandUtils {
    public static BufferedImage combineImages(List<BufferedImage> images, int width, int height)
            throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics imgGraphics = image.getGraphics();
        int resultHeight = 0;
        for (BufferedImage img : images) {
            imgGraphics.drawImage(img, width, resultHeight, null);
            resultHeight += img.getHeight(null);
        }
        return image;
    }

}
