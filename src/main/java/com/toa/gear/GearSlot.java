package com.toa.gear;

import net.runelite.api.kit.KitType;

/** Only nine equipped item slots are part of another player's public appearance. */
enum GearSlot
{
    HEAD("Head", KitType.HEAD, 1, 0),
    CAPE("Cape", KitType.CAPE, 0, 1),
    NECK("Neck", KitType.AMULET, 1, 1),
    WEAPON("Weapon", KitType.WEAPON, 0, 2),
    BODY("Body", KitType.TORSO, 1, 2),
    SHIELD("Shield", KitType.SHIELD, 2, 2),
    LEGS("Legs", KitType.LEGS, 1, 3),
    HANDS("Hands", KitType.HANDS, 0, 4),
    FEET("Feet", KitType.BOOTS, 1, 4);

    final String label;
    final KitType kit;
    final int column;
    final int row;

    GearSlot(String label, KitType kit, int column, int row)
    {
        this.label = label;
        this.kit = kit;
        this.column = column;
        this.row = row;
    }
}
