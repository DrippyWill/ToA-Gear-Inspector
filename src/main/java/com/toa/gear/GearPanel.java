package com.toa.gear;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.WeakHashMap;
import javax.inject.Inject;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;

/** Passive equipment card. All coordinates are game-canvas coordinates (also in stretched mode). */
final class GearPanel
{
    static final int WIDTH = 360;
    static final int HEIGHT = 280;
    static final int CELL_SIZE = 40;
    static final int CELL_PITCH = 42;
    static final Color GOLD = new Color(255, 198, 88);
    static final Color MUTED = new Color(174, 166, 146);
    private final ItemManager itemManager;
    private final Map<BufferedImage, Rectangle> spriteContentBounds = new WeakHashMap<>();

    @Inject
    GearPanel(ItemManager itemManager)
    {
        this.itemManager = itemManager;
    }

    static Rectangle place(Rectangle anchor, Dimension canvas)
    {
        int x = anchor.x + anchor.width + 4;
        if (x + WIDTH > canvas.width - 4)
        {
            x = anchor.x - WIDTH - 4;
        }
        x = Math.max(4, Math.min(x, canvas.width - WIDTH - 4));
        int y = Math.max(4, Math.min(anchor.y, canvas.height - HEIGHT - 4));
        return new Rectangle(x, y, WIDTH, HEIGHT);
    }

    void render(Graphics2D graphics, Rectangle bounds, ApplicantRow applicant,
        java.awt.Point mouse)
    {
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.translate(bounds.x, bounds.y);
            g.setColor(new Color(10, 9, 6, 100));
            g.fillRect(3, 3, WIDTH, HEIGHT);
            g.setColor(new Color(38, 34, 26, 250));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setColor(new Color(135, 111, 67));
            g.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);
            g.setColor(new Color(77, 66, 45));
            g.drawRect(2, 2, WIDTH - 5, HEIGHT - 5);
            g.setFont(FontManager.getRunescapeBoldFont());
            text(g, applicant.name, 12, 23, GOLD, WIDTH - 24);
            g.setColor(new Color(91, 78, 53));
            g.drawLine(12, 32, WIDTH - 13, 32);

            if (!applicant.gear.available())
            {
                g.setFont(FontManager.getRunescapeFont());
                text(g, applicant.gear.unavailableReason, 14, 66, GOLD, WIDTH - 28);
                text(g, "Stand nearby in the same world to inspect.", 14, 94, MUTED, WIDTH - 28);
                return;
            }

            java.awt.Point localMouse = new java.awt.Point(mouse.x - bounds.x, mouse.y - bounds.y);
            String hovered = null;
            int line = 0;
            for (GearSlot slot : GearSlot.values())
            {
                int itemId = applicant.gear.items.getOrDefault(slot, -1);
                Rectangle cell = new Rectangle(12 + slot.column * CELL_PITCH,
                    41 + slot.row * CELL_PITCH, CELL_SIZE, CELL_SIZE);
                Rectangle row = new Rectangle(148, 41 + line * 23, WIDTH - 160, 23);
                String itemName = itemId < 0 ? "Empty" : itemManager.getItemComposition(itemId).getName();
                boolean hover = cell.contains(localMouse) || row.contains(localMouse);
                if (hover)
                {
                    hovered = slot.label + ": " + itemName;
                }
                g.setColor(hover ? new Color(88, 75, 48) : new Color(49, 43, 31));
                g.fillRect(cell.x, cell.y, cell.width, cell.height);
                g.setColor(hover ? GOLD : new Color(96, 82, 56));
                g.drawRect(cell.x, cell.y, cell.width - 1, cell.height - 1);
                if (itemId >= 0)
                {
                    BufferedImage icon = itemManager.getImage(itemId);
                    if (icon != null)
                    {
                        drawSprite(g, cell, icon);
                    }
                }
                else
                {
                    g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
                    FontMetrics fm = g.getFontMetrics();
                    String emptyLabel = slot.label;
                    g.setColor(MUTED);
                    g.drawString(emptyLabel, cell.x + (cell.width - fm.stringWidth(emptyLabel)) / 2, cell.y + 22);
                }
                g.setFont(FontManager.getRunescapeSmallFont());
                text(g, slot.label, row.x, row.y + 15, MUTED, 46);
                text(g, itemName, row.x + 46, row.y + 15,
                    hover ? GOLD : itemId < 0 ? MUTED : Color.WHITE, row.width - 46);
                line++;
            }
            if (hovered != null)
            {
                g.setFont(FontManager.getRunescapeSmallFont());
                text(g, hovered, 12, 270, GOLD, WIDTH - 24);
            }
        }
        finally
        {
            g.dispose();
        }
    }

    /** Centre the artwork rather than the sprite canvas, which may have unequal transparent margins. */
    void drawSprite(Graphics2D g, Rectangle cell, BufferedImage icon)
    {
        Rectangle source = spriteContentBounds.get(icon);
        if (source == null)
        {
            source = visibleBounds(icon);
            if (source.isEmpty())
            {
                // RuneLite returns blank AsyncBufferedImages before loading. Retry next frame.
                return;
            }
            spriteContentBounds.put(icon, source);
        }
        Rectangle target = spriteBounds(cell, source.width, source.height);
        g.drawImage(icon, target.x, target.y, target.x + target.width, target.y + target.height,
            source.x, source.y, source.x + source.width, source.y + source.height, null);
    }

    static Rectangle visibleBounds(BufferedImage icon)
    {
        int minX = icon.getWidth();
        int minY = icon.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < icon.getHeight(); y++)
        {
            for (int x = 0; x < icon.getWidth(); x++)
            {
                if ((icon.getRGB(x, y) >>> 24) != 0)
                {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return maxX < minX ? new Rectangle() : new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    /** Artwork keeps native size unless it exceeds the padded interior of the cell. */
    static Rectangle spriteBounds(Rectangle cell, int width, int height)
    {
        int available = Math.min(cell.width, cell.height) - 4;
        double scale = Math.min(1.0, Math.min((double) available / Math.max(1, width),
            (double) available / Math.max(1, height)));
        int drawWidth = Math.max(1, (int) Math.floor(width * scale));
        int drawHeight = Math.max(1, (int) Math.floor(height * scale));
        return new Rectangle(cell.x + (cell.width - drawWidth) / 2,
            cell.y + (cell.height - drawHeight) / 2, drawWidth, drawHeight);
    }

    static void text(Graphics2D g, String value, int x, int y, Color color, int maxWidth)
    {
        FontMetrics fm = g.getFontMetrics();
        String text = value == null ? "Unknown item" : value;
        if (fm.stringWidth(text) > maxWidth)
        {
            while (!text.isEmpty() && fm.stringWidth(text + "...") > maxWidth)
            {
                text = text.substring(0, text.length() - 1);
            }
            text += "...";
        }
        g.setColor(Color.BLACK);
        g.drawString(text, x + 1, y + 1);
        g.setColor(color);
        g.drawString(text, x, y);
    }
}
