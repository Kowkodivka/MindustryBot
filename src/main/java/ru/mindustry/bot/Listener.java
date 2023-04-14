package ru.mindustry.bot;

import arc.func.Cons;
import arc.graphics.Color;
import arc.struct.ObjectMap;
import com.github.artbits.quickio.api.Collection;
import com.github.artbits.quickio.api.DB;
import com.github.artbits.quickio.core.QuickIO;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import ru.mindustry.bot.util.ContentUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static arc.graphics.Color.scarlet;
import static arc.util.Strings.getSimpleMessage;
import static arc.util.serialization.Base64Coder.decode;
import static mindustry.Vars.schematicBaseStart;
import static mindustry.graphics.Pal.accent;
import static net.dv8tion.jda.api.interactions.commands.build.Commands.slash;
import static net.dv8tion.jda.api.utils.FileUpload.fromData;
import static ru.mindustry.bot.Vars.*;

public class Listener extends ListenerAdapter
{

    private static final ArrayList<SlashCommandData> rawCommands = new ArrayList<>();
    private static final ObjectMap<SlashCommandData, Cons<SlashCommandInteractionEvent>> commandsData = new ObjectMap<>();

    private static void register(SlashCommandData data, Cons<SlashCommandInteractionEvent> runner)
    {
        commandsData.put(data, runner);
    }

    private static void loadCommands(@NotNull Guild guild)
    {
        commandsData.forEach(command -> rawCommands.add(command.key));
        guild.updateCommands().addCommands(rawCommands).queue();
    }

    public static void registerCommands()
    {
        register(
                slash("map", "Отправить карту в специальный канал").addOption(OptionType.ATTACHMENT, "map", "Карта, которая будет отправлена в специальный канал", true),
                event ->
                {
                    try
                    {
                        var member = event.getMember();
                        var attachment = Objects.requireNonNull(event.getOption("map")).getAsAttachment();

                        var map = new ContentUtils.Map(attachment, member);
                        var embed = map.builder;

                        schematicsChannel
                                .sendMessageEmbeds(embed.build())
                                .addFiles(
                                        fromData(map.image, "image.png"),
                                        fromData(attachment.getProxy().download().get(), attachment.getFileName())
                                )
                                .queue();

                        reply(event, ":map: Успешно", "Карта отправлена в " + mapsChannel.getAsMention(), accent);
                    }
                    catch (IllegalArgumentException e)
                    {
                        reply(event, ":warning: Ошибка", ":link: Необходимо прикрепить файл с расширением **.msav**", scarlet);
                    }
                    catch (ExecutionException | InterruptedException | IOException e)
                    {
                        reply(event, ":warning: Ошибка", getSimpleMessage(e), scarlet);
                    }
                }
        );

        register(
                slash("schematic", "Отправить схему в специальный канал").addOption(OptionType.ATTACHMENT, "schematic", "Схема, которая будет отправлена в специальный канал", true),
                event ->
                {
                    try
                    {
                        var member = event.getMember();
                        var attachment = Objects.requireNonNull(event.getOption("schematic")).getAsAttachment();

                        var schematic = new ContentUtils.Schematic(attachment, member);
                        var embed = schematic.builder;

                        schematicsChannel
                                .sendMessageEmbeds(embed.build())
                                .addFiles(
                                        fromData(schematic.image, "image.png"),
                                        fromData(attachment.getProxy().download().get(), attachment.getFileName())
                                )
                                .queue();

                        reply(event, ":wrench: Успешно", "Схема отправлена в " + schematicsChannel.getAsMention(), accent);
                    }
                    catch (IllegalArgumentException e)
                    {
                        reply(event, ":warning: Ошибка", ":link: Необходимо прикрепить файл с расширением **.msch**", scarlet);
                    }
                    catch (ExecutionException | InterruptedException | IOException e)
                    {
                        reply(event, ":warning: Ошибка", getSimpleMessage(e), scarlet);
                    }
                }
        );

        register(
                slash("warn", "Выдать предупреждение пользователю")
                        .addOption(OptionType.USER, "user", "Пользователь, которому будет выдано предупреждение", true)
                        .addOption(OptionType.STRING, "reason", "Причина, по которой будет выдано предупреждение", true),
                event ->
                {
                    if (Objects.requireNonNull(event.getMember()).getRoles().stream().noneMatch(moderatorRoles::contains))
                    {
                        reply(event, ":raised_hand: Недостаточно прав для использования команды", scarlet);
                        return;
                    }

                    var user = Objects.requireNonNull(event.getOption("user")).getAsUser();
                    var reason = Objects.requireNonNull(event.getOption("reason")).getAsString();

                    try (DB storage = QuickIO.usingDB("mindustry_bot_db"))
                    {
                        Collection<Warning> warnings = storage.collection(Warning.class);
                        var warning = new Warning(user.getId(), reason, event.getMember());
                        warnings.save(warning);

                        if (warnings.findAll().stream().filter(object -> Objects.equals(object.memberId, user.getId())).count() >= warningsLimit)
                        {
                            guild.ban(user, 0, TimeUnit.SECONDS).reason("Достигнут лимит предупреждений. Последнее #" + warning.id).queue();
                        }
                    }

                    reply(event, ":pencil: Предупреждение выдано", accent);
                });

        register(
                slash("unwarn", "Убрать предупреждение пользователю").addOption(OptionType.INTEGER, "id", "ID предупреждения"),
                event ->
                {
                    if (Objects.requireNonNull(event.getMember()).getRoles().stream().noneMatch(moderatorRoles::contains))
                    {
                        reply(event, ":raised_hand: Недостаточно прав для использования команды", scarlet);
                        return;
                    }

                    var id = Objects.requireNonNull(event.getOption("id")).getAsInt();

                    try (DB storage = QuickIO.usingDB("mindustry_bot_db"))
                    {
                        Collection<Warning> warnings = storage.collection(Warning.class);
                        warnings.delete(warning -> warning.id == id);
                    }

                    reply(event, ":pencil: Предупреждение удалено", accent);
                });

        register(
                slash("warnings", "Просмотреть предупреждения пользователя").addOption(OptionType.USER, "user", "Пользователь, у которого необходимо просмотреть предупреждения", true),
                event ->
                {
                    var user = Objects.requireNonNull(event.getOption("user")).getAsUser();

                    try (DB storage = QuickIO.usingDB("mindustry_bot_db"))
                    {
                        Collection<Warning> warnings = storage.collection(Warning.class);

                        var embed = new EmbedBuilder();
                        var foundWarnings = warnings.find(warning -> warning.memberId.equals(user.getId()));

                        embed
                                .setTitle(":ledger: Предупреждения пользователя " + user.getAsTag())
                                .setColor(accent.argb8888());

                        foundWarnings.forEach(warning ->
                                embed.addField(
                                        "#" + warning.id,
                                        "Выдано: " + warning.moderator + "\nПо причине:\n```" + warning.reason + "```",
                                        true
                                ));

                        if (foundWarnings.size() == 0)
                        {
                            embed.setDescription("```Предупреждения отсутствуют```");
                        }

                        event.replyEmbeds(embed.build()).queue();
                    }
                });

        register(
                slash("mywarnings", "Просмотреть свои предупреждения"),
                event ->
                {
                    try (DB storage = QuickIO.usingDB("mindustry_bot_db"))
                    {
                        Collection<Warning> warnings = storage.collection(Warning.class);

                        var member = event.getMember();
                        var embed = new EmbedBuilder();
                        var foundWarnings = warnings.find(warning -> warning.memberId.equals(Objects.requireNonNull(member).getId()));

                        embed
                                .setTitle(":ledger: Предупреждения пользователя " + Objects.requireNonNull(member).getUser().getAsTag())
                                .setColor(accent.argb8888());

                        foundWarnings.forEach(warning ->
                                embed.addField(
                                        "#" + warning.id,
                                        "Выдано: " + warning.moderator + "\nПо причине:\n```" + warning.reason + "```",
                                        true
                                ));

                        if (foundWarnings.size() == 0)
                        {
                            embed.setDescription("```Предупреждения отсутствуют```");
                        }

                        event.replyEmbeds(embed.build()).queue();
                    }
                });

        loadCommands(guild);
    }

