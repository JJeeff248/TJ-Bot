package org.togetherjava.tjbot.commands.mathcommands.wolframalpha;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.mikael.urlbuilder.UrlBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageUpdateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import org.jetbrains.annotations.NotNull;
import org.togetherjava.tjbot.commands.SlashCommandAdapter;
import org.togetherjava.tjbot.commands.SlashCommandVisibility;
import org.togetherjava.tjbot.config.Config;

import java.io.IOException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

public final class WolframAlphaCommand extends SlashCommandAdapter {
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
    public static final int HTTP_STATUS_CODE_OK = 200;

    private final HttpClient client = HttpClient.newHttpClient();

    public WolframAlphaCommand() {
        super("wolframalpha", "Renders mathematical queries using WolframAlpha",
                SlashCommandVisibility.GUILD);
        getData().addOption(OptionType.STRING, QUERY_OPTION, "the query to send to WolframAlpha",
                true);
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        String query = Objects.requireNonNull(event.getOption(QUERY_OPTION)).getAsString();

        // Send query
        HttpRequest request = HttpRequest
            .newBuilder(UrlBuilder.fromString(API_ENDPOINT)
                .addParameter("appid", Config.getInstance().getWolframAlphaAppId())
                .addParameter("format", "image,plaintext")
                .addParameter("input", query)
                .toUri())
            .GET()
            .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            // TODO Respond to user and possibly also log something
            e.printStackTrace(); // TODO Remove me afterwards
            return;
        } catch (InterruptedException e) {
            // TODO Respond to user and possibly also log something
            e.printStackTrace(); // TODO Remove me afterwards
            Thread.currentThread().interrupt();
            return;
        }

        if (response.statusCode() != HTTP_STATUS_CODE_OK) {
            // TODO Respond to user and also log something, errors can be extracted from the result
            System.err.println("not OK"); // TODO Remove me afterwards
            return;
        }

        // Parse query
        QueryResult result;
        try {
            result = XML.readValue(response.body(), QueryResult.class);
        } catch (JsonProcessingException e) {
            // TODO Respond to user and also log something
            e.printStackTrace(); // TODO Remove me afterwards
            return;
        }

        if (!result.isSuccess()) {
            // TODO Respond to user and also log something, errors can be extracted from the result
            System.err.println("not successful"); // TODO Remove me afterwards

            // TODO The exact error details have a different POJO structure,
            // POJOs have to be added to get those details. See the Wolfram doc.
            return;
        }

        // Building takes a bit since we have to read all the images
        event.deferReply().queue();

        // Create result
        WebhookMessageUpdateAction<Message> action =
                event.getHook().editOriginal("Computed in: " + result.getTiming());
        for (Pod pod : result.getPods()) {
            for (SubPod subPod : pod.getSubPods()) {
                Image image = subPod.getImage();
                try {
                    String name = image.getTitle();
                    if (name.isEmpty()) {
                        name = pod.getTitle();
                    }
                    // TODO Figure out how to tell JDA that those are gifs (but sometimes also JPEG,
                    // see Wolfram doc)
                    action = action.addFile(new URL(image.getSource()).openStream(), name);
                } catch (IOException e) {
                    // TODO Respond to user and also log something
                    e.printStackTrace(); // TODO Remove me afterwards
                    return;
                }
            }
        }
        action.queue();
    }
}
