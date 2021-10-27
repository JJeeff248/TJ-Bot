package org.togetherjava.tjbot.commands.mathcommands.wolframalpha;

import org.junit.jupiter.api.Test;
import org.togetherjava.tjbot.commands.utils.WolfCommandUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WolframAlphaCommandTest {

    @Test
    void combineImagesTest() throws IOException {
        BufferedImage image1 = new BufferedImage(100, 100, 6);
        BufferedImage image2 = new BufferedImage(100, 100, 6);
        BufferedImage mergedImage = new BufferedImage(100, 200, 6);
        assertEquals(mergedImage,
                WolfCommandUtils.combineImages(List.of(image1, image2), 100, 200));
    }

}
