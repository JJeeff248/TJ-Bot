package org.togetherjava.tjbot.commands.mathcommands.wolframalpha;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.mikael.urlbuilder.UrlBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
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
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class WolframAlphaCommand extends SlashCommandAdapter {
    public static final Logger logger = LoggerFactory.getLogger(WolframAlphaCommand.class);
    private static final int HTTP_STATUS_CODE_OK = 200;
    private static final XmlMapper XML = new XmlMapper();
    private static final String QUERY_OPTION = "query";
    /**
     * WolframAlpha API endpoint to connect to.
     *
     * @see <a href=
     * "https://products.wolframalpha.com/docs/WolframAlpha-API-Reference.pdf">WolframAlpha API
     * Reference</a>.
     */
    private static final String API_ENDPOINT = "http://api.wolframalpha.com/v2/query";
    private static final int MAX_IMAGE_HEIGHT_PX = 300;
    /**
     * WolframAlpha text Color
     */
    private static final Color WOLFRAM_ALPHA_TEXT_COLOR = Color.decode("#3C3C3C");
    /**
     * WolframAlpha Font
     */
    private static final Font WOLFRAM_ALPHA_FONT = new Font("Times", Font.PLAIN, 15).deriveFont(
            Map.of(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON));
    private final HttpClient client = HttpClient.newHttpClient();

    public WolframAlphaCommand() {
        super("wolf", "Renders mathematical queries using WolframAlpha",
                SlashCommandVisibility.GUILD);
        getData().addOption(OptionType.STRING, QUERY_OPTION, "the query to send to WolframAlpha",
                true);
    }

    @Override public void onSlashCommand(@NotNull SlashCommandEvent event) {

        // The processing takes some time
        event.deferReply().queue();
        String query = Objects.requireNonNull(event.getOption(QUERY_OPTION)).getAsString();
        HttpRequest request = sendQuery(query);
        Optional<HttpResponse<String>> optResponse = getResponse(event, request);
        if (optResponse.isEmpty())
            return;
        HttpResponse<String> response = optResponse.get();
        Optional<QueryResult> optResult = parseQuery(response, event);
        if (optResult.isEmpty())
            return;
        QueryResult result = optResult.get();
        Optional<WebhookMessageUpdateAction<Message>> optAction = createResult(result, event);
        optAction.ifPresent(RestAction::queue);
    }

    private @NotNull Optional<HttpResponse<String>> getResponse(@NotNull SlashCommandEvent event,
            @NotNull HttpRequest request) {
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            event.getHook()
                    .setEphemeral(true)
                    .editOriginal("Unable to get a response from WolframAlpha API")
                    .queue();
            logger.warn("Could not get the response from the server", e);
            return Optional.empty();
        } catch (InterruptedException e) {
            event.getHook()
                    .setEphemeral(true)
                    .editOriginal("Connection to WolframAlpha was interrupted")
                    .queue();
            logger.info("Connection to WolframAlpha was interrupted", e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }

        if (response.statusCode() != HTTP_STATUS_CODE_OK) {
            event.getHook()
                    .setEphemeral(true)
                    .editOriginal("The response' status code was incorrect")
                    .queue();
            logger.warn("Unexpected status code: Expected: {} Actual: {}", HTTP_STATUS_CODE_OK,
                    response.statusCode());
            return Optional.empty();
        }
        return Optional.of(response);
    }

    private @NotNull HttpRequest sendQuery(@NotNull String query) {
        return HttpRequest.newBuilder(UrlBuilder.fromString(API_ENDPOINT)
                .addParameter("appid", Config.getInstance().getWolframAlphaAppId())
                .addParameter("format", "image,plaintext")
                .addParameter("input", query)
                .toUri()).GET().build();

    }

    private @NotNull Optional<QueryResult> parseQuery(@NotNull HttpResponse<String> response,
            @NotNull SlashCommandEvent event) {
        QueryResult result;
        try {
            result = XML.readValue(response.body(), QueryResult.class);
        } catch (JsonProcessingException e) {
            event.getHook()
                    .setEphemeral(true)
                    .editOriginal("Unexpected response from WolframAlpha API")
                    .queue();
            logger.error("Unable to deserialize the class ", e);
            return Optional.empty();
        }

        if (!result.isSuccess()) {
            event.getHook()
                    .setEphemeral(true)
                    .editOriginal("Could not successfully receive the result %s".formatted(
                            result.getTips().toMessage()))
                    .queue();

            // TODO The exact error details have a different POJO structure,
            // POJOs have to be added to get those details. See the Wolfram doc.
            return Optional.empty();
        }
        return Optional.of(result);
    }

    private @NotNull Optional<List<MessageAction>> createImages(SlashCommandEvent event,
            QueryResult result) {
        MessageChannel channel = event.getChannel();
        List<MessageAction> messages = new ArrayList<>();
        // For each probably much more readable.
        try {
            result.getPods()
                    .forEach(pod -> pod.getSubPods()
                            .stream()
                            .map(SubPod::getImage)
                            .forEach(image -> {
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
                                    logger.error(
                                            "Failed to read image {} from the WolframAlpha response",
                                            image, e);
                                    throw new Error();
                                }
                            }));
        } catch (Error e) {
            return Optional.empty();
        }
        return Optional.of(messages);
    }

    private @NotNull Optional<WebhookMessageUpdateAction<Message>> createResult(
            @NotNull QueryResult result, @NotNull SlashCommandEvent event) {

        WebhookMessageUpdateAction<Message> action =
                event.getHook().editOriginal("Computed in " + result.getTiming());
        int filesAttached = 0;
        int resultHeight = 0;
        int maxWidth = Integer.MIN_VALUE;
        List<BufferedImage> images = new ArrayList<>();
        List<byte[]> bytes = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<Pod> pods = new ArrayList<>(result.getPods());
        logger.info("There are {} pods", pods.size());
        OUTER:
        for (int i = 0; i < pods.size(); ) {
            Pod pod = pods.get(i);
            List<SubPod> subPods = new ArrayList<>(pod.getSubPods());
            logger.info("pod number {}", ++i);
            logger.info("There are {} sub pods within this pod", subPods.size());
            for (int j = 0; j < subPods.size(); ) {

                SubPod subPod = subPods.get(j);
                logger.info("sub pod number {}", ++j);
                WolfImage image = subPod.getImage();
                try {
                    String name = image.getTitle();
                    String source = image.getSource();
                    String extension = ".png";
                    String header = pod.getTitle();
                    BufferedImage readImage =
                            new BufferedImage(image.getWidth(), image.getHeight() + 20,
                                    BufferedImage.TYPE_4BYTE_ABGR);
                    Graphics graphics = readImage.getGraphics();
                    graphics.setFont(WOLFRAM_ALPHA_FONT);
                    graphics.setColor(Color.WHITE);
                    graphics.setColor(WOLFRAM_ALPHA_TEXT_COLOR);
                    graphics.drawString(header, 10, 10);
                    graphics.drawImage(ImageIO.read(new URL(source)), 0, 20, null);

                    if (name.isEmpty()) {
                        name = header;
                    }
                    name += extension;
                    maxWidth = Math.max(maxWidth, image.getWidth());
                    // FIXME get the attachments <= 10 or return a collection
                    if (filesAttached == 10) {
                        break OUTER;
                    }


                    if (resultHeight + image.getHeight() > MAX_IMAGE_HEIGHT_PX) {
                        byte[] arr = WolfCommandUtils.imageToBytes(
                                WolfCommandUtils.combineImages(images, maxWidth, resultHeight));
                        images.clear();
                        bytes.add(arr);
                        names.add(name);
                        filesAttached++;
                        resultHeight = 0;
                        maxWidth = Integer.MIN_VALUE;
                    } else if (pod == pods.get(pods.size() - 1) && subPod == subPods.get(
                            subPods.size() - 1)) {
                        logger.info("The last image");
                        byte[] arr = WolfCommandUtils.imageToBytes(
                                WolfCommandUtils.combineImages(images, maxWidth, resultHeight));
                        filesAttached++;
                        bytes.add(arr);
                        names.add(name);
                    }
                    resultHeight += image.getHeight();
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
        for (int i = bytes.size() - 1; i >= 0; i--) {
            action = action.addFile(bytes.get(i), names.get(i));
        }
        return Optional.of(action);
    }
}
