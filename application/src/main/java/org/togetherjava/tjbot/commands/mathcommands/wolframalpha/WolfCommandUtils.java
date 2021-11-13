package org.togetherjava.tjbot.commands.mathcommands.wolframalpha;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class WolfCommandUtils {

    private WolfCommandUtils() {

    }

    public static byte[] imageToBytes(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    public static BufferedImage combineImages(List<BufferedImage> images, int width, int height) {
        width = images.stream().mapToInt(BufferedImage::getWidth).max().orElse(Integer.MAX_VALUE);
        height = images.stream().mapToInt(BufferedImage::getHeight).sum();
        WolframAlphaCommand.logger.info("In Method: Width {} Height {}", width, height);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics imgGraphics = image.getGraphics();
        imgGraphics.setColor(Color.WHITE);
        imgGraphics.fillRect(0, 0, width, height);
        int resultHeight = 0;
        int writtenImage = 0;
        for (BufferedImage img : images) {
            imgGraphics.drawImage(img, 0, resultHeight, null);
            try {
                ImageIO.write(image, "png", Path
                    .of("C:\\Users\\Abc\\IdeaProjects\\TJ-Bot-baseRepo\\application\\src\\main\\java\\org\\togetherjava\\tjbot\\commands\\utils\\combinedImage%d.png"
                        .formatted(++writtenImage))
                    .toFile());
            } catch (IOException e) {
                WolframAlphaCommand.logger.error("e");
            }
            resultHeight += img.getHeight(null);
        }
        return image;
    }

    public static boolean compareImages(BufferedImage img1, BufferedImage img2) {
        int width1 = img1.getWidth();
        int height1 = img1.getHeight();
        return width1 == img2.getWidth() && height1 == img2.getHeight()
                && IntStream.range(0, width1)
                    .mapToObj(x -> IntStream.range(0, height1)
                        .anyMatch(y -> img1.getRGB(x, y) != img2.getRGB(x, y)))
                    .noneMatch(x -> x);
    }

    public static String handleMisunderstoodQuery(QueryResult result) {
        List<String> output = new ArrayList<>();
        output
            .add("The Wolfram|Alpha API was unable to produce a successful result. Visit the URI");
        Tips tips = result.getTips();
        if (tips != null) {
            output.add("Here are some tips \n"
                    + tips.getTips().stream().map(Tip::getText).collect(Collectors.joining("\n")));
        }
        FutureTopic futureTopic = result.getFutureTopic();
        if (futureTopic != null) {
            output.add(
                    "Your query is regarding The topic \"%s\" which might be supported by Wolfram Alpha in the future"
                        .formatted(futureTopic.getTopic()));
        }
        LanguageMsg languageMsg = result.getLanguageMsg();
        if (languageMsg != null) {
            output.add(languageMsg.getEnglish() + "\n" + languageMsg.getOther());
        }
        DidYouMeans didYouMeans = result.getDidYouMeans();
        if (didYouMeans != null) {
            output.add("Did you mean \n" + didYouMeans.getDidYouMeans()
                .stream()
                .map(DidYouMean::getMessage)
                .collect(Collectors.joining("\n")));
        }
        RelatedExamples relatedExamples = result.getRelatedExamples();
        if (relatedExamples != null) {
            output.add("Here are some related examples \n" + relatedExamples.getRelatedExamples()
                .stream()
                .map(RelatedExample::getCategoryThumb)
                .collect(Collectors.joining("\n")));
        }
        WolframAlphaCommand.logger.info("Error Message \n {}", String.join("\n", output));
        return String.join("\n", output);
    }
}
