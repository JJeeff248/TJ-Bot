package org.togetherjava.tjbot.commands.mathcommands.wolframalpha;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.mikael.urlbuilder.UrlBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageUpdateAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.togetherjava.tjbot.commands.SlashCommandAdapter;
import org.togetherjava.tjbot.commands.SlashCommandVisibility;
import org.togetherjava.tjbot.commands.utils.WolfCommandUtils;
import org.togetherjava.tjbot.config.Config;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;


public final class WolframAlphaCommand extends SlashCommandAdapter {
    public static final Logger logger = LoggerFactory.getLogger(WolframAlphaCommand.class);
    /**
     * Starting part of a regular wolframalpha query link.
     */
    public static final String USER_ENDPOINT = "https://www.wolframalpha.com/input";
    private static final int HTTP_STATUS_CODE_OK = 200;
    private static final XmlMapper XML = new XmlMapper();
    private static final String QUERY_OPTION = "query";
    /**
     * WolframAlpha API endpoint to connect to.
     *
     * @see <a href=
     *      "https://products.wolframalpha.com/docs/WolframAlpha-API-Reference.pdf">WolframAlpha API
     *      Reference</a>.
     */
    private static final String API_ENDPOINT = "http://api.wolframalpha.com/v2/query";
    private static final int MAX_IMAGE_HEIGHT_PX = 400;
    /**
     * WolframAlpha text Color
     */
    private static final Color WOLFRAM_ALPHA_TEXT_COLOR = Color.decode("#3C3C3C");
    /**
     * WolframAlpha Font
     */
    private static final Font WOLFRAM_ALPHA_FONT = new Font("Times", Font.PLAIN, 15)
        .deriveFont(Map.of(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON));
    /**
     * Height of the unscaled text displayed in Font {@link #WOLFRAM_ALPHA_FONT}
     */
    private static final int TEXT_HEIGHT = 30;
    private final HttpClient client = HttpClient.newHttpClient();

