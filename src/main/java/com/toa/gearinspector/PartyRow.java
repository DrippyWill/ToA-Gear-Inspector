package com.toa.gearinspector;

import java.awt.Rectangle;

final class PartyRow
{
    final String name;
    final Rectangle nameBounds;
    final Rectangle gearBounds;
    final GearSnapshot gear;

    PartyRow(String name, Rectangle nameBounds, Rectangle gearBounds, GearSnapshot gear)
    {
        this.name = name;
        this.nameBounds = new Rectangle(nameBounds);
        this.gearBounds = new Rectangle(gearBounds);
        this.gear = gear;
    }
}
