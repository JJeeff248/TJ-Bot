package org.togetherjava.tjbot.commands.mathcommands.wolframalpha;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.mikael.urlbuilder.UrlBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageUpdateAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.togetherjava.tjbot.commands.SlashCommandAdapter;
import org.togetherjava.tjbot.commands.SlashCommandVisibility;
import org.togetherjava.tjbot.config.Config;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class WolframAlphaCommand extends SlashCommandAdapter {
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
    private static final Logger logger = LoggerFactory.getLogger(WolframAlphaCommand.class);
    private static final int MAX_IMAGE_HEIGHT_PX = 300;
    private final HttpClient client = HttpClient.newHttpClient();

    public WolframAlphaCommand() {
        super("wolf", "Renders mathematical queries using WolframAlpha",
                SlashCommandVisibility.GUILD);
        getData().addOption(OptionType.STRING, QUERY_OPTION, "the query to send to WolframAlpha",
                true);
    }

    private static byte[] combineImages(List<String> urls, int width, int height)
            throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics imgGraphics = image.getGraphics();
        int resultHeight = 0;
        for (String str : urls) {
            Image resultImage = ImageIO.read(new URL(str));
            imgGraphics.drawImage(resultImage, width, resultHeight, null);
            resultHeight += resultImage.getHeight(null);
        }
        ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
        ImageIO.write(image, "gif", imageStream);
        return imageStream.toByteArray();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {

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
        return HttpRequest
            .newBuilder(UrlBuilder.fromString(API_ENDPOINT)
                .addParameter("appid", Config.getInstance().getWolframAlphaAppId())
                .addParameter("format", "image,plaintext")
                .addParameter("input", query)
                .toUri())
            .GET()
            .build();

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
                .editOriginal("Could not successfully receive the result %s"
                    .formatted(result.getTips().toMessage()))
                .queue();

            // TODO The exact error details have a different POJO structure,
            // POJOs have to be added to get those details. See the Wolfram doc.
            return Optional.empty();
        }
        return Optional.of(result);
    }

    private @NotNull Optional<WebhookMessageUpdateAction<Message>> createResult(
            @NotNull QueryResult result, @NotNull SlashCommandEvent event) {
        WebhookMessageUpdateAction<Message> action =
                event.getHook().editOriginal("Computed in: " + result.getTiming());
        int filesAttached = 0;
        OUTER: for (Pod pod : result.getPods()) {
            List<String> imageURLs = new ArrayList<>();
            int resultHeight = 0;
            for (SubPod subPod : pod.getSubPods()) {
                WolfImage image = subPod.getImage();
                try {
                    String name = image.getTitle();
                    String source = image.getSource();
                    String extension = ".jpg";

                    if (name.isEmpty()) {
                        name = pod.getTitle();
                    }
                    name += extension;
                    if (filesAttached == 10) {
                        break OUTER;
                    }
                    if (resultHeight + image.getHeight() > MAX_IMAGE_HEIGHT_PX) {
                        action = action.addFile(
                                combineImages(imageURLs, image.getWidth(), resultHeight), name);
                        filesAttached++;
                        resultHeight = 0;
                    } else if (subPod == pod.getSubPods().get(pod.getNumberOfSubPods() - 1)) {
                        filesAttached++;
                        action = action.addFile(
                                combineImages(List.of(source), image.getWidth(), image.getHeight()),
                                name);
                    }
                    resultHeight += image.getHeight();
                    imageURLs.add(source);

                } catch (IOException e) {
                    event.reply("Unable to generate message based on the WolframAlpha response")
                        .setEphemeral(true)
                        .queue();
                    logger.error("Failed to read image {} from the WolframAlpha response", image,
                            e);
                    return Optional.empty();
                }
            }
        }
        return Optional.of(action);
    }
}
