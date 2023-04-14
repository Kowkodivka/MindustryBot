package ru.mindustry.bot;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.util.serialization.Json;
import com.github.artbits.quickio.core.IOEntity;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import ru.mindustry.bot.util.ConfigUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class Vars
{
    public static final Json json = new Json();

    public static final Fi dataDirectory = Fi.get(".mindustry");
    public static final Fi cache = dataDirectory.child("cache");
    public static final Fi resources = dataDirectory.child("resources");
    public static final Fi sprites = dataDirectory.child("sprites");

    public static final ObjectMap<String, BufferedImage> regions = new ObjectMap<>();

    public static ConfigUtils.Config config;
    public static JDA jda;
    public static TextChannel mapsChannel, schematicsChannel;
    public static Guild guild, emojiGuild;
    public static ArrayList<Role> moderatorRoles = new ArrayList<>();

    public static BufferedImage currentImage;
    public static Graphics2D currentGraphics;

    public static int warningsLimit;

    public static class Warning extends IOEntity
    {
        public int id;
        public String memberId;
        public String reason;
        public String moderator;

        public Warning(String memberId, String reason, Member member)
        {
            this.id = 1000 + new Random().nextInt(9000);
            this.memberId = memberId;
            this.reason = reason;
            this.moderator = member.getAsMention();
        }
    }
}
