package com.toa.gear;

import java.awt.Dimension;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import net.runelite.client.game.ItemManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GearPanelTest
{
    @Test
    public void centresArtworkWithUnequalTransparentMargins()
    {
        BufferedImage icon = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D source = icon.createGraphics();
        source.setColor(Color.WHITE);
        source.fillRect(1, 2, 8, 20);
        source.dispose();
        assertEquals(new Rectangle(1, 2, 8, 20), GearPanel.visibleBounds(icon));
        BufferedImage result = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D target = result.createGraphics();
        new GearPanel(mock(ItemManager.class)).drawSprite(target, new Rectangle(0, 0, 40, 40), icon);
        target.dispose();
        assertEquals(new Rectangle(16, 10, 8, 20), GearPanel.visibleBounds(result));
    }

    @Test
    public void blankAsyncSpriteIsRetriedOnceItLoads()
    {
        GearPanel panel = new GearPanel(mock(ItemManager.class));
        BufferedImage icon = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
        BufferedImage result = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D target = result.createGraphics();
        panel.drawSprite(target, new Rectangle(0, 0, 40, 40), icon);
        assertTrue(GearPanel.visibleBounds(result).isEmpty());
        icon.setRGB(2, 28, Color.WHITE.getRGB());
        panel.drawSprite(target, new Rectangle(0, 0, 40, 40), icon);
        target.dispose();
        Rectangle visible = GearPanel.visibleBounds(result);
        assertEquals(1, visible.width);
        assertEquals(1, visible.height);
        assertTrue(Math.abs(visible.getCenterX() - 20) <= 0.5);
        assertTrue(Math.abs(visible.getCenterY() - 20) <= 0.5);
    }

    @Test
    public void fullSizeAndOversizedSpritesStayPaddedInsideTheCell()
    {
        Rectangle cell = new Rectangle(12, 41, GearPanel.CELL_SIZE, GearPanel.CELL_SIZE);
        Rectangle normal = GearPanel.spriteBounds(cell, 36, 32);
        assertEquals(36, normal.width);
        assertEquals(32, normal.height);
        for (Rectangle sprite : new Rectangle[]{normal, GearPanel.spriteBounds(cell, 72, 64),
            GearPanel.spriteBounds(cell, 12, 96)})
        {
            assertTrue(sprite.x >= cell.x + 2);
            assertTrue(sprite.y >= cell.y + 2);
            assertTrue(sprite.x + sprite.width <= cell.x + cell.width - 2);
            assertTrue(sprite.y + sprite.height <= cell.y + cell.height - 2);
        }
    }

    @Test
    public void popupFitsFixedResizableAndBottomRightCanvasEdges()
    {
        for (Dimension canvas : new Dimension[]{new Dimension(765, 503), new Dimension(503, 335),
            new Dimension(1920, 1080)})
        {
            for (Rectangle anchor : new Rectangle[]{new Rectangle(110, 145, 26, 22),
                new Rectangle(canvas.width - 40, canvas.height - 24, 26, 22)})
            {
                Rectangle popup = GearPanel.place(anchor, canvas);
                assertTrue(popup.x >= 0 && popup.y >= 0);
                assertTrue(popup.x + popup.width <= canvas.width);
                assertTrue(popup.y + popup.height <= canvas.height);
            }
        }
    }

    @Test
    public void mouseCanCrossFourPixelGapIntoEitherSideOfPopup()
    {
        Rectangle anchor = new Rectangle(110, 145, 26, 22);
        assertEquals(new Rectangle(136, 145, 4, 22),
            ApplicantGearOverlay.bridge(anchor, new Rectangle(140, 145, 360, 282)));
        anchor = new Rectangle(600, 145, 26, 22);
        assertEquals(new Rectangle(596, 145, 4, 22),
            ApplicantGearOverlay.bridge(anchor, new Rectangle(236, 145, 360, 282)));
    }
}
