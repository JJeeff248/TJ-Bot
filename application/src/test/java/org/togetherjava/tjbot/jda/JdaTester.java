package org.togetherjava.tjbot.jda;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.Response;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.dv8tion.jda.api.utils.AttachmentOption;
import net.dv8tion.jda.api.utils.ConcurrentSessionController;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.*;
import net.dv8tion.jda.internal.requests.Requester;
import net.dv8tion.jda.internal.requests.restaction.AuditableRestActionImpl;
import net.dv8tion.jda.internal.requests.restaction.MessageActionImpl;
import net.dv8tion.jda.internal.requests.restaction.interactions.ReplyActionImpl;
import net.dv8tion.jda.internal.utils.config.AuthorizationConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import org.togetherjava.tjbot.commands.SlashCommand;
import org.togetherjava.tjbot.commands.componentids.ComponentIdGenerator;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Mockito.*;

/**
 * Utility class for testing {@link SlashCommand}s.
 * <p>
 * Mocks JDA and can create events that can be used to test {@link SlashCommand}s, e.g.
 * {@link #createSlashCommandEvent(SlashCommand)}. The created events are Mockito mocks, which can
 * be exploited for testing.
 * <p>
 * An example test using this class might look like:
 *
 * <pre>
 * {
 *     &#64;code
 *     SlashCommand command = new PingCommand();
 *     JdaTester jdaTester = new JdaTester();
 *
 *     SlashCommandEvent event = jdaTester.createSlashCommandEvent(command).build();
 *     command.onSlashCommand(event);
 *
 *     verify(event).reply("Pong!");
 * }
 * </pre>
 */
public final class JdaTester {
    private static final ScheduledExecutorService GATEWAY_POOL = new ScheduledThreadPoolExecutor(4);
    private static final ScheduledExecutorService RATE_LIMIT_POOL =
            new ScheduledThreadPoolExecutor(4);
    private static final String TEST_TOKEN = "TEST_TOKEN";
    private static final long USER_ID = 1;
    private static final long SELF_USER_ID = 2;
    private static final long APPLICATION_ID = 1;
    private static final long PRIVATE_CHANNEL_ID = 1;
    private static final long GUILD_ID = 1;
    private static final long TEXT_CHANNEL_ID = 1;

    private final JDAImpl jda;
    private final MemberImpl member;
    private final GuildImpl guild;
    private final ReplyActionImpl replyAction;
    private final AuditableRestActionImpl<Void> auditableRestAction;
    private final MessageActionImpl messageAction;
    private final TextChannelImpl textChannel;
    private final PrivateChannelImpl privateChannel;

    /**
     * Creates a new instance. The instance uses a fresh and isolated mocked JDA setup.
     * <p>
     * Reusing this instance also means to reuse guilds, text channels and such from this JDA setup,
     * which can have an impact on tests. For example a previous text that already send messages to
     * a channel, the messages will then still be present in the instance.
     */
    @SuppressWarnings("unchecked")
    public JdaTester() {
        // TODO Extend this functionality, make it nicer.
        // Maybe offer a builder for multiple users and channels and what not
        jda = mock(JDAImpl.class);
        when(jda.getCacheFlags()).thenReturn(EnumSet.noneOf(CacheFlag.class));

        SelfUserImpl selfUser = spy(new SelfUserImpl(SELF_USER_ID, jda));
        UserImpl user = spy(new UserImpl(USER_ID, jda));
        guild = spy(new GuildImpl(jda, GUILD_ID));
        Member selfMember = spy(new MemberImpl(guild, selfUser));
        member = spy(new MemberImpl(guild, user));
        textChannel = spy(new TextChannelImpl(TEXT_CHANNEL_ID, guild));
        privateChannel = spy(new PrivateChannelImpl(PRIVATE_CHANNEL_ID, user));
        messageAction = mock(MessageActionImpl.class);
        EntityBuilder entityBuilder = mock(EntityBuilder.class);
        Role everyoneRole = new RoleImpl(GUILD_ID, guild);

        when(entityBuilder.createUser(any())).thenReturn(user);
        when(entityBuilder.createMember(any(), any())).thenReturn(member);
        doReturn(true).when(member).hasPermission(any(Permission.class));
        doReturn(true).when(member).hasPermission(any(GuildChannel.class), any(Permission.class));
        doReturn(true).when(selfMember).hasPermission(any(Permission.class));
        doReturn(true).when(selfMember)
            .hasPermission(any(GuildChannel.class), any(Permission.class));

        doReturn(String.valueOf(APPLICATION_ID)).when(selfUser).getApplicationId();
        doReturn(APPLICATION_ID).when(selfUser).getApplicationIdLong();
        doReturn(selfUser).when(jda).getSelfUser();
        when(jda.getGuildChannelById(anyLong())).thenReturn(textChannel);
        when(jda.getPrivateChannelById(anyLong())).thenReturn(privateChannel);
        when(jda.getGuildById(anyLong())).thenReturn(guild);
        when(jda.getEntityBuilder()).thenReturn(entityBuilder);

        when(jda.getGatewayPool()).thenReturn(GATEWAY_POOL);
        when(jda.getRateLimitPool()).thenReturn(RATE_LIMIT_POOL);
        when(jda.getSessionController()).thenReturn(new ConcurrentSessionController());
        doReturn(new Requester(jda, new AuthorizationConfig(TEST_TOKEN))).when(jda).getRequester();
        when(jda.getAccountType()).thenReturn(AccountType.BOT);

        doReturn(messageAction).when(privateChannel).sendMessage(anyString());

        replyAction = mock(ReplyActionImpl.class);
        when(replyAction.setEphemeral(anyBoolean())).thenReturn(replyAction);
        when(replyAction.addActionRow(anyCollection())).thenReturn(replyAction);
        when(replyAction.addActionRow(ArgumentMatchers.<Component>any())).thenReturn(replyAction);
        when(replyAction.setContent(anyString())).thenReturn(replyAction);
        when(replyAction.addFile(any(byte[].class), any(String.class), any(AttachmentOption.class)))
            .thenReturn(replyAction);
        doNothing().when(replyAction).queue();

        auditableRestAction = (AuditableRestActionImpl<Void>) mock(AuditableRestActionImpl.class);
        doNothing().when(auditableRestAction).queue();

        doNothing().when(messageAction).queue();

        doReturn(everyoneRole).when(guild).getPublicRole();
        doReturn(selfMember).when(guild).getMember(selfUser);
        doReturn(member).when(guild).getMember(not(eq(selfUser)));

        doReturn(null).when(textChannel).retrieveMessageById(any());
    }

