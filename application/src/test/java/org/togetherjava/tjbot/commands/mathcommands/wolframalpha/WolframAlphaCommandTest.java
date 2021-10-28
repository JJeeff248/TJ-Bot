package org.togetherjava.tjbot.commands.mathcommands.wolframalpha;

import org.junit.jupiter.api.Test;
import org.togetherjava.tjbot.commands.utils.WolfCommandUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WolframAlphaCommandTest {
    @Test
    void compareImagesTest() {
        BufferedImage image1 = new BufferedImage(100, 100, 6);
        BufferedImage image2 = new BufferedImage(100, 100, 6);
        image1.getGraphics().setColor(Color.RED);
        image2.getGraphics().setColor(Color.YELLOW);
        assertFalse(WolfCommandUtils.compareImages(image1, image2));
    }

    @Test
    void combineImagesTest() {
        BufferedImage image1 = new BufferedImage(100, 100, 6);
        BufferedImage image2 = new BufferedImage(100, 100, 6);
        BufferedImage mergedImage = new BufferedImage(100, 200, 6);
        assertTrue(WolfCommandUtils.compareImages(mergedImage,
                WolfCommandUtils.combineImages(List.of(image1, image2), 100, 200)));
        Graphics g1 = image1.getGraphics();
        g1.setColor(Color.RED);
        g1.fillRect(0, 0, 100, 100);
        Graphics g2 = image2.getGraphics();
        g2.setColor(Color.RED);
        g2.fillRect(0, 0, 100, 100);
        assertTrue(WolfCommandUtils.compareImages(mergedImage,
                WolfCommandUtils.combineImages(List.of(image1, image2), 100, 200)));
    }
}
