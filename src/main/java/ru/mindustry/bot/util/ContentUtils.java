package ru.mindustry.bot.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import ru.mindustry.bot.mindustry.ContentHandler;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static mindustry.graphics.Pal.accent;
import static net.dv8tion.jda.api.entities.Message.Attachment;
import static ru.mindustry.bot.mindustry.ContentHandler.getRequirements;

public class ContentUtils
{
    public static class Schematic
    {
        public byte[] image;
        public EmbedBuilder builder;

        public Schematic(Attachment attachment, Member member) throws ExecutionException, InterruptedException, IOException
        {
            if (!Objects.equals(attachment.getFileExtension(), "msch"))
                throw new IllegalArgumentException("Attachment is not a schematic");

            try (var file = attachment.getProxy().download().get())
            {

                var schematic = ContentHandler.parseSchematic(file);
                var temp = ContentHandler.parseSchematicImage(schematic);

                var embed = new EmbedBuilder()
                        .setTitle(schematic.name())
                        .setDescription(schematic.description())
                        .setAuthor(
                                Objects.requireNonNull(member).getEffectiveName(),
                                attachment.getUrl(),
                                member.getEffectiveAvatarUrl()
                        )
                        .addField("Необходимые ресурсы", getRequirements(schematic), false)
                        .setFooter(schematic.width + "x" + schematic.height + ", " + schematic.tiles.size + " blocks")
                        .setColor(accent.argb8888())
                        .setImage("attachment://image.png");

                this.image = temp;
                this.builder = embed;
            }
        }

        public Schematic(String text, Message message) throws IOException
        {
            var member = message.getMember();

            var schematic = ContentHandler.parseSchematic(text);
            var temp = ContentHandler.parseSchematicImage(schematic);

            var embed = new EmbedBuilder()
                    .setTitle(schematic.name())
                    .setDescription(schematic.description())
                    .setAuthor(
                            Objects.requireNonNull(member).getEffectiveName(),
                            message.getJumpUrl(),
                            member.getEffectiveAvatarUrl()
                    )
                    .addField("Необходимые ресурсы", getRequirements(schematic), false)
                    .setFooter(schematic.width + "x" + schematic.height + ", " + schematic.tiles.size + " blocks")
                    .setColor(accent.argb8888())
                    .setImage("attachment://image.png");

            this.image = temp;
            this.builder = embed;
        }
    }

    public static class Map
    {
        public byte[] image;
        public EmbedBuilder builder;

        public Map(Attachment attachment, Member member) throws ExecutionException, InterruptedException, IOException
        {
            if (!Objects.equals(attachment.getFileExtension(), "msav"))
                throw new IllegalArgumentException("Attachment is not a map");

            try (var file = attachment.getProxy().download().get())
            {
                var map = ContentHandler.parseMap(file);
                var temp = ContentHandler.parseMapImage(map);

                var embed = new EmbedBuilder()
                        .setTitle(map.name())
                        .setDescription(map.description())
                        .setAuthor(
                                Objects.requireNonNull(member).getEffectiveName(),
                                attachment.getUrl(),
                                member.getEffectiveAvatarUrl()
                        )
                        .setFooter(map.width + "x" + map.height)
                        .setColor(accent.argb8888())
                        .setImage("attachment://image.png");

                this.image = temp;
                this.builder = embed;
            }
        }
    }
}
