package ru.mindustry.bot;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import ru.mindustry.bot.util.ConfigUtils;
import ru.mindustry.bot.util.ResourceUtils;

import java.util.Arrays;
import java.util.EnumSet;

import static arc.util.Log.err;
import static net.dv8tion.jda.api.entities.Message.MentionType.CHANNEL;
import static net.dv8tion.jda.api.entities.Message.MentionType.EMOJI;
import static net.dv8tion.jda.api.requests.GatewayIntent.*;
import static ru.mindustry.bot.Vars.*;

public class Main
{

    public static void main(String[] args)
    {
        cache.delete();

        dataDirectory.mkdirs();
        cache.mkdirs();
        resources.mkdirs();
        sprites.mkdirs();

        ConfigUtils.init();
        ResourceUtils.init();

        RestAction.setDefaultFailure(null);
        MessageRequest.setDefaultMentions(EnumSet.of(CHANNEL, EMOJI));

        try
        {
            jda =
                    JDABuilder
                            .createLight(config.token)
                            .enableIntents(GUILD_MEMBERS, MESSAGE_CONTENT, GUILD_EMOJIS_AND_STICKERS)
                            .enableCache(CacheFlag.EMOJI)
                            .addEventListeners(new Listener())
                            .build()
                            .awaitReady();

            // These variables imported from Vars with *
            guild = jda.getGuildById(config.guildId);
            emojiGuild = jda.getGuildById(config.emojiGuildId);
            logsChannel = jda.getTextChannelById(config.logsChannel);
            mapsChannel = jda.getTextChannelById(config.mapsChannelId);
            schematicsChannel = jda.getTextChannelById(config.schematicsChannelId);

            warningsLimit = config.warningsLimit;
            Arrays.stream(config.moderatorRoles).forEach(id -> moderatorRoles.add(jda.getRoleById(id)));
        }
        catch (Exception e)
        {
            err("Failed to launch the bot. Make sure the provided token and guild/channel IDs in the configuration are correct.");
            err(e);
        }

        Listener.registerCommands();
    }
}
