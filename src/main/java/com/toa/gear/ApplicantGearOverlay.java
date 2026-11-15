package com.toa.gear;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Singleton
final class ApplicantGearOverlay extends Overlay
{
    private final Client client;
    private final ToaApplicantGearConfig config;
    private final ApplicantInterface applicants;
    private final GearPanel panel;
    private String hoveredName;
    private volatile Rectangle popupBounds;
    private Rectangle bridge;

    @Inject
    ApplicantGearOverlay(Client client, ToaApplicantGearConfig config,
        ApplicantInterface applicants, GearPanel panel)
    {
        this.client = client;
        this.config = config;
        this.applicants = applicants;
        this.panel = panel;
        setPosition(OverlayPosition.DYNAMIC);
        // Widget callbacks are conditional on native interface drawing. The final buffer pass
        // runs each rendered frame, after native redraws, in both software and GPU clients.
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        Rectangle header = applicants.header();
        if (header == null || client.isMenuOpen())
        {
            clearHover();
            return null;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setColor(GearPanel.GOLD);
            int hx = header.x + header.width / 2;
            int hy = header.y + header.height / 2;
            g.drawPolygon(new int[]{hx - 5, hx + 5, hx + 5, hx, hx - 5},
                new int[]{hy - 5, hy - 5, hy + 2, hy + 6, hy + 2}, 5);
            g.drawLine(hx, hy - 3, hx, hy + 3);
            net.runelite.api.Point point = client.getMouseCanvasPosition();
            java.awt.Point mouse = new java.awt.Point(point.getX(), point.getY());
            ApplicantRow selected = null;
            // Keep the card open while hovering its items; discard it if that applicant disappears.
            if (hoveredName != null && ((popupBounds != null && popupBounds.contains(mouse))
                || (bridge != null && bridge.contains(mouse))))
            {
                for (ApplicantRow row : applicants.rows())
                {
                    if (Names.key(row.name).equals(hoveredName))
                    {
                        selected = row;
                        break;
                    }
                }
            }
            for (ApplicantRow row : applicants.rows())
            {
                boolean rowHovered = row.gearBounds.contains(mouse)
                    || (config.hoverName() && row.nameBounds.contains(mouse));
                // The existing popup is on top of underlying applicant rows.
                if (selected == null && rowHovered)
                {
                    selected = row;
                }
                Rectangle b = row.gearBounds;
                g.setColor(rowHovered ? new Color(119, 94, 48, 170) : new Color(0, 0, 0, 65));
                g.fillRect(b.x + 2, b.y + 2, Math.max(0, b.width - 4), Math.max(0, b.height - 4));
                int x = b.x + b.width / 2;
                int y = b.y + b.height / 2;
                Graphics2D icon = (Graphics2D) g.create();
                try
                {
                    icon.clip(b);
                    icon.setColor(row.gear.available() ? new Color(169, 207, 128) : GearPanel.MUTED);
                    icon.drawOval(x - 6, y - 6, 8, 8);
                    icon.drawLine(x + 1, y + 1, x + 6, y + 6);
                }
                finally
                {
                    icon.dispose();
                }
            }
            if (selected == null)
            {
                clearHover();
                return null;
            }
            hoveredName = Names.key(selected.name);
            Rectangle bounds = GearPanel.place(selected.gearBounds,
                new Dimension(client.getCanvasWidth(), client.getCanvasHeight()));
            popupBounds = bounds;
            bridge = bridge(selected.gearBounds, bounds);
            panel.render(g, bounds, selected, mouse);
            return null;
        }
        finally
        {
            g.dispose();
        }
    }

    static Rectangle bridge(Rectangle anchor, Rectangle popup)
    {
        int left = Math.min(anchor.x + anchor.width, popup.x + popup.width);
        int right = Math.max(anchor.x, popup.x);
        int top = Math.max(anchor.y, popup.y);
        int bottom = Math.min(anchor.y + anchor.height, popup.y + popup.height);
        return new Rectangle(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
    }

    boolean blocks(java.awt.Point point)
    {
        Rectangle bounds = popupBounds;
        return bounds != null && bounds.contains(point);
    }

    void clearHover()
    {
        hoveredName = null;
        popupBounds = null;
        bridge = null;
    }
}