    private static void reply(SlashCommandInteractionEvent event, String title, Color color)
    {
        event
                .replyEmbeds(new EmbedBuilder().setTitle(title).setColor(color.argb8888()).build())
                .setEphemeral(true)
                .queue();
    }

    private static void reply(SlashCommandInteractionEvent event, String title, String description, Color color)
    {
        event
                .replyEmbeds(new EmbedBuilder().setTitle(title).setDescription(description).setColor(color.argb8888()).build())
                .setEphemeral(true)
                .queue();
    }

    private static void reply(MessageReceivedEvent event, String title, String description, Color color)
    {
        event
                .getMessage()
                .replyEmbeds(new EmbedBuilder().setTitle(title).setDescription(description).setColor(color.argb8888()).build())
                .queue();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event)
    {
        commandsData.forEach(command ->
        {
            if (command.key.getName().equals(event.getName())) command.value.get(event);
        });
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        if (event.getAuthor().isBot()) return;

        var message = event.getMessage();
        var raw = message.getContentRaw();
        var attachments = message.getAttachments();

        if (raw.startsWith(schematicBaseStart))
        {
            try
            {
                var schematic = new ContentUtils.Schematic(raw, message);
                var embed = schematic.builder;

                message
                        .replyEmbeds(embed.build())
                        .addFiles(
                                fromData(schematic.image, "image.png"),
                                fromData(decode(raw), "schematic.msch")
                        )
                        .queue();
            }
            catch (IOException e)
            {
                reply(event, ":warning: Ошибка", getSimpleMessage(e), scarlet);
            }
        }

        if (attachments.size() > 0 && attachments.size() <= 4)
        {
            attachments
                    .stream()
                    .filter(attachment -> Objects.equals(attachment.getFileExtension(), "msch"))
                    .forEach(attachment ->
                    {
                        try
                        {
                            var schematic = new ContentUtils.Schematic(attachment, event.getMember());
                            var embed = schematic.builder;

                            message
                                    .replyEmbeds(embed.build())
                                    .addFiles(fromData(schematic.image, "image.png"));
                        }
                        catch (ExecutionException | InterruptedException | IOException e)
                        {
                            reply(event, ":warning: Ошибка", getSimpleMessage(e), scarlet);
                        }
                    });
        }
    }
}
