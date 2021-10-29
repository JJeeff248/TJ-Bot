package org.togetherjava.tjbot.commands.mathcommands.wolframalpha;

import org.junit.jupiter.api.Test;
import org.togetherjava.tjbot.commands.utils.WolfCommandUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WolframAlphaCommandTest {
    @Test
    void compareImagesTest() {
        BufferedImage image1 = new BufferedImage(100, 100, 6);
        BufferedImage image2 = new BufferedImage(100, 100, 6);
        image1.getGraphics().setColor(Color.RED);
        image2.getGraphics().setColor(Color.YELLOW);
        assertFalse(WolfCommandUtils.compareImages(image1, image2));
    }

    @Test
    void combineImagesTest() throws IOException {
        BufferedImage image1 = new BufferedImage(100, 100, 6);
        BufferedImage image2 = new BufferedImage(100, 100, 6);
        BufferedImage mergedImage = new BufferedImage(100, 200, 6);
        assertTrue(WolfCommandUtils.compareImages(mergedImage,
                WolfCommandUtils.combineImages(List.of(image1, image2), 100, 200)));
        Graphics g1 = image1.getGraphics();
        g1.setColor(Color.RED);
        g1.fillRect(0, 0, 100, 100);
        ImageIO.write(image1, "png", new File(
                "C:\\Users\\Abc\\IdeaProjects\\TJ-Bot-baseRepo\\application\\src\\test\\java\\org\\togetherjava\\tjbot\\commands\\mathcommands\\wolframalpha\\img1.png"));
        Graphics g2 = image2.getGraphics();
        g2.setColor(Color.BLUE);
        g2.fillRect(0, 0, 100, 100);
        ImageIO.write(image2, "png", new File(
                "C:\\Users\\Abc\\IdeaProjects\\TJ-Bot-baseRepo\\application\\src\\test\\java\\org\\togetherjava\\tjbot\\commands\\mathcommands\\wolframalpha\\img2.png"));
        Graphics g3 = mergedImage.getGraphics();
        g3.setColor(Color.RED);
        g3.fillRect(0, 0, 100, 100);
        g3.setColor(Color.BLUE);
        g3.fillRect(0, 100, 100, 100);
        ImageIO.write(mergedImage, "png", new File(
                "C:\\Users\\Abc\\IdeaProjects\\TJ-Bot-baseRepo\\application\\src\\test\\java\\org\\togetherjava\\tjbot\\commands\\mathcommands\\wolframalpha\\manuallyMergedImg.png"));
        BufferedImage mergedByMethod =
                WolfCommandUtils.combineImages(List.of(image1, image2), 100, 200);
        ImageIO.write(mergedByMethod, "png", new File(
                "C:\\Users\\Abc\\IdeaProjects\\TJ-Bot-baseRepo\\application\\src\\test\\java\\org\\togetherjava\\tjbot\\commands\\mathcommands\\wolframalpha\\methodMergedimage.png"));
        assertTrue(WolfCommandUtils.compareImages(mergedImage, mergedByMethod));
    }
}
