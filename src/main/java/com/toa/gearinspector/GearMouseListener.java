package com.toa.gearinspector;

import java.awt.event.MouseEvent;
import javax.inject.Inject;
import net.runelite.client.input.MouseAdapter;

/** Prevent clicks on the hover card from reaching obscured native Accept / Reject buttons. */
final class GearMouseListener extends MouseAdapter
{
    private final GearInspectorOverlay overlay;
    private boolean captured;

    @Inject
    GearMouseListener(GearInspectorOverlay overlay)
    {
        this.overlay = overlay;
    }

    @Override
    public MouseEvent mousePressed(MouseEvent event)
    {
        captured = overlay.blocks(event.getPoint());
        if (captured)
        {
            event.consume();
        }
        return event;
    }

    @Override
    public MouseEvent mouseReleased(MouseEvent event)
    {
        if (captured || overlay.blocks(event.getPoint()))
        {
            event.consume();
        }
        captured = false;
        return event;
    }

    @Override
    public MouseEvent mouseClicked(MouseEvent event)
    {
        if (overlay.blocks(event.getPoint()))
        {
            event.consume();
        }
        return event;
    }
}