    /**
     * Creates a Mockito mocked slash command event, which can be used for
     * {@link SlashCommand#onSlashCommand(SlashCommandEvent)}.
     * <p>
     * The method creates a builder that can be used to further adjust the event before creation,
     * e.g. provide options.
     *
     * @param command the command to create an event for
     * @return a builder used to create a Mockito mocked slash command event
     */
    public @NotNull SlashCommandEventBuilder createSlashCommandEvent(
            @NotNull SlashCommand command) {
        UnaryOperator<SlashCommandEvent> mockOperator = event -> {
            SlashCommandEvent slashCommandEvent = spy(event);
            mockInteraction(slashCommandEvent);
            return slashCommandEvent;
        };

        return new SlashCommandEventBuilder(jda, mockOperator).setCommand(command)
            .setToken(TEST_TOKEN)
            .setChannelId(String.valueOf(TEXT_CHANNEL_ID))
            .setApplicationId(String.valueOf(APPLICATION_ID))
            .setGuildId(String.valueOf(GUILD_ID))
            .setUserId(String.valueOf(USER_ID))
            .setUserWhoTriggered(member);
    }

    /**
     * Creates a Mockito mocked button click event, which can be used for
     * {@link SlashCommand#onButtonClick(ButtonClickEvent, List)}.
     * <p>
     * The method creates a builder that can be used to further adjust the event before creation,
     * e.g. provide options.
     *
     * @return a builder used to create a Mockito mocked slash command event
     */
    public @NotNull ButtonClickEventBuilder createButtonClickEvent() {
        Supplier<ButtonClickEvent> mockEventSupplier = () -> {
            ButtonClickEvent event = mock(ButtonClickEvent.class);
            mockButtonClickEvent(event);
            return event;
        };

        UnaryOperator<Message> mockMessageOperator = event -> {
            Message message = spy(event);
            mockMessage(message);
            return message;
        };

        return new ButtonClickEventBuilder(mockEventSupplier, mockMessageOperator)
            .setUserWhoClicked(member);
    }

    /**
     * Creates a Mockito spy on the given slash command.
     * <p>
     * The spy is also prepared for mocked execution, e.g. attributes such as
     * {@link SlashCommand#acceptComponentIdGenerator(ComponentIdGenerator)} are filled with mocks.
     *
     * @param command the command to spy on
     * @param <T> the type of the command to spy on
     * @return the created spy
     */
    public <T extends SlashCommand> @NotNull T spySlashCommand(@NotNull T command) {
        T spiedCommand = spy(command);
        spiedCommand
            .acceptComponentIdGenerator((componentId, lifespan) -> UUID.randomUUID().toString());
        return spiedCommand;
    }

    /**
     * Creates a Mockito spy for a member with the given user id.
     *
     * @param userId the id of the member to create
     * @return the created spy
     */
    public @NotNull Member createMemberSpy(long userId) {
        UserImpl user = spy(new UserImpl(userId, jda));
        return spy(new MemberImpl(guild, user));
    }

    /**
     * Gets the Mockito mock used as universal reply action by all mocks created by this tester
     * instance.
     * <p>
     * For example the events created by {@link #createSlashCommandEvent(SlashCommand)} will return
     * this mock on several of their methods.
     *
     * @return the reply action mock used by this tester
     */
    public @NotNull ReplyAction getReplyActionMock() {
        return replyAction;
    }