    public WolframAlphaCommand() {
        super("wolf", "Renders mathematical queries using WolframAlpha",
                SlashCommandVisibility.GUILD);
        getData().addOption(OptionType.STRING, QUERY_OPTION, "the query to send to WolframAlpha",
                true);
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {

        // The processing takes some time
        event.deferReply().queue();

        String query = Objects.requireNonNull(event.getOption(QUERY_OPTION)).getAsString();

        URI userUri = UrlBuilder.fromString(USER_ENDPOINT).addParameter("i", query).toUri();

        MessageEmbed uriEmbed = new EmbedBuilder()
            .setTitle(query + "- Wolfram|Alpha", userUri.toString())
            .setDescription(
                    "Wolfram|Alpha brings expert-level knowledge and capabilities to the broadest possible range of people-spanning all professions and education levels.")
            .build();

        WebhookMessageUpdateAction<Message> action =
                event.getHook().editOriginal("").setEmbeds(uriEmbed);

        URI apiUri = UrlBuilder.fromString(API_ENDPOINT)
            .addParameter("appid", Config.getInstance().getWolframAlphaAppId())
            .addParameter("format", "image,plaintext")
            .addParameter("input", query)
            .toUri();
        HttpRequest request = sendQuery(apiUri);
        Optional<HttpResponse<String>> optResponse = getResponse(event, request, action);
        if (optResponse.isEmpty())
            return;
        HttpResponse<String> response = optResponse.get();
        Optional<QueryResult> optResult = parseQuery(response, event, action);
        if (optResult.isEmpty())
            return;
        QueryResult result = optResult.get();
        action = action.setContent("Computed in:" + result.getTiming());
        /*
         * Optional<List<MessageAction>> optAction = createResult(result, event);
         * optAction.ifPresent(list -> list.forEach(MessageAction::queue));
         */
        String content = "Computed in:" + result.getTiming() + "\n"
                + (result.getNumberOfTimedOutPods() == 0 ? ""
                        : "Some pods have timed out. Visit the URI")
                + "\n" + createResult$1(result, event, action, uriEmbed);
        action.setContent(content).queue();
    }

    private @NotNull Optional<HttpResponse<String>> getResponse(@NotNull SlashCommandEvent event,
            @NotNull HttpRequest request, @NotNull WebhookMessageUpdateAction<Message> action) {
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            /*
             * event.getHook() .setEphemeral(true)
             * .editOriginal("Unable to get a response from WolframAlpha API") .queue();
             */
            action.setContent("Unable to get a response from WolframAlpha API").queue();
            logger.warn("Could not get the response from the server", e);
            return Optional.empty();
        } catch (InterruptedException e) {
            /*
             * event.getHook() .setEphemeral(true)
             * .editOriginal("Connection to WolframAlpha was interrupted") .queue();
             */
            action.setContent("Connection to WolframAlpha was interrupted").queue();
            logger.info("Connection to WolframAlpha was interrupted", e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }

        if (response.statusCode() != HTTP_STATUS_CODE_OK) {
            /*
             * event.getHook() .setEphemeral(true)
             * .editOriginal("The response' status code was incorrect") .queue();
             */
            action.setContent("The response' status code was incorrect").queue();
            logger.warn("Unexpected status code: Expected: {} Actual: {}", HTTP_STATUS_CODE_OK,
                    response.statusCode());
            return Optional.empty();
        }
        return Optional.of(response);
    }

    private @NotNull HttpRequest sendQuery(@NotNull URI uri) {
        logger.info("The query URI is {}", uri);
        return HttpRequest.newBuilder(uri).GET().build();
    }

    private @NotNull Optional<QueryResult> parseQuery(@NotNull HttpResponse<String> response,
            @NotNull SlashCommandEvent event, @NotNull WebhookMessageUpdateAction<Message> action) {
        QueryResult result;
        try {
            Files.writeString(Path
                .of("C:\\Users\\Abc\\IdeaProjects\\TJ-Bot-baseRepo\\application\\src\\main\\java\\org\\togetherjava\\tjbot\\commands\\mathcommands\\wolframalpha\\responsebody.xml"),
                    response.body());
            result = XML.readValue(response.body(), QueryResult.class);

        } catch (IOException e) {
            /*
             * event.getHook() .setEphemeral(true)
             * .editOriginal("Unexpected response from WolframAlpha API") .queue();
             */
            action.setContent("Unexpected response from WolframAlpha API").queue();
            logger.error("Unable to deserialize the class ", e);
            return Optional.empty();
        }

        if (!result.isSuccess()) {
            /*
             * event.getHook() .setEphemeral(true)
             * .editOriginal("Could not successfully receive the result") .queue();
             */
            action.setContent("Could not successfully receive the result").queue();
            // TODO The exact error details have a different POJO structure,
            // POJOs have to be added to get those details. See the Wolfram doc.
            return Optional.empty();
        }
        return Optional.of(result);
    }

    @Deprecated
    private @NotNull Optional<List<MessageAction>> createImages(SlashCommandEvent event,
            QueryResult result) {
        MessageChannel channel = event.getChannel();
        List<MessageAction> messages = new ArrayList<>();
        // For each probably much more readable.
        try {
            result.getPods()
                .forEach(pod -> pod.getSubPods().stream().map(SubPod::getImage).forEach(image -> {
                    try {
                        String name = image.getTitle();
                        String source = image.getSource();
                        String extension = ".png";
                        if (name.isEmpty())
                            name = pod.getTitle();
                        name += extension;
                        BufferedImage img = ImageIO.read(new URL(source));
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        ImageIO.write(img, "png", stream);
                        messages.add(channel.sendFile(stream.toByteArray(), name));
                    } catch (IOException e) {
                        event.getHook()
                            .setEphemeral(true)
                            .editOriginal(
                                    "Unable to generate message based on the WolframAlpha response")

                            .queue();
                        logger.error("Failed to read image {} from the WolframAlpha response",
                                image, e);
                        throw new Error();
                    }
                }));
        } catch (Error e) {
            return Optional.empty();
        }
        return Optional.of(messages);
    }

    private @NotNull Optional<List<MessageAction>> createResult(@NotNull QueryResult result,
            @NotNull SlashCommandEvent event) {

        MessageChannel channel = event.getChannel();
        /* WebhookMessageUpdateAction<Message> action = */
        event.getHook().editOriginal("Computed in " + result.getTiming()).queue();
        int filesAttached = 0;
        int resultHeight = 0;
        int maxWidth = Integer.MIN_VALUE;
        MessageAction message = null;
        EmbedBuilder embedBuilder = new EmbedBuilder();
        List<MessageEmbed> embeds = new ArrayList<>();
        List<BufferedImage> images = new ArrayList<>();
        List<byte[]> bytes = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<Pod> pods = new ArrayList<>(result.getPods());
        List<MessageAction> messages = new ArrayList<>();
        logger.info("There are {} pods", pods.size());
        int imageNo = 0;
        OUTER: for (int i = 0; i < pods.size();) {
            Pod pod = pods.get(i);
            List<SubPod> subPods = new ArrayList<>(pod.getSubPods());
            logger.info("pod number {}", ++i);
            logger.info("There are {} sub pods within this pod", subPods.size());
            for (int j = 0; j < subPods.size();) {

                SubPod subPod = subPods.get(j);
                logger.info("sub pod number {}", ++j);
                WolfImage image = subPod.getImage();
                try {
                    String name = image.getTitle();
                    String source = image.getSource();
                    String extension = ".png";
                    String header = pod.getTitle();
                    boolean jIs1 = j == 1;
                    int width =
                            (jIs1 ? Math.max(getWidth(header), image.getWidth()) : image.getWidth())
                                    + 10;
                    int height = image.getHeight();
                    if (j == 1)
                        height += TEXT_HEIGHT;
                    BufferedImage readImage =
                            new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
                    Graphics graphics = readImage.getGraphics();
                    if (jIs1) {
                        graphics.setFont(WOLFRAM_ALPHA_FONT);
                        graphics.setColor(Color.WHITE);
                        graphics.setColor(WOLFRAM_ALPHA_TEXT_COLOR);
                        graphics.drawString(header, 10, 15);
                    }
                    var srcImg = ImageIO.read(new URL(source));
                    graphics.drawImage(srcImg, 10, jIs1 ? 20 : 0, null);

                    ImageIO.write(readImage, "png", Path
                        .of("C:\\Users\\Abc\\IdeaProjects\\TJ-Bot-baseRepo\\application\\src\\main\\java\\org\\togetherjava\\tjbot\\commands\\mathcommands\\wolframalpha\\img%d.png"
                            .formatted(++imageNo))
                        .toFile());
                    ImageIO.write(srcImg, "png", Path
                        .of("C:\\Users\\Abc\\IdeaProjects\\TJ-Bot-baseRepo\\application\\src\\main\\java\\org\\togetherjava\\tjbot\\commands\\mathcommands\\wolframalpha\\ogImg%d.png"
                            .formatted(imageNo))
                        .toFile());

                    if (name.isEmpty()) {
                        name = header;
                    }
                    name += extension;
                    maxWidth = Math.max(maxWidth, width);
                    // FIXME get the attachments <= 10 or return a collection
                    if (filesAttached == 10) {
                        break OUTER;
                    }


                    if (resultHeight + image.getHeight() > MAX_IMAGE_HEIGHT_PX) {
                        BufferedImage combinedImage =
                                WolfCommandUtils.combineImages(images, maxWidth, resultHeight);
                        images.clear();
                        ImageIO.write(combinedImage, "png", Path
                            .of("C:\\Users\\Abc\\IdeaProjects\\TJ-Bot-baseRepo\\application\\src\\main\\java\\org\\togetherjava\\tjbot\\commands\\mathcommands\\wolframalpha\\sentImage%d.png"
                                .formatted(++filesAttached))
                            .toFile());
                        if (message == null) {
                            message = channel.sendFile(WolfCommandUtils.imageToBytes(combinedImage),
                                    "result%d.png".formatted(++filesAttached));
                        } else {
                            message.addFile(WolfCommandUtils.imageToBytes(combinedImage),
                                    "result%d.png".formatted(++filesAttached));
                        }
                        /*
                         * messages.add(channel.sendFile(WolfCommandUtils.imageToBytes(combinedImage
                         * ), name));
                         */
                        /*
                         * bytes.add(WolfCommandUtils.imageToBytes(combinedImage));
                         *
                         */
                        // filesAttached++;
                        // names.add(name);
                        resultHeight = 0;
                        maxWidth = Integer.MIN_VALUE;
                        embeds.add(embedBuilder
                            .setImage("attachment://result%d.png".formatted(filesAttached))
                            .build());
                    } else if (pod == pods.get(pods.size() - 1)
                            && subPod == subPods.get(subPods.size() - 1)) {
                        logger.info("The last image");
                        BufferedImage combinedImage = WolfCommandUtils.combineImages(images,
                                Math.max(maxWidth, image.getWidth()),
                                resultHeight + image.getHeight());
                        images.clear();
                        ImageIO.write(combinedImage, "png", Path
                            .of("C:\\Users\\Abc\\IdeaProjects\\TJ-Bot-baseRepo\\application\\src\\main\\java\\org\\togetherjava\\tjbot\\commands\\mathcommands\\wolframalpha\\sentImage%d.png"
                                .formatted(++filesAttached))
                            .toFile());
                        if (message == null) {
                            message = channel.sendFile(WolfCommandUtils.imageToBytes(combinedImage),
                                    "result%d.png".formatted(++filesAttached));
                        } else {
                            message.addFile(WolfCommandUtils.imageToBytes(combinedImage),
                                    "result%d.png".formatted(++filesAttached));
                        }
                        /*
                         * messages.add(channel.sendFile(WolfCommandUtils.imageToBytes(combinedImage
                         * ), name));
                         */
                        /*
                         * bytes.add(WolfCommandUtils.imageToBytes(combinedImage));//
                         * filesAttached++;
                         */
                        embeds.add(embedBuilder
                            .setImage("attachment://result%d.png".formatted(filesAttached))
                            .build());
                    }
                    resultHeight += readImage.getHeight();
                    images.add(readImage);
                    logger.info(
                            "Max Width {} Result Height {} Current Width {} Current Height {} Images {}",
                            maxWidth, resultHeight, readImage.getWidth(), readImage.getHeight(),
                            filesAttached);
                } catch (IOException e) {
                    event.reply("Unable to generate message based on the WolframAlpha response")
                        .setEphemeral(true)
                        .queue();
                    logger.error("Failed to read image {} from the WolframAlpha response", image,
                            e);
                    return Optional.empty();
                }
            }
            logger.info("Exiting pod number {}", i);
        }
        /*
         * for (int i = bytes.size() - 1; i >= 0; i--) { try { ImageIO.write(ImageIO.read(new
         * ByteArrayInputStream(bytes.get(i))), "png", Path .of(
         * "C:\\Users\\Abc\\IdeaProjects\\TJ-Bot-baseRepo\\application\\src\\main\\java\\org\\togetherjava\\tjbot\\commands\\mathcommands\\wolframalpha\\sentImage%d.png"
         * .formatted(i)) .toFile()); } catch (IOException e) { e.printStackTrace(); } action =
         * action.addFile(bytes.get(i), names.get(i)); }
         */
        messages.add(message.setEmbeds(embeds));
        return Optional.of(messages);
    }

    private @NotNull String createResult$1(@NotNull QueryResult result,
            @NotNull SlashCommandEvent event, WebhookMessageUpdateAction<Message> action,
            MessageEmbed embed) {

        int filesAttached = 0;
        int resultHeight = 0;
        int maxWidth = Integer.MIN_VALUE;
        boolean podsTimedOut = result.getNumberOfTimedOutPods() != 0;
        EmbedBuilder embedBuilder = new EmbedBuilder();
        List<MessageEmbed> embeds = new ArrayList<>(List.of(embed));
        List<BufferedImage> images = new ArrayList<>();
        List<byte[]> bytes = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<Pod> pods = new ArrayList<>(result.getPods());
        logger.info("There are {} pods", pods.size());
        int imageNo = 0;
        OUTER: for (int i = 0; i < pods.size();) {
            Pod pod = pods.get(i);
            List<SubPod> subPods = new ArrayList<>(pod.getSubPods());
            logger.info("pod number {}", ++i);
            logger.info("There are {} sub pods within this pod", subPods.size());
            for (int j = 0; j < subPods.size();) {

                SubPod subPod = subPods.get(j);
                logger.info("sub pod number {}", ++j);
                WolfImage image = subPod.getImage();
                try {
                    String name = image.getTitle();
                    String source = image.getSource();
                    String extension = ".png";
                    String header = pod.getTitle();
                    boolean jIs1 = j == 1;
                    int width =
                            (jIs1 ? Math.max(getWidth(header), image.getWidth()) : image.getWidth())
                                    + 10;
                    int height = image.getHeight();
                    if (j == 1)
                        height += TEXT_HEIGHT;
                    BufferedImage readImage =
                            new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
                    Graphics graphics = readImage.getGraphics();
                    if (jIs1) {
                        graphics.setFont(WOLFRAM_ALPHA_FONT);
                        graphics.setColor(Color.WHITE);
                        graphics.setColor(WOLFRAM_ALPHA_TEXT_COLOR);
                        graphics.drawString(header, 10, 15);
                    }
                    var srcImg = ImageIO.read(new URL(source));
                    graphics.drawImage(srcImg, 10, jIs1 ? 20 : 0, null);

                    ImageIO.write(readImage, "png", Path
                        .of("C:\\Users\\Abc\\IdeaProjects\\TJ-Bot-baseRepo\\application\\src\\main\\java\\org\\togetherjava\\tjbot\\commands\\mathcommands\\wolframalpha\\img%d.png"
                            .formatted(++imageNo))
                        .toFile());
                    ImageIO.write(srcImg, "png", Path
                        .of("C:\\Users\\Abc\\IdeaProjects\\TJ-Bot-baseRepo\\application\\src\\main\\java\\org\\togetherjava\\tjbot\\commands\\mathcommands\\wolframalpha\\ogImg%d.png"
                            .formatted(imageNo))
                        .toFile());

                    if (name.isEmpty()) {
                        name = header;
                    }
                    name += extension;
                    maxWidth = Math.max(maxWidth, width);
                    // FIXME get the attachments <= 10 or return a collection
                    if (filesAttached == 10) {
                        action.setEmbeds(embeds);
                        return "Too many images. Visit the URI";
                    }


                    if (resultHeight + image.getHeight() > MAX_IMAGE_HEIGHT_PX) {
                        BufferedImage combinedImage =
                                WolfCommandUtils.combineImages(images, maxWidth, resultHeight);
                        images.clear();
                        ImageIO.write(combinedImage, "png", Path
                            .of("C:\\Users\\Abc\\IdeaProjects\\TJ-Bot-baseRepo\\application\\src\\main\\java\\org\\togetherjava\\tjbot\\commands\\mathcommands\\wolframalpha\\sentImage%d.png"
                                .formatted(++filesAttached))
                            .toFile());
                        action = action.addFile(WolfCommandUtils.imageToBytes(combinedImage),
                                "result%d.png".formatted(++filesAttached));
                        /*
                         * messages.add(channel.sendFile(WolfCommandUtils.imageToBytes(combinedImage
                         * ), name));
                         */
                        /*
                         * bytes.add(WolfCommandUtils.imageToBytes(combinedImage));
                         *
                         */
                        // filesAttached++;
                        // names.add(name);
                        resultHeight = 0;
                        maxWidth = Integer.MIN_VALUE;
                        embeds.add(embedBuilder
                            .setImage("attachment://result%d.png".formatted(filesAttached))
                            .build());
                    } else if (pod == pods.get(pods.size() - 1)
                            && subPod == subPods.get(subPods.size() - 1)) {
                        logger.info("The last image");
                        BufferedImage combinedImage = WolfCommandUtils.combineImages(images,
                                Math.max(maxWidth, image.getWidth()),
                                resultHeight + image.getHeight());
                        images.clear();
                        ImageIO.write(combinedImage, "png", Path
                            .of("C:\\Users\\Abc\\IdeaProjects\\TJ-Bot-baseRepo\\application\\src\\main\\java\\org\\togetherjava\\tjbot\\commands\\mathcommands\\wolframalpha\\sentImage%d.png"
                                .formatted(++filesAttached))
                            .toFile());

                        action = action.addFile(WolfCommandUtils.imageToBytes(combinedImage),
                                "result%d.png".formatted(++filesAttached));

                        /*
                         * messages.add(channel.sendFile(WolfCommandUtils.imageToBytes(combinedImage
                         * ), name));
                         */
                        /*
                         * bytes.add(WolfCommandUtils.imageToBytes(combinedImage));//
                         * filesAttached++;
                         */
                        embeds.add(embedBuilder
                            .setImage("attachment://result%d.png".formatted(filesAttached))
                            .build());
                    }
                    resultHeight += readImage.getHeight();
                    images.add(readImage);
                    logger.info(
                            "Max Width {} Result Height {} Current Width {} Current Height {} Images {}",
                            maxWidth, resultHeight, readImage.getWidth(), readImage.getHeight(),
                            filesAttached);
                } catch (IOException e) {
                    /*
                     * event.reply("Unable to generate message based on the WolframAlpha response")
                     * .setEphemeral(true) .queue();
                     */
                    logger.error("Failed to read image {} from the WolframAlpha response", image,
                            e);
                    return "Unable to generate message based on the WolframAlpha response";
                }
            }
            logger.info("Exiting pod number {}", i);
        }
        /*
         * for (int i = bytes.size() - 1; i >= 0; i--) { try { ImageIO.write(ImageIO.read(new
         * ByteArrayInputStream(bytes.get(i))), "png", Path .of(
         * "C:\\Users\\Abc\\IdeaProjects\\TJ-Bot-baseRepo\\application\\src\\main\\java\\org\\togetherjava\\tjbot\\commands\\mathcommands\\wolframalpha\\sentImage%d.png"
         * .formatted(i)) .toFile()); } catch (IOException e) { e.printStackTrace(); } action =
         * action.addFile(bytes.get(i), names.get(i)); }
         */
        action.setEmbeds(embeds);
        return "";
    }

    private int getWidth(String header) {
        BufferedImage image = new BufferedImage(100, 100, 6);
        Graphics g = image.getGraphics();
        g.setFont(WOLFRAM_ALPHA_FONT);
        return g.getFontMetrics().stringWidth(header);
    }
}
