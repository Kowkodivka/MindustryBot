package ru.mindustry.bot.mindustry;

import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.graphics.PixmapIO.PngWriter;
import arc.graphics.g2d.Draw;
import arc.util.io.Streams.OptimizedByteArrayOutputStream;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Schematic;
import mindustry.game.Schematics;
import mindustry.graphics.Drawf;
import mindustry.io.MapIO;
import mindustry.maps.Map;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

import static arc.util.io.Streams.emptyBytes;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static ru.mindustry.bot.Vars.*;

public class ContentHandler
{
    public static String getRequirements(Schematic schematic)
    {
        var builder = new StringBuilder();
        schematic.requirements().each((item, amount) -> builder.append(emojiGuild.getEmojisByName(item.name, true).get(0).getAsMention()).append(amount).append(" "));
        return builder.toString();
    }

    public static Map parseMap(InputStream stream) throws IOException
    {
        var file = Fi.tempFile("map");
        file.writeBytes(stream.readAllBytes());
        return MapIO.createMap(file, true);
    }

    public static byte[] parseMapImage(Map map) throws IOException
    {
        return parseImage(MapIO.generatePreview(map));
    }

    public static Schematic parseSchematic(InputStream stream) throws IOException
    {
        return Schematics.read(stream);
    }

    public static byte[] parseSchematicImage(Schematic schematic)
    {
        var image = new BufferedImage(schematic.width * 32 + 64, schematic.height * 32 + 64, TYPE_INT_ARGB);
        var plans = schematic.tiles.map(stile -> new BuildPlan(stile.x + 1, stile.y + 1, stile.rotation, stile.block, stile.config));

        currentImage = image;
        currentGraphics = image.createGraphics();

        Draw.reset();

        for (int x = 0; x < schematic.width + 2; x++)
            for (int y = 0; y < schematic.height + 2; y++)
                Draw.rect(
                        "metal-floor",
                        x * 8f,
                        y * 8f
                );

        plans.each(plan -> Drawf.squareShadow(plan.drawx(), plan.drawy(), plan.block.size * 16f, 1f));

        plans.each(plan ->
        {
            plan.animScale = 1f;
            plan.worldContext = false;
            plan.block.drawPlanRegion(plan, plans);
            Draw.reset();
        });

        plans.each(plan -> plan.block.drawPlanConfigTop(plan, plans));

        return parseImage(image);
    }

    private static byte[] parseImage(Pixmap pixmap)
    {
        var writer = new PngWriter(pixmap.width * pixmap.height);
        var stream = new OptimizedByteArrayOutputStream(pixmap.width * pixmap.height);

        try
        {
            writer.setFlipY(false);
            writer.write(stream, pixmap);
            return stream.toByteArray();
        }
        catch (Exception e)
        {
            return emptyBytes;
        }
        finally
        {
            writer.dispose();
        }
    }

    private static byte[] parseImage(BufferedImage image)
    {
        var stream = new ByteArrayOutputStream();

        try
        {
            ImageIO.write(image, "png", stream);
            return stream.toByteArray();
        }
        catch (Exception e)
        {
            return emptyBytes;
        }
    }
}