    /**
     * Gets the text channel spy used as universal text channel by all mocks created by this tester
     * instance.
     * <p>
     * For example the events created by {@link #createSlashCommandEvent(SlashCommand)} will return
     * this spy on several of their methods.
     *
     * @return the text channel spy used by this tester
     */
    public @NotNull TextChannel getTextChannelSpy() {
        return textChannel;
    }

    /**
     * Creates a mocked action that always succeeds and consumes the given object.
     * <p>
     * Such an action is useful for testing things involving calls like
     * {@link TextChannel#retrieveMessageById(long)} or similar, example:
     * 
     * <pre>
     * {
     *     &#64;code
     *     var jdaTester = new JdaTester();
     *
     *     var message = new MessageBuilder("Hello World!").build();
     *     var action = jdaTester.createSucceededActionMock(message);
     *
     *     doReturn(action).when(jdaTester.getTextChannelSpy()).retrieveMessageById("1");
     * }
     * </pre>
     * 
     * @param t the object to consume on success
     * @param <T> the type of the object to consume
     * @return the mocked action
     */
    @SuppressWarnings("unchecked")
    public <T> @NotNull RestAction<T> createSucceededActionMock(@Nullable T t) {
        RestAction<T> action = (RestAction<T>) mock(RestAction.class);

        Answer<Void> successExecution = invocation -> {
            Consumer<? super T> successConsumer = invocation.getArgument(0);
            successConsumer.accept(t);
            return null;
        };

        doNothing().when(action).queue();

        doAnswer(successExecution).when(action).queue(any());
        doAnswer(successExecution).when(action).queue(any(), any());

        return action;
    }

    /**
     * Creates a mocked action that always fails and consumes the given failure reason.
     * <p>
     * Such an action is useful for testing things involving calls like
     * {@link TextChannel#retrieveMessageById(long)} or similar, example:
     * 
     * <pre>
     * {
     *     &#64;code
     *     var jdaTester = new JdaTester();
     *
     *     var reason = new FooException();
     *     var action = jdaTester.createFailedActionMock(reason);
     *
     *     doReturn(action).when(jdaTester.getTextChannelSpy()).retrieveMessageById("1");
     * }
     * </pre>
     * 
     * @param failureReason the reason to consume on failure
     * @param <T> the type of the object the action would contain if it would succeed
     * @return the mocked action
     */
    @SuppressWarnings("unchecked")
    public <T> @NotNull RestAction<T> createFailedActionMock(@NotNull Throwable failureReason) {
        RestAction<T> action = (RestAction<T>) mock(RestAction.class);

        Answer<Void> failureExecution = invocation -> {
            Consumer<? super Throwable> failureConsumer = invocation.getArgument(1);
            failureConsumer.accept(failureReason);
            return null;
        };

        doNothing().when(action).queue();
        doNothing().when(action).queue(any());

        doAnswer(failureExecution).when(action).queue(any(), any());

        return action;
    }

    /**
     * Creates an exception used by JDA on failure in most calls to the Discord API.
     * <p>
     * The exception merely wraps around the given reason and has no valid error code or message
     * set.
     * 
     * @param reason the reason of the error
     * @return the created exception
     */
    public @NotNull ErrorResponseException createErrorResponseException(
            @NotNull ErrorResponse reason) {
        return ErrorResponseException.create(reason, new Response(null, -1, "", -1, Set.of()));
    }

    private void mockInteraction(@NotNull Interaction interaction) {
        doReturn(replyAction).when(interaction).reply(anyString());
        doReturn(replyAction).when(interaction).replyEmbeds(ArgumentMatchers.<MessageEmbed>any());
        doReturn(replyAction).when(interaction).replyEmbeds(anyCollection());

        doReturn(member).when(interaction).getMember();
        doReturn(member.getUser()).when(interaction).getUser();

        doReturn(textChannel).when(interaction).getChannel();
        doReturn(textChannel).when(interaction).getMessageChannel();
        doReturn(textChannel).when(interaction).getTextChannel();
        doReturn(textChannel).when(interaction).getGuildChannel();
        doReturn(privateChannel).when(interaction).getPrivateChannel();
    }

    private void mockButtonClickEvent(@NotNull ButtonClickEvent event) {
        mockInteraction(event);

        doReturn(replyAction).when(event).editButton(any());
    }

    private void mockMessage(@NotNull Message message) {
        doReturn(messageAction).when(message).reply(anyString());
        doReturn(messageAction).when(message).replyEmbeds(ArgumentMatchers.<MessageEmbed>any());
        doReturn(messageAction).when(message).replyEmbeds(anyCollection());

        doReturn(auditableRestAction).when(message).delete();

        doReturn(auditableRestAction).when(message).addReaction(any(Emote.class));
        doReturn(auditableRestAction).when(message).addReaction(any(String.class));

        doReturn(member).when(message).getMember();
        doReturn(member.getUser()).when(message).getAuthor();
    }
}
